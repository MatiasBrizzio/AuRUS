package owl.ltl.visitors;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import owl.ltl.BooleanConstant;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.LtlParser;
import owl.ltl.tlsf.Tlsf;
import tlsf.Formula_Utils;
import tlsf.TLSF_Utils;

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

	@Test
	void testMinePump() throws IOException, InterruptedException {
		String filename = "examples/minepump.tlsf";
		Tlsf tlsf = TLSF_Utils.toBasicTLSF(new File(filename));
		List<String> vars = tlsf.variables();
		Formula f = tlsf.toFormula().formula();
//		System.out.println("After replace subformula! "+ tlsf.toFormula() + " mutation rate=" + Formula_Utils.formulaSize(f));
		int size = Formula_Utils.formulaSize(f);
		int N = 10;
		System.out.print("\"-ltl="+ tlsf.toFormula() + "\" ");
		int i = 0;
		while(i < N) {
			FormulaMutator formVisitor = new FormulaMutator(vars, size, size);
			Formula m = f.nnf().accept(formVisitor);
			if(formVisitor.numOfAllowedMutations < size) {
				System.out.print("\"-ref=" + LabelledFormula.of(m, vars) + "\" ");
				i++;
			}
		}
	}

	@Test
	void testSimple() throws IOException, InterruptedException {
		List<String> vars = List.of("a","b","c");
		Formula f = LtlParser.syntax("G (a -> b)", vars);

//		System.out.println("After replace subformula! "+ tlsf.toFormula() + " mutation rate=" + Formula_Utils.formulaSize(f));
		int size = Formula_Utils.formulaSize(f);
		int N = 10;
		System.out.print("\"-ltl=G (a -> b)\" ");
		int i = 0;
		LinkedList<Formula> list = new LinkedList<>();
		while(i < N) {
			FormulaMutator formVisitor = new FormulaMutator(vars, size, size);
			Formula m = f.nnf().accept(formVisitor);
			if(formVisitor.numOfAllowedMutations < size && m != BooleanConstant.TRUE && m != BooleanConstant.FALSE && !list.contains(m)) {
				System.out.print("\"-ref=" + LabelledFormula.of(m, vars) + "\" ");
				list.add(m);
				i++;
			}
		}
	}

}
