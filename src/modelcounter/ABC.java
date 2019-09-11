package modelcounter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedList;

import de.uni_luebeck.isp.rltlconv.automata.Nfa;
import vlab.cs.ucsb.edu.DriverProxy;
import vlab.cs.ucsb.edu.DriverProxy.Option;


public class ABC {

  public static DriverProxy abcDriver = new DriverProxy();
  public static boolean result = false;
  
  public static BigInteger count(LinkedList<String> formulas, long bound, boolean positive) {

    
//    abcDriver.setOption(Option.ENABLE_IMPLICATIONS);
//    abcDriver.setOption(Option.USE_SIGNED_INTEGERS);
    abcDriver.setOption(Option.REGEX_FLAG,Option.REGEX_FLAG_ANYSTRING);

    String constraint = "(set-logic QF_S)\n"
		+ "(declare-fun x () String)\n";
    
    for(String f : formulas){
    	if (positive)
    		constraint+= "(assert (in x /"+f+"/))\n";
    	else
    		constraint+= "(assert (not (in x /"+f+"/)))\n";
    		
    }
//    constraint += "(assert (= (len x) "+bound+"))\n";
    constraint += "(check-sat)\n";
    
//    System.out.println(constraint);
//    System.out.println(bound);
    result = abcDriver.isSatisfiable(constraint);
    BigInteger count = BigInteger.ZERO;
    
    if (result) {
//      System.out.println("Satisfiable");
      
      count = abcDriver.countVariable("x",bound);

//      if (count != null) {
//        System.out.println("Number of solutions: " + count.toString());
//      } else {
//        System.out.println("An error occured during counting, please contact vlab@cs.ucsb.edu");
//      }
      
//    byte[] func = abcDriver.getModelCounterForVariable("x"); 
//      BigInteger count2 = abcDriver.countVariable("x", bound, func);
//      System.out.println("cache count: " + count2);
      
//      abcDriver.printResultAutomaton();
      
//      Map<String, String> results = abcDriver.getSatisfyingExamples();
//      for (Entry<String, String> var_result : results.entrySet()) {
//        System.out.println(var_result.getKey() + " : \"" + var_result.getValue() + "\"");
//      }
    } else {
//      System.out.println("Unsatisfiable");
    }
    
//    abcDriver.dispose(); // release resources
    return count;
  }
  
  public static BigInteger count(long bound) {
	  BigInteger count = BigInteger.ZERO;
//	  System.out.println("k: "+bound);
	  if(result){
		  count = abcDriver.countVariable("x",bound);	      
//		  if (count != null) {
//	        System.out.println("Number of solutions: " + count.toString());
//	      } else {
//	        System.out.println("An error occured during counting, please contact vlab@cs.ucsb.edu");
//	      }
		  
//		  abcDriver.dispose(); // release resources
	  }
	  return count;
	  
  }
  
  public static void reset() {
	  abcDriver.dispose();
	  abcDriver = new DriverProxy();
  }
  
}


