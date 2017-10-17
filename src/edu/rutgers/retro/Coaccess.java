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
    final HashMap<Integer,PrivacyLog> utSet;

    Coaccess(UserActionReader _uar, Vector<Integer> _articles, Vector<Integer> _usersToTest) {
	uar = _uar;
	articles = _articles;
	usersToTest =  _usersToTest;
	aSet = new HashMap<Integer,CAAList>();
	for(Integer aid: articles) {
	    //	    aSet.put(aid, new CAAHashMap());
	    aSet.put(aid, new CAACompact());
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
	final int n = 10;
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
    

    /** Information about an action whose privacy impact we want to measure 
     */
    static class MinusData extends HashMap<Integer,CAAHashMap>  {
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

    /** A PrivacyLog object, associated with one user, keeps track of
	the potential visibiliy of this user's actions to others. */
    class PrivacyLog {	
	int actionCnt=0;
	int visisbleActionCnt=0;
	int recListCnt =0;
	int changedRecListCnt =0;
	/** not yet processed actions from this incremental step */
	Vector<MinusData> minusDataVec = new Vector<MinusData>();
	void analyze() {
	    final int n=10;
	    if (minusDataVec.size()==0) return;
	    int uid = minusDataVec.elementAt(0).ad.uid;
	    System.out.println("Testing user " + uid + " ("+uar.userNameTable.nameAt(uid)+")" );
	
	    HashSet<Integer> testedAids = new HashSet<Integer>();
	    
	    for(MinusData minusSet: minusDataVec) {
		boolean visible=false;
		for(int aid: minusSet.keySet()) {
		    recListCnt++;
		    CAAHashMap cab = minusSet.get(aid);
		    CAAList caa = aSet.get(aid);

		    if (!testedAids.contains(aid)) {
			int[] tops0 = caa.topCAA(n);
			testedAids.add(aid);
		    }

		    boolean visibleNow = false;
		    if (caa.topCAAHaveChanged(n, cab)) {
			changedRecListCnt++;
			visible =  visibleNow =true;
			System.out.println("Top CAA for A["+aid+"]=" + uar.aidNameTable.nameAt(aid) + " affected");
		    }

		    if (doubleCheck) {

			int[] tops0 = caa.topCAA(n);
			int[] tops1 = caa.topCAA(n, cab);
			boolean visible2 = !arraysEqual(tops0, tops1);

			if (visibleNow && !visible2) {
			    System.out.println("HOWEVER, Top CAA for A["+aid+"]=" + uar.aidNameTable.nameAt(aid) + " don't show change: " +  topToString(caa, tops0));
			    //			    caa.topCAAHaveChangedDebug(n, cab);
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

	
		}
		if (visible) visisbleActionCnt++;			
		actionCnt++;		
	    }
	    for(Integer aid: testedAids) {
		aSet.get(aid).dropCandidates(); // for GC
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
	if (pb<b.length) throw new IllegalArgumentException();
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

	System.out.println("Incremental mode=" + inc);
	Vector<Integer> articles = new Vector<Integer>();
	Vector<Integer> usersToTest = new Vector<Integer>();
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
	Coaccess coa = new Coaccess(uar, articles, usersToTest);
	if (inc) {
	    coa.coaccessIncremental();
	} else {
	    coa.coaccessFinal();
	}
    }

}
