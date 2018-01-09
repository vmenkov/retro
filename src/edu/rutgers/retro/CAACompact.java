package edu.rutgers.retro;

import java.io.*;
import java.util.*;

import org.apache.commons.lang.mutable.*;

/** Sparse implementation of CAAList: storing one row of the coaccess matrix */
class CAACompact extends CompressedRow   implements CAAList {
    
    /** The usual constructor */
    CAACompact() { 
	super(0); 
	fixedStructure = false;
    }

    /** Reads the structure from index file, if available. The values are
	initialized with zeros.
	@param fi Access to data files (which should be already opened)
     */
    CAACompact(PredictStructure.IndexFiles fi, int j) { 
	super(0); 
	boolean found=false;
	try {
	    keys = fi.readRow(j);
	    values = new int[keysCnt=keys.length];
	    found = true;
	    ones=new int[0]; // won't be needed anymore
	} catch(IOException ex) {}
	fixedStructure = found;
    }

    /** If this flag is true, the vector's structure (positions where
	non-zero values are allowed), precomputed by PredictStructure,
	has been read in from the structure files in the constructor. 
	After that, the structure won't change, and new values added 
	outside of the allowed positions will be discarded, because
	we know that these positions will never enter the top n
	(or even the candidate list for the top n).  This means that if
	fixedStructure=true, the array ones[] won't be used at all,
	and onesCnt will stay 0.
    */
    final boolean fixedStructure;
    /** The number of actually used elements in ones[]. */
    int onesCnt=0;
    /** The keys of newly added values, still not incorporated into
      keys[] and values[]. These keys are not orderd. The values are
       all 1s, and are not explicitly stored.    */
    int[] ones = new int[100];

    /** Used in debugging */
    void validate(String msg) {
	super.validate(msg);
    }

  
    /** Moves all data from ones[] into the main CRS structure. Also
	tests if any of the recently added values is high enough to
	potentially enter the candidate array. */
    private void pack() {
	if (onesCnt==0) return;
	CompressedRow x =  new CompressedRow(ones, onesCnt);	
	add(x);	
	onesCnt=0;
	if (hasCandidates &&  x.reachesThreshold(threshold)) dropCandidates();
	//	validate("PACK3");
    }

    /** Trivial wrapper here; will be overridden in CAACompact2 */
    void dropCandidates2(int because) {
	dropCandidates();
    }


    /** Adds a single-component vector (x[j]=inc) to this vector. Either 
	increments an existing component in CRS, or adds an element to ones[]
	(unless fixedStructure is in effect).
     */
    public void addValue(final int j, int inc) {
	
	int k = findKey(j);
	if (k>=0) {
	    int val0 = values[k];
	    values[k] += inc;
	    if (hasCandidates && val0 < threshold && values[k]>=threshold) dropCandidates2(j);
	} else if (fixedStructure) {
	    // just ignore an out-of-structure element!
	} else if (keysCnt<keys.length && j>keys[keysCnt-1]) {
	    keys[keysCnt]=j;
	    values[keysCnt]=inc;
	    if (hasCandidates && values[keysCnt]>=threshold) dropCandidates2(j);
	    keysCnt++;
	} else if (inc!=1) {
	    throw new IllegalArgumentException("addValue() only supported with inc=1");
	} else {
	    if (onesCnt == ones.length) pack();
	    ones[onesCnt++] = j;
	}
	//	validate("Addvalue");
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
	call; it will contain the indexes for the top n values (in the
	order of descending values), as well as those whose values are
	within 1 from those in the top n.  When we study the effect of
	a small (up to 1) decrement on the top values, it is
	sufficient to compare the candidates[], because only articles
	listed in that array may enter the top n according to the
	modified scores.
     */
    int[] candidates=null;
    boolean hasCandidates=false;
    /** The candidate array should include all indexes whose values are &ge; threshold */
    int threshold=1;

    /** The number of "occupied spaces". If fixedStructure is in effect,
	this is simply the size of this fixed structure; otherwise,
	it is &ge; the actual number of non-zeros.
     */
    public int size() { return keysCnt + onesCnt; }


    /** Returns the n article IDs with the highest counts (coaccess
	values). For tie breaking, articles' internal IDs are used. 
    */	
    public int[] topCAA(int n) {
	if (n==0) return new int[0];
	Profiler.profiler.push(Profiler.Code.COA_top);
	final int n0 = n;

	// Check if the next pack() would cause candidate change
	if (hasCandidates && maxValueInOnes()>=threshold) {
	    dropCandidates();
	}

	int ot = threshold;
	ME[] entries;
	int ecnt = 0;
	if (candidates==null) {
	    pack();
	    entries = new ME[size()];
	    for(int i=0;i<keysCnt;i++) {
		if (values[i] >= threshold) entries[ecnt++] = new ME(keys[i], values[i]);
	    }
	} else {
	    entries = new ME[candidates.length];
	    for(int i=0; i<candidates.length; i++) {
		int key = candidates[i];
		entries[ecnt++] = new ME(key, getValue(key));
	    }
	}
	// descendant sort by value; key (ascendant) used for tie-breaking
	Arrays.sort(entries, 0, ecnt);
	int m;
	if (ecnt < n) { // save them all. 
	    m = n = ecnt;
	    threshold = 1; // any new component will become a new candidate!
	} else {   // How many candidates do we need to save?
	    m = n;
	    threshold = entries[n-1].val-1;
	    if (threshold==0) threshold = 1;
	    while(m < ecnt && entries[m].val >= threshold) m++;
	}
	//	if (threshold != ot) System.out.println("Threshold changed: " + ot + " to " + threshold + " (|cc|="+m+")");
	//	if (threshold < ot) {
	//	    System.out.print("Threshold dropped!? Entries = ");
	//	    for( int i=0;i<m; i++) System.out.print(" " +  entries[i]);
	//System.out.println();
	//}

	hasCandidates=true;
	candidates=new int[m];
	for(int i=0;i<candidates.length; i++) {
	    candidates[i]=entries[i].key;
	    //if (i>0 && entries[i].val >entries[i-1].val) throw new AssertionError("After sorting, values are not in descending order! i=" +i);
	    //if(entries[i].val==0) throw new AssertionError("After sorting, zero value found! i=" +i);
	    //int gv=getValue(entries[i].key);
	    //if (entries[i].val != gv)  throw new AssertionError("After sorting, content mismatch for i=" +i +", me=" + entries[i] +", getVal="+gv);
	}
	int[] q = Arrays.copyOf(candidates,n);
	Profiler.profiler.pop(Profiler.Code.COA_top);
	return q; 
    }

    /** Comparese the list of top n elements of this coaccess matrix row
	with the list that would obtain if  incrementMap were to be 
	added to it. incrementMap consists of negative value (representing
	NOT including a particular action which is included in this 
	row), which means that the modified row will have fewer non-zeros,
	and smaller positive values of non-zeros, than the non-modified row.

	@param n the top-n lists are being compared
	@param incrementMap The only expected increment values are -1, so we know that the "incremented" values are &le; than the original ones.
	@param cutoff Only documents with scores &ge; cutoff  are included into the top-n vector. Docs with lower scores are "invisible". If cutoff=1, no documents are ignored due to low scores
	@return A structure that indicates the (zero-based) positions
	and IDs of all "visibly promoted" articles. in the rec list.
     */
    public PromotedArticles topCaaChanges(int n, final CAAList incrementMap, int cutoff) {
	if (cutoff < 1) throw new IllegalArgumentException("Expect cutoff>=1");
	if (!hasCandidates) throw new AssertionError("This method can only be called after toCAA(n) has been called");
	Profiler.profiler.push(Profiler.Code.COA_check);

	PromotedArticles result= new PromotedArticles(n);

	try {
	    pack(); // FIXME: what if pack() drops candidates[]?
	    if (n>candidates.length) n=candidates.length;
	    if (n==0) return result;
	    int xlast=0, ilast=0;
	    boolean haveLast = false;
	    for(int i=candidates.length-1; i>=0; i--) {		
		int x= getValue(candidates[i]);
		if (x < cutoff) continue;	
		x += incrementMap.getValue(candidates[i]);

		boolean newHigh = !haveLast ||
		    x>xlast || x==xlast && candidates[i]<candidates[ilast];

		if (i<n) {
		    if (x<cutoff || !newHigh) {
			result.add( i, candidates[i]);
		    } 
		}

		if (newHigh) {
		    xlast = x;
		    ilast = i;
		    haveLast = true;
		}
	    }
	    return result;
	} finally {
	    Profiler.profiler.pop(Profiler.Code.COA_check);
	}
    }

   /** Comparese the list of top n elements of this coaccess matrix row
	with the list that would obtain if  incrementMap were to be 
	added to it. incrementMap consists of negative value (representing
	NOT including a particular action which is included in this 
	row), which means that the modified row will have fewer non-zeros,
	and smaller positive values of non-zeros, than the non-modified row.

	@param n the top-n lists are being compared
	@param incrementMap The only expected increment values are -1, so we know that the "incremented" values are &le; than the original ones.
	@param cutoff Only documents with scores &ge; cutoff  are included into the top-n vector. Docs with lower scores are "invisible". If cutoff=1, no documents are ignored due to low scores
	@return the (zero-based) position of the first changed element,
	or -1 if there are no changes within the top 10 elements.
     */
  public int topCaaHaveChanged(int n, final CAAList incrementMap, int cutoff) {
	if (cutoff < 1) throw new IllegalArgumentException("Expect cutoff>=1");
	if (!hasCandidates) throw new AssertionError("This method can only be called after toCAA(n) has been called");
	Profiler.profiler.push(Profiler.Code.COA_check);

	try {
	    pack(); // FIXME: what if pack() drops candidates[]?
	    if (n>candidates.length) n=candidates.length;
	    if (n==0) return -1;
	    int last=0, ilast=0;
	    for(int i=0; i<candidates.length; i++) {		
		int x= getValue(candidates[i]);
		if (x < cutoff) break;
		x += incrementMap.getValue(candidates[i]);
		if (i<n && x<cutoff) {
		    return i;
		}
		if (i>0) {
		    if (x>last) return ilast;
		    if (x==last && candidates[i]<candidates[ilast]) return ilast;
		}
		if (i<n) {
		    last = x;
		    ilast = i;
		}
	    }
	    return -1;
	} finally {
	    Profiler.profiler.pop(Profiler.Code.COA_check);
	}
    }


    public boolean topCAAHaveChangedDebug(int n, final CAAList incrementMap) {
	if (!hasCandidates) throw new AssertionError("This method can only be called after toCAA(n) has been called");
	pack();
	if (n>candidates.length) n=candidates.length;
	if (n==0) {
	    //	    System.out.println("DEBUG: n=0");
	    return false;
	}
	int last=0, ilast=0;
	for(int i=0; i<candidates.length; i++) {
	    int x= getValue(candidates[i])+incrementMap.getValue(candidates[i]);
	    if (i<n && x==0) {
		return true;
	    }
	    if (i>0) {
		if (x>last) {
		    return true;
		}
		if (x==last && candidates[i]<candidates[ilast])  {
		    return true;
		}
	    }
	    if (i<n) {
		last = x;
		ilast = i;
	    }
	}
	//	System.out.println("DEBUG: NO CHANGE");
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
	Profiler.profiler.push(Profiler.Code.COA_top);
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


	int a[] = new int[n];
	for(int i=0; i<n; i++) a[i] = entries[i].key;
	Profiler.profiler.pop(Profiler.Code.COA_top);
	return a;	       
    }
       

    /** Increments the values in this map as per incrementMap */
    public void add(final CAAList _incrementMap) {
	if (!(_incrementMap instanceof CAAHashMap)) throw new IllegalArgumentException();
	CAAHashMap incrementMap = (CAAHashMap)_incrementMap;
	pack();
	boolean drop=!hasCandidates;
	if (fixedStructure) {
	    drop= add1fixed(new CompressedRow(incrementMap),drop,threshold);
	} else {
	    if (!hasCandidates) 	add(new CompressedRow(incrementMap));
	    else {
		drop= add1(new CompressedRow(incrementMap),drop,threshold);
	    }
	}
	if (hasCandidates && drop) dropCandidates();

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
	candidates=null;
	hasCandidates=false;
    }

    /** The C-norm of a vector represented by ones[] */
    int maxValueInOnes() {
	if (onesCnt==0) return 0;
	Arrays.sort(ones, 0, onesCnt);
	int max = 0;
	int startJ = 0;
	for(int j=0; j<onesCnt;j++) {
	    if (ones[j] == ones[startJ]) {
		int m = j-startJ+1;
		if (m > max) max = m;
	    } else {
		startJ = j;
	    }
	}
	return max;
    }

}

