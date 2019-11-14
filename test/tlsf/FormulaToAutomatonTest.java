package tlsf;

import org.junit.jupiter.api.Test;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.LtlParser;
import owl.ltl.rewriter.SyntacticSimplifier;
import owl.ltl.tlsf.Tlsf;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FormulaToAutomatonTest {


    @Test
    public void testAutomata(){
        List<String> vars = List.of("a", "b");
//        LabelledFormula f0 = LtlParser.parse("(G(G!p4||!p0||!p1)&&((GFp3&&GFp2&&GFp1&&GFp0) <-> GFp4)&&G(G!p4||!p0||!p2)&&G(G!p4||!p1||!p2)&&G(!p3||G!p4||!p1)&&G(!p3||G!p4||!p2)&&G(!p3||G!p4||!p0))");
        LabelledFormula f0 =  LtlParser.parse("G F (a && (b))",vars);
        System.out.println(f0);
        FormulaToAutomaton translator = new FormulaToAutomaton();
        translator.generateLabels(vars);
        automata.Automaton dfa = translator.formulaToDfa(f0);
        System.out.println(dfa);
    }

    @Test
    public void testAutomataExamples() throws IOException, InterruptedException {
        String filename = "examples/firefighting.tlsf";
        Tlsf tlsf = TLSF_Utils.toBasicTLSF(new File(filename));
        List<String> vars = tlsf.variables();
        LabelledFormula f0 =  tlsf.toFormula();
        System.out.println(f0);
        FormulaToAutomaton translator = new FormulaToAutomaton();
        translator.generateLabels(vars);
        automata.Automaton dfa = translator.formulaToDfa(f0);
        System.out.println(dfa);
    }
}
