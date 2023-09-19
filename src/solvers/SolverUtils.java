package solvers;

import owl.ltl.Formula;
import owl.ltl.LabelledFormula;

import java.util.*;

public class SolverUtils {
    private static final Map<String, String> replacements = Map.of(
            "&", "&&",
            "|", "||"
    );

    public static String toSolverSyntax(Formula f) {
        String ltlFormula = f.toString();
        ltlFormula = replaceSymbols(ltlFormula);
        ltlFormula = insertSpaceBeforeUppercase(ltlFormula);
        return ltlFormula;
    }

    public static String toLambConvSyntax(String ltlFormula) {
        ltlFormula = insertSpaceBeforeUppercase(ltlFormula);
        ltlFormula = replaceSymbols(ltlFormula);
        return ltlFormula;
    }

    public static List<String> genAlphabet(int n) {
        List<String> alphabet = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            char letter = (char) ('a' + i);
            alphabet.add(String.valueOf(letter));
        }
        return alphabet;
    }

    public static String toSolverSyntax(LabelledFormula f) {
        String ltlFormula = f.toString().toLowerCase();
        ltlFormula = insertSpaceBeforeUppercase(ltlFormula);
        ltlFormula = replaceSymbols(ltlFormula);
        return ltlFormula;
    }

    private static String insertSpaceBeforeUppercase(String input) {
        return input.replaceAll("([A-Z])", " $1 ");
    }

    private static String replaceSymbols(String input) {
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            String symbol = entry.getKey();
            String replacement = entry.getValue();
            input = input.replace(symbol, replacement);
        }
        return input;
    }
}
