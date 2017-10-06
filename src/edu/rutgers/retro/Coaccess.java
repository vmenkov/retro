package edu.rutgers.retro;

import java.io.*;
import java.util.*;
//import java.util.zip.*;
//import java.util.regex.*;
//import java.text.*;

//import org.json.*;

import org.apache.commons.lang.mutable.*;

public class Coaccess {
    
    static interface CAAList {
	void addValue(int j, int inc);
    }

    static class CAAHashMap extends HashMap<Integer,MutableInt> implements CAAList {
	public void addValue(int j, int inc) {
            MutableInt v = get(j);
            if (v == null) {
                put(j, new MutableInt(inc));
            } else {
                v.add(inc);
            }
        }
    }

    static void coaccessFinal(UserActionReader uar, Vector<Integer> articles) throws IOException {
	HashMap<Integer,CAAList> aSet = new HashMap<Integer,CAAList>();
	for(Integer aid: articles) {
	    aSet.put(aid, new CAAHashMap());
	}
	
	for(int i=0; i< uar.users.length; i++) {
	    //System.out.println("User["+i+"] has " + uar.users[i].total + " actions");
	    UserActionReader.ActionDetails[] as = uar.actionsForUser(i);
	    for(UserActionReader.ActionDetails x: as) {
		CAAList caa = aSet.get(x.aid);
		if (caa!=null) {
		    for(UserActionReader.ActionDetails y: as) {	    
			caa.addValue(y.aid, 1);
		    }
		}
	    }
	}

	
    }

    static public void main(String argv[]) throws IOException {
	ParseConfig ht = new ParseConfig();

	String indexPath = ht.getOption("index", "out");
	File indexDir = new File(indexPath);
	UserActionReader uar = new UserActionReader(indexDir);

	Vector<Integer> articles = new Vector<Integer>();
	String aname = argv[0];
	int aid = uar.aidNameTable.get(aname);
	System.out.println("Article " + aid + " ("+aname+")");
	articles.add(aid);
    }

}
