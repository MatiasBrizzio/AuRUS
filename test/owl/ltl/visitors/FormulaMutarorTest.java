package owl.ltl.visitors;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.LtlParser;
import tlsf.Formula_Utils;

class FormulaMutarorTest {

//  @Test
//  void testMutate0() throws IOException {
//	  List<String> vars = List.of("grant0","grant1");
//	  LabelledFormula f = LtlParser.parse("G (grant0 && grant1) -> grant1", vars);
//	  System.out.println("After replace subformula! "+ f.formula());
//	  Formula m = FormulaMutator.mutate(f.formula(), vars);
//	  System.out.println("After replace subformula! "+ m);
//  }
  
//@Test
//void testMutate1() throws IOException {
//	  List<String> vars = List.of("grant0","grant1");
//	  LabelledFormula f = LtlParser.parse("G (grant0 && grant1 | grant1)", vars);
//	  System.out.println("After replace subformula! "+ f.formula());
//	  Formula m = FormulaMutator.mutate(f.formula(), vars);
//	  System.out.println("After replace subformula! "+ m);
//	  
//}
  
  @Test
  void testMutate0() throws IOException {
	  List<String> vars = List.of("grant0","grant1", "grant2");
	  LabelledFormula f = LtlParser.parse("G (grant0 && grant1 | grant1)", vars);
	  System.out.println("After replace subformula! "+ f.formula() + " mutation rate=" + Formula_Utils.formulaSize(f.formula()));
	  FormulaMutator visitor = new FormulaMutator(vars, Formula_Utils.formulaSize(f.formula()), 1);
	  Formula m = f.formula().accept(visitor);
	  System.out.println("After weaken subformula! "+ m);
  }

}
