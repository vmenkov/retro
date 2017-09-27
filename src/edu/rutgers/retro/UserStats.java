package edu.rutgers.retro;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.regex.*;
import java.text.*;

import org.json.*;
//import javax.json.*;


/** Collecting statistics about users from arxiv.org usage logs */
public class UserStats {
  
    final ArxivUserInferrer inferrer;
    UserStats(ArxivUserInferrer _inferrer) {
	inferrer = _inferrer;
    }

    /** This is used to track how many articles the user has viewed within
	a recent time window
     */
    static class HistoryWindow extends LinkedList<Integer> {
	final int windowSize, maxCnt;
	HistoryWindow(int _windowSize, int _maxCnt) {
	    windowSize =  _windowSize;
	    maxCnt =  _maxCnt;	    
	}
	/** Adds the most recent events into the list, and removes
	    "expired" old ones, if any */
	boolean accept(int utc) {
	    Integer first = null;
	    while((first = peekFirst())!=null && 
		  first.intValue()<=utc-windowSize) {
		removeFirst();
	    }
	    if (size()>=maxCnt) return false;
	    add(utc);
	    return true;	    
	}
    }

    static class UserInfo implements Comparable<UserInfo> {
	final String uid;
	String userAgent;
	int utc0,utc1;
	int cnt;
	boolean userAgentsVary=false;
	/** We set that flag to true if we decide that this user is too
	    robot-like, and his activity should not be taken into account 
	    anymore */
	boolean excludeFromNowOn=false;
	final static int[] windowSizes = {300, 24*3600};
	final static int[] maxCntWindow = {20, 100};
	//boolean[] exceededWindow = new boolean[windowSizes.length];
	HistoryWindow historyWindow[] = new HistoryWindow[windowSizes.length];

	UserInfo(String _uid, int utc, String _userAgent) {
	    uid = _uid;
	    utc0 = utc1 = utc;
	    cnt = 1;
	    userAgent =	_userAgent;
	    for(int i=0; i<windowSizes.length; i++) {
		historyWindow[i] = new HistoryWindow(windowSizes[i], maxCntWindow[i]);
	    }
	}
	void add(int utc, String _userAgent) {
	    cnt ++;
	    if (utc<utc0) utc0=utc;
	    if (utc>utc1) utc1=utc;
	    if (_userAgent!=null && !_userAgent.equals(userAgent)) userAgentsVary=true;
	    if (excludeFromNowOn) return;
	    for(HistoryWindow hw: historyWindow) {
		if (!hw.accept(utc)) {
		    excludeFromNowOn = true;
		    historyWindow = null; // so that it can be GC-ed
		}
	    }
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

    /** Data from one entry of the JSON file, corresponding to 1 log entry
     */
    static class ActionLine {
	String type;
	String ip_hash;
	String arxiv_id;
	String aid;
	String cookie;
	String user_agent;
	int utc;
	boolean ignorableAction = false;
	boolean unexpectedAction = false;

	ActionLine(JSONObject jso) {
 	    type =  jso.getString( "type");

	    //	    String arxiv_id=jso.getString( "arxiv_id",null);
	    if (Json.typeIsAcceptable(type)) {
		if (!jso.has("arxiv_id"))  throw new IllegalArgumentException("No arxiv_id field in entry: " + jso);
	    } else {
		ignorableAction = true;
		if (jso.has("arxiv_id"))    unexpectedAction = true;
		return;		
	    } 
	    
	    ip_hash = jso.getString("ip_hash");
	    arxiv_id=jso.getString( "arxiv_id");
 	    aid = Json.canonicAid(arxiv_id);
	    cookie = jso.getString("cookie_hash");
	    if (cookie==null) cookie = jso.getString("cookie");
	    if (cookie==null) cookie = "";
	    // Older logs have some entries w/o user_agent, but these are
	    // extremely few (16 out of 500,000 in one sample)
	    user_agent = jso.has("user_agent") ? 
		jso.getString("user_agent").intern() : "unknown";
	    utc = jso.getInt("utc");

	}
   }
  	
    void addFromJsonFile(File f) throws IOException, JSONException {

	JSONObject jsoOuter = Json.readJsonFile(f);
	JSONArray jsa = jsoOuter.getJSONArray("entries");
	int len = jsa.length();
	System.out.println("Length of the JSON data array = " + len);

	System.out.println("Json data file action entry count = " + len);

	int cnt=0, ignorableActionCnt=0, invalidAidCnt = 0, unexpectedActionCnt=0, botCnt=0, ignorableUserCnt=0;
	for(int i=0; i< len; i++) {
	    JSONObject jso = jsa.getJSONObject(i);
	    ActionLine z = new ActionLine(jso);

	    if (z.ignorableAction) {
		ignorableActionCnt++;
		if (z.unexpectedAction) unexpectedActionCnt++;
		continue;		
	    } 

	    if (skipBots && isKnownBot(z.user_agent)) {
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
	    if (u==null) allUsers.put(uid, u=new UserInfo(uid, z.utc, z.user_agent));
	    else u.add(z.utc, z.user_agent);


	    allAidsSet.add(z.aid);
	    userNameTable.addIfNew(uid);

	     
	}
	
	System.out.println("Analyzable action entries count = " + cnt);
	System.out.println("Ignorable  action entries count = " + ignorableActionCnt);
	if (unexpectedActionCnt>0) {
	    System.out.println("There were also " + unexpectedActionCnt + " entries with an arxiv_id field, but with an unacceptable action type");
	}

	if (botCnt>0) 	System.out.println("Skipped known bot entries count = " + botCnt);
	if (ignorableUserCnt>0) System.out.println("Ignored user count = " +ignorableUserCnt);


	System.out.println("" + allUsers.size() +  " users identified");
    }


    /** Writes info about all users, except single-article ones, to a CSV file */
    void save(File f) throws IOException {
	PrintWriter w = new PrintWriter(new FileWriter(f));
	UserInfo users[] = allUsers.values().toArray(new UserInfo[0]);
	Arrays.sort(users);
	for(int i=0; i<users.length; i++) {
	    UserInfo u = users[i];
	    w.println("" + u.cnt + ",\"" + u.uid+ "\",\""+ u.userAgent+"\"," + 
		      (u.userAgentsVary? 1:0) +"," + u.utc0 +","+u.utc1 +","+
		      u.excludeFromNowOn 
		      );
	}
	w.close();
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

    /** Substrings (of the user_agent header of the HTTP request) used
	to identify some bots.
     */
    static final String[] botMid = {
	"webarchive.nlc.gov.cn",
	"ZumBot",
	"YandexBot",
	"naver.me/bot",
	"Spider",
	"spider",
	"webcrawler",
	"crawler",
	"archive.org_bot",
	"BLEXBot",
	"BrokenLinkCheck.com",
	"http://fess.codelibs.org/bot.html"
    };
    static final String[] botStart = {
	"Sogou web spider",
	"AndroidDownloadManager",
	"ShortLinkTranslate",
	"WikiDo",
    };
    

    /** Checks if the userAgent identifies a known bot */
    static boolean isKnownBot(String userAgent) {
	for(String x: botStart) {
	    if (userAgent.startsWith(x)) return true;
	}
  	for(String x: botMid) {
	    if (userAgent.indexOf(x)>=0) return true;
	}
	return false;
    }

    private static ParseConfig ht = null;
    private static boolean skipBots=true;

    public static void main(String [] argv) throws IOException, java.text.ParseException, JSONException {

	ht = new ParseConfig();
	skipBots = ht.getBoolean("skipBots", skipBots);

	final String tcPath = ht.getOption("tc", "/data/json/usage/tc.json.gz");

	String usageFrom=getDateStringOption(ht,"usageFrom");
	String usageTo  =getDateStringOption(ht,"usageTo");
	// are we using stats for anon users too?
	boolean anon=ht.getOption("anon", true);
	final boolean useCookies=true;


	if (argv.length < 1) {
	    usage("Command not specified");
	} else if (argv[0].equals("users")) {
	    
	    ArxivUserInferrer inferrer = useCookies?
		new CookieArxivUserInferrer(new ArxivUserTable(tcPath), anon):
		new IPArxivUserInferrer();
	    UserStats us = new UserStats(inferrer);

	    // String fname = "../json/user_data_0/" + "100510_user_data.json";
	    //String fname = "../json/user_data/" + "110301_user_data.json";
	    String fname = argv[1];
	    File f = new File(fname);
	    if (!f.exists()) usage("File " + f + " does not exist");
	    //	    System.out.println("");

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
	    us.save(new File("users.csv"));
	    us.userNameTable.save(new File("users.dat"));

	    // Save sorted list of article IDs
	    String[] allAids = (String[])us.allAidsSet.toArray(new String[0]);
	    Arrays.sort(allAids);
	    NameTable aidNameTable = new NameTable(allAids);
	    aidNameTable.save(new File("aid.dat"));

	}  else {
	    usage();
	}
    }

}
