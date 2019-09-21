package modelcounter;

import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import de.uni_luebeck.isp.rltlconv.automata.Nba;
import owl.ltl.LabelledFormula;
import tlsf.FormulaToRE;


public class Count {
	Rltlconv_LTLModelCounter translatorLTLtoRE = null ;
	public Count () {
		translatorLTLtoRE = new Rltlconv_LTLModelCounter();
	}

	public BigInteger count(List<String> formulas, String alphStr, int bound, boolean exhaustive, boolean positive) throws IOException, InterruptedException{
		ABC abc = new ABC();
		LinkedList<String> abcStrs = new LinkedList<>();
		for(String f: formulas){
			String abcStr = genABCString(f, alphStr);
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
	
	public String genABCString(String ltl, String alph) throws IOException, InterruptedException{
		String form = "LTL="+ltl;
		if(alph!=null && alph!="")
			form += ",ALPHABET="+alph;
//		Nfa dfa = LTLModelCounter.ltl2dfa(formula);
		if(translatorLTLtoRE.props && alph!=null && alph.split(",").length>5 && alph.split(",").length<12)
			translatorLTLtoRE.encoded_alphabet = 0;
		else if(translatorLTLtoRE.props && alph!=null && alph.split(",").length>=12)
			translatorLTLtoRE.encoded_alphabet = 1;
//		System.out.println("Translating from LTL to NBA...");
		Nba nba = translatorLTLtoRE.ltl2nba(form);
//		System.out.println(LTLModelCounter.encoded_alphabet);
		System.out.println();
		System.out.println("NBA: " + nba.states().size() +  "(" + nba.accepting().size() + ") " + nba.transitions().size()); 
//		Nfa dfa = nba.toDeterministicNfa();
//		System.out.println("Generating RE...");
		String s = translatorLTLtoRE.automata2RE(nba);
		return translatorLTLtoRE.toABClanguage(s);
	}
	
}
