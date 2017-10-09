package edu.rutgers.retro;

import java.io.*;
import java.util.*;
//import java.util.zip.*;
//import java.util.regex.*;
//import java.text.*;

//import org.json.*;

import org.apache.commons.lang.mutable.*;

public class Coaccess {

    /** A CAAList represents one row of the coaccess matrxi. That is,
	for a given j, a CAAList stores the coaccess values for pairs (k,j)
	for all j. A dense and sparse implementations are possible.
     */       
    static interface CAAList {
	void addValue(int j, int inc);
	int[] topCAA(int n);
	int getValue(int j);
	/** Increments the values in this map as per incrementMap */
	/*
	default void add(final CAAHashMap incrementMap) {
	    for(Integer z: incrementMap.keySet()) {
		addValue(z, incrementMap.get(z).intValue());
	    }	    
	}
	*/

    }

    /** Sparse implementation of CAAList */
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
	
	private int getDiff(int j1, int j2) {
	    return getValue(j1) - getValue(j2);
	}

	/** Returns the n article IDs with the highest counts (coaccess
	    values). For tie breaking, articles' internal IDs are used. 
	*/	
	public int[] topCAA(int n) {
	    Integer[] aids = (Integer[])keySet().toArray(new Integer[0]);
	    Arrays.sort(aids,
			new Comparator<Integer>() {			    
			    public int compare(Integer o1, Integer o2) {
				int d = getDiff(o2, o1);
				if (d==0) d=o1-o2;
				return d;
			    }
			});
	    int a[] = new int[aids.length < n? aids.length: n];
	    for(int i=0; i<a.length; i++) a[i] = aids[i];
	    return a;	    
	}
 
	/** Returns the n article IDs with the highest counts (coaccess
	    values) in this+incrementMap. 
	    For tie breaking, articles' internal IDs are used.
	    @param incrementMap Values from incrementMap map are to be
	    added to values from this map    
	*/	
	public int[] topCAA(int n, final CAAHashMap incrementMap) {
	    Vector<Integer> extra =new Vector<Integer>();
	    for(Integer z: incrementMap.keySet()) {
		if (!containsKey(z)) extra.add(z);
	    }
	    Integer[] aids = new Integer[size() + extra.size()];
	    int j=0;
	    for(Integer z: keySet()) aids[j++] = z;
	    for(Integer z: extra) aids[j++] = z;
	    Arrays.sort(aids,
			new Comparator<Integer>() {			    
			    public int compare(Integer o1, Integer o2) {
				int d=getDiff(o2,o1)+incrementMap.getDiff(o2,o1);
				if (d==0) d=o1-o2;
				return d;
			    }
			});
	    int a[] = new int[aids.length < n? aids.length: n];
	    for(int i=0; i<a.length; i++) a[i] = aids[i];
	    return a;	    
	}

	/** Increments the values in this map as per incrementMap */
	void add(final CAAHashMap incrementMap) {
	    for(Integer z: incrementMap.keySet()) {
		addValue(z, incrementMap.get(z).intValue());
	    }	    
	}


    }

    /** Used to read the action index */
    UserActionReader uar;
    /** This map has an entry for every article of interest */
    final HashMap<Integer,CAAList> aSet;
    /** Articles of interest for this run (i.e. the articles for which
	we want to compute rec lists) */
    final Vector<Integer> articles;

    Coaccess(UserActionReader _uar, Vector<Integer> _articles) {
	uar = _uar;
	articles = _articles;
	aSet = makeBlankMap();
    }    

    /** Creates a blank map with a slot for each article of interest. */
    private HashMap<Integer,CAAList> makeBlankMap() {
	HashMap<Integer,CAAList> bSet = new HashMap<Integer,CAAList>();
	for(Integer aid: articles) {
	    bSet.put(aid, new CAAHashMap());
	}
	return bSet;
    }

    /** Computes the coaccess vectors for specified articles based on
	the community's entire history (i.e. as the coaccess matrix
	would stand at the end of the period represented in the history 
	data.
	@param articles The list of articles for which we want to compute
	the coaccess values
     */
    void coaccessFinal() throws IOException {
	
	for(int i=0; i< uar.users.length; i++) {
	    //System.out.println("User["+i+"] has " + uar.users[i].total + " actions");
	    ActionDetails[] as = uar.actionsForUser(i);
	    for(ActionDetails x: as) {
		CAAList caa = aSet.get(x.aid);
		if (caa!=null) {
		    for(ActionDetails y: as) {	    
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

    /** One step of incremental coaccess computation. Covers the range
	of actions with pos &ge; pos0 and time &lt; t1
	@return new start position in the action list
     */
    int coaccessIncrementalStep(int pos0, int t1) throws IOException {
	int pos = pos0;
	HashMap<Integer,CAAList> bSet = makeBlankMap();
	final int len = (int)uar.actionRAF.lengthObject();
	ActionDetails a = new ActionDetails();
	for(; pos < len && uar.actionRAF.read(a,pos).utc < t1; pos++) {
	    UserActionReader.UserEntry user = uar.users[a.uid];
	    CAAList caa = bSet.get(a.aid);
	    if (user.ofInterest || caa!=null) {
		ActionDetails[] as = null;
		
		if (user.ofInterest) {    // update other articles' CAA
		    for(ActionDetails y: as) {	    
			CAAList cay = bSet.get(y.aid);
			if (cay!=null && y.aid!=a.aid) {
			    cay.addValue(a.aid, 1);
			}
		    }
		}

		if (caa!=null) {
		    user.ofInterest=true;
		    for(ActionDetails y: as) {	    
			if (y.aid!=a.aid) {
			    caa.addValue(y.aid, 1);
			}
		    }
		}
	    }
	    user.readCnt++;
	}
	return pos;
    }

    void coaccessIncremental() throws IOException {
	int pos = 0;
	final int step = 3600*24;
	final int utc0 = (uar.actionRAF.read(new ActionDetails(),pos).utc/step) * step;
	final int len = (int)uar.actionRAF.lengthObject();
	int utc1 = utc0;
	while(pos<len) {
	    utc1 += step;
	    pos = coaccessIncrementalStep(pos, utc1);	    
	}
    }

    static public void main(String argv[]) throws IOException {
	ParseConfig ht = new ParseConfig();

	String indexPath = ht.getOption("index", "out");
	File indexDir = new File(indexPath);
	UserActionReader uar = new UserActionReader(indexDir);

	Vector<Integer> articles = new Vector<Integer>();
	int ja=0;
	String cmd = argv[ja++];
	if (cmd.equals("article") || cmd.equals("aid")) {
	    while(ja<argv.length) {
		String s = argv[ja++];
		int aid =  cmd.equals("aid")? Integer.parseInt(s): uar.aidNameTable.get(s);
		System.out.println("Article " + aid + " ("+uar.aidNameTable.nameAt(aid)+")");
		articles.add(aid);
	    }
	} else if (cmd.equals("uname") || cmd.equals("uid")) {
	    while(ja<argv.length) {
		String s = argv[ja++];
		int uid = cmd.equals("uid")? Integer.parseInt(s) :
		    uar.userNameTable.get(s);
		ActionDetails[] as = uar.actionsForUser(uid);
		System.out.println("For user " + uid + " ("+uar.userNameTable.nameAt(uid)+"), adding " + as.length + " articles" );
		for(ActionDetails x: as) {
		    articles.add(x.aid);
		}
	    }
	} else {
	    throw new IllegalArgumentException("Unknown command: " + cmd);
	}
	Coaccess coa = new Coaccess(uar, articles);
	coa.coaccessFinal();
    }

}
