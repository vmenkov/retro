package edu.rutgers.retro;

import java.io.*;
import java.util.*;

/** An auxiliary utility for converting a binary file of int values
  into an equivalent file of long values. The output file will be
  twice the size of the input file. */

public class ConvertFile {
    
    public static void main(String argv[]) throws IOException {
	File inFile=new File(argv[0]), outFile=new File(argv[1]);
	ObjectRandomAccessFile inRAF=new ObjectRandomAccessFile(inFile,"r", Integer.SIZE/8);
	ObjectRandomAccessFile outRAF=new ObjectRandomAccessFile(outFile,"rw", Long.SIZE/8);
	outRAF.setLength(0);
	long len = inRAF.lengthObject();
	for(long j=0; j<len; j++) {
	    long x = inRAF.readInt();
	    outRAF.writeLong(x);
	}

	inRAF.close();
	outRAF.close();
	
    }

}
