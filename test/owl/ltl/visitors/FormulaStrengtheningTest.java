package owl.ltl.visitors;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.LtlParser;
import tlsf.Formula_Utils;

class FormulaStrengtheningTest {

	 @Test
	  void testStrengthening1() throws IOException {
		  List<String> vars = List.of("grant0","grant1");
		  LabelledFormula f = LtlParser.parse("G (grant0 U grant1 | F grant1)", vars);
		  int n = Formula_Utils.formulaSize(f.formula());
		  System.out.println("Before stregthen subformula! "+ f.formula() + " mutation rate=" + n);
		  FormulaStrengthening visitor = new FormulaStrengthening(vars, n, n);
		  Formula m = f.formula().accept(visitor);
		  System.out.println("After stregthen subformula! "+ m);
	  }
	  
	  @Test
	  void testStrengthening2() throws IOException {
		  List<String> vars = List.of("grant0","grant1", "grant2");
		  LabelledFormula f = LtlParser.parse("G (grant0 U grant1 & F grant1)", vars);
		  int n = Formula_Utils.formulaSize(f.formula());
		  System.out.println("Before weaken subformula! "+ f.formula() + " mutation rate=" + n);
		  FormulaStrengthening visitor = new FormulaStrengthening(vars, n, n);
		  Formula m = visitor.apply(f.formula());
		  System.out.println("After weaken subformula! "+ m);
	  }

}
