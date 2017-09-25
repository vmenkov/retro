package edu.rutgers.retro;

import java.io.*;
import java.util.*;
import java.text.*;

/** A persistent table that can be used e.g. to store a list of user name or
    list of article IDs */

class NameTable extends HashMap<String, Integer> {
    private Vector<String> names = new Vector<String>();
    synchronized
	void addIfNew(String x) {
	    if (!containsKey(x)) add(x);
	}
    synchronized
	private void add(String x) {
	    if (containsKey(x)) throw new IllegalArgumentException("Table already contains key: " +x);
	    if (x.indexOf('\n')>=0) throw new IllegalArgumentException("Illegal name (contains a line break) : " + x);
	    names.add(x);
	    put(x, names.size()-1);
	}
    void save(File f) throws IOException {
	PrintWriter w = new PrintWriter(new FileWriter(f));
	for(String x: names) {
	    w.println(x);
	}
	w.close();
    }
    NameTable(File f) throws IOException { 
	LineNumberReader r =  new LineNumberReader(new FileReader(f));
	String s;
	while((s=r.readLine())!=null) add(s);
	r.close();
    }
}
