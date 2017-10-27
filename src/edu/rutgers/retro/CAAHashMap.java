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
    /** Retrieves the value for column j */
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

	ME[]  entries = new ME[size()];
	int j=0;
	for(Map.Entry<Integer,MutableInt> e: entrySet()) {
	    entries[j++] = new ME(e);
	}

  	Arrays.sort(entries);
	if (entries.length < n) n = entries.length;
	// How many candidates do we need to save?
        int m = n;
	if (m>0) {
	    int threshold = entries[n-1].val-1;
	    while(m < entries.length && entries[m].val >= threshold) m++;
	}
	candidates=new int[m];
	for(int i=0;i<candidates.length; i++) candidates[i]=entries[i].key;
	return Arrays.copyOf(candidates,n);
    }

    public int[] topCAA_orig(int n) {
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
	int threshold = getValue(aids[n-1])-1;
	while(m < aids.length && getValue(aids[m]) >= threshold) m++;
	candidates=new int[m];
	for(int i=0; i<candidates.length; i++) candidates[i] = aids[i];
	return Arrays.copyOf(candidates,n);
     }
 
    /** Comparese the list of top elements of this coaccess matrix row
	with the list that would obtain if  incrementMap were to be 
	added to it. incrementMap consists of negative value (representing
	NOT including a particular action which is included in this 
	row), which means that the modified row will have fewer non-zeros,
	and smaller positive values of non-zeros, than the non-modified row.
	@param incrementMap The only expected increment values are -1
     */
    public int topCAAHaveChanged(int n, final CAAList incrementMap) {
	if (candidates==null) throw new AssertionError("This method can only be called after toCAA(n) has been called");
	if (n>candidates.length) n=candidates.length;
	if (n==0) return -1;
	int last=0, ilast=0;
	for(int i=0; i<candidates.length; i++) {
	    int x= getValue(candidates[i])+incrementMap.getValue(candidates[i]);
	    if (i<n && x==0) return i;
	    if (i>0) {
		if (x>last) return ilast;
		if (x==last && candidates[i]<candidates[ilast])  return ilast;
	    }
	    if (i<n) {
		last = x;
		ilast = i;
	    }
	}
	return -1;
    }

   public boolean topCAAHaveChangedDebug(int n, final CAAList incrementMap) {
	if (candidates==null) throw new AssertionError("This method can only be called after toCAA(n) has been called");
	if (n>candidates.length) n=candidates.length;
	if (n==0) return false;
	int last=0, ilast=0;
	for(int i=0; i<candidates.length; i++) {
	    int x= getValue(candidates[i])+incrementMap.getValue(candidates[i]);
	    if (i<n && x==0) {
		System.out.println("DEBUG: x[" + i + ":"+candidates[i]+"]=0");
		return true;
	    }
	    if (i>0) {
		if (x>last) {
		    System.out.println("DEBUG: x[" + i + ":"+candidates[i]+"]=" +x+ " > xlast["+ilast+":"+candidates[ilast]+"]=" +last);
		    return true;
		}
		if (x==last && candidates[i]<candidates[ilast])  {
		    System.out.println("DEBUG: x[" + i +  ":"+candidates[i]+"]=" +x+ "=xlast["+ilast+":"+candidates[ilast]+"]");
		    return true;
		}
	    }
	    if (i<n) {
		last = x;
		ilast = i;
	    }
	}
	System.out.println("DEBUG: NO CHANGE");
	return false;
    }


   /** Returns the n article IDs that have the highest counts
	(coaccess values) in this+incrementMap. For tie breaking,
	articles' internal IDs are used. Entries with 0 values are
	ignored (never included in the returned array). So if this
	matrix row has fewer than n non-zeros, the returned array
	will have fewer than n values.
	@param incrementMap Values from incrementMap map are to be
	added to values from this map. They can be positive or negative.
	
    */	
    public int[] topCAA(int n, final CAAList _incrementMap) {
	if (!(_incrementMap instanceof CAAHashMap)) throw new IllegalArgumentException();
	CAAHashMap incrementMap = (CAAHashMap)_incrementMap;
	if (n==0) return new int[0];

	Vector<ME>  v = new Vector<ME>(size());
	for(Map.Entry<Integer,MutableInt> e: entrySet()) {
	    ME me = new ME(e);
	    me.val += incrementMap.getValue(me.key);
	    if (me.val!=0) v.add( me ); // ignore zeros
	}
	for(Map.Entry<Integer,MutableInt> e: incrementMap.entrySet()) {
	    if (!containsKey(e.getKey())) v.add(new ME(e));
	}

	ME[] entries = (ME[])v.toArray(new ME[0]);
  	Arrays.sort(entries);
	if (entries.length < n) n = entries.length;
	int a[] = new int[n];
	for(int i=0; i<n; i++) a[i] = entries[i].key;
	return a;	       
    }
    

    public int[] topCAA_orig(int n, final CAAList incrementMap) {
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

    public void dropCandidates() {
	candidates=null; // for GC
    }


}
