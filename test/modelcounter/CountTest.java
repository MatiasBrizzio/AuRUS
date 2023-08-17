package modelcounter;

import static org.junit.jupiter.api.Assertions.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import main.Settings;
import org.junit.jupiter.api.Test;

import owl.ltl.Conjunction;
import owl.ltl.Disjunction;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.LtlParser;
import owl.ltl.parser.TlsfParser;
import owl.ltl.rewriter.NormalForms;
import owl.ltl.tlsf.Tlsf;
import solvers.LTLSolver;
import solvers.SolverUtils;
import solvers.StrixHelper;
import tlsf.CountREModels;

class CountTest {

	 @Test
    public void testSimple1() throws IOException, InterruptedException{
        List<String> vars = List.of("a", "b");
        LabelledFormula f0 =  LtlParser.parse("G F (!b) -> G F(a)",vars);
//        List<LabelledFormula> list = new LinkedList();
//        list.add(f0);
        System.out.println(f0);
        for(int i=0;i<100;i++) {
        Count counter = new Count();
		BigInteger result = counter.count(f0, 5,false,true);
        System.out.println(result);
        }
    }
	 
	@Test
	void test() throws IOException, InterruptedException {
		FileReader f = new FileReader("examples/minepump.tlsf");
		Tlsf spec = TlsfParser.parse(f);
		
		FileReader f2 = new FileReader("examples/minepump-2.tlsf");
		Tlsf spec2 = TlsfParser.parse(f2);
//		List<LabelledFormula> formulas = new LinkedList<>();
//		formulas.add(spec.toFormula());
//		LabelledFormula form = LtlParser.parse(spec.toFormula().toString() + " && " + spec2.toFormula().toString(), spec.variables());
		
		for(int i=0;i<10;i++) {
			Count counter = new Count();
			Formula cnf = NormalForms.toCnfFormula(Conjunction.of(spec.toFormula().formula(),spec2.toFormula().formula()));
			LabelledFormula form = LabelledFormula.of(cnf, spec.variables());
			
			System.out.println(form);
			BigInteger result = counter.count(form, 5,false,true);
			System.out.println(result);
		}
	}


	@Test
	public void testSimple2() throws IOException, InterruptedException{
		List<String> vars = List.of("p", "e0", "e1", "h1","h0");
		Settings.USE_DOCKER = false;
		Formula assumption = LtlParser.parse("G(p) || G (!p)",vars).formula();
		Formula g1 = LtlParser.parse("G(!e0 || !e1)",vars).formula();
		Formula g2 = LtlParser.parse("G(F(!h0 || e0))",vars).formula();
		Formula g3 = LtlParser.parse("G(F(!h1 || e1))",vars).formula();
		Formula g4 = LtlParser.parse("G(p) -> G (!e0 && !e1)",vars).formula();
		Formula spec = Disjunction.of(assumption.not(), Conjunction.of(g1,g2,g3,g4));
		System.out.println(LabelledFormula.of(spec,vars).toString());
		System.out.println(LTLSolver.isSAT(spec.toString()));
		System.out.println(StrixHelper.executeStrix(LabelledFormula.of(spec,vars).toString(), "h1,h0,p", "e0,e1"));
		Formula spec2 = Disjunction.of(assumption.not(), Conjunction.of(g2));
		System.out.println(LTLSolver.isSAT(spec2.toString()));
		System.out.println(StrixHelper.executeStrix(LabelledFormula.of(spec2,vars).toString(), "h1,h0,p", "e0,e1"));
	}




}