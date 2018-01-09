package edu.rutgers.retro;

import java.io.*;
import java.util.*;

import org.apache.commons.lang.mutable.*;

/** A version of CAACompact modified for the "predict structure" application.
    During a usual coccess runs, it keeps remembering the positions of "candidate
    top values" at all stages.
 */
class CAACompact2  extends  CAACompact {
    
    CAACompact2() { super(); }

    TreeSet<Integer> allTimeCandidates = new  TreeSet<Integer>();    

    void dropCandidates2(int because) {
	if (!allTimeCandidates.contains(because))  dropCandidates();
    }
    
   

  /** Returns the n article IDs with the highest counts (coaccess
	values). For tie breaking, articles' internal IDs are used. 
    */	
    public int[] topCAA(int n) {
	int q[] = super.topCAA(n);
	for(int key: candidates) {
	    allTimeCandidates.add(key);
	}
	return q;
    }

}

