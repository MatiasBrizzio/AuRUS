package solvers;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import owl.ltl.*;
import owl.ltl.parser.LtlParser;
import owl.ltl.rewriter.NormalForms;
import owl.ltl.rewriter.SyntacticSimplifier;
import solvers.LTLSolver.SolverResult;

public class LTLModelCounter {
	public static int BOUND = 5;
	public static final String BASENAME = "lib/ltl-model-counter/result/numofmodels";
	public static final String INFILE = BASENAME+".ltl";
	public static final String PROP_FILE = BASENAME+"-k"+BOUND+".sat";
	public static final String PROP_CNF_FILE = BASENAME+"-k"+BOUND+".cnf";

	public static int numOfTimeout = 0;
	public static int numOfError = 0;
	public static int numOfCalls = 0;
	public static int TIMEOUT = 60;

	public static String getCommandLTL2PL(){
		String cmd = "";
		String currentOS = System.getProperty("os.name");
		if (currentOS.startsWith("Mac"))
			cmd = "./lib/ltl-model-counter/ltl2pl_macos.sh "+ INFILE + " " + BASENAME + " " + BOUND;
		else
			cmd = "./lib/ltl-model-counter/ltl2pl.sh"+ INFILE + " " + BASENAME + " " + BOUND;
		return cmd;
	}

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
			toCNF(formula, variables);
//			File f = new File(INFILE);
//			FileWriter fw = new FileWriter(f);
//			for (String v : variables)
//				fw.append(v + "\n");
//			fw.append("###\n");
//			fw.append(formula + "\n");
//			fw.close();
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
		    	if (aux.startsWith("s ")){
		    		System.out.println(aux);
		    		String val = aux.replace("s ", "");
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


	private static void toCNF(String formula, List<String> variables) throws IOException, InterruptedException {
		File f = new File(INFILE);
		FileWriter fw = new FileWriter(f);
		for (String v : variables)
			fw.append(v + "\n");
		fw.append("###\n");
		fw.append(formula + "\n");
		fw.close();

		// run ltl2pl command
		String cmdltl2pl = getCommandLTL2PL();
		Process p = Runtime.getRuntime().exec(cmdltl2pl);
		p.waitFor(TIMEOUT, TimeUnit.SECONDS);

		//read all CNF clauses
		List<String> vars = new LinkedList<>(); //BMC variables
		for(String v : variables){
			for(int i = 0; i < BOUND+1; i++)
				vars.add(v+i);
		}
		for(int i = 0; i < BOUND; i++)
			vars.add("l"+i);

		List<Formula> clauses = new LinkedList<>();
		BufferedReader br = new BufferedReader(new FileReader(PROP_FILE));
		String prop_str = br.readLine();
		while (prop_str != null && prop_str != ""){
			Formula clause = LtlParser.syntax(prop_str,vars);
			clauses.add(clause);
			prop_str = br.readLine();
		}
		br.close();
//		System.out.println(prop_str);
		fw = new FileWriter(new File(PROP_CNF_FILE));
		fw.append("p cnf " + vars.size() + " " + clauses.size() + "\n");
		for (Formula clause : clauses)
			fw.append(cnfStr(clause) + "\n");
//		System.out.println(cnf_formula.toString());
//
//		fw.append(cnf_formula.toString());
		fw.close();
	}

	private static String cnfStr (Formula f){
		if (!(f instanceof Disjunction))
			throw new IllegalArgumentException("LTLModelCounter: formula is not in cnf format");
		Disjunction clause = (Disjunction) f;
		String cnf = "";
		for(Formula c : clause.children()){
			Literal l = (Literal) c;
			if(l.isNegated()) cnf += "-";
			cnf += l.getAtom()+1;
			cnf += " ";
		}
		cnf += "0";
		return cnf;
	}
}
