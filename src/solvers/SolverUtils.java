package solvers;

import owl.ltl.Formula;
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

}
