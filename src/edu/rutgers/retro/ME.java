 package edu.rutgers.retro;

import java.io.*;
import java.util.*;

import org.apache.commons.lang.mutable.*;

/** An auxiliary class used for sorting coaccess values from CAAHashMap. */
class ME implements Comparable { 
    int key, val;
    ME(int _key, int _val) {
	key=_key;
	val=_val;
    }
    ME( Map.Entry<Integer,MutableInt> x) {
	this(x.getKey(), x.getValue().intValue());
    }
    /** Descendant by value; tie-breaking: ascendant by key */
    public int compareTo(Object _o2) {
	if (!(_o2 instanceof ME)) throw new IllegalArgumentException();
	ME o2 = (ME)_o2;
	int d = o2.val - val;
	if (d==0) d=key-o2.key;
	return d;		
    }
    public String toString() {
	return "("+key+","+val+")";
    }
}

