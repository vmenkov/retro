package edu.rutgers.retro;

import java.io.*;
import java.util.*;

import org.apache.commons.lang.mutable.*;

/** Sparse implementation of CAAList: storing one row of the coaccess matrix */
class CAACompact extends CompressedRow   implements CAAList {
    
    CAACompact() { super(0); }

    int onesCnt=0;
    int[] ones = new int[100];

    /** Moves all data from ones[] into the main CRS structure. Also
	tests if any of the newly added values if high enough to
	potentially enter the candidate array. */
    private void pack() {
	if (onesCnt==0) return;
	CompressedRow x =  new CompressedRow(ones, onesCnt);	
	add(x);	
	onesCnt=0;
	if (candidates!=null && x.reachesThreshold(threshold)) dropCandidates();
    }

    /** Adds a single-component vector (x[j]=inc) to this vector. Either 
	increments an existing component in CRS, or adds an element to ones[].
     */
    public void addValue(int j, int inc) {
	int k = findKey(j);
	if (k>=0) {
	    int val0 = values[k];
	    values[k] += inc;
	    if (val0 < threshold && values[k]>=threshold) dropCandidates();
	} else if (keysCnt<keys.length && j>keys[keysCnt-1]) {
	    keys[keysCnt]=j;
	    values[keysCnt]=inc;
	    if (values[k]>=threshold) dropCandidates();
	    keysCnt++;
	} else if (inc!=1) {
	    throw new IllegalArgumentException("addValue() only supported with inc=1");
	} else {
	    if (onesCnt == ones.length) pack();
	    ones[onesCnt++] = j;
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
    
    /** This array may be created during the most recent topCAA(n)
	call; it will contain the indexes for the top n values, as
	well as those whose values are within 1 from those in the top n. 
	When we study the effect of a small (up to 1) decrement on the top
	values, it is sufficient to compare the candidates.
     */
    int[] candidates=null;
    /** The candidate array should include all indexes whose values are &ge; threshold */
    int threshold=0;

    public int size() { return keysCnt + onesCnt; }


    /** Returns the n article IDs with the highest counts (coaccess
	values). For tie breaking, articles' internal IDs are used. 
    */	
    public int[] topCAA(int n) {
	if (n==0) return new int[0];
	final int n0 = n;

	if (candidates!=null &&  maxValueInOnes()>=threshold) dropCandidates();

	ME[] entries;
	int ecnt = 0;
	if (candidates==null) {
	    pack();
	    entries = new ME[size()];
	    for(int i=0;i<keysCnt;i++) {
		//if (values[i]==0) throw new AssertionError("Zero found in CRS, i=" + i+", key=" + keys[i]);
		if (values[i] >= threshold) entries[ecnt++] = new ME(keys[i], values[i]);
	    }
	} else {
	    entries = new ME[candidates.length];
	    for(int i=0; i<candidates.length; i++) {
		int key = candidates[i];
		entries[ecnt++] = new ME(key, getValue(key));
	    }
	}
	Arrays.sort(entries, 0, ecnt);
	int m;
	if (ecnt <= n) { // save them all
	    m = n = ecnt;
	    threshold = 0;
	} else {   // How many candidates do we need to save?
	    m = n;
	    threshold = entries[n-1].val-1;
	    while(m < ecnt && entries[m].val >= threshold) m++;
	}
	
	candidates=new int[m];
	for(int i=0;i<candidates.length; i++) {
	    candidates[i]=entries[i].key;
	    //if (i>0 && entries[i].val >entries[i-1].val) throw new AssertionError("After sorting, values are not in descending order! i=" +i);
	    //if(entries[i].val==0) throw new AssertionError("After sorting, zero value found! i=" +i);
	    //int gv=getValue(entries[i].key);
	    //if (entries[i].val != gv)  throw new AssertionError("After sorting, content mismatch for i=" +i +", me=" + entries[i] +", getVal="+gv);
	}
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

    public boolean topCAAHaveChangedDebug(int n, final CAAList incrementMap) {
	if (candidates==null) throw new AssertionError("This method can only be called after toCAA(n) has been called");
	pack();
	if (n>candidates.length) n=candidates.length;
	if (n==0) {
	    System.out.println("DEBUG: n=0");
	    return false;
	}
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
	pack();
	if (!(_incrementMap instanceof CAAHashMap)) throw new IllegalArgumentException();
	CAAHashMap incrementMap = (CAAHashMap)_incrementMap;
	if (n==0) return new int[0];
	
	Vector<ME> v = new Vector<ME>();
	for(int i=0;i<keysCnt;i++) {
	    ME e = new ME(keys[i], values[i]);
	    e.val += incrementMap.getValue(e.key);
	    if (e.val!=0) v.add(e);
	}

	for(Map.Entry<Integer,MutableInt> e: incrementMap.entrySet()) {
	    if (findKey(e.getKey())<0) v.add(new ME(e));
	}

	ME[] entries = (ME[])v.toArray(new ME[0]);
  	Arrays.sort(entries);
	if (entries.length < n) n = entries.length;

	/*
	for(int i=0;i<entries.length; i++) {
	    if (i>0 && entries[i].val >entries[i-1].val) throw new AssertionError("T2: After sorting, values are not in descending order! i=" +i);
	    if(entries[i].val==0) throw new AssertionError("T2: After sorting, zero value found! i=" +i);
	}
	*/

	int a[] = new int[n];
	for(int i=0; i<n; i++) a[i] = entries[i].key;
	return a;	       
    }
       

    /** Increments the values in this map as per incrementMap */
    public void add(final CAAList _incrementMap) {
	if (!(_incrementMap instanceof CAAHashMap)) throw new IllegalArgumentException();
	CAAHashMap incrementMap = (CAAHashMap)_incrementMap;
	pack();
	// FIXME: need threshold management
	if (candidates==null) 	add(new CompressedRow(incrementMap));
	else {
	    boolean drop= add1(new CompressedRow(incrementMap),false,threshold);
	    if (drop) dropCandidates();
	}
    }

    public Set<Integer> keySet() {
	pack();
	TreeSet<Integer> s=new TreeSet<Integer>();
	for(int key: keys) s.add(key);
	for(int key: ones) s.add(key);
	return s;
    }

    /** Drops the candidates[] array, to indicate that the set of "candidate 
	top values" may have changed, and needs to be recomputed */
    public void dropCandidates() {
	candidates=null; // for GC
    }

    /** The C-norm of a vector represented by ones[] */
    int maxValueInOnes() {
	Arrays.sort(ones);
	int max = 1;
	int startJ = 0;
	for(int j=0; j<onesCnt;j++) {
	    if (ones[j] == startJ) {
		int m = j-startJ+1;
		if (m > max) max = m;
	    } else {
		startJ = j;
	    }
	}
	return max;
    }

}

