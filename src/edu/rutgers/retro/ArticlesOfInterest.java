package edu.rutgers.retro;

import java.io.*;
import java.util.*;

/** A an  ArticlesOfInterest object describes the list of articles viewed so far by a particular user, which are still of interest for the coaccess computation. There are 2 concrete classes implementing this interface. The BasicArticlesOfInterest stores all viewed articles permanently (there is no time window), while he WindowArticlesOfInterest removes articles after a specified age, so that they won't be used for more coaccess pairs anymore.
 */
interface ArticlesOfInterest {
    /** The value of windowSec for which a BasicArticleOfInterest should be created */
    final int BASIC = -1;

    boolean isEmpty();
    Collection<Integer> listArticles(int utc);
    void addAction(ActionDetails ad);
}
