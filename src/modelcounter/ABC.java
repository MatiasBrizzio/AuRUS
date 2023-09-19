package modelcounter;

import vlab.cs.ucsb.edu.DriverProxy;
import vlab.cs.ucsb.edu.DriverProxy.Option;

import java.math.BigInteger;
import java.util.LinkedList;


public class ABC {

    public DriverProxy abcDriver;
    public boolean result;

    public ABC() {
        result = false;
        abcDriver = new DriverProxy();
    }

    public BigInteger count(LinkedList<String> formulas, int bound, boolean exhaustive, boolean positive) {


        abcDriver.setOption(Option.REGEX_FLAG, Option.REGEX_FLAG_ANYSTRING);

        StringBuilder constraint = new StringBuilder("(set-logic QF_S)\n"
                + "(declare-fun x () String)\n");

        for (String f : formulas) {
            if (positive)
                constraint.append("(assert (in x /").append(f).append("/))\n");
            else
                constraint.append("(assert (not (in x /").append(f).append("/)))\n");

        }
        constraint.append("(assert (= (len x) ").append(bound).append("))\n");
        constraint.append("(check-sat)\n");
        result = abcDriver.isSatisfiable(constraint.toString());
        BigInteger count = BigInteger.ZERO;

        if (result) {
            if (!exhaustive)
                count = abcDriver.countVariable("x", bound);
            else {
                for (long k = 1; k <= bound; k++) {
                    BigInteger r = abcDriver.countVariable("x", k);
                    count = count.add(r);
                }
            }
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
        }  //      System.out.println("Unsatisfiable");


        abcDriver.dispose(); // release resources
        return count;
    }

    public BigInteger count(long bound) {
        BigInteger count = BigInteger.ZERO;
//	  System.out.println("k: "+bound);
        if (result) {
            count = abcDriver.countVariable("x", bound);
//		  if (count != null) {
//	        System.out.println("Number of solutions: " + count.toString());
//	      } else {
//	        System.out.println("An error occured during counting, please contact vlab@cs.ucsb.edu");
//	      }

//		  abcDriver.dispose(); // release resources
        }
        return count;

    }

}


