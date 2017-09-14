package edu.rutgers.retro;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.regex.*;
import java.text.*;

//import javax.persistence.*;

//import org.json.*;
import javax.json.*;

//import edu.rutgers.axs.indexer.Common;
//import edu.rutgers.axs.sql.Categories;
//import edu.rutgers.axs.sql.DataFile;


/** Auxiliary methods for processing usage logs in Json format.
    
    <p>
    (This class is based on edu.rutgers.axs.ee4.Json)

    <p> The usage logs show arxiv.org's entire usage history for 10+
    years back, and are supplied by Paul Ginsparg's team via rsync.
    The files are in JSON format; the kind of entries we find there
    may be in one of the following two formats:

    <pre>
    Old format
      {
            "referrer": "http://arxiv.org/find", 
            "ip_hash": "30505f2428eb9b6dd2617307ced6d8b3", 
            "arxiv_id": "0609223", 
            "datetime": 1272844866, 
            "cookie": "293365f47a5597355e2d6a53360d6846", 
            "entry": 2, 
            "type": "abstract"
        },

	New format
   {
      "arxiv_id": "0902.0656", 
      "cookie_hash": "a1bab5ac50bfd9aa", 
      "entry": 3, 
      "ip_hash": "414a807fcda84084", 
      "type": "txt", 
      "user_agent": "Mozilla/5.0 (X11; U; Linux i686; en-GB; rv:1.9.0.10) Gecko/2009042523", "utc": 126230400
2
    }
    </pre>

    <p>There is also an auxiliary file that can be used to associate multiple
    cookies with a single user.

    */
public class Json {

    public static JsonObject readJsonFile(String fname) throws IOException, JsonException {
	return readJsonFile(new File(fname));
    }

    public static JsonObject readJsonFile(File f) throws IOException, JsonException {
	Reader fr = f.getName().endsWith(".gz") ?
	    new InputStreamReader(new GZIPInputStream(new FileInputStream(f))) :
	    new FileReader(f);
	//	JsonTokener tok = new JsonTokener(fr);
	//	JsonObject jsoOuter = new JsonObject(tok);

	JsonReader tok =  javax.json.Json.createReader(fr);
	JsonObject jsoOuter = tok.readObject();
	fr.close();
	return jsoOuter;
    }



    /** Do we process Json records with this particular action type?
     */
    static boolean typeIsAcceptable(String x) {
	final String types[] = {
	    "abstract", "download", "ftp_download",
	    //  as seen in /data/json/usage/2010/100101_usage.json.gz  :
	    "txt", "abs", "src"
	};
	for(String q: types) { 
	    if (x.equals(q)) return true;
	} 
	return false;
    }

    /** Is this action type one of the "download" types?
	@param x One of the action types considered "acceptable"
	by typeIsAcceptable()
	@return true if x is a "download" action type
     */
    static private boolean typeIsDownload(String x) {
	final String types[] = { "download", "ftp_download"};
	for(String q: types) { 
	    if (x.equals(q)) return true;
	} 
	return false;
    }

    /** Converts an ArXiv article ID to the standard format. (Removes
	the version-indicator part, which may be found in some log
	entries).
     */
    public static String canonicAid(String aid) {
	aid = aid.replaceAll("v(\\d+)$", "");
	//if (aid.endsWith("v")) return aid.substring(0, aid.length()-1);
	return aid;
    } 

    /** Reads a Json file, extracts relevant entries, and writes them
	into separate files (one per category).

	@param fname The input file name (complete path)
     */
    static void splitJsonFile(String fname) throws IOException, JsonException {

	JsonObject jsoOuter = Json.readJsonFile(fname);
	JsonArray jsa = jsoOuter.getJsonArray("entries");
	int len = jsa.size();
	System.out.println("Length of the Json data array = " + len);

	DataSaver saver = new DataSaver(fname);

	int cnt=0, ignorableActionCnt=0, invalidAidCnt = 0, unexpectedActionCnt=0;
	for(int i=0; i< len; i++) {
	    JsonObject jso = jsa.getJsonObject(i);
	    String type =  jso.getString( "type");
	    if (!typeIsAcceptable(type)) {
		ignorableActionCnt++;
		if (jso.getString("arxiv_id",null)!=null) unexpectedActionCnt++;
		continue;		
	    }

	    String ip_hash = jso.getString("ip_hash");
	    String aid = canonicAid(jso.getString( "arxiv_id"));
	    String cookie = jso.getString("cookie_hash", null);
	    if (cookie==null) cookie = jso.getString("cookie", null);
	    if (cookie==null) cookie = "";

	    cnt ++;

	  
	    saver.save( ip_hash, cookie, aid);
	}
	saver.closeAll();
	
	System.out.println("Analyzable action entries count = " + cnt);
	System.out.println("Ignorable  action entries count = " + ignorableActionCnt);

	if (unexpectedActionCnt>0) {
	    System.out.println("There were also " + unexpectedActionCnt + " entries with an arxiv_id field, but with an unacceptable action type");
	}
    }

    
    /** Used to save relevant data in a compact format (CSV) in
	separate files (one per major category).
     */
    private static class DataSaver {
	/** The name of the original (non-split) data file, with the
	    dir name and the extension (including any ".gz") removed. 
	 */
	//	private final File origFile;	
	DataSaver(String origFname) throws IOException {
	    //origFile = _origFile;
	    String name = origFname.replaceAll("\\..*", ".csv"); // replace the extension
	    name = "/data/retro/split/" + name;
	    File f = new File(name);
	    File dir = f.getParentFile();
	    if (!dir.exists()) {
		System.out.println("Creating dir " + dir);
		dir.mkdirs();
	    }
	    w = new PrintWriter(new FileWriter(f));
	}

	PrintWriter w;
	/** Saves one log entry into an appropriate file */
	void save(String ip_hash, String cookie, String aid) throws IOException {
	    w.println(ip_hash + "," + cookie + "," + aid);
	}
	/** Must call this method once done processing input data, in order
	 to close all output streams. */
	void closeAll() {
	    w.flush();
	    w.close();
	}
    }

    /**  Produces output for David Blei's team, as per his 2013-11-11 msg:
	 <pre>
	 2. a file with user data.  each line contains

	 user_hash article_id date downloaded_y/n
	 </pre>
    */
    static void convertJsonFileBlei(String fname, ArxivUserInferrer inferrer,
				    File outfile) throws IOException, JsonException {


	JsonObject jsoOuter = Json.readJsonFile(fname);
	JsonArray jsa = jsoOuter.getJsonArray("entries");
 	int len = jsa.size();


	File d= outfile.getParentFile();
	System.out.println("Creating directory "+d+", if required");
	if (d!=null) d.mkdirs();

	PrintWriter w = new PrintWriter(new FileWriter(outfile));


	int cnt=0, ignorableActionCnt=0, unexpectedActionCnt=0;

 	for(int i=0; i< len; i++) {
	    JsonObject jso = jsa.getJsonObject(i);
	    String type =  jso.getString( "type");
	    String arxiv_id=jso.getString( "arxiv_id",null);
	    if (typeIsAcceptable(type)) {
		if (arxiv_id==null) throw new IllegalArgumentException("No arxiv_id field in entry: " + jso);
	    } else {
		ignorableActionCnt++;
		if (arxiv_id!=null)    unexpectedActionCnt++;
		continue;		
	    } 
	    cnt ++;

	    String ip_hash = jso.getString("ip_hash");
	    String aid = canonicAid(arxiv_id);
	    String cookie = jso.getString("cookie_hash",null);
	    if (cookie==null) cookie = jso.getString("cookie",null);
	    if (cookie==null) cookie = "";
	    int utc = jso.getInt("utc");

	    String user = inferrer.inferUser(ip_hash,cookie);
	    if (user==null) { 
		// let's ignore no-cookie entries (which, actually,
		// don't exist in Paul Ginsparg's arxiv.org logs)
		continue;
	    }
	    //user_hash article_id date downloaded_y/n
	    boolean down = typeIsDownload(type);
	    w.println(user + " " + aid + " " + utc + " " + (down?1:0));
	}
	w.flush();
	w.close();
    }

  
}
