package solvers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import owl.ltl.Formula;
import solvers.LTLSolver.SolverResult;

public class LTLModelCounter {
	
	public static final String BASENAME = "lib/ltl-model-counter/result/numofmodels";
	public static final String INFILE = BASENAME+".ltl";
	public static final int BOUND = 5;
	
	public static int numOfTimeout = 0;
	public static int numOfError = 0;
	public static int numOfCalls = 0;
	public static int TIMEOUT = 20;
	
	public static String getCommand(){
		String cmd = "";
		String currentOS = System.getProperty("os.name");
		if (currentOS.startsWith("Mac"))
			cmd = "./lib/ltl-model-counter/ltl-modelcountermac.sh "+ INFILE + " " + BASENAME + " " + BOUND;
		else
			cmd = "./lib/ltl-model-counter/ltl-modelcounterlinux.sh "+ INFILE + " " + BASENAME + " " + BOUND;
		return cmd;
	}
	
	public static BigInteger count(Formula f, int numOfVars) throws IOException, InterruptedException {
		String formula = f.toString().replaceAll("([A-Z])", " $1 ");
		List<String> vars = new LinkedList<String>();
		 for (int i = 0; i < numOfVars; i++)
			 vars.add("p"+i);
		 return count(formula, vars);
	}
			
	public static BigInteger count(String formula, List<String> variables) throws IOException, InterruptedException {
		numOfCalls++;
//		System.out.println(formula);
		Process p = null;
    	
		// make formula file 
		if (formula != null && variables != null) {
			File f = new File(INFILE);
			FileWriter fw = new FileWriter(f);
			for (String v : variables)
				fw.append(v + "\n");
			fw.append("###\n");
			fw.append(formula + "\n");
			fw.close();
		
			// run counting command
			String cmd = getCommand();
			p = Runtime.getRuntime().exec(cmd);
//	    	OutputStream out = p.getOutputStream();
//	    	OutputStreamWriter bufout = new OutputStreamWriter(out);
//	    	BufferedWriter bufferedwriter = new BufferedWriter(bufout, formula.getBytes().length);
//    		bufferedwriter.write(formula);
//	    	bufferedwriter.close();
//	    	bufout.close();
//	    	out.close();
    	}
		
		boolean timeout = false;
		if(!p.waitFor(TIMEOUT, TimeUnit.SECONDS)) {
		    timeout = true; //kill the process. 
			p.destroy(); // consider using destroyForcibly instead
		}
		
		String aux;
		BigInteger numOfModels = null;
		if (timeout){
			numOfTimeout++;
			numOfModels = BigInteger.ZERO;
			p.destroy();
		}
		else {
			InputStream in = p.getInputStream();
	    	InputStreamReader inread = new InputStreamReader(in);
	    	BufferedReader bufferedreader = new BufferedReader(inread);
		    while ((aux = bufferedreader.readLine()) != null) {
		    	if (aux.startsWith("Number of solutions:")){
//		    		System.out.println(aux);
		    		String val = aux.replace("Number of solutions: ", "");
		    		numOfModels = new BigInteger(val);
		    		break;
		    	}
		    	else if (aux.startsWith("UNSAT")){
		    		numOfModels = BigInteger.ZERO;
		    		break;
		    	}
		    }
		    
		 // Leer el error del programa.
	    	InputStream err = p.getErrorStream();
	    	InputStreamReader errread = new InputStreamReader(err);
	    	BufferedReader errbufferedreader = new BufferedReader(errread);
		    while ((aux = errbufferedreader.readLine()) != null) {
		    	System.out.println("ERR: " + aux + " Formula: " + formula);
		    }
		   
		    // Check for failure
			if (p.waitFor() != 0) {
				System.out.println("exit value = " + p.exitValue());
				System.out.println(formula);
				numOfError++;
			}
			
			// Close the InputStream
	    	bufferedreader.close();
	    	inread.close();
	    	in.close();
	  
	   		// Close the ErrorStream
	   		errbufferedreader.close();
	   		errread.close();
	   		err.close();
		}
		
		if (p!=null) {
  			OutputStream os = p.getOutputStream();
			if (os!=null) os.close();
   		}
		
		return numOfModels;
	}
}
