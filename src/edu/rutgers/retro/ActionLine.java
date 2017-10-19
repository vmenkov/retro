package edu.rutgers.retro;

import java.io.*;
import java.util.*;

import org.json.*;


/** Data from one entry of the JSON file, corresponding to 1 log entry
 */
class ActionLine {
    static boolean skipBots=true;

    String type;
    String ip_hash;
    String arxiv_id;
    String aid;
    String cookie;
    String user_agent;
    int utc;
    boolean ignorableAction = false;
    boolean unexpectedAction = false;
    boolean isBot = false;

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
	isBot = (skipBots && isKnownBot(user_agent));
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


}
  	
