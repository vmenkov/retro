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
	if (msg==null) msg="pack() or add()";
	for(int i=0;i<keysCnt;i++) {
	    if (i>0 && keys[i]<=keys[i-1]) throw new AssertionError("Some method ("+msg+") is broken - keys are not in order in CRS!");
	    if (values[i]==0)  throw new AssertionError("Some method ("+msg+") is broken - zero value is stored in CRS!");
	}
    }


    CompressedRow(int n) {
	keys=new int[n];
	values=new int[n];
	keysCnt=0;
    }

    CompressedRow(int [] ones, int onesCnt) {
	this(onesCnt);
	Arrays.sort(ones);
	for(int j=0; j<onesCnt;j++) {
	    if (keysCnt>0 && ones[j]==keys[keysCnt-1]) {
		values[keysCnt-1]++;
	    } else {
		keys[keysCnt]=ones[j];
		values[keysCnt]=1;
		keysCnt++;
	    }
	}
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


    /** this += x */
    void add(CompressedRow x) {
	int[] newKeys = new int[keysCnt + x.keysCnt];
	int[] newValues = new int[keysCnt + x.keysCnt];
	int ia=0, ib=0, ic=0;
	while(ia<keysCnt && ib<x.keysCnt) {
	    if ( keys[ia]< x.keys[ib] ) {
		newKeys[ic] = keys[ia];
		newValues[ic++] = values[ia++];
	    } else if ( keys[ia] >  x.keys[ib] ) {
		newKeys[ic] = x.keys[ib];
		newValues[ic++] = x.values[ib++];
	    } else { // equality
		newKeys[ic] = keys[ia];
		newValues[ic++] = values[ia++] + x.values[ib++];
	    }
	}
	while(ia<keysCnt) {
	    newKeys[ic] = keys[ia];
	    newValues[ic++] = values[ia++];
	}
	while( ib<x.keysCnt) {
	    newKeys[ic] = x.keys[ib];
	    newValues[ic++] = x.values[ib++];
	}
	keysCnt=ic;
	keys = newKeys;
	values=newValues;
	validate("pack()");
    }

    int findKey(int key) {
	return Arrays.binarySearch(keys, 0, keysCnt, key);
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

}
