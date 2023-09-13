package modelcounter;

import gov.nasa.ltl.trans.ParseErrorException;
import org.junit.Test;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.LtlParser;
import owl.ltl.parser.TlsfParser;
import owl.ltl.rewriter.SyntacticSimplifier;
import owl.ltl.tlsf.Tlsf;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

public class AutomataBasedMCTest {

//	@Test
//	public void test0() throws ParseErrorException, IOException, InterruptedException  {
//		String formula = "LTL=G((p && q) -> X(X(! h)))";
//
//		AutomataBasedModelCounting counter = new AutomataBasedModelCounting(formula);
//		BigInteger d =  counter.eval(4);
//		assertTrue (d.compareTo(BigInteger.ZERO) >= 0);
//	}

    @Test
    public void test1() throws ParseErrorException, IOException, InterruptedException {
        List<String> vars = List.of("a", "b");
        LabelledFormula formula = LtlParser.parse("G(a & b)", vars);
        AutomataBasedModelCounting counter = new AutomataBasedModelCounting(formula, false);
        BigInteger d = counter.count(2);
        System.out.println(d);
    }

    @Test
    public void test2() throws ParseErrorException, IOException, InterruptedException {
        List<String> vars = List.of("a", "b");
        LabelledFormula formula = LtlParser.parse("G(a -> X(b))", vars);

        AutomataBasedModelCounting counter = new AutomataBasedModelCounting(formula, false);
        BigInteger d = counter.count(4);
        System.out.println(d);
    }


    @Test
    public void test3() throws ParseErrorException, IOException, InterruptedException {
        List<String> vars = List.of("a", "b");
        LabelledFormula formula = LtlParser.parse("F (a && b)", vars);
        AutomataBasedModelCounting counter = new AutomataBasedModelCounting(formula, false);
        BigInteger d = counter.count(3);
        System.out.println(d);
    }

    @Test
    public void testMinepump() throws ParseErrorException, IOException, InterruptedException {

        FileReader f = new FileReader("examples/minepump.tlsf");
        Tlsf spec = TlsfParser.parse(f);

        FileReader f2 = new FileReader("examples/minepump-3.tlsf");
        Tlsf spec2 = TlsfParser.parse(f2);

//		Formula cnf = Conjunction.of(spec.toFormula().formula().not(),spec2.toFormula().formula().not());
//		SyntacticSimplifier simp = new SyntacticSimplifier();
//	    Formula simplified = cnf.accept(simp);
//		LabelledFormula formula = LabelledFormula.of(simplified, spec.variables());
        LabelledFormula formula = LtlParser.parse(spec.toFormula().not().toString() + " && " + spec2.toFormula().not().toString(), spec.variables());
        System.out.println(formula);
        AutomataBasedModelCounting counter = new AutomataBasedModelCounting(formula, false);
        BigInteger d = counter.count(5);
        System.out.println(d);
    }

    @Test
    public void testMinePumpBrokenMC() throws ParseErrorException, IOException, InterruptedException {
        List<String> vars = List.of("methane", "high_water", "pump_on");
//		LabelledFormula formula =  LtlParser.parse("((F(X!p2&p1)|F(Xp2&p0))&F(X!p2&p1)&G(X(X!p1|!p1)|!p2|!p1))",vars);
//		LabelledFormula formula =  LtlParser.parse("((F((methane & X(pump_on))) | F((high_water & X(!pump_on)))) & (F((methane & X(pump_on))) | F((!methane & high_water & X(!pump_on)))) & G((!high_water | !pump_on | X((!high_water | X(!high_water))))))", vars);
        LabelledFormula formula = LtlParser.parse("((F((methane & X(pump_on))) | F((high_water & X(!pump_on)))) & (F((methane & X(pump_on))) | F((!methane & high_water & X(!pump_on)))) & G((!high_water | !pump_on | X((!high_water | X(!high_water))))))", vars);

        AutomataBasedModelCounting counter = new AutomataBasedModelCounting(formula, false);
        BigInteger d = counter.count(5);
        System.out.println(d);
    }

    @Test
    public void testMinePumpBroken() throws ParseErrorException, IOException, InterruptedException {
        List<String> vars = List.of("methane", "high_water", "pump_on");
        LabelledFormula formula = LtlParser.parse("(((G((!methane | X(!pump_on))) & G((!high_water | X(pump_on)))) | F((high_water & pump_on & X((high_water & X(high_water)))))) & ((G((!methane | X(!pump_on))) & G((methane | !high_water | X(pump_on)))) | F((high_water & pump_on & X((high_water & X(high_water)))))))", vars);

        AutomataBasedModelCounting counter = new AutomataBasedModelCounting(formula, true);
        BigInteger d = counter.count(10);
        System.out.println(d);
    }

    @Test
    public void testDetector() throws ParseErrorException, IOException, InterruptedException {

        FileReader f = new FileReader("examples/syntcomp2019/unreal/9158508/detector_unreal_4_basic.tlsf");
        Tlsf spec = TlsfParser.parse(f);

        Formula cnf = spec.toFormula().formula();
        SyntacticSimplifier simp = new SyntacticSimplifier();
        Formula simplified = cnf.accept(simp);
        System.out.println(simplified);
        LabelledFormula formula = LabelledFormula.of(simplified, spec.variables());

        AutomataBasedModelCounting counter = new AutomataBasedModelCounting(formula, true);
        BigInteger d = counter.count(5);
        System.out.println(d);
    }

}