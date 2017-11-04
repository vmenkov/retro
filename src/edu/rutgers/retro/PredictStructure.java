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
	    CAAList caa = aSet.get(a.aid); 
			
	    if (user.ofInterest!=null) {  // update CAA for the articles of interest seen earlier by this user
		for(int j: user.ofInterest) {
		    aSet.get(j).addValue(a.aid, 1);
		}
	    }

	    if (caa!=null) { // this is an article of interest
		if (user.ofInterest==null) user.ofInterest = new Vector<Integer>(2,4);
		user.ofInterest.add(a.aid);

		ActionDetails[] as = uar.earlyActionsForUser(a.uid);
		for(ActionDetails y: as) {	    
		    caa.addValue(y.aid, 1);
		}
	    }
	    user.readCnt++;

	    if (a.utc > nextPrintUtc) {
		System.out.println("At t=" + a.utc +" ("+new Date((long)a.utc*1000L)+"); CA nnz=" + mapSize());
		nextPrintUtc += stepSec;
	    }

	}

	reportTop();
    }
    


}
