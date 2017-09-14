package edu.rutgers.retro;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.text.*;

/** A tool to find usage files (whose names are date-based) within a specified
    directory tree, possibly subject to a date range restriction.
    Works with directory structures that look like this:
    <pre>
     ls /data/json/usage/
     2003/  2010/  2014/  2015/  2017/  tc1.json.gz  ...
     
     ls /data/json/usage/2017
     170902_usage.json.gz  ...
     </pre>
*/
public class ListFiles {
    /** YYMMDD */
    private static final Pattern datePattern6 = Pattern.compile("[0-9][0-9][0-9][0-9][0-9][0-9]");
   /** Pattern for file names: YYMMDD_usage */
    private static final Pattern fnamePattern6 = Pattern.compile("[0-9][0-9][0-9][0-9][0-9][0-9]_usage");
 
    /** Pattern for subdirectory names: YYYY */
  private static final Pattern yearPattern4 = Pattern.compile("[0-9][0-9][0-9][0-9]");

    /** Optional restrictions for the date range, in the YYMMDD
	format. A null value means the absence of a restriction in
	this direction. */
    private String usageFrom, usageTo;
    File root;
    /** 
	@param _root The root of the directory tree within which files will be listed.
	@param _usageFrom Either YYMMDD or null
     */
    ListFiles(	String _usageFrom, String _usageTo, File _root) {
	usageFrom =_usageFrom;
	usageTo = _usageTo;
	root = _root;
    }

    static DecimalFormat fmtD4 = new DecimalFormat("0000");
    
    /** @param Dir name, in the format '20YY' */
    private boolean acceptYearDir(String name) {
	int y = Integer.parseInt(name) + 1; // next year
	String nextYearName = fmtD4.format(y);
	
	if (usageFrom != null && nextYearName.compareTo("20" + usageFrom)<0) return false;
	if (usageTo != null && name.compareTo("20" + usageTo)>0) return false;
	return true;
    }

    /** Returns true if the file name describes a date, and the date is within
	the selector's range.
	@param name A file name, in the form "YYMMDD_usage....." */
    private boolean acceptDay(String name) {
	Matcher dm = fnamePattern6.matcher(name);
	if (!dm.find() || dm.start()>0) return false;
	String yymmdd  = name.substring(0,6);
 	if (usageFrom != null && yymmdd.compareTo(usageFrom)<0) return false;
	if (usageTo != null && yymmdd.compareTo(usageTo)>=0) return false;
	return true;
   }

    /** Obtains a sorted list of "acceptable" files from a single year's
	directory. No recursion into subdirs is done here, as at this point
	all data files are supposed to be on the same level.
    */
    private Vector<File> filesFromYearDir(File dir) {
	String[] fnames = dir.list();
	Arrays.sort(fnames);
	Vector<File> v = new Vector<File>();
	for(String q: fnames) {
	    File f = new File(dir, q);
	    if (f.isFile()) {
		if (acceptDay(q))  {
		    v.add(f);
		}		    
	    }
	}	
	return v;
    } 

    /** Recursively creates the list of daily usage files from a specified
	directory tree, subject to date restriction. Recursion won't go
	deeper than the yearly directories, though.
     */
    private Vector<File> filesFromDir(File dir) {
	String name=dir.getName();
	Matcher ym = yearPattern4.matcher(name);
	if (ym.matches()) {
	    // dir=YYYY ; list files in this dir, w/o further recursion
	    return  acceptYearDir(name) ? filesFromYearDir(dir) :
		new Vector<File>();
	} else { // any other dir name; we recurse here
	    String[] fnames = dir.list();
	    Arrays.sort(fnames);
	    Vector<File> v = new Vector<File>();
	    for(String q: fnames) {
		File f = new File(dir, q);
		if (f.isFile()) {
		    if (acceptDay(q))  {
			v.add(f);
		    }		    
		} else if (f.isDirectory()) {
		    v.addAll(filesFromDir(f));
		}
	    }
	    return v;
	}
    }

    File[] list() {	
	if (root.isFile()) {
	    return new File[] {root};
	} else if (root.isDirectory()) {
	    Vector<File> v = filesFromDir(root);
	    return (File[])v.toArray(new File[0]);
	} else {
	    throw new IllegalArgumentException("What kind of file is " + root + " ?!");
	}

    }

}
  
  
