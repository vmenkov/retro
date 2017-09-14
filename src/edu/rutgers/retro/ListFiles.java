package edu.rutgers.retro;

import java.io.*;
import java.util.*;
import java.util.regex.*;

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
   /** YYMMDD_usage */
    private static final Pattern fnamePattern6 = Pattern.compile("[0-9][0-9][0-9][0-9][0-9][0-9]_usage");
 
    /** YYYY */
  private static final Pattern yearPattern4 = Pattern.compile("[0-9][0-9][0-9][0-9]");

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

    /** @param Dir name, in the format '20YY' */
    private boolean acceptYearDir(String name) {
	if (usageFrom != null && name.compareTo("20" + usageFrom)<0) return false;
	if (usageTo != null && name.compareTo("20" + usageTo)>0) return false;
	return true;
    }

    /** Returns true if the name describes a date, and the date is within
	the selector's range.
	@param name YYMMDD_usage.... */
    private boolean acceptDay(String name) {
	Matcher dm = fnamePattern6.matcher(name);
	if (!dm.find() || dm.start()>0) return false;
	String yymmdd  = name.substring(0,6);
 	if (usageFrom != null && yymmdd.compareTo(usageFrom)<0) return false;
	if (usageTo != null && yymmdd.compareTo(usageTo)>=0) return false;
	return true;
   }

    private Vector<File> filesFromYearDir(File dir) {
	String[] fnames = dir.list();
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

    private Vector<File> filesFromDir(File dir) {
	String name=dir.getName();
	Matcher ym = yearPattern4.matcher(name);
	if (ym.matches()) {  // dir=YYYY
	    return  acceptYearDir(name) ? filesFromYearDir(dir) :
		new Vector<File>();
	} else { // any other dir name; we recurse here
	    String[] fnames = dir.list();
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
  
  
