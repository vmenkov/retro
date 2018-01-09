package edu.rutgers.retro;

import java.io.*;
import java.util.*;

/** Used in incremental coacces computation, to indicate that this user has had some actions involving this run's articles of interest. Contains the list of articles of interest the user has seen so far, or null if none has been seen yet. */
class BasicArticlesOfInterest extends Vector<Integer> implements ArticlesOfInterest {
    BasicArticlesOfInterest() { super(2,4); }
    public Collection<Integer> listArticles(int utc) { return this; }
    public void addAction(ActionDetails ad) {
	add(ad.aid);
    }
}
