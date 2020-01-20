package solvers;

import java.util.LinkedList;
import java.util.List;

import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.tlsf.Tlsf;

public class SolverUtils {
	
	public static String toSolverSyntax(Formula f) {
		String LTLFormula = f.toString();
		LTLFormula = LTLFormula.replaceAll("\\!", "~");
		LTLFormula = LTLFormula.replaceAll("([A-Z])", " $1 ");
		return new String(LTLFormula); 
	}
	
	public static String toLambConvSyntax(Formula f) {
		String LTLFormula = f.toString();
		LTLFormula = LTLFormula.replaceAll("\\!", " ! ");
		LTLFormula = LTLFormula.replaceAll("&", "&&");
		LTLFormula = LTLFormula.replaceAll("\\|", "||");
		return new String(LTLFormula); 
	}
	
	public static String toLambConvSyntax(String LTLFormula) {
		LTLFormula = LTLFormula.replaceAll("\\!", " ! ");
		LTLFormula = LTLFormula.replaceAll("&", "&&");
		LTLFormula = LTLFormula.replaceAll("\\|", "||");
		return new String(LTLFormula); 
	}
	
	public static String createLambConvAlphabet(Tlsf spec) {
		String alphabet = "[";
		for (int i=0; i < spec.variables().size(); i++) {
			if (i > 0)
				alphabet += ",";
			alphabet += "p"+i;
		}
		alphabet += "]";
		return alphabet;
	}
	
	public static String createLambConvAlphabet(Formula formula, int numOfVars) {
		String alphabet = "[";
		for (int i=0; i < numOfVars; i++) {
			if (i > 0)
				alphabet += ",";
			alphabet += "p"+i;
		}
		alphabet += "]";
		return alphabet;
	}
	
	public static String createLambConvAlphabet(LabelledFormula formula) {
		String alphabet = "[";
		for (int i=0; i < formula.variables().size(); i++) {
			if (i > 0)
				alphabet += ",";
			alphabet += "p"+i;
		}
		alphabet += "]";
		return alphabet;
	}

	public static List<String> genAlphabet(int n){
    	List<String> alphabet = new LinkedList();
		for (int i = 0; i < n; i++) {
			String v = ""+Character.toChars(97+i)[0];
			alphabet.add(v);
		}
		return alphabet;
    }
}
