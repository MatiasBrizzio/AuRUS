package solvers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import main.Settings;
import owl.ltl.LabelledFormula;
import owl.ltl.spectra.Spectra;
import owl.ltl.tlsf.Tlsf;
import tlsf.TLSF_Utils;

public class StrixHelper {

	private static FileWriter writer;
//	private static int TIMEOUT = 180;
	
	static Map<String, String> replacements = new HashMap<String, String>(){
		private static final long serialVersionUID = 1L;
	{
        put("&", "&&");
        put("|", "||");
    }};
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
		TLSF_Utils.toBasicTLSF(tlsf);
		String tlsfBasic = tlsf.getPath().replace(".tlsf","_basic.tlsf");
		return executeStrix(tlsfBasic);
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
		
		Tlsf tlsf2 = TLSF_Utils.toBasicTLSF(TLSF_Utils.toTLSF((tlsf)));
		String spec_string = TLSF_Utils.adaptTLSFSpec(tlsf2);
		File file = null;
		
		if (Settings.USE_DOCKER)
			file = new File("docker/Spec.tlsf");
		else
			file = new File( (tlsf.title().replace("\"", "")+".tlsf").replaceAll("\\s",""));
		//Create the file
		try {
			writer = new FileWriter(file);
			writer.write(spec_string);
			writer.flush();
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
		Process pr = null;
		if (Settings.USE_DOCKER)
			pr = Runtime.getRuntime().exec( new String[]{"./run-docker-strix.sh"});
		else
			pr = Runtime.getRuntime().exec( new String[]{"lib/strix_tlsf.sh","./"+path, "-r"});
		boolean timeout = false;
		if(!pr.waitFor(Settings.STRIX_TIMEOUT, TimeUnit.SECONDS)) {
		    timeout = true; //kill the process. 
			pr.destroy(); // consider using destroyForcibly instead
		}
		
		RealizabilitySolverResult realizable = RealizabilitySolverResult.UNREALIZABLE;
		String aux;
		if (timeout){
			realizable = RealizabilitySolverResult.TIMEOUT;
			pr.destroy();
			pr = Runtime.getRuntime().exec( new String[]{"./run-docker-stop.sh"});
		}
		else {
			
			InputStream in = pr.getInputStream();
	    	InputStreamReader inread = new InputStreamReader(in);
	    	BufferedReader bufferedreader = new BufferedReader(inread);
	    	
		    while ((aux = bufferedreader.readLine()) != null) {
		    	//System.out.println(aux);
		    	if (aux.equals("REALIZABLE")){
		    		realizable = RealizabilitySolverResult.REALIZABLE;
		    		break;
		    	}
		    	if (aux.contains("Error")){
			    	System.out.println("ERR: " + aux);
			    	realizable = RealizabilitySolverResult.ERROR;
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
	/**
	 * Checks the realizability of the system specified by the TLSF spec passed as parameter
	 * @param tlsf tlsf file containing all specification.
	 * @return {@link RealizabilitySolverResult} value indicating if the TLSF spec is realizable, unrealizable 
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public static RealizabilitySolverResult checkRealizability(Spectra spectra) throws IOException, InterruptedException {
		String formula = toSolverSyntax(spectra.toFormula());
		String inputs = "";
		String outputs = "";
		int i = 0;
		while (spectra.inputs().get(i)) {
			inputs += spectra.variables().get(i) + ",";
			i++;
		}
		while (spectra.outputs().get(i)) {
			
			outputs += spectra.variables().get(i) + ",";
			i++;
		}
		for (String v : spectra.variables()) { 
			inputs = inputs.replaceAll(v, v.toLowerCase());
			outputs = outputs.replaceAll(v, v.toLowerCase());
		}
		outputs = outputs.substring(0, outputs.length() - 1);
		inputs = inputs.substring(0, inputs.length() - 1);
		return executeStrix(formula,inputs,outputs);
		
	}
	
	
	/**
	 * Calls strix program with the file in path
	 * @param path path to TLSF file
	 * @return {@link RealizabilitySolverResult} value indicating if the TLSF spec is realizable, unrealizable 
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	private static RealizabilitySolverResult executeStrix(String formula, String ins, String outs) throws IOException, InterruptedException {
		Process pr = null;
		if (Settings.USE_DOCKER)
			pr = Runtime.getRuntime().exec( new String[]{"./run-docker-strix.sh"});
		else
			pr = Runtime.getRuntime().exec( new String[]{"lib/new_strix/strix","-f "+formula, "--ins=" + ins, "--outs="+outs});
		boolean timeout = false;
		if(!pr.waitFor(Settings.STRIX_TIMEOUT, TimeUnit.SECONDS)) {
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
		    	//System.out.println(aux);
		    	if (aux.equals("REALIZABLE")){
		    		realizable = RealizabilitySolverResult.REALIZABLE;
		    		break;
		    	}
		    	if (aux.contains("Error")){
			    	System.out.println("ERR: " + aux);
			    	realizable = RealizabilitySolverResult.ERROR;
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
	private static String toSolverSyntax(LabelledFormula f) {
		String LTLFormula = f.toString();
		
		for (String v : f.variables()) 
			LTLFormula = LTLFormula.replaceAll(v, v.toLowerCase());			

		LTLFormula = LTLFormula.replaceAll("([A-Z])", " $1 ");
		LTLFormula = LTLFormula.replaceAll("\\!", " ! ");
		
		return new String(replaceLTLConstructions(LTLFormula)); 
	}
	
	private static String replaceLTLConstructions(String line) {
		Set<String> keys = replacements.keySet();
	    for(String key: keys)
	        line = line.replace(key, replacements.get(key));    
		return line;
	}
}
