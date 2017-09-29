package edu.rutgers.retro;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.regex.*;
import java.text.*;

import org.json.*;
//import javax.json.*;


/** An auxiliary class used to save the information of user's action history
    to a random-access file */
class UserActionSaver {	

    static class UserEntry {
	/** how many actions will be saved, in total, for this user. This value
	    may be slightly adjusted, if and when duplicates are discovered. */
	int total;
	/** The beginning of this user's data in the file. The units are action
	    records, rather than bytes */
	int offset0;
	UserActionSaver(int _total, int _offset) {
	    total = _total;
	    offset0 = _offset;
	}
    }

    class ActionDetails {
	int uid, utc, aid;
    }

    NameTable userNameTable;
    UserEntry [] users;

    UserActionSaver(UserStats.UserInfo[] ui, NameTable _userNameTable) {
	userNameTable =	_userNameTable;
	users = new UserEntry[ui.length];
	int offset = 0;
	for(int i=0; i<ui.length; i++) {
	    users[i] = new UserActionSaver(ui[i].acceptCnt, offset);
	    offset += ui[i].acceptCnt;
	}	
    }

    void addFromJsonFile(File f) throws IOException, JSONException {

	JSONObject jsoOuter = Json.readJsonFile(f);
	JSONArray jsa = jsoOuter.getJSONArray("entries");
	int len = jsa.length();
	System.out.println("Json data file action entry count = " + len);

	//	int cnt=0, ignorableActionCnt=0, invalidAidCnt = 0, unexpectedActionCnt=0, botCnt=0, ignorableUserCnt=0;
	for(int i=0; i< len; i++) {
	    JSONObject jso = jsa.getJSONObject(i);
	    ActionLine z = new ActionLine(jso);

	}
    }
    
    void saveActions(File[] jsonFiles) {
	for(File g: jsonFiles) {
	    System.out.println("Processing " + g);
	    us.addFromJsonFile(g);
	}

    }
 

}
