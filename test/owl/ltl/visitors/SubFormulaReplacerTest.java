package owl.ltl.visitors;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.LtlParser;

class SubFormulaReplacerTest {

	  
	  @Test
	  void testSubFormulaReplacer() throws IOException {
		  List<String> vars = List.of("grant0","grant1","grant2");
		  LabelledFormula f = LtlParser.parse("G (grant1 || F ( grant0 && grant2))", vars);
		  LabelledFormula src = LtlParser.parse("F ( grant0 && grant2)",vars);
		  LabelledFormula trg = LtlParser.parse("G (grant2 && grant0)",vars);
		  System.out.println("before repalce subformula! "+ LabelledFormula.of(f.formula(), vars));
		  SubformulaReplacer visitor = new SubformulaReplacer(src.formula(),trg.formula());
		  Formula m = f.formula().accept(visitor);
		  System.out.println("After reaplace subformula! "+ LabelledFormula.of(m, vars));
	  }
	  
	  @Test
	  void testSubFormulaReplacer2() throws IOException {
		  List<String> vars = List.of("grant0","grant1","grant2");
		  LabelledFormula f = LtlParser.parse("F (grant1 || F ( grant0 && grant2)) ", vars);
		  LabelledFormula src = LtlParser.parse("F (grant1 || F ( grant0 && grant2))",vars);
		  LabelledFormula trg = LtlParser.parse("grant1",vars);
		  System.out.println("before repalce subformula! "+ LabelledFormula.of(f.formula(), vars));
		  SubformulaReplacer visitor = new SubformulaReplacer(src.formula(),trg.formula());
		  Formula m = f.formula().accept(visitor);
		  System.out.println("After reaplace subformula! "+ LabelledFormula.of(m, vars));
	  }
	  
	  @Test
	  void testSubFormulaReplacer3() throws IOException {
		  List<String> vars = List.of("grant0","grant1","grant2");
		  LabelledFormula f = LtlParser.parse("(F (grant1 || F ( grant0 && grant2))) U grant2", vars);
		  LabelledFormula src = LtlParser.parse("F (grant1 || F ( grant0 && grant2))",vars);
		  LabelledFormula trg = LtlParser.parse("grant1",vars);
		  System.out.println("before repalce subformula! "+ LabelledFormula.of(f.formula(), vars));
		  SubformulaReplacer visitor = new SubformulaReplacer(src.formula(),trg.formula());
		  Formula m = f.formula().accept(visitor);
		  System.out.println("After reaplace subformula! "+ LabelledFormula.of(m, vars));
	  }
	  
	  @Test
	  void testSubFormulaReplacer4() throws IOException {
		  List<String> vars = List.of("grant0","grant1","grant2");
		  LabelledFormula f = LtlParser.parse("(F (grant1 || F ( grant0 && grant2))) U grant2", vars);
		  LabelledFormula src = LtlParser.parse("F (grant1 || F ( grant0 && grant2))",vars);
		  LabelledFormula trg = LtlParser.parse("grant1 && (G (grant1 || grant2))",vars);
		  System.out.println("before repalce subformula! "+ LabelledFormula.of(f.formula(), vars));
		  SubformulaReplacer visitor = new SubformulaReplacer(src.formula(),trg.formula());
		  Formula m = f.formula().accept(visitor);
		  System.out.println("After reaplace subformula! "+ LabelledFormula.of(m, vars));
	  }
	  
	  @Test
	  void testSubFormulaReplacer5() throws IOException {
		  List<String> vars = List.of("grant0","grant1","grant2");
		  LabelledFormula f = LtlParser.parse("(F (grant1 || F ( grant0 && grant2))) U grant2", vars);
		  LabelledFormula src = LtlParser.parse("F (grant1 || F ( grant0 && grant2))",vars);
		  LabelledFormula trg = LtlParser.parse("grant1",vars);
		  System.out.println("before repalce subformula! "+ LabelledFormula.of(f.formula(), vars));
		  SubformulaReplacer visitor = new SubformulaReplacer(src.formula(),trg.formula());
		  Formula m = f.formula().accept(visitor);
		  System.out.println("After reaplace subformula! "+ LabelledFormula.of(m, vars));
		  org.junit.Assert.assertTrue(LtlParser.parse("grant1 U grant2",vars).equals( LabelledFormula.of(m, vars)));
	  }
	  
	  
	  @Test
	  void testSubFormulaReplacer6() throws IOException {
		  List<String> vars = List.of("grant0","grant1","grant2");
		  LabelledFormula f = LtlParser.parse("(F (grant1 <-> F ( grant1 && grant2))) W (grant2 U grant1)", vars);
		  LabelledFormula src = LtlParser.parse("grant2",vars);
		  LabelledFormula trg = LtlParser.parse("grant0 <-> grant1",vars);
		  System.out.println("before repalce subformula! "+ LabelledFormula.of(f.formula(), vars));
		  SubformulaReplacer visitor = new SubformulaReplacer(src.formula(),trg.formula());
		  Formula m = f.formula().accept(visitor);
		  System.out.println("After reaplace subformula! "+ LabelledFormula.of(m, vars));
	  }
	  
	  @Test
	  void testSubFormulaReplacer7() throws IOException {
		  List<String> vars = List.of("grant0","grant1","grant2");
		  LabelledFormula f = LtlParser.parse("X (grant0)", vars);
		  LabelledFormula src = LtlParser.parse("grant0",vars);
		  LabelledFormula trg = LtlParser.parse("grant0 && (G (grant0 && grant1 U grant2))",vars);
		  System.out.println("before repalce subformula! "+ LabelledFormula.of(f.formula(), vars));
		  SubformulaReplacer visitor = new SubformulaReplacer(src.formula(),trg.formula());
		  Formula m = f.formula().accept(visitor);
		  System.out.println("After reaplace subformula! "+ LabelledFormula.of(m, vars));
	  }
	  
	  @Test
	  void testSubFormulaReplacer8() throws IOException {
		  List<String> vars = List.of("grant0","grant1","grant2");
		  LabelledFormula f = LtlParser.parse("grant0 R grant2", vars);
		  LabelledFormula src = LtlParser.parse("grant0",vars);
		  LabelledFormula trg = LtlParser.parse("grant0 && (G (grant0 && grant1 U grant2))",vars);
		  System.out.println("before repalce subformula! "+ LabelledFormula.of(f.formula(), vars));
		  SubformulaReplacer visitor = new SubformulaReplacer(src.formula(),trg.formula());
		  Formula m = f.formula().accept(visitor);
		  System.out.println("After reaplace subformula! "+ LabelledFormula.of(m, vars));
	  }
	  
	  @Test
	  void testSubFormulaReplacer9() throws IOException {
		  List<String> vars = List.of("grant0","grant1","grant2");
		  LabelledFormula f = LtlParser.parse("grant0 M grant2", vars);
		  LabelledFormula src = LtlParser.parse("grant0",vars);
		  LabelledFormula trg = LtlParser.parse("grant0 && (G (grant0 && grant1 U grant2))",vars);
		  System.out.println("before repalce subformula! "+ LabelledFormula.of(f.formula(), vars));
		  SubformulaReplacer visitor = new SubformulaReplacer(src.formula(),trg.formula());
		  Formula m = f.formula().accept(visitor);
		  System.out.println("After reaplace subformula! "+ LabelledFormula.of(m, vars));
	  }

}
