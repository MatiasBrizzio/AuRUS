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

public class FormulaToRETest {

    @Test
    public void testSimple0() {
        List<String> vars = List.of("a", "b");
        LabelledFormula f0 = LtlParser.parse("false", vars);
        System.out.println(f0);
        FormulaToRE translatorLTLtoRE = new FormulaToRE();
        String re = translatorLTLtoRE.formulaToRegularExpression(f0);
        System.out.println(re);
    }

    @Test
    public void testSimple1() {
        List<String> vars = List.of("a", "b");
        LabelledFormula f0 = LtlParser.parse("G(a -> F(b))", vars);
        System.out.println(f0);
        FormulaToRE translatorLTLtoRE = new FormulaToRE();
        translatorLTLtoRE.generateLabels(f0.variables());
        String re = translatorLTLtoRE.formulaToRegularExpression(f0);
        System.out.println(re);
    }

    @Test
    public void testSimple2() {
        List<String> vars = List.of("a", "b");
        LabelledFormula f0 = LtlParser.parse("G(a -> X(b))", vars);
        System.out.println(f0);
        FormulaToRE translatorLTLtoRE = new FormulaToRE();
        translatorLTLtoRE.generateLabels(f0.variables());
        String re = translatorLTLtoRE.formulaToRegularExpression(f0);
        System.out.println(re);
    }

    @Test
    public void testSimple3() {
        List<String> vars = List.of("a", "b");
        LabelledFormula f0 = LtlParser.parse("G(a -> (b))", vars);
        System.out.println(f0);
        FormulaToRE translatorLTLtoRE = new FormulaToRE();
        translatorLTLtoRE.generateLabels(f0.variables());
        String re = translatorLTLtoRE.formulaToRegularExpression(f0);
        System.out.println(re);
    }

    @Test
    public void testMinepump0() throws IOException, InterruptedException {
        String filename = "examples/minepump-2.tlsf";
        Tlsf tlsf = TLSF_Utils.toBasicTLSF(new File(filename));
        List<String> vars = tlsf.variables();
        LabelledFormula orig = tlsf.toFormula();
        SyntacticSimplifier simp = new SyntacticSimplifier();
        Formula simplified = orig.formula().accept(simp);
        LabelledFormula f0 = LabelledFormula.of(simplified, vars);
        System.out.println(f0);
        FormulaToRE translatorLTLtoRE = new FormulaToRE();
        translatorLTLtoRE.generateLabels(vars);
        String re = translatorLTLtoRE.formulaToRegularExpression(f0);
        System.out.println(re);
    }


}
