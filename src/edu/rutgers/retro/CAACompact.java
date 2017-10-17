package edu.rutgers.retro;

import java.io.*;
import java.util.*;

import org.apache.commons.lang.mutable.*;

/** Sparse implementation of CAAList: storing one row of the coaccess matrix */
class CAACompact extends CompressedRow
//extends HashMap<Integer,MutableInt> 
    implements CAAList {
    
    CAACompact() { super(0); }

    int onesCnt=0;
    int[] ones = new int[100];

    private void pack() {
	if (onesCnt==0) return;
	add( new CompressedRow(ones, onesCnt));	
	onesCnt=0;
    }

    public void addValue(int j, int inc) {
	int k = findKey(j);
	if (k>=0) {
	    values[k] += inc;
	    return;
	} else if (keysCnt<keys.length && j>keys[keysCnt-1]) {
	    keys[keysCnt]=j;
	    values[keysCnt]=inc;
	    keysCnt++;
	} else if (inc==1) {
	    throw new IllegalArgumentException("addValue() only supported with inc=1");
	}
    }

    /** Retrieves the value for column j */
    public int getValue(int j) {	
	int k = findKey(j);
	if (k>=0) return values[k];
	int s=0;
	for(int i=0; i<onesCnt; i++) {
	    if (ones[i]==j) s++;
	}
	return s;
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

    public int size() { return keysCnt + onesCnt; }

    /** Returns the n article IDs with the highest counts (coaccess
	values). For tie breaking, articles' internal IDs are used. 
    */	
    public int[] topCAA(int n) {
	if (n==0) return new int[0];
	pack();

	CAAHashMap.ME[]  entries = new CAAHashMap.ME[size()];
	for(int i=0;i<keysCnt;i++) {
	    entries[i] = new CAAHashMap.ME(keys[i], values[i]);
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

    /** Comparese the list of top elements of this coaccess matrix row
	with the list that would obtain if  incrementMap were to be 
	added to it. incrementMap consists of negative value (representing
	NOT including a particular action which is included in this 
	row), which means that the modified row will have fewer non-zeros,
	and smaller positive values of non-zeros, than the non-modified row.
	@param incrementMap The only expected increment values are -1
     */
    public boolean topCAAHaveChanged(int n, final CAAList incrementMap) {
	if (candidates==null) throw new AssertionError("This method can only be called after toCAA(n) has been called");
	pack();
	if (n>candidates.length) n=candidates.length;
	if (n==0) return false;
	int last=0, ilast=0;
	for(int i=0; i<candidates.length; i++) {
	    int x= getValue(candidates[i])+incrementMap.getValue(candidates[i]);
	    if (i<n && x==0) return true;
	    if (i>0) {
		if (x>last) return true;
		if (x==last && candidates[i]<candidates[ilast])  return true;
	    }
	    if (i<n) {
		last = x;
		ilast = i;
	    }
	}
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
	pack();
	if (!(_incrementMap instanceof CAAHashMap)) throw new IllegalArgumentException();
	CAAHashMap incrementMap = (CAAHashMap)_incrementMap;
	if (n==0) return new int[0];
	
	Vector<CAAHashMap.ME> v = new Vector<CAAHashMap.ME>();
	for(int i=0;i<keysCnt;i++) {
	    CAAHashMap.ME e = new CAAHashMap.ME(keys[i], values[i]);
	    e.val += incrementMap.getValue(e.key);
	    v.add(e);
	}

	for(Map.Entry<Integer,MutableInt> e: incrementMap.entrySet()) {
	    if (findKey(e.getKey())<0) v.add(new CAAHashMap.ME(e));
	}

	CAAHashMap.ME[] entries = (CAAHashMap.ME[])v.toArray(new CAAHashMap.ME[0]);
  	Arrays.sort(entries);
	if (entries.length < n) n = entries.length;
	int a[] = new int[n];
	for(int i=0; i<n; i++) a[i] = entries[i].key;
	return a;	       
    }
       

    /** Increments the values in this map as per incrementMap */
    public void add(final CAAList _incrementMap) {
	if (!(_incrementMap instanceof CAAHashMap)) throw new IllegalArgumentException();
	CAAHashMap incrementMap = (CAAHashMap)_incrementMap;
	pack();
	add(new CompressedRow(incrementMap));
    }

    public Set<Integer> keySet() {
	pack();
	TreeSet<Integer> s=	new TreeSet<Integer>();
	for(int key: keys) s.add(key);
	for(int key: ones) s.add(key);
	return s;
    }

    public void dropCandidates() {
	candidates=null; // for GC
    }


}

