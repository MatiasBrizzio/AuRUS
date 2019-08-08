package modelcounter;

import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import de.uni_luebeck.isp.rltlconv.automata.Nba;


public class Count {

	public static BigInteger count(List<String> formulas, String alphStr, int k, boolean exhaustive) throws IOException, InterruptedException{
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
				count = count(formulas, alphStr, bound);
				time = getTimeInSecond(iTime,System.currentTimeMillis());
//				System.out.println("Time: " + time); 
				first = false;
			}
			else{
				double iTime = System.currentTimeMillis();
				if(ABC.result){
					if(LTLModelCounter.encoded_alphabet==0)
						count = ABC.count(bound*2);//each state is characterised by 2 characters
					else if(LTLModelCounter.encoded_alphabet==1)
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
	
	private static BigInteger count(List<String> formulas, String alph, long bound) throws IOException, InterruptedException{
		
		LinkedList<String> abcStrs = new LinkedList<>();
		for(String f: formulas){
			String abcStr = genABCString(f, alph);
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
		if(LTLModelCounter.encoded_alphabet==0)
			count = ABC.count(abcStrs,bound*2);//each state is characterised by 2 characters
		else if(LTLModelCounter.encoded_alphabet==1)
			count = ABC.count(abcStrs,bound*3);//each state is characterised by 3 characters
		else
			count = ABC.count(abcStrs,bound);


		return count;
	}
	
	public static double getTimeInSecond (double initialTime,double finalTime){
		//Compute execution time
		double time = finalTime - initialTime;
		//Translate to minutes and seconds
		double sec = (time/1000);
		return sec;
	}
	
	public static String genABCString(String ltl, String alph) throws IOException, InterruptedException{
		String form = "LTL="+ltl;
		if(alph!=null && alph!="")
			form += ",ALPHABET="+alph;
//		Nfa dfa = LTLModelCounter.ltl2dfa(formula);
		if(LTLModelCounter.props && alph!=null && alph.split(",").length>5 && alph.split(",").length<12)
			LTLModelCounter.encoded_alphabet = 0;
		else if(LTLModelCounter.props && alph!=null && alph.split(",").length>=12)
			LTLModelCounter.encoded_alphabet = 1;
//		System.out.println("Translating from LTL to NBA...");
		Nba nba = LTLModelCounter.ltl2nba(form);
//		Nfa dfa = nba.toDeterministicNfa();
//		System.out.println("Generating RE...");
		String s = LTLModelCounter.automata2RE(nba);
		return LTLModelCounter.toABClanguage(s);
	}
	
}
