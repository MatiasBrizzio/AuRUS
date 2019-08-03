package solvers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import owl.ltl.tlsf.Tlsf;
import tlsf.TLSF_Utils;

public class StrixHelper {

	private static FileWriter writer;
	private static int TIMEOUT = 10;
	
	public static enum RealizabilitySolverResult {
		REALIZABLE,
		UNREALIZABLE,
		TIMEOUT,
		ERROR;
		public boolean inconclusive () { return this == TIMEOUT || this == ERROR; }
	}
	
	/**
	 * Checks the realizability of the system specified by the TLSF spec passed as parameter
	 * @param tlsf a TLSF File containing all tlsf specifiaction
	 * @return {@link solvers.StrixHelper.RealizabilitySolverResult} value indicating if the TLSF spec is realizable, unrealizable 
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public static RealizabilitySolverResult checkRealizability(File tlsf) throws IOException, InterruptedException {
		return executeStrix(tlsf.getPath());
	}
	
	
	
	
	/**
	 * Checks the realizability of the system specified by the TLSF spec passed as parameter
	 * @param tlsf a string containing all tlsf specifiaction
	 * @return {@link RealizabilitySolverResult} value indicating if the TLSF spec is realizable, unrealizable 
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public static RealizabilitySolverResult checkRealizability(String tlsf) throws IOException, InterruptedException {
		Tlsf tlsf2 = TLSF_Utils.toBasicTLSF(tlsf);
		return checkRealizability(tlsf2);
	}
	
	/**
	 * Checks the realizability of the system specified by the TLSF spec passed as parameter
	 * @param tlsf tlsf file containing all specification.
	 * @return {@link RealizabilitySolverResult} value indicating if the TLSF spec is realizable, unrealizable 
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public static RealizabilitySolverResult checkRealizability(Tlsf tlsf) throws IOException, InterruptedException {
		//Writes the tlsf object into file...
		Tlsf tlsf2 = TLSF_Utils.toBasicTLSF(TLSF_Utils.toTLSF(tlsf));
		String tlsf_string = TLSF_Utils.toTLSF(tlsf2);
		File file = new File(tlsf.title().replace("\"", "")+".tlsf");
		//Create the file
		try {
			if (file.createNewFile()){
			  //System.out.println("File is created!");
			}else{
			  //System.out.println("File already exists.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			writer = new FileWriter(file);
			writer.write(tlsf_string);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return executeStrix(file.getPath());
	}

	/**
	 * Calls strix program with the file in path
	 * @param path path to TLSF file
	 * @return {@link RealizabilitySolverResult} value indicating if the TLSF spec is realizable, unrealizable 
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	private static RealizabilitySolverResult executeStrix(String path) throws IOException, InterruptedException {
		Process pr = Runtime.getRuntime().exec( new String[]{"lib/strix/bin/strix", "./"+path});
		
		boolean timeout = false;
		if(!pr.waitFor(TIMEOUT, TimeUnit.SECONDS)) {
		    timeout = true; //kill the process. 
			pr.destroy(); // consider using destroyForcibly instead
		}
		
		RealizabilitySolverResult realizable = RealizabilitySolverResult.UNREALIZABLE;
		String aux;
		if (timeout){
			realizable = RealizabilitySolverResult.TIMEOUT;
			pr.destroy();
		}
		else {
			
			InputStream in = pr.getInputStream();
	    	InputStreamReader inread = new InputStreamReader(in);
	    	BufferedReader bufferedreader = new BufferedReader(inread);
	    	
		    while ((aux = bufferedreader.readLine()) != null) {
		    	if (aux.equals("REALIZABLE")){
		    		realizable = RealizabilitySolverResult.REALIZABLE;
		    		break;
		    	}
		    }

			//read program's error
	    	InputStream err = pr.getErrorStream();
	    	InputStreamReader errread = new InputStreamReader(err);
	    	BufferedReader errbufferedreader = new BufferedReader(errread);
		    while ((aux = errbufferedreader.readLine()) != null) {
		    	System.out.println("ERR: " + aux);
		    	realizable = RealizabilitySolverResult.ERROR;
		    }
		   
		    // Check for failure
			if (pr.waitFor() != 0) {
				System.out.println("exit value = " + pr.exitValue());
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
    		
   		if (pr!=null) {
  			OutputStream os = pr.getOutputStream();
			if (os!=null) os.close();
   		}
   		
   		return realizable;
	}
}
