package edu.rutgers.retro;

import java.io.*;
import java.util.*;

import org.apache.commons.lang.mutable.*;

/** The main module for coaccess data computation and analysis. Includes the
    "privacy" research component.
*/
public class Coaccess {

    /** Used to read data from the action index */
    UserActionReader uar;
    /** This map has an entry for every article of interest */
    final HashMap<Integer,CAAList> aSet;
    /** Articles of interest for this run (i.e. the articles for which
	we want to compute rec lists) */
    final Vector<Integer> articles;
    /** Users whose actions we want to test for "visibility" to others */
    final Vector<Integer> usersToTest;
    /** For each user of interest, this HashMap contains the PrivacyLog object,
	with information about the visibility of this user's actions.
    */
    final HashMap<Integer,PrivacyLog> utSet;
    /** The number of top articles that are displayed as rec list */
    final int n;

       
    Coaccess(UserActionReader _uar, Vector<Integer> _articles, Vector<Integer> _usersToTest, boolean useCompact, int _n) {
	n = _n;
	uar = _uar;
	articles = _articles;
	usersToTest =  _usersToTest;
	aSet = new HashMap<Integer,CAAList>();
	for(Integer aid: articles) {
	    aSet.put(aid, useCompact? new CAACompact():new CAAHashMap());
	}
	utSet = new HashMap<Integer,PrivacyLog>();
	for(Integer uid: usersToTest) {
	    utSet.put(uid, new PrivacyLog());
	}
    }    

    /** Creates a blank map with a slot for each article of interest. */
    private HashMap<Integer,CAAHashMap> makeBlankMap() {
	HashMap<Integer,CAAHashMap> bSet = new HashMap<Integer,CAAHashMap>();
	for(Integer aid: articles) {
	    bSet.put(aid, new CAAHashMap());
	}
	return bSet;
    }

    private int mapSize() {
	int sum=0;
	for(CAAList caa: aSet.values()) sum += caa.size();
	return sum;
    }

    /** Computes the coaccess vectors for specified articles based on
	the community's entire history (i.e. as the coaccess matrix
	would stand at the end of the period represented in the history 
	data).
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
	reportTop();
    }

    /** Prints the top coaccess values for all articles of interest. */
    void reportTop()     {
	for(int aid: articles) {
	    CAAList caa = aSet.get(aid);
	    int[] tops = caa.topCAA(n);
	    System.out.println("Top CAA for A["+aid+"]=" + uar.aidNameTable.nameAt(aid) + " ("+caa.size()+") are:");
	    System.out.println( topToString(caa, tops));
	}
    }

    String topToString(CAAList caa, int[] tops) {
	return  topToString( caa, null, tops);
    }

    String topToString(CAAList caa, CAAList cab,int[] tops) {
	StringBuffer b = new StringBuffer();
	for(int baid: tops) {
	    //if (baid==aid)  throw new AssertionError("Duplicates should have been excluded!");
	    int x=caa.getValue(baid);
	    if (cab!=null) x+= cab.getValue(baid);
	    b.append(" A["+baid+"]=" + uar.aidNameTable.nameAt(baid)+":"+x+",");
	}
	return b.toString();
    }
    

    /** Information about an action whose privacy impact we want to
	measure.  The main data structure is a HashMap, which, for
	each potentially affected article, contains a CAAHashMap
	object that describes the effect of the action to the coaccess
	matrix row for that article. By analyzing this CAAHashMap
	(together with the "base" coaccess data for the article in
	question), we can later find out whether the rec list produced
	by the system with that article as a stymulus will be affected.
     */
    static class MinusData extends HashMap<Integer,CAAHashMap>  {
	/** Contains information about the user whose action it is,
	    and the page the user viewed. */
	ActionDetails ad;
	/** This creates a copy of ad, rather than a link to it,
	    because the caller may reuse the ActionDetails object as
	    it reads data from a file */
	MinusData(ActionDetails _ad) {
	    ad = new ActionDetails(_ad);
	}
	void add(int i, int j, int inc) {
	    CAAHashMap z = get(i);
	    if (z==null) put(i, z=new CAAHashMap());
	    z.addValue(j,inc);
	}
    }

    static final boolean doubleCheck = false;

    /** A PrivacyLog object, associated with one user of interest,
	keeps track of the potential visibiliy of all actions of this user
	to other observers. */
    class PrivacyLog {
	/** The number of top articles that are displayed as rec list */
	//	final int n=10;
	int actionCnt=0;
	int visisbleActionCnt=0;
	int recListCnt =0;
	int changedRecListCnt =0;
	/** Not yet analyzed actions, for this users, from the current incremental step */
	Vector<MinusData> minusDataVec = new Vector<MinusData>();
	void analyze() {
	    if (minusDataVec.size()==0) return;
	    Profiler.profiler.push(Profiler.Code.COA_analyze);
	    Profiler.profiler.push(Profiler.Code.COA_analyze_0);

	    int uid = minusDataVec.elementAt(0).ad.uid;
	    System.out.println("Analyzing actions of user " + uid + " ("+uar.userNameTable.nameAt(uid)+")" );
	
	    // the set of articles for which we have updated the
	    // end-of-step topCAA
	    HashSet<Integer> testedAids = new HashSet<Integer>();
	    Profiler.profiler.pop(Profiler.Code.COA_analyze_0);
	    
	    for(MinusData minusSet: minusDataVec) { // for each recent action of this user
		Profiler.profiler.push(Profiler.Code.COA_analyze_1);
		boolean visible=false;
		int actionAid = minusSet.ad.aid;
		if (minusSet.ad.uid != uid) throw new AssertionError();
		Vector<int[]> visibleFrom = new Vector<int[]>();
		Vector<Integer> affected = new Vector<Integer>();
		Profiler.profiler.replace(Profiler.Code.COA_analyze_1, Profiler.Code.COA_analyze_23);
		for(int aid: minusSet.keySet()) { // for each article whose coaccess vector this action has affected
		    Profiler.profiler.push(Profiler.Code.COA_analyze_2);
		    recListCnt++;
		    CAAHashMap cab = minusSet.get(aid);
		    CAAList caa = aSet.get(aid);

		    if (!testedAids.contains(aid)) {
			int[] tops0 = caa.topCAA(n);
			testedAids.add(aid);
		    }

		    boolean visibleNow = false;
		    int changePos = caa.topCAAHaveChanged(n, cab);
		    Profiler.profiler.replace(Profiler.Code.COA_analyze_2,
					      Profiler.Code.COA_analyze_3);
		    if (changePos>=0) {
			changedRecListCnt++;
			visible = visibleNow =true;
			//System.out.println("Top CAA for A["+aid+"]=" + uar.aidNameTable.nameAt(aid) + " affected")
			visibleFrom.add(new int[]{aid,changePos});
		    }
		    if (doubleCheck) doubleChecking(aid, caa, cab, visibleNow);	
		    Profiler.profiler.pop(Profiler.Code.COA_analyze_3);
		}

		
		Profiler.profiler.replace( Profiler.Code.COA_analyze_23,
					  Profiler.Code.COA_analyze_reporting);
		if (visible) {
		    visisbleActionCnt++;			
		    System.out.print("U["+uid+"]="+uar.userNameTable.nameAt(uid)+" viewing of A["+actionAid+"]=" + uar.aidNameTable.nameAt(actionAid) + " is seen from");
		    
		    for(int[] x: visibleFrom) {
			int aid=x[0];
			int rank=x[1];
			System.out.print(" A["+aid+"]=" + uar.aidNameTable.nameAt(aid) + ":" + rank);
		    }
		    System.out.println();
		}
		Profiler.profiler.pop(Profiler.Code.COA_analyze_reporting);

		actionCnt++;		
	    }
	    Profiler.profiler.pop(Profiler.Code.COA_analyze);
	    /* 
	    for(Integer aid: testedAids) {
		aSet.get(aid).dropCandidates(); // for GC
	    }
	    */
	}	
    }

    /** Was used to check correctness of some optimized methods, comparing
	their results with those of unoptimized ones */
    private void doubleChecking(int aid, CAAList caa, CAAHashMap cab,
				boolean visibleNow) {
	((CAACompact)caa).validate("doubleCheck");
	int[] tops0 = caa.topCAA(n);
	int[] tops1 = caa.topCAA(n, cab);
	boolean visible2 = !arraysEqual(tops0, tops1);

	if (visibleNow && !visible2) {
	    System.out.println("HOWEVER, Top CAA for A["+aid+"]=" + uar.aidNameTable.nameAt(aid) + " don't show change: " +  topToString(caa, tops0));
	    caa.topCAAHaveChangedDebug(n, cab);
	} else if (!visibleNow && visible2) {
	    System.out.print("HOWEVER, Top CAA for A["+aid+"]=" + uar.aidNameTable.nameAt(aid) + " show change:");
	    if (diffIsInsertionsOfOnesOnly(caa, tops1, tops0)) {
		System.out.println(" Trivial diff (addition of singles)");	
	    } else {
		System.out.println();
		System.out.println("With action: " + topToString(caa, tops0));
		System.out.println("W/o  action: " + topToString(caa, cab, tops1));
	    }
	}
    }

    static boolean arraysEqual( int[] a, int[] b) {
	if (a.length != b.length) return false;
	for(int i=0; i<a.length; i++) {
	    if (a[i]!=b[i]) return false;
	}
	return true;
    }

    /** Returns true if b[] only differs from a[] by insertion of some ones
     */
    static boolean diffIsInsertionsOfOnesOnly(CAAList caa, int[] a, int[] b) {
	int pa=0, pb=0;
	while(pa<a.length && pb<b.length) {
	    if (a[pa] == b[pb]) { pa++; pb++; continue;}
	    if (caa.getValue(b[pb])==1) { pb++; continue;}
	    return false;
	}
	if (pb<b.length) return false;
	return true;
    }		    

    /** One step of incremental coaccess computation. Covers the range
	of actions with pos &ge; pos0 and time &lt; t1.

	<p>For any action of a "user of interest" (which, by onstruction, 
	necessarily involves and "article of interest") triggers the
	"privacy test", i.e. test of influence of this action on the all
	rec lists associated with articles in this user's view set.

	@return new start position in the action list
     */
    int coaccessIncrementalStep(int pos0, int t1) throws IOException {
	System.out.println("Step ending at t=" + t1 +" ("+new Date((long)t1*1000L)+"); CA nnz=" + mapSize());
	int pos = pos0;
	HashMap<Integer,CAAHashMap> bSet = makeBlankMap();
	final int len = (int)uar.actionRAF.lengthObject();
	ActionDetails a = new ActionDetails();

	for(PrivacyLog pLog: utSet.values()) pLog.minusDataVec.clear();


	for(; pos < len && uar.actionRAF.read(a,pos).utc < t1; pos++) {
	    UserActionReader.UserEntry user = uar.users[a.uid];
	    CAAList caa = bSet.get(a.aid);

	    boolean doMinus = utSet.containsKey(a.uid);
	    MinusData minusSet = null;
	    if (doMinus) minusSet = new MinusData(a);
			
	    if (user.ofInterest!=null) {  // update CAA for the articles of interest seen earlier by this user
		for(int j: user.ofInterest) {
		    bSet.get(j).addValue(a.aid, 1);
		    if (doMinus) minusSet.add(j,a.aid, -1);
		}
	    }

	    if (caa!=null) { // this is an article of interest
		if (user.ofInterest==null) user.ofInterest = new Vector<Integer>(2,4);
		user.ofInterest.add(a.aid);

		CAAHashMap minusCaa = null;
		if (doMinus) minusSet.put(a.aid, minusCaa=new CAAHashMap());
		ActionDetails[] as = uar.earlyActionsForUser(a.uid);
		for(ActionDetails y: as) {	    
		    caa.addValue(y.aid, 1);
		    if (doMinus) minusCaa.addValue(y.aid, -1);
		}
	    }
	    user.readCnt++;
	    if (doMinus) utSet.get(a.uid).minusDataVec.add(minusSet);

	}
	for(Integer aid: bSet.keySet()) { 
	    aSet.get(aid).add(bSet.get(aid));
	}

	for(PrivacyLog pLog: utSet.values()) pLog.analyze();


	return pos;
    }

    /**
       @param stepSec in seconds
     */
    void coaccessIncremental(int stepSec) throws IOException {
	int pos = 0;
	final int utc0 = (uar.actionRAF.read(new ActionDetails(),pos).utc/stepSec) * stepSec;
	final int len = (int)uar.actionRAF.lengthObject();
	int utc1 = utc0;
	while(pos<len) {
	    utc1 += stepSec;
	    pos = coaccessIncrementalStep(pos, utc1);	    
	}
	reportTop();

	System.out.println("Privacy report");
	for(Integer uid: utSet.keySet()) {
	    PrivacyLog pLog = utSet.get(uid);
	    System.out.println("For user " + uid + " ("+uar.userNameTable.nameAt(uid)+"), out of " + pLog.actionCnt + ", visible " + pLog.visisbleActionCnt + 
			       " (" +pLog.changedRecListCnt+ " rec lists out of " +pLog.recListCnt +")");

	}

    }

    /** Emulates a recommender with immediately-updated coaccess data */
    void coaccessImmediate() throws IOException {
	final int stepSec = 3600 * 24 * 7;
	int nextPrintUtc = (uar.actionRAF.read(new ActionDetails(),0).utc/stepSec) * stepSec;

	System.out.println("Immediate-update recommender starts; CA nnz=" + mapSize());
	HashMap<Integer,CAAHashMap> bSet = makeBlankMap();
	final int len = (int)uar.actionRAF.lengthObject();
	ActionDetails a = new ActionDetails();
	for(PrivacyLog pLog: utSet.values()) pLog.minusDataVec.clear();

	for(int pos = 0; pos<len; pos++) {

	    uar.actionRAF.read(a,pos); 
	    UserActionReader.UserEntry user = uar.users[a.uid];
	    CAAList caa = bSet.get(a.aid);

	    boolean doMinus = utSet.containsKey(a.uid);
	    MinusData minusSet = null;
	    if (doMinus) minusSet = new MinusData(a);
			
	    if (user.ofInterest!=null) {  // update CAA for the articles of interest seen earlier by this user
		for(int j: user.ofInterest) {
		    bSet.get(j).addValue(a.aid, 1);
		    if (doMinus) minusSet.add(j,a.aid, -1);
		}
	    }

	    if (caa!=null) { // this is an article of interest
		if (user.ofInterest==null) user.ofInterest = new Vector<Integer>(2,4);
		user.ofInterest.add(a.aid);

		CAAHashMap minusCaa = null;
		if (doMinus) minusSet.put(a.aid, minusCaa=new CAAHashMap());
		ActionDetails[] as = uar.earlyActionsForUser(a.uid);
		for(ActionDetails y: as) {	    
		    caa.addValue(y.aid, 1);
		    if (doMinus) minusCaa.addValue(y.aid, -1);
		}
	    }
	    user.readCnt++;
	    PrivacyLog pLog = doMinus? utSet.get(a.uid) : null;
	    
	    for(Integer aid: bSet.keySet()) { 
		aSet.get(aid).add(bSet.get(aid));
	    }

	    if (doMinus) {
		pLog.minusDataVec.add(minusSet);
		pLog.analyze();
		pLog.minusDataVec.clear();
	    }

	    if (a.utc > nextPrintUtc) {
		System.out.println("At t=" + a.utc +" ("+new Date((long)a.utc*1000L)+"); CA nnz=" + mapSize());

		nextPrintUtc += stepSec;
	    }



	}

	reportTop();

	System.out.println("Privacy report");
	for(Integer uid: utSet.keySet()) {
	    PrivacyLog pLog = utSet.get(uid);
	    System.out.println("For user " + uid + " ("+uar.userNameTable.nameAt(uid)+"), out of " + pLog.actionCnt + ", visible " + pLog.visisbleActionCnt + 
			       " (" +pLog.changedRecListCnt+ " rec lists out of " +pLog.recListCnt +")");

	}


    }
    


    static public void main(String argv[]) throws IOException {
	ParseConfig ht = new ParseConfig();

	String indexPath = ht.getOption("index", "out");
	File indexDir = new File(indexPath);
	UserActionReader uar = new UserActionReader(indexDir);

	boolean inc = ht.getOption("inc", false);
	boolean testUsers = ht.getOption("testUsers", true);
	boolean useCompact = ht.getOption("compact", true);
	double hours = ht.getOption("step", 24);

	// The number of top articles that are displayed as rec list 
	final int n = ht.getOption("n", 10);
	
	System.out.println("Incremental mode=" + inc +
			   (inc? ", with step=" + hours + " hrs": ""));
	Vector<Integer> articles = new Vector<Integer>();
	Vector<Integer> usersToTest = new Vector<Integer>();

	System.out.println("Compact data format=" + useCompact);

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
		usersToTest.add(uid);
		ActionDetails[] as = uar.actionsForUser(uid);
		System.out.println("For user " + uid + " ("+uar.userNameTable.nameAt(uid)+"), adding " + as.length + " articles" );
		for(ActionDetails x: as) {
		    articles.add(x.aid);
		}
	    }
	} else {
	    throw new IllegalArgumentException("Unknown command: " + cmd);
	}
	if (usersToTest.size()>0) {
	    System.out.print("Will test actions of " +usersToTest.size()+" users:");
	    for(int uid: usersToTest) {
		System.out.print(" " +uid+ " ("+uar.userNameTable.nameAt(uid)+")");
	    }
	    System.out.println();
	}
	System.out.println("Will compute coaccess data for " + articles.size() + " articles");
	Coaccess coa = new Coaccess(uar, articles, usersToTest, useCompact, n);
	if (inc) {
	    if (hours==0) {
		coa.coaccessImmediate();
	    } else {
		final int stepSec = (int)(3600*hours);
		coa.coaccessIncremental(stepSec);
	    }
	} else {
	    coa.coaccessFinal();
	}

	System.out.println("===Profiler report (wall clock time)===");
	System.out.println(     Profiler.profiler.report());

    }


}
