package edu.rutgers.retro;

import java.io.*;
import java.util.*;


/** This application is used to find out which elements of the
    coaccess matrix need to be kept. The structure it creates is an
    index describing the position of all matrix elements that need to
    be kept during the coaccess matrix "pseudo-real time" computation
    because at some time during the system's life they may be
    within the top n elements, or may be "candidates" for the top n.
*/
public class PredictStructure extends Coaccess {

     PredictStructure(UserActionReader _uar, Vector<Integer> _articles, int _n) {
	 super(_uar, _articles,  new Vector<Integer>(), _n);
	 for(Integer aid: articles) {
	     aSet.put(aid, new CAACompact2());
	 }
	 for(Integer uid: usersToTest) {
	    utSet.put(uid, new PrivacyLog());
	 }
     }    

    void predictStructure(boolean willWrite) throws IOException {
	final int stepSec = 3600 * 24 * 7;
	int nextPrintUtc = (uar.actionRAF.read(new ActionDetails(),0).utc/stepSec) * stepSec;

	System.out.println("Immediate-update recommender starts; CA nnz=" + mapSize());

	final int len = (int)uar.actionRAF.lengthObject();
	ActionDetails a = new ActionDetails();

	for(int pos = 0; pos<len; pos++) { // for all actions, ever

	    uar.actionRAF.read(a,pos); 
	    // the user who carried out this action
	    UserActionReader.UserEntry user = uar.users[a.uid];
	    CAACompact2 caa = (CAACompact2)aSet.get(a.aid); 
	    user.enableOfInterest(ArticlesOfInterest.BASIC);
	    if (!user.ofInterest.isEmpty()) {  // update CAA for the articles of interest seen earlier by this user
		for(int j: user.ofInterest.listArticles(a.utc)) {
		    CAACompact2 caz = (CAACompact2)aSet.get(j);
		    caz.addValue(a.aid, 1);
		    if (!caz.hasCandidates) caz.topCAA(n);
		}
	    }

	    if (caa!=null) { // this is an article of interest
		user.ofInterest.addAction(a);

		ActionDetails[] as = uar.earlyActionsForUser(a.uid);
		for(ActionDetails y: as) {	    
		    caa.addValue(y.aid, 1);
		}
		if (!caa.hasCandidates) caa.topCAA(n);
	    }
	    user.readCnt++;

	    if (a.utc > nextPrintUtc) {
		System.out.println("At t=" + a.utc +" ("+new Date((long)a.utc*1000L)+"); CA nnz=" + mapSize());
		nextPrintUtc += stepSec;
	    }

	}

	reportStructure(false);
	if (willWrite) {
	    IndexFiles fi = new IndexFiles(indexDir);
	    fi.writeIndex(this);
	}

    }

    /** Prints the top coaccess values for all articles of interest. */
    void reportStructure(boolean detailed)     {
	int sumKept=0, sumAll=0;
	for(int aid: articles) {
	    CAACompact2 caa = (CAACompact2)aSet.get(aid);
	    int[] tops = caa.topCAA(n);
	    System.out.println("Top CAA for A["+aid+"]=" + uar.aidNameTable.nameAt(aid) + " ("+caa.size()+") are:");
	    System.out.println( topToString(caa, tops));
	    System.out.print("Keep " + caa.allTimeCandidates.size() + " candidates out of "+caa.size());
	    sumKept += caa.allTimeCandidates.size();
	    sumAll += caa.size();
	    if (detailed) {
		System.out.print(":");
		for(int k: caa.allTimeCandidates) {
		    System.out.print(" " + k + ":" +  uar.aidNameTable.nameAt(k) +
				     ":" + caa.getValue(k));
		}
	    }
	    System.out.println();
	}
	System.out.println("For all " + articles.size() + " articles, keep " + sumKept + " coaccess matrix elements out of " + sumAll);
    }

    /** Tools for reading and writing to the structure index files */
    static class IndexFiles {
	/** Index file location */
	File structureFile, structureIndexFile;
	/** @param outdir The directory where the index files are (or will be) */
	IndexFiles(File outdir) {
	    structureFile = new File(outdir, "structure.dat");
	    structureIndexFile = new File(outdir, "structureIndex.dat");
	}
	/** Do both files exist already? */
	boolean indexExists()  throws IOException {
	    return structureFile.exists() && structureIndexFile.exists();
	}
    
	ObjectRandomAccessFile structureRAF, 	structureIndexRAF;
   
	/** Opens the random access files for the structure index */
	void openFiles(String mode) throws IOException {
	    structureRAF=new ObjectRandomAccessFile(structureFile,mode, Integer.SIZE/8);
	    
	    structureIndexRAF=new ObjectRandomAccessFile(structureIndexFile,mode, Long.SIZE/8);
	}

	void closeFiles() throws IOException {
	    structureRAF.close();
	    structureIndexRAF.close();
	}

 
	/** Checks if the structure index file and data file exist,
	    and contain the data for the first a1 rows of the coaccess
	    matrix. This method can be called before we start generating
	    row No. a1, in order to check that the structure index
	    file contains exactly a1+1 pointers (pointing to the
	    beginnings of the existing a1 rows numbered 0 thru (a1-1),
	    and to the beginning of row a1 which is to be created
	    now), and the structure data file contains the data for
	    row a1-1.

	    <P>
	    We expect the structure index file to contain a1+1 values (the
	    last one, pointer to the place where more data are to be
	    written), and the structure file, to contain the appropriate
	    matrix elements for row No. a1-1

	    @param canRewrite If true, we'll ignore the fact that the
	    index contains <em>more</em> data than needed. (It is assumed
	    that the caller intends to rewrite the "extra"  data later).
	    Otherwise, the expectation is that the index contains the
	    data for exactly a1-1 matrix rows.

	    @return true, if all expected data are in place (or
	    if a1==0, and there is still no index). false if the index
	    looks "strange" in some way.

	*/
	boolean olderIndexDataExist(int a1, boolean canRewrite) throws IOException {
	    if (a1<0) throw new IllegalArgumentException();
	    
	    if (a1==0 && ! indexExists()) return true;
	    
	    openFiles("r");
	    try {
		int len = (int)structureIndexRAF.lengthObject();
		if (!canRewrite && len!=a1+1) {
		    System.out.println("Structure index file size mismatch: expected " 
				       + a1 + " values, found " + len);
		    return false;
		}
		if (a1==0) return true;
		len = canRewrite?
		    (int)structureRAF.lengthObjectLenient() :
		    (int)structureRAF.lengthObject();

		int[] data = readRow(a1-1);
		for(int j=0; j<data.length; j++) {
		    if (j>0 && data[j]<= data[j-1]) {
			System.out.println("Structure data for row No. "+(a1-1)+" not in order: key["+a1+"]["+(j-1)+"]="+data[j-1]+" >= key["+a1+"]["+j+"]="+data[j]);
			return false;
		    }
		}
		return true; // everything in place
	    } finally {
		closeFiles();
	    }
	}
    
	/** Saves the newly generated structure information into this index.
	    @param ps contains the matrix structure data to be stored
	*/
	void writeIndex(PredictStructure ps)  throws IOException {
	    openFiles("rw");
	    long end=0;
	    for(int i=0; i<ps.articles.size(); i++) {
		int aid=ps.articles.elementAt(i);
		if (i>0 && aid-1!=ps.articles.elementAt(i-1)) throw new IllegalArgumentException("Articles list is not contiguous");
		if (aid==0) { // write start position
		    structureIndexRAF.seekObject(aid);	
		    structureIndexRAF.writeLong(0);		
		}
		structureIndexRAF.seekObject(aid);	
		final long start = structureIndexRAF.readLong();
		structureRAF.seekObject(start);
		
		if (i==0) {
		    System.out.println("Start writing, at " + start + " values mark"); 
		}


		CAACompact2 caa = (CAACompact2)ps.aSet.get(aid);
		for(int k: caa.allTimeCandidates) {
		    structureRAF.writeInt(k);
		}
		end = start + caa.allTimeCandidates.size();
		structureIndexRAF.seekObject(aid+1);	
		structureIndexRAF.writeLong(end);		   
	    }	    
	    System.out.println("Reported structure index file size=" + structureIndexRAF.length() + " bytes = " +  structureIndexRAF.lengthObject() + " values");
	    System.out.println("Reported structure index file size=" + structureRAF.length() + " bytes = " +  structureRAF.lengthObject() + " values");
	    closeFiles();
	    System.out.println("Done writing; structure file ends at " + end + " values mark"); 
	    
	}

	/** Reads one precomputed row of the data structure from an already
	    opened file */
	int[] readRow(int aid) throws IOException {
	    structureIndexRAF.seekObject(aid);
	    long b1 = structureIndexRAF.readLong();
	    long b2 = structureIndexRAF.readLong();			
	    structureRAF.seekObject(b1);
	    int [] data = new int[(int)(b2-b1)];
	    for(int j=0; j<data.length; j++) {
		data[j] = structureRAF.readInt();
	    }
	    return data;
	}

    }

    static void usage() {
	usage(null);
    }

    static void usage(String m) {
	System.out.println("Usage:");
	System.out.println(" java PredictStructure [article|aid] a1 a2 ...");
	System.out.println(" java PredictStructure [arange|arangewrite] a1 a2");
	System.out.println(" java PredictStructure [arange|arangewrite] a1 +d");
	if (m!=null) {
	    System.out.println(m);
	}
	System.exit(1);
    }
       
    static File indexDir;

    static public void main(String argv[]) throws IOException {
	ParseConfig ht = new ParseConfig();

	Profiler.profiler.setOn( ht.getOption("profile", true));

	String indexPath = ht.getOption("index", "out");
	// File 
	    indexDir = new File(indexPath);
	UserActionReader uar = new UserActionReader(indexDir);

	// The number of top articles that are displayed as rec list 
	final int n = ht.getOption("n", 10);
	
	// articles whose coaccess lists we will monitor
	Vector<Integer> articles = new Vector<Integer>();
	// will be set to true by the "arangewrite" command only
	boolean willWrite = false; 
	// if true, we can rewrite the "tail end" of the index.
	boolean canRewrite = ht.getOption("canRewrite", false);


	int ja=0;
	String cmd = argv[ja++];
	if (cmd.equals("article") || cmd.equals("aid")) {
	    while(ja<argv.length) {
		String s = argv[ja++];
		int aid =  cmd.equals("aid")? Integer.parseInt(s): uar.aidNameTable.get(s);
		System.out.println("Article " + aid + " ("+uar.aidNameTable.nameAt(aid)+")");
		articles.add(aid);
	    }
	} else if (cmd.equals("arange") || cmd.equals("arangewrite")) { 
	    // The remaining args should be "a1 a2" or "a1  +d" (where a2=a1+d)
	    // a1 <= aid < a2
	    String q[] = {argv[ja], argv[ja+1]};
	    boolean plus = q[1].startsWith("+");
	    if (plus) q[1] = q[1].substring(1);
	    int a[] = { Integer.parseInt(q[0]), Integer.parseInt(q[1])};
	    if (plus) a[1] = a[0] + a[1];

	    ja +=2 ;
	    System.out.println("Adding articles " +a[0]+ " thru " +a[1]+ "-1");
	    if (!(0<=a[0] && a[0] < a[1])) usage("Invalid range: " +a[0]+ " thru " +a[1]);


	    for(int aid=a[0]; aid<a[1]; aid++) {
		System.out.println("Article " + aid + " ("+uar.aidNameTable.nameAt(aid)+")");	
		articles.add(aid);
	    }    
	    willWrite =  cmd.equals("arangewrite");
	} else if (cmd.equals("uname") || cmd.equals("uid")) {
	    while(ja<argv.length) {
		String s = argv[ja++];
		int uid = cmd.equals("uid")? Integer.parseInt(s) :
		    uar.userNameTable.get(s);
		ActionDetails[] as = uar.actionsForUser(uid);
		System.out.println("For user " + uid + " ("+uar.userNameTable.nameAt(uid)+"), adding " + as.length + " articles" );
		for(ActionDetails x: as) {
		    articles.add(x.aid);
		}
	    }
	} else {
	    throw new IllegalArgumentException("Unknown command: " + cmd);
	}

	System.out.println("Will compute coaccess data for " + articles.size() + " articles");
	PredictStructure coa = new PredictStructure(uar, articles, n);

	if (willWrite) {
	    if (!(new IndexFiles(indexDir)).olderIndexDataExist(articles.elementAt(0), canRewrite)) {
		System.out.println("Can't do writing to index - the files with older data for a<" +articles.elementAt(0) + " don't exist or have unexpected size");
		System.exit(1);
	    }
	}
	    
	Profiler.profiler.push(Profiler.Code.OTHER);

	coa.predictStructure(willWrite);

	Profiler.profiler.pop(Profiler.Code.OTHER);

	System.out.println("===Profiler report (wall clock time)===");
	System.out.println(     Profiler.profiler.report());

    }


}
