package edu.rutgers.retro;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.regex.*;
import java.text.*;

//import javax.persistence.*;

import org.json.*;
//import javax.json.*;


/** Collecting statistics about users from arxiv.org usage logs */
public class UserStats {
  
    final ArxivUserInferrer inferrer;
    UserStats(ArxivUserInferrer _inferrer) {
	inferrer = _inferrer;
    }

    static class UserInfo implements Comparable<UserInfo> {
	final String uid;
	String userAgent;
	int utc0,utc1;
	int cnt;
	boolean userAgentsVary=false;
	UserInfo(String _uid, int utc, String _userAgent) {
	    uid = _uid;
	    utc0 = utc1 = utc;
	    cnt = 1;
	    userAgent = _userAgent;
	}
	void add(int utc, String _userAgent) {
	    cnt ++;
	    if (utc<utc0) utc0=utc;
	    if (utc>utc1) utc1=utc;
	    if (_userAgent!=null && !_userAgent.equals(userAgent)) userAgentsVary=true;
	}
	/** This is used for the reverse sorting of UserInfo objects by count (i.e. descening sort) */
	public int compareTo(UserInfo x) {
	    return -(cnt - x.cnt);
	}
    }

    HashMap<String, UserInfo> allUsers = new HashMap<String, UserInfo>();

    int unexpectedActionCnt = 0;		
  	
    void addFromJsonFile(File f) throws IOException, JSONException {

	JSONObject jsoOuter = Json.readJsonFile(f);
	JSONArray jsa = jsoOuter.getJSONArray("entries");
	int len = jsa.length();
	System.out.println("Length of the JSON data array = " + len);

	System.out.println("Json data file action entry count = " + len);

	int cnt=0, ignorableActionCnt=0, invalidAidCnt = 0, unexpectedActionCnt=0, botCnt=0;
	for(int i=0; i< len; i++) {
	    JSONObject jso = jsa.getJSONObject(i);
	    String type =  jso.getString( "type");

	    //	    String arxiv_id=jso.getString( "arxiv_id",null);
	    if (Json.typeIsAcceptable(type)) {
		if (!jso.has("arxiv_id"))  throw new IllegalArgumentException("No arxiv_id field in entry: " + jso);
	    } else {
		ignorableActionCnt++;
		if (jso.has("arxiv_id"))    unexpectedActionCnt++;
		continue;		
	    } 


	    String ip_hash = jso.getString("ip_hash");
	    String arxiv_id=jso.getString( "arxiv_id");
 	    String aid = Json.canonicAid(arxiv_id);
	    String cookie = jso.getString("cookie_hash");
	    if (cookie==null) cookie = jso.getString("cookie");
	    if (cookie==null) cookie = "";
	    // Older logs have some entries w/o user_agent, but these are
	    // extremely few (16 out of 500,000 in one sample)
	    String user_agent = jso.has("user_agent") ? 
		jso.getString("user_agent") : "unknown";
	    if (skipBots && isKnownBot(user_agent)) {
		botCnt++;
		continue;
	    }

	    int utc = jso.getInt("utc");

	    cnt ++;

	     String uid= inferrer.inferUser(ip_hash,  cookie);
	     UserInfo u = allUsers.get(uid);
	     if (u==null) allUsers.put(uid, u=new UserInfo(uid, utc, user_agent));
	     else u.add(utc, user_agent);
	     
	     //	    saver.save( ip_hash, cookie, aid);
	}
	
	System.out.println("Analyzable action entries count = " + cnt);
	System.out.println("Ignorable  action entries count = " + ignorableActionCnt);
	if (botCnt>0) 	System.out.println("Skipped known bot entries count = " + botCnt);

	if (unexpectedActionCnt>0) {
	    System.out.println("There were also " + unexpectedActionCnt + " entries with an arxiv_id field, but with an unacceptable action type");
	}

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
		      (u.userAgentsVary? 1:0) +"," + u.utc0 +","+u.utc1);
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

    static final String[] botMid = {
	"webarchive.nlc.gov.cn",
	"ZumBot",
	"YandexBot",
	"naver.me/bot",
	"Spider",
	"spider",
	"archive.org_bot",
	"BLEXBot",
    };
    static final String[] botStart = {
	"Sogou web spider"
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
	final boolean useCookies=true;

	String usageFrom=getDateStringOption(ht,"usageFrom");
	String usageTo  =getDateStringOption(ht,"usageTo");


	if (argv.length < 1) {
	    usage("Command not specified");
	} else if (argv[0].equals("users")) {
	    
	    ArxivUserInferrer inferrer = useCookies?
		new CookieArxivUserInferrer(new ArxivUserTable(tcPath)):
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

	    us.save(new File("users.csv"));

	}  else {
	    usage();
	}
    }





}
