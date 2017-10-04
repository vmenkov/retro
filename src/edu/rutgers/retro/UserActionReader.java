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
	    userHistoryRAF.seek(users[i].offset0 * (Integer.SIZE/8));
	    for(int k=0; k< users[i].total; k++) {
		int actionID = userHistoryRAF.readInt();
		ActionDetails a = actionRAF.read(new ActionDetails(), actionID);
		System.out.println("a["+actionID+"]=" + a);
	    }
	}
    }


    UserActionReader(NameTable _userNameTable, 
		     NameTable _aidNameTable,
		     File indexDir)  throws IOException{
	super(_userNameTable, _aidNameTable);
	openFiles(indexDir, "r");
	File historyIndexFile = new File(indexDir, "userHistoryIndex.dat");
	readIndexFile(historyIndexFile);
   }

	
}
