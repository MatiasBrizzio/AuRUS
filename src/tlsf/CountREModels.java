package tlsf;

import de.uni_luebeck.isp.rltlconv.automata.Nba;
import modelcounter.ABC;
import owl.ltl.LabelledFormula;

import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;


public class CountREModels{

	public static BigInteger count(List<LabelledFormula> formulas, int k, boolean exhaustive, boolean positive) throws IOException, InterruptedException{
		long bound = 1;
		if(!exhaustive)
			bound = k;
		else{
			bound = 1;
		}
		List<BigInteger> results = new LinkedList<>();
		
		boolean first = true;
		
		while(bound<=k){
			BigInteger count = BigInteger.ZERO;
			double time = 0;
			if (first){
				double iTime = System.currentTimeMillis();
				ABC.reset();
				FormulaToRE.reset();
				count = count(formulas, bound, positive);
				time = getTimeInSecond(iTime,System.currentTimeMillis());
//				System.out.println("Time: " + time); 
				first = false;
			}
			else{
				double iTime = System.currentTimeMillis();
				if(ABC.result){
					if(FormulaToRE.encoded_alphabet==0)
						count = ABC.count(bound*2);//each state is characterised by 2 characters
					else if(FormulaToRE.encoded_alphabet==1)
						count = ABC.count(bound*3);//each state is characterised by 3 characters
					else
						count = ABC.count(bound);
				}
				else{
//					System.out.println("Unsatisfiable constraint");
					break;
				}
				time = getTimeInSecond(iTime,System.currentTimeMillis());
//				System.out.println("Time: " + time); 
			}
			results.add(count);
			bound++;
		}

		//dispose ABC 
//		ABC.abcDriver.dispose(); // release resources
		if(results.isEmpty())
			return BigInteger.ZERO;
		BigInteger avg = BigInteger.ZERO;
		for (BigInteger c : results){
			avg = avg.add(c);
		}
		BigInteger res = avg.divide(BigInteger.valueOf(results.size()));
		return res;
	}
	
	private static BigInteger count(List<LabelledFormula> formulas, long bound, boolean positive) throws IOException, InterruptedException{
		
		LinkedList<String> abcStrs = new LinkedList<>();
		for(LabelledFormula f: formulas){
			String abcStr = genABCString(f);
			abcStrs.add(abcStr);
		}

//		String [] arr = Discretizer.cat(s);
//		String abcStr = "";
//		for(int i=0; i < arr.length-1; i++){
//			abcStr += arr[i];
//		}
//		abcStrs.add(abcStr);
			
//		System.out.println("Model Counting...");
		BigInteger count = BigInteger.ZERO;
		if(FormulaToRE.encoded_alphabet==0)
			count = ABC.count(abcStrs,bound*2, positive);//each state is characterised by 2 characters
		else if(FormulaToRE.encoded_alphabet==1)
			count = ABC.count(abcStrs,bound*3, positive);//each state is characterised by 3 characters
		else
			count = ABC.count(abcStrs,bound, positive);


		return count;
	}
	
	public static double getTimeInSecond (double initialTime,double finalTime){
		//Compute execution time
		double time = finalTime - initialTime;
		//Translate to minutes and seconds
		double sec = (time/1000);
		return sec;
	}
	
	public static String genABCString(LabelledFormula ltl) throws IOException, InterruptedException{
		int vars = ltl.variables().size();
		if(vars>5 && vars<12)
			FormulaToRE.encoded_alphabet = 0;
		else if(vars>=12)
			FormulaToRE.encoded_alphabet = 1;
//		System.out.println("Translating from LTL to NBA...");
//		System.out.println(LTLModelCounter.encoded_alphabet);
//		System.out.println("NBA: " + nba.states().size() +  "(" + nba.accepting().size() + ") " + nba.transitions().size()); 
//		Nfa dfa = nba.toDeterministicNfa();
//		System.out.println("Generating RE...");
		String s = FormulaToRE.formulaToRegularExpression(ltl);
		return toABClanguage(s);
	}

	public static String toABClanguage(String re){
		String abcStr = "";
		abcStr = re.replace("λ", "\"\"");
		abcStr = abcStr.replace("+", "|");
		return abcStr;
	}
}
