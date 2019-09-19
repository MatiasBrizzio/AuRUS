package tlsf;

import org.junit.jupiter.api.Test;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.LtlParser;

import java.util.List;

public class FormulaToRETest {

    @Test
    public void testSimple0(){
        List<String> vars = List.of("a", "b");
        LabelledFormula f0 =  LtlParser.parse("false",vars);
        System.out.println(f0);
        FormulaToRE translatorLTLtoRE = new FormulaToRE();
        String re = translatorLTLtoRE.formulaToRegularExpression(f0);
        System.out.println(re);
    }

    @Test
    public void testSimple1(){
        List<String> vars = List.of("a", "b");
        LabelledFormula f0 =  LtlParser.parse("G(a -> F(b))",vars);
        System.out.println(f0);
        FormulaToRE translatorLTLtoRE = new FormulaToRE();
        String re = translatorLTLtoRE.formulaToRegularExpression(f0);
        System.out.println(re);
    }

    @Test
    public void testSimple2(){
        List<String> vars = List.of("a", "b");
        LabelledFormula f0 =  LtlParser.parse("G(a -> X(b))",vars);
        System.out.println(f0);
        FormulaToRE translatorLTLtoRE = new FormulaToRE();
        String re = translatorLTLtoRE.formulaToRegularExpression(f0);
        System.out.println(re);
    }

    @Test
    public void testSimple3(){
        List<String> vars = List.of("a", "b");
        LabelledFormula f0 =  LtlParser.parse("G(a -> (b))",vars);
        System.out.println(f0);
        FormulaToRE translatorLTLtoRE = new FormulaToRE();
        String re = translatorLTLtoRE.formulaToRegularExpression(f0);
        System.out.println(re);
    }
}
