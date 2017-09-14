package edu.rutgers.retro;

import java.io.*;
import java.util.*;
import java.util.regex.*;

//import javax.persistence.*;

import org.json.*;

/** This class implements our original assumption: IP = user.
*/

class IPArxivUserInferrer extends ArxivUserInferrer 
{
    String inferUser(String ip_hash, String cookie_hash) {
	fromIPCnt++;
	return ip_hash;
    }
}
