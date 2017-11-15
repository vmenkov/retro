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


    int topCAAHaveChanged(int n, final CAAList incrementMap, int cutoff);
    boolean topCAAHaveChangedDebug(int n, final CAAList incrementMap);
    /** nnz */
    int size();
    void dropCandidates();

}
