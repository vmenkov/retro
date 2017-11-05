package edu.rutgers.retro;

import java.io.*;
import java.util.*;

import org.apache.commons.lang.mutable.*;

/** This class is used to find out which elements of the coaccess matrix need to be kept.
*/
public class PredictStructure extends Coaccess {


     PredictStructure(UserActionReader _uar, Vector<Integer> _articles, int _n) {
	 super(_uar, _articles,  new Vector<Integer>(), _n);
	 for(Integer aid: articles) {
	     aSet.put(aid, new CAACompact2());
	 }
	 for(Integer uid: usersToTest) {
	    utSet.put(uid, new PrivacyLog());
	 }
     }    


    void predictStructure() throws IOException {
	final int stepSec = 3600 * 24 * 7;
	int nextPrintUtc = (uar.actionRAF.read(new ActionDetails(),0).utc/stepSec) * stepSec;

	System.out.println("Immediate-update recommender starts; CA nnz=" + mapSize());

	final int len = (int)uar.actionRAF.lengthObject();
	ActionDetails a = new ActionDetails();

	for(int pos = 0; pos<len; pos++) { // for all actions, ever

	    uar.actionRAF.read(a,pos); 
	    // the user who carried out this action
	    UserActionReader.UserEntry user = uar.users[a.uid];
	    CAACompact2 caa = (CAACompact2)aSet.get(a.aid); 
			
	    if (user.ofInterest!=null) {  // update CAA for the articles of interest seen earlier by this user
		for(int j: user.ofInterest) {
		    CAACompact2 caz = (CAACompact2)aSet.get(j);
		    caz.addValue(a.aid, 1);
		    if (!caz.hasCandidates) caz.topCAA(n);
		}
	    }

	    if (caa!=null) { // this is an article of interest
		if (user.ofInterest==null) user.ofInterest = new Vector<Integer>(2,4);
		user.ofInterest.add(a.aid);

		ActionDetails[] as = uar.earlyActionsForUser(a.uid);
		for(ActionDetails y: as) {	    
		    caa.addValue(y.aid, 1);
		}
		if (!caa.hasCandidates) caa.topCAA(n);
	    }
	    user.readCnt++;

	    if (a.utc > nextPrintUtc) {
		System.out.println("At t=" + a.utc +" ("+new Date((long)a.utc*1000L)+"); CA nnz=" + mapSize());
		nextPrintUtc += stepSec;
	    }

	}

	reportStructure();
    }

    /** Prints the top coaccess values for all articles of interest. */
    void reportStructure()     {
	for(int aid: articles) {
	    CAACompact2 caa = (CAACompact2)aSet.get(aid);
	    int[] tops = caa.topCAA(n);
	    System.out.println("Top CAA for A["+aid+"]=" + uar.aidNameTable.nameAt(aid) + " ("+caa.size()+") are:");
	    System.out.println( topToString(caa, tops));
	    System.out.print("Keep " + caa.allTimeCandidates.size() + " candidates out of "+caa.size()+":" );
	    for(int k: caa.allTimeCandidates) {
		System.out.print(" " + k + ":" +  uar.aidNameTable.nameAt(k) +
				 ":" + caa.getValue(k));
	    }
	    System.out.println();
	}
    }

    
    static public void main(String argv[]) throws IOException {
	ParseConfig ht = new ParseConfig();

	Profiler.profiler.setOn( ht.getOption("profile", true));

	String indexPath = ht.getOption("index", "out");
	File indexDir = new File(indexPath);
	UserActionReader uar = new UserActionReader(indexDir);

	// The number of top articles that are displayed as rec list 
	final int n = ht.getOption("n", 10);
	
	// articles whose coaccess lists we will monitor
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
	} else if (cmd.equals("arange")) { // a1 <= aid < a2
	    int a[] = { Integer.parseInt(argv[ja]), Integer.parseInt(argv[ja+1])};
	    ja +=2 ;
	    for(int aid=a[0]; aid<a[1]; aid++) {
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

	System.out.println("Will compute coaccess data for " + articles.size() + " articles");
	PredictStructure coa = new PredictStructure(uar, articles, n);
	Profiler.profiler.push(Profiler.Code.OTHER);

	coa. predictStructure();


	Profiler.profiler.pop(Profiler.Code.OTHER);

	System.out.println("===Profiler report (wall clock time)===");
	System.out.println(     Profiler.profiler.report());

    }


}
