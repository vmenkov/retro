package edu.rutgers.retro;


import java.io.*;
import java.util.*;


public class RAF<T extends Storable> extends ObjectRandomAccessFile  {
      RAF(File file, String mode, T dummy) throws IOException {
	super(file, mode, dummy.sizeof());
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
 

}
