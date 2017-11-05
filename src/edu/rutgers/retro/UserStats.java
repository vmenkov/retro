package edu.rutgers.retro;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.regex.*;
import java.text.*;

import org.json.*;


/** Collecting statistics about users from arxiv.org usage logs */
public class UserStats {
  
    final ArxivUserInferrer inferrer;
    UserStats(ArxivUserInferrer _inferrer) {
	inferrer = _inferrer;
    }

    /** This is used to track how many articles the user has viewed
	within a recent time window. Each HistoryWindow object is used
	to monitor the user's activity against one activity criterion
	(max number of articles views allowed over a certain period of time).
     */
    static class HistoryWindow extends LinkedList<Integer> {
	/** The criterion is: no more than maxCnt viewes within windowSize sec */
	final int windowSize, maxCnt;
	/** How many entries have been accepted before the user has
	    been rejected by this window's criterion. (This counter is
	    incremented until the criterion's activity threshold is
	    exceeded) */
	int acceptCnt=0;
	boolean rejected=false;
	HistoryWindow(int _windowSize, int _maxCnt) {
	    windowSize =  _windowSize;
	    maxCnt =  _maxCnt;	    
	}
	/** Adds the most recent events into the list, and removes
	    "expired" old ones, if any */
	synchronized boolean accept(int utc) {
	    if (rejected) return false;
	    Integer first = null;
	    while((first = peekFirst())!=null && 
		  first.intValue()<=utc-windowSize) {
		removeFirst();
	    }
	    if (size()>=maxCnt) {
		rejected = true;
		clear(); // to allow the list to be GC-ed
		return false;
	    }
	    add(utc);
	    acceptCnt++;
	    return true;	    
	}
    }

    static class UserInfo implements Comparable<UserInfo> {
	final String uid;
	String userAgent;
	int utc0,utc1;
	int cnt=0, acceptCnt=0;
	boolean userAgentsVary=false;
	/** We set that flag to true if we decide that this user is too
	    robot-like, and his activity should not be taken into account 
	    anymore */
	boolean excludeFromNowOn=false;
	/** Criteria for excluding overly active users */
	final static int[] windowSizes = {300, 24*3600};
	final static int[] maxCntWindow = {20, 60};
	HistoryWindow historyWindow[] = new HistoryWindow[windowSizes.length];

	UserInfo(String _uid, int utc, String _userAgent) {
	    uid = _uid;
	    utc0 = utc1 = utc;
	    userAgent =	_userAgent;
	    for(int i=0; i<windowSizes.length; i++) {
		historyWindow[i] = new HistoryWindow(windowSizes[i], maxCntWindow[i]);
	    }
	    windowCntTest(utc);
	}
	void add(int utc, String _userAgent) {
	    if (utc<utc0) utc0=utc;
	    if (utc>utc1) utc1=utc;
	    if (_userAgent!=null && !_userAgent.equals(userAgent)) userAgentsVary=true;
	    windowCntTest(utc);
	}
	/** Increments counters, and checks if the user seems to be
	    too active (based on any of several criteria), and if he
	    is, sets the excludeFromNowOn flag. A note is made on
	    which criteria (and when) triggers the exclusion. */
	private void windowCntTest(int utc) {
	    cnt ++;
	    for(HistoryWindow hw: historyWindow) {
		if (!hw.rejected && !hw.accept(utc) && !excludeFromNowOn) {
		    excludeFromNowOn = true;
		}
	    }
	    if (!excludeFromNowOn) acceptCnt=cnt;
	}
	/** This is used for the reverse sorting of UserInfo objects by count (i.e. descening sort) */
	public int compareTo(UserInfo x) {
	    return -(cnt - x.cnt);
	}
    }

    HashMap<String, UserInfo> allUsers = new HashMap<String, UserInfo>();
    NameTable userNameTable = new NameTable();
    HashSet<String> allAidsSet = new HashSet<String>();

    int unexpectedActionCnt = 0;		

    void addFromJsonFile(File f) throws IOException, JSONException {

	JSONObject jsoOuter = Json.readJsonFile(f);
	JSONArray jsa = jsoOuter.getJSONArray("entries");
	int len = jsa.length();
	System.out.println("Json data file action entry count = " + len);

	int cnt=0, ignorableActionCnt=0, invalidAidCnt = 0, unexpectedActionCnt=0, botCnt=0, ignorableUserCnt=0, usedCnt = 0;
	for(int i=0; i< len; i++) {
	    JSONObject jso = jsa.getJSONObject(i);
	    ActionLine z = new ActionLine(jso);

	    if (z.ignorableAction) {
		ignorableActionCnt++;
		if (z.unexpectedAction) unexpectedActionCnt++;
		continue;		
	    } 
	    
	    if (z.isBot) {
		botCnt++;
		continue;
	    }

	    cnt ++;

	    String uid= inferrer.inferUser(z.ip_hash,  z.cookie);
	    if (uid==null) {
		ignorableUserCnt++;
		continue;
	    }
	    UserInfo u = allUsers.get(uid);
	    if (u==null) allUsers.put(uid, u=new UserInfo(uid,z.utc,z.user_agent));
	    else u.add(z.utc, z.user_agent);

	    allAidsSet.add(z.aid);
	    userNameTable.addIfNew(uid);
	    usedCnt++;
	}
	
	System.out.println("Analyzable action entries count = " + cnt);
	System.out.println("Ignorable  action entries count = " + ignorableActionCnt);
	if (unexpectedActionCnt>0) {
	    System.out.println("There were also " + unexpectedActionCnt + " entries with an arxiv_id field, but with an unacceptable action type");
	}

	if (botCnt>0) 	System.out.println("Skipped known bot entries count = " + botCnt);
	if (ignorableUserCnt>0) System.out.println("Ignored user entries count = " +ignorableUserCnt);

	System.out.println("Eventually used actions (including those from too-active users, some to be later discarded) = " + usedCnt);	

	int rejectedUserCnt=0;
	for(UserInfo ui: allUsers.values()) {
	    if (ui.excludeFromNowOn) rejectedUserCnt++;
	}

	System.out.println("" + allUsers.size() +  " users identified; of them, " +rejectedUserCnt  +  " found to be too active");


    }


    /** Writes info about all users, except single-article ones, to a CSV file */
    void save(File f) throws IOException {
	PrintWriter w = new PrintWriter(new FileWriter(f));
	UserInfo users[] = allUsers.values().toArray(new UserInfo[0]);
	Arrays.sort(users);
	for(int i=0; i<users.length; i++) {
	    UserInfo u = users[i];
	    w.print("" + u.cnt + ",\"" + u.uid+ "\",\""+ u.userAgent+"\"," + 
		      (u.userAgentsVary? 1:0) +"," + u.utc0 +","+u.utc1 +","+
		    u.excludeFromNowOn);
	    for(HistoryWindow hw: u.historyWindow) {
		w.print("," + hw.rejected +"," + hw.acceptCnt);
	    }
	    w.println();
	}
	w.close();
    }

    /** Creates the user action index, using the UserActionSaver class */
    void saveActions(NameTable aidNameTable, File[] jsonFiles)  throws IOException {
	// create user list aligned with userNameTable order
	//UserInfo users[] = new UserInfo[userNameTable.size()];
	//for(int i=0; i<users.length; i++) {
	//	    users[i] = allUsers.get( userNameTable.nameAt(i));
	//}

	UserActionSaver uas = new UserActionSaver(inferrer, userNameTable, aidNameTable, allUsers);
	//allUsers.values().toArray(new UserInfo[0]));

	allUsers.clear(); // enable GC
	outdir.mkdirs();
	uas.saveActions(jsonFiles, outdir);
   }

    static void usage() {
	usage(null);
    }

    static void usage(String m) {
	//	System.out.println("Usage: HistoryClustering [split filename|svd cat]");
	if (m!=null) {
	    System.out.println(m);
	}
	System.exit(1);
    }

    private static final Pattern datePattern6 = Pattern.compile("[0-9][0-9][0-9][0-9][0-9][0-9]");
 

    /** Looks up for the value for the specified key in the config
	table, and interprets it as a date (either YYMMDD or YYYYMMDD).

	@param name The key
	@return a string in the format "YYMMDD", or null if no value for the 
	key has been supplied in the table. We have the "reverse year 2000
	problem" here, meaning that dates before 2000 can't be represented :-)
     */
    private static String getDateStringOption(ParseConfig ht, String name) {
	String x= ht.getOption(name, null);
	if (x==null || x.equals("null")) return null;

	if (x.length()==6 && datePattern6.matcher(x).matches()) return x;
	if (x.length()==8 && x.startsWith("20")) {
	    String y = x.substring(2);
	    if (datePattern6.matcher(y).matches()) return y;	
	}

	usage("Option " + name + " must be in the format YYMMDD or YYYYMMDD");
	return null;
    }

    private static ParseConfig ht = null;
    static File outdir;

 
    public static void main(String [] argv) throws IOException, java.text.ParseException, JSONException {

	ht = new ParseConfig();
	ActionLine.skipBots = ht.getBoolean("skipBots", ActionLine.skipBots);

	final String tcPath = ht.getOption("tc", "/data/json/usage/tc.json.gz");

	String usageFrom=getDateStringOption(ht,"usageFrom");
	String usageTo  =getDateStringOption(ht,"usageTo");
	// are we using stats for anon users too?
	boolean anon=ht.getOption("anon", true);
	final boolean useCookies=true;

	String outPath = ht.getOption("out", "out");
	outdir = new File(outPath);
	if (argv.length < 1) {
	    usage("Command not specified");
	} else if (argv[0].equals("users") ||
		   argv[0].equals("userActions")) {
	    
	    ArxivUserInferrer inferrer = useCookies?
		new CookieArxivUserInferrer(new ArxivUserTable(tcPath), anon):
		new IPArxivUserInferrer();
	    UserStats us = new UserStats(inferrer);

	    // String fname = "../json/user_data_0/" + "100510_user_data.json";
	    //String fname = "../json/user_data/" + "110301_user_data.json";
	    String fname = argv[1];
	    File f = new File(fname);
	    if (!f.exists()) usage("File " + f + " does not exist");

	    System.out.println("Too active user exclusion criteria are as follows:");
	    for(int i=0; i<UserInfo.windowSizes.length; i++) {
		System.out.println("More than " + UserInfo.maxCntWindow[i] + " articles in " + UserInfo.windowSizes[i]  + " sec");
	    }


	    System.out.println("Will look for data files under " + f +
			       ", date range " + usageFrom + " to " + usageTo);
	

	    
	    ListFiles lister = new ListFiles(usageFrom, usageTo, f);
	    File[] files = lister.list();
	    System.out.println("Found " +files.length+ " data files to process in " + f);

	    for(File g: files) {
		System.out.println("Processing " + g);
		us.addFromJsonFile(g);
	    }

	    // Save list of users
	    outdir.mkdirs();
	    us.save(new File(outdir, "users.csv"));
	    us.userNameTable.save(new File(outdir, "users.dat"));

	    // Save sorted list of article IDs
	    String[] allAids = (String[])us.allAidsSet.toArray(new String[0]);
	    Arrays.sort(allAids);
	    NameTable aidNameTable = new NameTable(allAids);
	    aidNameTable.save(new File(outdir, "aid.dat"));

	    if (argv[0].equals("userActions")) {
		System.out.println("Now, saving user actions...");
		us.saveActions(aidNameTable, files);
	    }
 
	} else if (argv[0].equals("readActions")) {
	    //	    NameTable userNameTable = new NameTable(new File(outdir, "users.dat"));
	    //	    NameTable aidNameTable = new NameTable(new File(outdir, "aid.dat"));
	    UserActionReader uar = new UserActionReader(
							//userNameTable, aidNameTable, 
							outdir);
	    uar.report();
	    uar.closeFiles();

	}  else {
	    usage();
	}
    }

}
