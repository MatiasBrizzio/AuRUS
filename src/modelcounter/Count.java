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

    public BigInteger count(LabelledFormula formula, int bound, boolean exhaustive, boolean positive) throws IOException, InterruptedException {
        String abcRE = genABCString(formula);
        String[] arr = Discretizer.or(abcRE);
        BigInteger result = BigInteger.ZERO;
        for (String string : arr) {
            BigInteger count;
            LinkedList<String> abcStrs = new LinkedList<>();
            String s = translatorLTLtoRE.toABClanguage(string);
            abcStrs.add(s);
            System.out.print(string.length() + " ");
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
        return result;

    }

    public String genABCString(LabelledFormula formula) throws IOException, InterruptedException {
        List<String> alphabet = SolverUtils.genAlphabet(formula.variables().size());
        LabelledFormula label_formula = LabelledFormula.of(formula.formula(), alphabet);
        String ltl = SolverUtils.toLambConvSyntax(label_formula.toString());
        String alph = alphabet.toString();
        String form = "LTL=" + ltl;
        if (alph != null && !alph.isEmpty())
            form += ",ALPHABET=" + alph;
        if (alph != null && formula.variables().size() > 5 && formula.variables().size() < 12)
            translatorLTLtoRE.encoded_alphabet = 0;
        else if (alph != null && formula.variables().size() >= 12)
            translatorLTLtoRE.encoded_alphabet = 1;
        System.out.println(form);
        return translatorLTLtoRE.ltl2RE(form);
    }


}
