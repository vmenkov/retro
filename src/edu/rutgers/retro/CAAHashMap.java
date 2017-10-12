package edu.rutgers.retro;

import java.io.*;
import java.util.*;

import org.apache.commons.lang.mutable.*;

/** Sparse implementation of CAAList: storing one row of the coaccess matrix */
class CAAHashMap extends HashMap<Integer,MutableInt> implements CAAList {
    public void addValue(int j, int inc) {
	MutableInt v = get(j);
	if (v == null) {
	    put(j, new MutableInt(inc));
	} else {
	    v.add(inc);
	}
    }
    /** Retrieve the value for column j */
    public int getValue(int j) {
	MutableInt v = get(j);         
	return (v==null) ? 0: v.intValue();
    }	
    
    /** Computes difference between the values in 2 columns */
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
    public void add(final CAAHashMap incrementMap) {
	for(Integer z: incrementMap.keySet()) {
	    addValue(z, incrementMap.get(z).intValue());
	}	    
    }

}

