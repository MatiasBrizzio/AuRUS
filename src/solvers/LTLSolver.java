package solvers;

import main.Settings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.concurrent.TimeUnit;

public class LTLSolver {

	public static enum SolverResult {
		SAT,
		UNSAT,
		TIMEOUT,
		ERROR;
		
		public boolean inconclusive () { return this == TIMEOUT || this == ERROR; }
	}
	 
	public static int numOfTimeout = 0;
	public static int numOfError = 0;
	public static int numOfCalls = 0;
//	public static int TIMEOUT = 30;
	
	private static String getCommand(){
		String cmd = "";
		String currentOS = System.getProperty("os.name");
		if (currentOS.startsWith("Mac"))
//			if (useAalta)
				cmd = "./lib/aalta";
//			else
//				cmd = "./lib/pltl graph";
		else
			cmd = "./lib/aalta_linux";
		return cmd;
	}
	
	public static SolverResult isSAT(String formula) throws IOException, InterruptedException{
		numOfCalls++;
//		System.out.println(formula);
		Process p = null;
    	
		if (formula != null) {
			String cmd = getCommand();
			p = Runtime.getRuntime().exec(new String[]{cmd,formula});
//	    	OutputStream out = p.getOutputStream();
//	    	OutputStreamWriter bufout = new OutputStreamWriter(out);
//	    	BufferedWriter bufferedwriter = new BufferedWriter(bufout, formula.getBytes().length);
//    		bufferedwriter.write(formula);
//	    	bufferedwriter.close();
//	    	bufout.close();
//	    	out.close();
    	}

		boolean timeout = false;
		if(!p.waitFor(Settings.SAT_TIMEOUT, TimeUnit.SECONDS)) {
		    timeout = true; //kill the process. 
			p.destroy(); // consider using destroyForcibly instead
		}
		
		SolverResult sat = SolverResult.ERROR;
		String aux;
		if (timeout){
			numOfTimeout++;
			sat = SolverResult.TIMEOUT;
			p.destroy();
		}
		else {
			InputStream in = p.getInputStream();
	    	InputStreamReader inread = new InputStreamReader(in);
	    	BufferedReader bufferedreader = new BufferedReader(inread);
			sat = SolverResult.UNSAT;
		    while ((aux = bufferedreader.readLine()) != null) {
		    	if ((aux.equals("sat")) || (aux.contains("Formula 1: satisfiable"))){
		    		sat = SolverResult.SAT;
		    		break;
		    	}
		    }
			
		    
		    
		 // Leer el error del programa.
	    	InputStream err = p.getErrorStream();
	    	InputStreamReader errread = new InputStreamReader(err);
	    	BufferedReader errbufferedreader = new BufferedReader(errread);
		    while ((aux = errbufferedreader.readLine()) != null) {
		    	System.out.println("ERR: " + aux);
		    	sat = SolverResult.ERROR;
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
   		return sat;
	}
}
