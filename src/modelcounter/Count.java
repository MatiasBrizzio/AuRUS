package modelcounter;

import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import de.uni_luebeck.isp.rltlconv.automata.Nba;
import owl.ltl.LabelledFormula;
import solvers.SolverUtils;
import tlsf.FormulaToRE;


public class Count {
	Rltlconv_LTLModelCounter translatorLTLtoRE = null ;
	public Count () {
		translatorLTLtoRE = new Rltlconv_LTLModelCounter();
	}

	public BigInteger count(List<LabelledFormula> formulas, int bound, boolean exhaustive, boolean positive) throws IOException, InterruptedException{
		ABC abc = new ABC();
		LinkedList<String> abcStrs = new LinkedList<>();
		for(LabelledFormula f: formulas){
			String abcStr = genABCString(f);
			if (abcStr != null)
				abcStrs.add(abcStr);
		}
		BigInteger count = BigInteger.ZERO;
		if(translatorLTLtoRE.encoded_alphabet==0)
			count = abc.count(abcStrs,bound*2, exhaustive,positive);//each state is characterised by 2 characters
		else if(translatorLTLtoRE.encoded_alphabet==1)
			count = abc.count(abcStrs,bound*3, exhaustive,positive);//each state is characterised by 3 characters
		else
			count = abc.count(abcStrs,bound, exhaustive,positive);
		if (!exhaustive)
			return count;
		else {
			BigInteger res = count.divide(BigInteger.valueOf(bound));
			return res;
		}
	}
	
	public double getTimeInSecond (double initialTime,double finalTime){
		//Compute execution time
		double time = finalTime - initialTime;
		//Translate to minutes and seconds
		double sec = (time/1000);
		return sec;
	}
	
	public String genABCString(LabelledFormula formula) throws IOException, InterruptedException{
//		String ltl = SolverUtils.toLambConvSyntax(formula.formula().toString());
//		String alph = SolverUtils.createLambConvAlphabet(formula);
		List<String> alphabet = SolverUtils.genAlphabet(formula.variables().size());
		LabelledFormula label_formula = LabelledFormula.of(formula.formula(), alphabet);
		String ltl = SolverUtils.toLambConvSyntax(label_formula.toString());
		String alph = alphabet.toString();
		
		String form = "LTL="+ltl;
		if(alph!=null && alph!="")
			form += ",ALPHABET="+alph;
		if(alph!=null && formula.variables().size()>5 && formula.variables().size()<12)
			translatorLTLtoRE.encoded_alphabet = 0;
		else if(alph!=null && formula.variables().size()>=12)
			translatorLTLtoRE.encoded_alphabet = 1;
//		System.out.println("Translating from LTL to NBA...");
		
//		translatorLTLtoRE = new Rltlconv_LTLModelCounter();
		String s = translatorLTLtoRE.ltl2RE(form);
		return translatorLTLtoRE.toABClanguage(s);
	}
	
}
