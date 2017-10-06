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
	int[] topCAA(int n);
	int getValue(int j);
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

	public int getValue(int j) {
	     MutableInt v = get(j);         
	     return (v==null) ? 0: v.intValue();
	}


	/** Returns the n article IDs with the highest counts (coaccess
	    values) */	    
	public int[] topCAA(int n) {
	    Integer[] aids = (Integer[])keySet().toArray(new Integer[0]);
	    Arrays.sort(aids,
			new Comparator<Integer>() {			    
			    public int compare(Integer o1, Integer o2) {
				return get(o2).intValue() -get(o1).intValue();
			    }
			});
	    int a[] = new int[aids.length < n? aids.length: n];
	    for(int i=0; i<a.length; i++) a[i] = aids[i];
	    return a;	    
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
			if (y.aid!=x.aid) {
			    caa.addValue(y.aid, 1);
			}
		    }
		}
	    }
	}

	final int n = 10;

	for(int aid: articles) {
	    CAAList caa = aSet.get(aid);
	    int[] tops = caa.topCAA(n);
	    System.out.print("Top CAA for A["+aid+"]=" + uar.aidNameTable.nameAt(aid) + " are:");
	    for(int baid: tops) {
		System.out.print(" A["+baid+"]=" + uar.aidNameTable.nameAt(baid) + ":" + caa.getValue(baid) +",");
	    }
	    System.out.println();
	
	}

    }

    static public void main(String argv[]) throws IOException {
	ParseConfig ht = new ParseConfig();

	String indexPath = ht.getOption("index", "out");
	File indexDir = new File(indexPath);
	UserActionReader uar = new UserActionReader(indexDir);

	Vector<Integer> articles = new Vector<Integer>();
	for(int j=0; j<argv.length; j++) {
	    String aname = argv[j];
	    int aid = uar.aidNameTable.get(aname);
	    System.out.println("Article " + aid + " ("+aname+")");
	    articles.add(aid);
	}
	coaccessFinal( uar, articles);
    }

}
