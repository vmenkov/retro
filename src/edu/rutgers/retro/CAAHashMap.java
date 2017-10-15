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
    public int getDiff(int j1, int j2) {
	return getValue(j1) - getValue(j2);
    }
    
    /** This array may be created during the most recent topCAA(n) call;
	it will contain the top n values, as well as those that are within 
	1 from it (i.e. those that could be pushed into the top n by a small
	increment).
     */
    int[] candidates=null;

    /** Returns the n article IDs with the highest counts (coaccess
	values). For tie breaking, articles' internal IDs are used. 
    */	
    public int[] topCAA(int n) {
	if (n==0) return new int[0];
	Integer[] aids = (Integer[])keySet().toArray(new Integer[0]);
	Arrays.sort(aids,
		    new Comparator<Integer>() {			    
			public int compare(Integer o1, Integer o2) {
			    int d = getDiff(o2, o1);
			    if (d==0) d=o1-o2;
			    return d;
			}
		    });
	if (aids.length < n) n = aids.length;
	// How many candidates do we need to save?
        int m = n;
	int threshold = getValue(n-1)-1;
	while(m < aids.length && getValue(m) >= threshold) m++;
	candidates=new int[m];
	for(int i=0; i<candidates.length; i++) candidates[i] = aids[i];
	int a[] = new int[n];
	for(int i=0; i<a.length; i++) a[i] = aids[i];
	return a;	    
    }
 
    /** @param incrementMap The only expected increment values are -1
     */
    public boolean topCAAHaveChanged(int n, final CAAList incrementMap) {
	if (candidates==null) throw new AssertionError("This method can only be called after toCAA(n) has been called");
	if (n>candidates.length) n=candidates.length;
	if (n==0) return false;
	int last=getValue(candidates[0]) + incrementMap.getValue(candidates[0]);
	for(int i=1; i<n; i++) {
	    int x= getValue(candidates[i])+incrementMap.getValue(candidates[i]);
	    if (x>last) return true;
	    if (x==last && candidates[i]<candidates[i-1])  return true;
	    last = x;
	}

	for(int i=n; i<candidates.length; i++) {
	    int x=getValue(candidates[i])+incrementMap.getValue(candidates[i]);
	    if (x>last) return true;
	    if (last>0 && x==last && candidates[i]<candidates[i-1])  return true;
	}	    
	return false;
    }

   /** Returns the n article IDs with the highest counts (coaccess
	values) in this+incrementMap. 
	For tie breaking, articles' internal IDs are used.
	@param incrementMap Values from incrementMap map are to be
	added to values from this map    
    */	
    public int[] topCAA(int n, final CAAList incrementMap) {
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
    public void add(final CAAList incrementMap) {
	for(Integer z: incrementMap.keySet()) {
	    addValue(z, incrementMap.getValue(z));
	}	    
    }

}

