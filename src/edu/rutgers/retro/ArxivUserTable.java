package edu.rutgers.retro;

import java.io.*;
import java.util.*;
import java.util.regex.*;

//import javax.persistence.*;

//import org.json.*;
import javax.json.*;

/** Information about registered Arxiv users, from tc.json. It links
    a registered user to the list of cookies known to be associated
    with him, and allows the reverse lookup (cookie to user) as well.
*/
class ArxivUserTable  {
    HashMap<String,Vector<String>> user2cookies = new HashMap<String,Vector<String>>();
    HashMap<String,String> cookie2user =  new HashMap<String,String>();
    
    ArxivUserTable(String fname) throws IOException, JsonException {
	JsonObject jsoOuter = Json.readJsonFile(fname);
	Set<String> names = jsoOuter.keySet();
	System.out.println("Processing user activity file that has data for " + names.size() + " users...");
	int keyCnt=0, cnt=0;
	for(String u: names) {
	    JsonArray a = jsoOuter.getJsonArray(u);
	    final int n=a.size();
	    Vector<String> v= new Vector<String>(n); 
	    for(int i=0; i<n; i++) {
		String cookie = a.getString(i);
		v.add(cookie);
		cookie2user.put(cookie, u);
	    }
	    user2cookies.put(u,v);
	    keyCnt++;
	    cnt += n;
	    if (keyCnt%1000==0) System.out.println("key cnt=" + keyCnt +", val cnt=" + cnt +", u=" + u);
	}
	System.out.println("User activity file "+fname+" contains "+cnt+" cookies for " + names.size() + " users");
    }

   public static void main(String [] argv) throws IOException, JsonException {

	if (argv.length != 1) {
	    System.out.println("Usage: ArxivUserTable filename.js");
	    return;
	}
	ArxivUserTable t = new  ArxivUserTable(argv[0]);
    }

}

