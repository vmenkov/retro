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
    
    void seekN(long n) throws IOException {
	seek(n * sizeof);
    }

    void store(T x) throws IOException  {
	x.write(this);
    }
    void store(T x, long posN) throws IOException  {
	seekN(posN);
	x.write(this);
    }

    T read(T blank) throws IOException  {
	blank.readFrom(this);
	return blank;
    }
    T read(T blank, long posN) throws IOException  {
	seekN(posN);
	blank.readFrom(this);
	return blank;
    }

    void seekToEnd()  throws IOException {
	seek(length());
    }

}
