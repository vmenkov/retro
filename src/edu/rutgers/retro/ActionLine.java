package edu.rutgers.retro;

import java.io.*;
import java.util.*;
//import java.util.zip.*;
//import java.util.regex.*;
//import java.text.*;

import org.json.*;


/** Data from one entry of the JSON file, corresponding to 1 log entry
 */
class ActionLine {
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
  	
