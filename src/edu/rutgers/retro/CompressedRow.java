package edu.rutgers.retro;

import java.io.*;
import java.util.*;
import org.apache.commons.lang.mutable.*;

/** A low-overhead compressed representation of a matrix row. Keys
    (column indexes) of elements are stored in ascending order. */
class CompressedRow  {   
    int keysCnt=0;
    int[] keys, values;

    void validate() {
	validate(null);
    }
    void validate(String msg) {
	Profiler.profiler.push(Profiler.Code.CRS_validate);
	if (msg==null) msg="pack() or add()";
	for(int i=0;i<keysCnt;i++) {
	    if (i>0 && keys[i]<=keys[i-1]) throw new AssertionError("Some method ("+msg+") is broken - keys are not in order in CRS!");
	    if (values[i]==0)  throw new AssertionError("Some method ("+msg+") is broken - zero value is stored in CRS!");
	    //	    if (keys[i]==0) throw new AssertionError("Suspicious zero key (i=" + i+")");
	}
	Profiler.profiler.pop(Profiler.Code.CRS_validate);
    }


    CompressedRow(int n) {
	keys=new int[n];
	values=new int[n];
	keysCnt=0;
    }

    /** Creates a CompressedRow object from an array of indexes (with the 
	implied value of 1 for each one).
     */
    CompressedRow(int [] ones, int onesCnt) {
	this(onesCnt);
	Profiler.profiler.push(Profiler.Code.CRS_pack_ones);
	Arrays.sort(ones, 0, onesCnt);
	for(int j=0; j<onesCnt;j++) {	 
	    if (keysCnt>0 && ones[j]==keys[keysCnt-1]) {
		values[keysCnt-1]++;
	    } else {
		keys[keysCnt]=ones[j];
		values[keysCnt]=1;
		keysCnt++;
	    }
	}
 	Profiler.profiler.pop(Profiler.Code.CRS_pack_ones);
   } 

    static class MEByKey extends ME {
	MEByKey( Map.Entry<Integer,MutableInt> x) {
	    super(x);
	}
	/** ascendant by key */
	public int compareTo(Object _o2) {
	    if (!(_o2 instanceof MEByKey)) throw new IllegalArgumentException();
	    MEByKey o2 = (MEByKey)_o2;
	    return key-o2.key;	
	}	
    }


    CompressedRow(CAAHashMap x) {
	this(x.size());
	MEByKey[] v = new MEByKey[x.size()];
	int j=0;
	for(Map.Entry<Integer,MutableInt> e: x.entrySet()) {
	    v[j++] = new MEByKey(e);
	}
	Arrays.sort(v);
	for(keysCnt=0; keysCnt<v.length;	    keysCnt++) {
	    keys[keysCnt] = v[keysCnt].key;
	    values[keysCnt] = v[keysCnt].val;
	}
    }


    /** this += x.
	// FIXME: need threshold management
     */
    void add(CompressedRow x) {
	//	x.validate("x");
	Profiler.profiler.push(Profiler.Code.CRS_add);
	//return add1(x, true, 0);
  	int[] newKeys = new int[keysCnt + x.keysCnt];
	int[] newValues = new int[keysCnt + x.keysCnt];
	int ia=0, ib=0, ic=0;
	for(;ia<keysCnt && ib<x.keysCnt;ic++) {
	    if ( keys[ia]< x.keys[ib] ) {
		newKeys[ic] = keys[ia];
		newValues[ic] = values[ia++];
	    } else if ( keys[ia] >  x.keys[ib] ) {		
		newKeys[ic] = x.keys[ib];
		newValues[ic] = x.values[ib++];
	    } else { // equality
		newKeys[ic] = keys[ia];
		newValues[ic] = values[ia++] + x.values[ib++];
	    }
	}
	for(; ia<keysCnt; ic++) {
	    newKeys[ic] = keys[ia];
	    newValues[ic] = values[ia++];
	}
	for(; ib<x.keysCnt; ic++) {
	    newKeys[ic] = x.keys[ib];
	    newValues[ic] = x.values[ib++];
	}
	keysCnt=ic;
	keys = newKeys;
	values=newValues;
	//	validate("CR.add()");
	Profiler.profiler.pop(Profiler.Code.CRS_add);
    }

    /** Also checked if the vector has changed so much that it may affect
	the candidate list */
    boolean add1(CompressedRow x, boolean drop, int threshold) {
	Profiler.profiler.push(Profiler.Code.CRS_add);
	int[] newKeys = new int[keysCnt + x.keysCnt];
	int[] newValues = new int[keysCnt + x.keysCnt];
	int ia=0, ib=0, ic=0;
	for(;ia<keysCnt && ib<x.keysCnt;ic++) {
	    if ( keys[ia]< x.keys[ib] ) {
		newKeys[ic] = keys[ia];
		newValues[ic] = values[ia++];
	    } else if ( keys[ia] >  x.keys[ib] ) {		
		newKeys[ic] = x.keys[ib];
		drop = drop || (x.values[ib] >= threshold);
		newValues[ic] = x.values[ib++];
	    } else { // equality
		newKeys[ic] = keys[ia];
		int val0 = values[ia];
		int val1 = values[ia++] + x.values[ib++];
		newValues[ic] = val1;
		drop = drop || (val0 < threshold) && (val1 >= threshold);
	    }
	}
	for(; ia<keysCnt; ic++) {
	    newKeys[ic] = keys[ia];
	    newValues[ic] = values[ia++];
	}
	for(; ib<x.keysCnt; ic++) {
	    newKeys[ic] = x.keys[ib];
	    drop = drop || (x.values[ib] >= threshold);
	    newValues[ic] = x.values[ib++];
	}
	keysCnt=ic;
	keys = newKeys;
	values=newValues;
	//	validate("pack()");
	Profiler.profiler.pop(Profiler.Code.CRS_add);
	return drop;
     }

    int findKey(int key) {
 	Profiler.profiler.push(Profiler.Code.CRS_find);
	int k= Arrays.binarySearch(keys, 0, keysCnt, key);
 	Profiler.profiler.pop(Profiler.Code.CRS_find);
	return k; 
    }


    /** nnz */
    int size() { return keysCnt; }

    /** Converts this object to an array of ME objects, for sorting */
    ME[] toME() {
	ME[]  entries = new ME[size()];
	for(int i=0;i<keysCnt;i++) {
	    //if (values[i]==0) throw new AssertionError("Zero found in CRS, i=" + i+", key=" + keys[i]);
	    entries[i] = new ME(keys[i], values[i]);
	}
	return entries;
    }

    /** Does this vector have a component with a value at or above
	the specified threshold?
    */
    boolean reachesThreshold(int threshold) {
	for(int i=0;i<keysCnt;i++) {
	    if (values[i]>=threshold) return true;
	}
	return false;
    }

}
