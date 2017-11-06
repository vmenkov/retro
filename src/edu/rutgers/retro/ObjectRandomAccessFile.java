package edu.rutgers.retro;


import java.io.*;
import java.util.*;

/** A RandomAccessFile which is aware of the size of objects stored in it */
public class ObjectRandomAccessFile extends RandomAccessFile {
    /** Size of one storable object, in bytes */
    final int sizeof;
    ObjectRandomAccessFile(File file, String mode, int _sizeof) throws IOException {
	super(file, mode);
	sizeof = _sizeof;
    }
    
    /** Positions the cursor.
	@param Offset, in terms of objects (rather than bytes) */
     final void seekObject(long n) throws IOException {
	seek(n * sizeof);
    }

    final void seekToEnd()  throws IOException {
	seek(length());
    }

  /** @return File length, measured in units of object size (rather than in bytes) */
    final long lengthObject()  throws IOException {
	long len = length();
	if (len % sizeof != 0) throw new IllegalArgumentException("File size is not a multiple of object size (" + sizeof +")");
	return len / sizeof;
    }

    /** Like super.setLength, 
	@param n New file length, measured in units of object size, rather than in bytes
     */
    final void setLengthObject(long n) throws IOException {
	setLength( n*sizeof);
    }
 

    /*
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
    */

}
