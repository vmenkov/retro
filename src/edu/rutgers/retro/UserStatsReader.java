package edu.rutgers.retro;

import java.io.*;
import java.util.*;
import org.apache.commons.csv.*;

/** A tool to read back users.csv, which has been written by UserStats */
public class UserStatsReader {
       
    static String[] csvRecordToStringArray(CSVRecord record) {
	String x[] = new String[record.size()];
	int j=0;
	for (String field : record) {
	    x[j++] = field;
	}
	return x;
    }

    static UserStats.UserInfo[] readUserList(File usersCsvFile) throws IOException {
	FileReader in = new FileReader(usersCsvFile);
	int cnt=0;
	Vector<UserStats.UserInfo> v = new Vector<UserStats.UserInfo>();
	for (CSVRecord record : CSVFormat.DEFAULT.parse(in)) {
	    String[] x = csvRecordToStringArray(record);
	    UserStats.UserInfo ui = new UserStats.UserInfo(x);
	    v.add(ui);
	    //System.out.println(ui.toStringCSV());
	    cnt++;
	    //if (cnt > 10) break;
	}
	return (UserStats.UserInfo[])v.toArray(new UserStats.UserInfo[0]);
    }

    /*
    static public void main(String argv[])  throws IOException{
	test2();
    }
    */

}

