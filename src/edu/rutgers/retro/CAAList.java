package edu.rutgers.retro;

import java.io.*;
import java.util.*;

import org.apache.commons.lang.mutable.*;

/** A CAAList represents one row of the coaccess matrix. That is,
    for a given j, a CAAList stores the coaccess values for pairs (k,j)
    for all j. A dense and sparse implementations are possible.
*/       
interface CAAList {
    void addValue(int j, int inc);
    /** Retrieves the n largest values */
    int[] topCAA(int n);
    int[] topCAA(int n, CAAList incrementMap);

    /** Retrieve the value for column j */
    int getValue(int j);
    int getDiff(int j1, int j2);

    // The "default" keyword is available in Java 1.8, but not in 1.6
    /** Increments the values in this map as per incrementMap */
    /*
	default void add(final CAAHashMap incrementMap) {
	    for(Integer z: incrementMap.keySet()) {
		addValue(z, incrementMap.get(z).intValue());
	    }	    
	}
    */
    /** Increments the values in this row as per incrementMap */
    void add(CAAList incrementMap);   
    Set<Integer> keySet();

    /** Describes the list of articles that have higher
	(i.e. numerically smaller) ranks in the affected list
	as compared to the un-affected list. These articles
	an adversary who monitors the rec list can identify
	as belonging to the observed user's history.
     */
    static class PromotedArticles {
	/** How many positions are actually used in ranks[] and aids[] */
	int size=0;
	int[] ranks, aids; 
	PromotedArticles(int n) {
	    ranks = new int[n];
	    aids = new int[n];
	}
	void add(int rank, int aid) {
	    ranks[size] = rank;
	    aids[size] = aid;
	    size++;
	}
	boolean isEmpty() { return size==0; }
	int firstChangedPos() {
	    int m=-1;
	    for(int i=0; i<size; i++) {
		int r = ranks[i];
		if (m<0 || r<m) m=r;
	    }
	    return m;
	}
	public String toString() {
	    StringBuffer b = new StringBuffer("(");
	    for(int i=0; i<size; i++) {
		b.append(" " + ranks[i] + ":" + aids[i]);
	    }
	    b.append(")");
	    return b.toString();
	}
    }

    PromotedArticles topCaaChanges(int n, final CAAList incrementMap, int cutoff);
    int topCaaHaveChanged(int n, final CAAList incrementMap, int cutoff);
    boolean topCAAHaveChangedDebug(int n, final CAAList incrementMap);
    /** nnz */
    int size();
    void dropCandidates();

}
