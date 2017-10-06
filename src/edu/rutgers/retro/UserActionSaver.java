package edu.rutgers.retro;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.regex.*;
import java.text.*;

import org.json.*;

/** An auxiliary class used to save the information of users' action history
    to a random-access file */
class UserActionSaver {	

    int dupCnt;
    /** An UserEntry object deals with one user's actions */
    class UserEntry {

	/** how many actions will be saved, in total, for this user. This value
	    may be slightly adjusted, if and when duplicates are discovered. */
	int total;
	final boolean willReject;
	/** The beginning of this user's data in the file. The units are action
	    records, rather than bytes */
	int offset0;
	/** How many actions for this user have been already read from the JSON file... and saved to the binary file. The two numbers are the same (readCnt eventually becoming equal to this.total), unless there are duplicates */
	int readCnt=0, savedCnt=0;
	UserEntry(int _total, int _offset, boolean _willReject) {
	    total = _total;
	    offset0 = _offset;
	    willReject = _willReject;
	}
	/** Records one more action for this user, unless it's a duplicate.
	    @return true if the action was recorded; false otherwise (i.e. when the action involved a page already seen by this user)
	 */
	boolean processAction(ActionDetails act) throws IOException {
	    readCnt++;
	    // Should we just ignore this action as a duplicate?
	    if (myPages==null) readMyPages( );
	    if (myPages.contains(act.aid)) {
		dupCnt ++;
		return false;
	    }  else myPages.add(act.aid);

	    // Record action details in the all-actions list
	    long len = actionRAF.length();
	    actionRAF.seek(len);	    
	    int actionID = (int)(len/act.sizeof());
	    actionRAF.store(act);
	    // Add a pointer to this action to this user's history
	    userHistoryRAF.seek((offset0 + savedCnt)*(Integer.SIZE/8));
	    userHistoryRAF.writeInt(actionID);
	    savedCnt++;
	    return true;
	}

	/** List of articles covered in this user's history */
	HashSet<Integer> myPages = null;
	/** Reads the list of articles already covered for this user */
	void readMyPages(  ) throws IOException {	   
	    myPages = new HashSet<Integer>();
	    userHistoryRAF.seek(offset0 * (Integer.SIZE/8));
	    for(int i=0; i<savedCnt; i++) {
		int actionId = userHistoryRAF.readInt();
		int aid = actionRAF.read(new ActionDetails(), actionId).aid;
		myPages.add(aid);
	    }	    
	}

	/** Moves down this user's section of the user history file, 
	    to remove blank space.
	    @param newOffset Where the beginning of this user's section should be moved to 
	*/
	void compact(int newOffset) throws IOException  {
	    if (newOffset>offset0) throw new IllegalArgumentException("This is not compacting!");

	    int byteCnt = savedCnt * (Integer.SIZE/8);
	    byte[] buf = new byte[byteCnt];
	    userHistoryRAF.seek(offset0 * (Integer.SIZE/8));
	    userHistoryRAF.read(buf);
	    offset0 = newOffset;
	    userHistoryRAF.seek(offset0 * (Integer.SIZE/8));
	    userHistoryRAF.write(buf);
	    total = savedCnt;
	}

    }

    /** Mapped to data file */
    class ActionDetails implements Storable {
	int uid, aid, utc;
	ActionDetails() {}
	ActionDetails(int _uid, int _aid, int _utc) {
	    uid = _uid;
	    aid = _aid;
	    utc = _utc;
	}

	public int sizeof() { return 3*(Integer.SIZE/8); }
	//	byte[] toBytes() {	}
	public void write(RandomAccessFile f) throws IOException {
	    f.writeInt(uid);
	    f.writeInt(aid);
	    f.writeInt(utc);
	}
	public void readFrom(RandomAccessFile f) throws IOException {
	    uid = f.readInt();
	    aid = f.readInt();
	    utc = f.readInt();
	}

	public String toString() {
	    return "(user="+uid+", aid="+aid+",utc="+utc+")";
	}
  
    }

   UserActionSaver( NameTable _userNameTable, 
		    NameTable _aidNameTable) {     
	userNameTable =	_userNameTable;
	aidNameTable = _aidNameTable;
   }

    NameTable userNameTable, aidNameTable;
    UserEntry [] users;
    ArxivUserInferrer inferrer;

    /** @param ui User list aligned with userNameTable order
     */
    UserActionSaver(ArxivUserInferrer _inferrer, NameTable _userNameTable, 
		    NameTable _aidNameTable,
		    HashMap<String, UserStats.UserInfo> allUsers) {
	// UserStats.UserInfo[] ui) {
	this(_userNameTable, _aidNameTable);
	inferrer = _inferrer;
	if (userNameTable.size() !=  allUsers.size()) throw new IllegalArgumentException("Table size mismatch");
	users = new UserEntry[ allUsers.size()];
	int offset = 0;
	for(int i=0; i<users.length; i++) {
	    String uname = userNameTable.nameAt(i);
	    UserStats.UserInfo us = allUsers.get(uname);
	    if (!us.uid.equals(uname)) throw new IllegalArgumentException("User name mismatch");
	    users[i] = new UserEntry(us.acceptCnt, offset, us.excludeFromNowOn);
	    offset += users[i].total;

	    if (i<40) {
		System.out.println("User["+i+"]=" +us.uid +": " +  users[i].offset0 + " + "+users[i].total+"/"+us.cnt+" = " + offset);
	    }

	}	
	System.out.println("Predicted length of the (uncompacted) history file = " + offset);
    }

    void addFromJsonFile(File f) throws IOException, JSONException {
	System.out.println("Processing log file " + f);

	JSONObject jsoOuter = Json.readJsonFile(f);
	JSONArray jsa = jsoOuter.getJSONArray("entries");
	int len = jsa.length();
	System.out.println("Json data file action entry count = " + len);

	int actionCnt=0, recordedActionCnt=0;
	dupCnt=0;
	for(int i=0; i< len; i++) {
	    JSONObject jso = jsa.getJSONObject(i);
	    ActionLine z = new ActionLine(jso);

	    if (z.ignorableAction || z.isBot) {
		continue;
	    }

	    String uname= inferrer.inferUser(z.ip_hash,  z.cookie);
	    if (z.aid==null || uname==null) continue;
	    int uid =  userNameTable.get(uname);
	    UserEntry u = users[uid];
	    int aid = aidNameTable.get(z.aid); // internal article aid
	    ActionDetails act = new ActionDetails(uid, aid, z.utc);

	    if (u.readCnt == u.total) {
		if (u.willReject) continue; // as expected
		
		throw new IllegalArgumentException("For user["+uid+"]="+uname+"  (offset0="+u.offset0+"), the readCnt has exceeded the predicted value=" + u.total +", even though the user was never rejected");
	    }
	    actionCnt++;

	    boolean rv = u.processAction(act);
	    if (rv) recordedActionCnt++;
	}
	System.out.println("Found " + actionCnt + " acceptable actions in this file, recorded " + recordedActionCnt + ". Detected " + dupCnt + " duplicates");
	for(UserEntry u: users) {
	    u.myPages = null; // to enable GC
	}
	
    }

    /** Moves down each user's section of the user history file, 
	to remove all blank space that exist whenever savedCnt!=total
	@param newOffset Where the beginning of this user's section should be moved to 
    */
    void compact() throws IOException {
	int offset = 0;
	for(int i=0; i< users.length; i++) {
	    users[i].compact(offset);
	    offset += users[i].total;
	}
	userHistoryRAF.setLength( (long)offset * (Integer.SIZE/8));
    }

    /** Creates a file which stores offsets into the userHistory file for
	each user
     */
    void writeIndexFile( File historyIndexFile ) throws IOException {
	RandomAccessFile userHistoryIndexRAF = new  RandomAccessFile(historyIndexFile,"rw");
	for(int i=0; i< users.length; i++) {
	    userHistoryIndexRAF.writeInt(users[i].offset0);
	}
	userHistoryIndexRAF.close();
    }

    RAF<ActionDetails> actionRAF;
    RandomAccessFile userHistoryRAF;

    void openFiles(File outdir, String mode) throws IOException {
	File actionFile = new File(outdir, "actions.dat");
	actionRAF=new RAF<ActionDetails>(actionFile, "rw", new ActionDetails());

	File historyFile = new File(outdir, "userHistory.dat");
	userHistoryRAF=new RandomAccessFile(historyFile,"rw");

    }
    
    void closeFiles() throws IOException {
	actionRAF.close();
	userHistoryRAF.close();
    };

    void saveActions(File[] jsonFiles, File outdir) throws IOException {
	openFiles(outdir, "rw");

	for(File g: jsonFiles) {
	    System.out.println("Processing " + g);
	    addFromJsonFile(g);
	}
	System.out.println("Processed all actions; |index|="+userHistoryRAF.length()/(Integer.SIZE/8)+". Will do compacting now");
	compact();
	System.out.println("Done compacting; |index|="+userHistoryRAF.length()/(Integer.SIZE/8));

	closeFiles();

	File historyIndexFile = new File(outdir, "userHistoryIndex.dat");
	writeIndexFile(historyIndexFile );
    }
 

}
