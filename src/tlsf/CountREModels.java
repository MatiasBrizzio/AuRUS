package tlsf;

import modelcounter.ABC;
import owl.ltl.LabelledFormula;

import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;


public class CountREModels {
    FormulaToRE translatorLTLtoRE;

    public CountREModels() {
        translatorLTLtoRE = new FormulaToRE();
    }

    public BigInteger count(List<LabelledFormula> formulas, int bound, boolean exhaustive, boolean positive) throws IOException, InterruptedException {
        ABC abc = new ABC();
        LinkedList<String> abcStrs = new LinkedList<>();
        for (LabelledFormula f : formulas) {
            String abcStr = genABCString(f);
            if (abcStr != null)
                abcStrs.add(abcStr);
        }
        BigInteger count;
        if (translatorLTLtoRE.encoded_alphabet == 0)
            count = abc.count(abcStrs, bound * 2, exhaustive, positive);//each state is characterised by 2 characters
        else if (translatorLTLtoRE.encoded_alphabet == 1)
            count = abc.count(abcStrs, bound * 3, exhaustive, positive);//each state is characterised by 3 characters
        else
            count = abc.count(abcStrs, bound, exhaustive, positive);
        if (!exhaustive)
            return count;
        else {
            BigInteger res = count.divide(BigInteger.valueOf(bound));
            return res;
        }
    }

    public String genABCString(LabelledFormula ltl) throws IOException, InterruptedException {
//		translatorLTLtoRE = new FormulaToRE();
        int vars = ltl.variables().size();
        if (vars > 5 && vars < 12)
            translatorLTLtoRE.encoded_alphabet = 0;
        else if (vars >= 12)
            translatorLTLtoRE.encoded_alphabet = 1;
        translatorLTLtoRE.generateLabels(ltl.variables());
        String s = translatorLTLtoRE.formulaToRegularExpression(ltl);
        if (s == null)
            return null;
        return toABClanguage(s);
    }

    public String toABClanguage(String re) {
        String abcStr;
        abcStr = re.replace("λ", "\"\"");
        abcStr = abcStr.replace("+", "|");
        return abcStr;
    }
}
