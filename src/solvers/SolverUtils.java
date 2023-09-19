package solvers;

import owl.ltl.Formula;

import java.util.LinkedList;
import java.util.List;

public class SolverUtils {

    public static String toSolverSyntax(Formula f) {
        String LTLFormula = f.toString();
        LTLFormula = LTLFormula.replaceAll("\\!", "~");
        LTLFormula = LTLFormula.replaceAll("([A-Z])", " $1 ");
        return LTLFormula;
    }

    public static String toLambConvSyntax(String LTLFormula) {
        LTLFormula = LTLFormula.replaceAll("\\!", " ! ");
        LTLFormula = LTLFormula.replaceAll("&", "&&");
        LTLFormula = LTLFormula.replaceAll("\\|", "||");
        return LTLFormula;
    }

    public static List<String> genAlphabet(int n) {
        List<String> alphabet = new LinkedList<>();
        for (int i = 0; i < n; i++) {
            String v = "" + Character.toChars(97 + i)[0];
            alphabet.add(v);
        }
        return alphabet;
    }
}
