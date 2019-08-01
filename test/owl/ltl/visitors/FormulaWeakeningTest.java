package owl.ltl.visitors;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.LtlParser;
import tlsf.Formula_Utils;

class FormulaWeakeningTest {

	@Test
	  void testWeaken1() throws IOException {
		  List<String> vars = List.of("grant0","grant1");
		  LabelledFormula f = LtlParser.parse("G (grant0 U grant1 | F grant1)", vars);
		  System.out.println("Before weaken subformula! "+ f.formula() + " mutation rate=" + Formula_Utils.formulaSize(f.formula()));
		  FormulaWeakening weakener = new FormulaWeakening(vars, Formula_Utils.formulaSize(f.formula()), 5);
		  Formula m = weakener.apply(f.formula());
		  System.out.println("After weaken subformula! "+ m);
	  }
	  
	  @Test
	  void testWeaken2() throws IOException {
		  List<String> vars = List.of("grant0","grant1", "grant2");
		  LabelledFormula f = LtlParser.parse("G (grant0 U grant1 & F grant1)", vars);
		  int n = Formula_Utils.formulaSize(f.formula());
		  System.out.println("Before weaken subformula! "+ f.formula() + " mutation rate=" + n);
		  FormulaWeakening weakener = new FormulaWeakening(vars, n, n);
		  Formula m = weakener.apply(f.formula());
		  System.out.println("After weaken subformula! "+ m);
	  }

}
