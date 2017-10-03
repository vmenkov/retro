package edu.rutgers.retro;
import java.io.*;

/*
import java.util.*;
import java.util.zip.*;
import java.util.regex.*;
import java.text.*;
*/

public interface Storable {
     int sizeof();
    /** Converts the data to an array of bytes */
    //byte[] toBytes();
    /** Initializes this object from an array of bytes */
    //void fill(byte[]);
    void write(RandomAccessFile f)  throws IOException;
    void readFrom(RandomAccessFile f) throws IOException;
    //default void readFrom(RandomAccessFile f, int posN) throws IOException {}
}
