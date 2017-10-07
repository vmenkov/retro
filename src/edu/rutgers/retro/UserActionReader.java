package edu.rutgers.retro;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.regex.*;
import java.text.*;

import org.json.*;

/** An auxiliary class used to read the index files created by 
     UserActionSaver */
public class  UserActionReader extends UserActionSaver {

    private void readIndexFile( File historyIndexFile ) throws IOException {
	RandomAccessFile userHistoryIndexRAF = new  RandomAccessFile(historyIndexFile,"r");
	users = new UserEntry[userNameTable.size()];
	if (users.length * (Integer.SIZE/8) != userHistoryIndexRAF.length()) throw new IllegalArgumentException("File size mismatch for " + userHistoryIndexRAF + "; should be " + users.length + "*" + (Integer.SIZE/8));
	int offset = userHistoryIndexRAF.readInt();
	for(int i=0; i< users.length; i++) {
	    int offsetNext =  (i<users.length-1)?
		userHistoryIndexRAF.readInt() : 
		(int)(userHistoryRAF.length() / (Integer.SIZE/8));
	    users[i] = new UserEntry(offsetNext-offset, offset, false);
	    offset = offsetNext;
	}
	userHistoryIndexRAF.close();
    }

    void report() throws IOException {
 	for(int i=0; i< users.length; i++) {
	    System.out.println("User["+i+"] has " + users[i].total + " actions");
	    /*
	    userHistoryRAF.seek(users[i].offset0 * (Integer.SIZE/8));
	    for(int k=0; k< users[i].total; k++) {
		int actionID = userHistoryRAF.readInt();
		ActionDetails a = actionRAF.read(new ActionDetails(), actionID);
		System.out.println("a["+actionID+"]=" + a);
	    }
	    */
	    for(Iterator<ActionDetails> it = actionsForUserIt(i); it.hasNext();) {
		ActionDetails a = it.next();
		System.out.println(a);
	    }
	}
    }

    ActionDetails[] actionsForUser(int uid)  throws IOException {
	ActionDetails[] as = new 	ActionDetails[users[uid].total];
	userHistoryRAF.seek(users[uid].offset0 * (Integer.SIZE/8));
	for(int k=0; k< as.length; k++) {
	    int actionID = userHistoryRAF.readInt();
	    as[k] = actionRAF.read(new ActionDetails(), actionID);
	}
	return as;
	
   }

    /** Iterator for reading the stored list of actions for a given user */
    Iterator<ActionDetails> actionsForUserIt(final int uid) {
	return new Iterator<ActionDetails>() {
	    int nextPtr = 0;
	    public boolean	hasNext() { return nextPtr < users[uid].total; }
	    public ActionDetails next() throws NoSuchElementException {
		try {
		    userHistoryRAF.seek((users[uid].offset0 + nextPtr) * (Integer.SIZE/8));
		    int actionID = userHistoryRAF.readInt();
		    nextPtr++;
		    return actionRAF.read(new ActionDetails(), actionID);	
		} catch (IOException ex) {
		    throw new  NoSuchElementException("IOException happened when reading action data");
		}
	    }
	    public void remove() {
		throw new UnsupportedOperationException();
	    }
	};
    }

    /**
       @param indexDir The directory where the index files are
     */
    UserActionReader(File indexDir)  throws IOException{
	super( new NameTable(new File(indexDir, "users.dat")),
	       new NameTable(new File(indexDir, "aid.dat")));

	openFiles(indexDir, "r");
	File historyIndexFile = new File(indexDir, "userHistoryIndex.dat");
	readIndexFile(historyIndexFile);
   }

	
}
