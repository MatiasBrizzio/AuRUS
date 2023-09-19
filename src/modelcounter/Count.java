package modelcounter;

import owl.ltl.LabelledFormula;
import regular.Discretizer;
import solvers.SolverUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;


public class Count {
    Rltlconv_LTLModelCounter translatorLTLtoRE;

    public Count() {
        translatorLTLtoRE = new Rltlconv_LTLModelCounter();
    }

//	public BigInteger count(List<LabelledFormula> formulas, int bound, boolean exhaustive, boolean positive) throws IOException, InterruptedException{
//		ABC abc = new ABC();
//		LinkedList<String> abcStrs = new LinkedList<>();
//		for(LabelledFormula f: formulas){
//			String abcStr = genABCString(f);
//			if (abcStr != null)
//				abcStrs.add(abcStr);
//		}
//		BigInteger count = BigInteger.ZERO;
//		if(translatorLTLtoRE.encoded_alphabet==0)
//			count = abc.count(abcStrs,bound*2, exhaustive,positive);//each state is characterised by 2 characters
//		else if(translatorLTLtoRE.encoded_alphabet==1)
//			count = abc.count(abcStrs,bound*3, exhaustive,positive);//each state is characterised by 3 characters
//		else
//			count = abc.count(abcStrs,bound, exhaustive,positive);
//		if (!exhaustive)
//			return count;
//		else {
//			BigInteger res = count.divide(BigInteger.valueOf(bound));
//			return res;
//		}
//	}


    public BigInteger count(LabelledFormula formula, int bound, boolean exhaustive, boolean positive) throws IOException, InterruptedException {
        String abcRE = genABCString(formula);
        String[] arr = Discretizer.or(abcRE);
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < arr.length; i++) {
            BigInteger count;
            LinkedList<String> abcStrs = new LinkedList<>();
            String s = translatorLTLtoRE.toABClanguage(arr[i]);
            abcStrs.add(s);
            System.out.print(arr[i].length() + " ");
            ABC abc = new ABC();
            if (translatorLTLtoRE.encoded_alphabet == 0)
                count = abc.count(abcStrs, bound * 2, exhaustive, positive);//each state is characterised by 2 characters
            else if (translatorLTLtoRE.encoded_alphabet == 1)
                count = abc.count(abcStrs, bound * 3, exhaustive, positive);//each state is characterised by 3 characters
            else
                count = abc.count(abcStrs, bound, exhaustive, positive);
            System.out.print(count + "; ");
            result = result.add(count);
        }
//        System.out.print(" "+ result);
        return result;

    }

    public String genABCString(LabelledFormula formula) throws IOException, InterruptedException {
//		String ltl = SolverUtils.toLambConvSyntax(formula.formula().toString());
//		String alph = SolverUtils.createLambConvAlphabet(formula);
        List<String> alphabet = SolverUtils.genAlphabet(formula.variables().size());
        LabelledFormula label_formula = LabelledFormula.of(formula.formula(), alphabet);
        String ltl = SolverUtils.toLambConvSyntax(label_formula.toString());
        String alph = alphabet.toString();

        String form = "LTL=" + ltl;
        if (alph != null && !alph.equals(""))
            form += ",ALPHABET=" + alph;
        if (alph != null && formula.variables().size() > 5 && formula.variables().size() < 12)
            translatorLTLtoRE.encoded_alphabet = 0;
        else if (alph != null && formula.variables().size() >= 12)
            translatorLTLtoRE.encoded_alphabet = 1;
//		System.out.println("Translating from LTL to NBA...");

//		translatorLTLtoRE = new Rltlconv_LTLModelCounter();
        System.out.println(form);
        return translatorLTLtoRE.ltl2RE(form);
    }


}
