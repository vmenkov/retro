package edu.rutgers.retro;


import java.io.*;
import java.util.*;
/*
import java.util.zip.*;
import java.util.regex.*;
import java.text.*;
*/

public class RAF<T extends Storable> extends RandomAccessFile {
    /** Size of one storable object, in bytes */
    final int sizeof;
    RAF(File file, String mode, T dummy) throws IOException {
	super(file, mode);
	sizeof = dummy.sizeof();
    }
    
    void seekObject(long n) throws IOException {
	seek(n * sizeof);
    }

    void store(T x) throws IOException  {
	x.write(this);
    }
    void store(T x, long posN) throws IOException  {
	seekObject(posN);
	x.write(this);
    }

    T read(T blank) throws IOException  {
	blank.readFrom(this);
	return blank;
    }
    T read(T blank, long posObject) throws IOException  {
	seekObject(posObject);
	return read(blank);
    }

    void seekToEnd()  throws IOException {
	seek(length());
    }

}
