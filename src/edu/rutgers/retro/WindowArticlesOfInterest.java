package edu.rutgers.retro;

import java.io.*;
import java.util.*;

/** Used in incremental coacces computation, to indicate that this user has had some actions involving this run's articles of interest. Contains the list of articles of interest the user has seen so far, or null if none has been seen yet. */
class WindowArticlesOfInterest  implements ArticlesOfInterest {
    final int windowSec;
    ArrayDeque<Integer> utcs = new ArrayDeque<Integer>(4);
    ArrayDeque<Integer> aids =new ArrayDeque<Integer>(4);

    WindowArticlesOfInterest(int _windowSec) { 
	windowSec = _windowSec;
    }
    public boolean isEmpty() {
	return aids.isEmpty();
    }

    /** List articles that this user has viewed no more than windowSec sec ago.
	Removes "obsolete" elements (older than windowSec sec).
	@param utc Current action's time stamp (in seconds)
     */
    public Collection<Integer> listArticles(int utc) { 
	while(!utcs.isEmpty() && utcs.peekFirst() < utc - windowSec) {
	    utcs.pop();
	    aids.pop();
	}

	return aids; 
    }
    public void addAction(ActionDetails ad) {	
	if (!utcs.isEmpty() &&  utcs.peekLast() > ad.utc) throw new IllegalArgumentException("Action inserted not it time-ascending order: " + ad);
	aids.add(ad.aid);
	utcs.add(ad.utc);
    }
}
