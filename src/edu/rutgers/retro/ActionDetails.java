package edu.rutgers.retro;

import java.io.*;
import java.util.*;

/** Information about one action, mapped to data file */
class ActionDetails implements Storable {
    int uid, aid, utc;
    ActionDetails() {}
    ActionDetails(int _uid, int _aid, int _utc) {
	uid = _uid;
	aid = _aid;
	utc = _utc;
    }
    /** Creates a copy of an object */
    ActionDetails(ActionDetails a) {
	this(a.uid, a.aid, a.utc);
    }
 
    public int sizeof() { return 3*(Integer.SIZE/8); }
    //	byte[] toBytes() {	}
    public void write(RandomAccessFile f) throws IOException {
	f.writeInt(uid);
	f.writeInt(aid);
	f.writeInt(utc);
    }
    public void readFrom(RandomAccessFile f) throws IOException {
	uid = f.readInt();
	aid = f.readInt();
	utc = f.readInt();
    }

    public String toString() {
	return "(user="+uid+", aid="+aid+",utc="+utc+")";
    }
    
}

