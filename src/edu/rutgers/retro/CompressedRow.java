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
	for(int i=1;i<keysCnt;i++) {
	    if (keys[i]<=keys[i-1]) throw new AssertionError("pack() or add() is broken - keys are not in order!");
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


    CompressedRow(CAAHashMap x) {
	this(x.size());

	CAAHashMap.MEByKey[] v = new CAAHashMap.MEByKey[x.size()];
	int j=0;
	for(Map.Entry<Integer,MutableInt> e: x.entrySet()) {
	    v[j++] = new CAAHashMap.MEByKey(e);
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
		newValues[ic] = values[ia];
		ia++;
		ic++;
	    } else if ( keys[ia] >  x.keys[ib] ) {
		newKeys[ic] = x.keys[ib];
		newValues[ic] = x.values[ib];
		ib++;
		ic++;
	    } else { // equality
		newKeys[ic] = keys[ia];
		newValues[ic] = values[ia] + x.values[ib];
		ia++;
		ib++;
		ic++;
	    }
	}
	while(ia<keysCnt) {
	    newKeys[ic] = keys[ia];
	    newValues[ic] = values[ia];
	    ia++;
	    ic++;
	}
	while( ib<x.keysCnt) {
	    newKeys[ic] = x.keys[ib];
	    newValues[ic] = x.values[ib];
	    ib++;
	    ic++;
	}
	keysCnt=ic;
	keys = newKeys;
	values=newValues;
	validate();
    }

    int findKey(int key) {
	return Arrays.binarySearch(keys, key);
    }


    /** nnz */
    int size() { return keysCnt; }

}
