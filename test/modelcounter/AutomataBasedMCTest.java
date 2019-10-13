package modelcounter;

import static org.junit.Assert.assertTrue;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;

import org.ejml.data.DMatrixRMaj;
import org.junit.Test;

import gov.nasa.ltl.graph.Edge;
import gov.nasa.ltl.graph.Graph;
import gov.nasa.ltl.graph.Node;
import gov.nasa.ltl.graphio.Writer;
import gov.nasa.ltl.trans.LTL2Buchi;
import gov.nasa.ltl.trans.ParseErrorException;
import owl.ltl.Conjunction;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.LtlParser;
import owl.ltl.parser.TlsfParser;
import owl.ltl.rewriter.NormalForms;
import owl.ltl.rewriter.SyntacticSimplifier;
import owl.ltl.tlsf.Tlsf;

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
	public void test1() throws ParseErrorException, IOException, InterruptedException  {
		List<String> vars = List.of("a", "b");
		LabelledFormula formula =  LtlParser.parse("G F (!b) -> G F(a)",vars);
		AutomataBasedModelCounting counter = new AutomataBasedModelCounting(formula,true);
		BigInteger d =  counter.count(2);
		System.out.println(d);
	}

	@Test
	public void testMinepump() throws ParseErrorException, IOException, InterruptedException  {

		FileReader f = new FileReader("examples/minepump.tlsf");
		Tlsf spec = TlsfParser.parse(f);

		FileReader f2 = new FileReader("examples/minepump-4.tlsf");
		Tlsf spec2 = TlsfParser.parse(f2);

		Formula cnf = Conjunction.of(spec.toFormula().formula(),spec2.toFormula().formula());
		SyntacticSimplifier simp = new SyntacticSimplifier();
	    Formula simplified = cnf.accept(simp);
		LabelledFormula formula = LabelledFormula.of(simplified, spec.variables());

		AutomataBasedModelCounting counter = new AutomataBasedModelCounting(formula,true);
		BigInteger d =  counter.count(5);
		System.out.println(d);
	}

}