package modelcounter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import owl.ltl.Conjunction;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.LtlParser;
import owl.ltl.parser.TlsfParser;
import owl.ltl.tlsf.Tlsf;
import solvers.SolverUtils;
import tlsf.CountREModels;

class CountTest {

	 @Test
    public void testSimple1() throws IOException, InterruptedException{
        List<String> vars = List.of("a", "b");
        LabelledFormula f0 =  LtlParser.parse("G(a -> F(b))",vars);
        List<LabelledFormula> list = new LinkedList();
        list.add(f0);
        System.out.println(f0);
        Count counter = new Count();
		BigInteger result = counter.count(list, 5,false,true);
        System.out.println(result);
    }
	 
	@Test
	void test() throws IOException, InterruptedException {
		FileReader f = new FileReader("examples/minepump.tlsf");
		Tlsf spec = TlsfParser.parse(f);
		List<LabelledFormula> formulas = new LinkedList<>();
		formulas.add(spec.toFormula());
		Count counter = new Count();
		BigInteger result = counter.count(formulas, 5,false,true);
		System.out.println(result);
	}


}