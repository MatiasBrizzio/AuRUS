package tlsf;

import de.uni_luebeck.isp.rltlconv.automata.Nba;
import modelcounter.ABC;
import owl.ltl.LabelledFormula;
import regular.Discretizer;

import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;


public class CountREModels{
	FormulaToRE translatorLTLtoRE = null ;
	public CountREModels() {
		translatorLTLtoRE = new FormulaToRE();
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
	
	public String genABCString(LabelledFormula ltl) throws IOException, InterruptedException{
//		translatorLTLtoRE = new FormulaToRE();
		int vars = ltl.variables().size();
		if(vars>5 && vars<12)
			translatorLTLtoRE.encoded_alphabet = 0;
		else if(vars>=12)
			translatorLTLtoRE.encoded_alphabet = 1;
//		System.out.println("Translating from LTL to NBA...");
//		System.out.println(LTLModelCounter.encoded_alphabet);
//		System.out.println("NBA: " + nba.states().size() +  "(" + nba.accepting().size() + ") " + nba.transitions().size()); 
//		Nfa dfa = nba.toDeterministicNfa();
//		System.out.println("Generating RE...");
		translatorLTLtoRE.generateLabels(ltl.variables());
		String s = translatorLTLtoRE.formulaToRegularExpression(ltl);
		if (s == null)
			return null;
		return toABClanguage(s);
	}

	public  String toABClanguage(String re){
		String abcStr = "";
		abcStr = re.replace("λ", "\"\"");
		abcStr = abcStr.replace("+", "|");
		return abcStr;
	}
}
