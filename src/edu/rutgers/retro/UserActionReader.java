package edu.rutgers.retro;

import java.io.*;
import java.util.*;

/** An auxiliary class used to read the index files created by 
     UserActionSaver */
public class  UserActionReader extends UserActionSaver {

    private void readIndexFile( File historyIndexFile ) throws IOException {
	ObjectRandomAccessFile userHistoryIndexRAF = new ObjectRandomAccessFile(historyIndexFile,"r", Integer.SIZE/8);
	users = new UserEntry[userNameTable.size()];
	if (users.length != userHistoryIndexRAF.lengthObject()) throw new IllegalArgumentException("File size mismatch for " + userHistoryIndexRAF + "; should be " + users.length + "*" + userHistoryIndexRAF.sizeof);
	int offset = userHistoryIndexRAF.readInt();
	for(int i=0; i< users.length; i++) {
	    int offsetNext =  (i<users.length-1)?
		userHistoryIndexRAF.readInt() : 
		(int)userHistoryRAF.lengthObject();
	    users[i] = new UserEntry(offsetNext-offset, offset, false);
	    offset = offsetNext;
	}
	userHistoryIndexRAF.close();
    }

    void report() throws IOException {
 	for(int i=0; i< users.length; i++) {
	    System.out.println("User["+i+"] has " + users[i].total + " actions");
	    /*
	    userHistoryRAF.seekObject(users[i].offset0);
	    for(int k=0; k< users[i].total; k++) {
		int actionID = userHistoryRAF.readInt();
		ActionDetails a = actionRAF.read(new ActionDetails(), actionID);
		System.out.println("a["+actionID+"]=" + a);
	    }
	    */
	    for(Iterator<ActionDetails> it = actionsForUserIt(i); it.hasNext();) {
		ActionDetails a = it.next();
		System.out.println(a);
	    }
	}
    }


    /** Reads the first n actions for the specified user */
    private ActionDetails[] someActionsForUser(int uid, final int n)  throws IOException {
	ActionDetails[] as = new ActionDetails[n];
	userHistoryRAF.seekObject(users[uid].offset0);
	for(int k=0; k< as.length; k++) {
	    int actionID = userHistoryRAF.readInt();
	    as[k] = actionRAF.read(new ActionDetails(), actionID);
	}
	return as;	
    }

    /** For a specified user, reads the actions that are within the
	first n actions of this user, and whose timestamp is
	&ge; startSec. 	Since it is not known in advance how many
	actions will satisfy this condition, this method does
	reading in reverse order, but returns a properly chronologically
	ordered array.
     */
    private ActionDetails[] someRecentActionsForUser(int uid, final int n, int startUtc)  throws IOException {
	ActionDetails[] as = new ActionDetails[n]; 
	int cnt = 0;
	long offset1 = users[uid].offset0 + n;
	for(int k=n-1; k>=0; k--) {
	    userHistoryRAF.seekObject(users[uid].offset0 + k);
	    int actionID = userHistoryRAF.readInt();
	    as[k] = actionRAF.read(new ActionDetails(), actionID);
	    if (as[k].utc < startUtc) break;
	    cnt++;
	}	
	return (cnt==n)? as: Arrays.copyOfRange(as, n-cnt, n);
    }


    /** Reads all actions for the specified user */
    ActionDetails[] actionsForUser(int uid)  throws IOException {
	return someActionsForUser(uid, users[uid].total);
    }

    /** The first readCnt actions for the specified user */
    ActionDetails[] earlyActionsForUser(int uid)  throws IOException {
	return someActionsForUser(uid, users[uid].readCnt);
    }

    /** For a specified user, reads the actions that are within the
	first readCnt actions of this user, and whose timestamp is
	&ge; startSec.
    */
    ActionDetails[] recentActionsForUser(int uid, int startUtc)  throws IOException {
	return someRecentActionsForUser(uid, users[uid].readCnt, startUtc);
    }

    /** Iterator for reading the stored list of actions for a given user */
    Iterator<ActionDetails> actionsForUserIt(final int uid) {
	return new Iterator<ActionDetails>() {
	    int nextPtr = 0;
	    public boolean	hasNext() { return nextPtr < users[uid].total; }
	    public ActionDetails next() throws NoSuchElementException {
		try {
		    userHistoryRAF.seekObject(users[uid].offset0 + nextPtr);
		    int actionID = userHistoryRAF.readInt();
		    nextPtr++;
		    return actionRAF.read(new ActionDetails(), actionID);	
		} catch (IOException ex) {
		    throw new  NoSuchElementException("IOException happened when reading action data");
		}
	    }
	    public void remove() {
		throw new UnsupportedOperationException();
	    }
	};
    }

    /**
       @param indexDir The directory where the index files are
     */
    UserActionReader(File indexDir)  throws IOException{
	super( new NameTable(new File(indexDir, "users.dat")),
	       new NameTable(new File(indexDir, "aid.dat")));

	openFiles(indexDir, "r");
	File historyIndexFile = new File(indexDir, "userHistoryIndex.dat");
	readIndexFile(historyIndexFile);
    }

    /** Used in incremental coacces computation, at the beginning of a new run */
    void reset() {
	
    }

}
