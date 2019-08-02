package tlsf;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;

import owl.grammar.LTLLexer;
import owl.grammar.LTLParser;
import owl.ltl.BooleanConstant;
import owl.ltl.Conjunction;
import owl.ltl.Formula;
import owl.ltl.Formula.LogicalOperator;
import owl.ltl.parser.LtlParser;
import owl.ltl.parser.TokenErrorListener;
import owl.ltl.visitors.SubformulaReplacer;
import owl.ltl.LabelledFormula;

public class Formula_Utils {
	
	public static List<LabelledFormula> subformulas (LabelledFormula f) {//, List<String> variables) {
		List<LabelledFormula> s = new LinkedList();
		
		for (Formula c : f.formula().children()) {
			LabelledFormula sf = LabelledFormula.of(c, f.variables());
			for(LabelledFormula e : subformulas(sf))
				s.add(e);		
		}
		s.add(LabelledFormula.of(f.formula(),f.variables()));
		return s;
	}
	
	public static int formulaSize (Formula f) {//, List<String> variables) {
		int size = 1;
		for (Formula c : f.children())
			size += formulaSize(c);		
		return size;
	}
	
	public static List<LabelledFormula> splitConjunction (LabelledFormula f){
		List<LabelledFormula> conjuncts = new LinkedList<>();
		if (f.formula() instanceof Conjunction) {
		      Conjunction conjunction = (Conjunction) f.formula();
		      for (Formula c : conjunction.children)
		    	  conjuncts.add(LabelledFormula.of(c, f.variables()));
		      
	    }
		else
			conjuncts.add(f);
		return conjuncts;
	}
	
	public static List<Formula> splitConjunction (Formula f){
		List<Formula> conjuncts = new LinkedList<>();
		if (f instanceof Conjunction) {
		      Conjunction conjunction = (Conjunction) f;
		      for (Formula c : conjunction.children)
		    	  conjuncts.add(c);
		      
	    }
		else
			conjuncts.add(f);
		return conjuncts;
	}
	
	public static LabelledFormula replaceSubformula (LabelledFormula f0, LabelledFormula f1) {
		if (!f0.variables().containsAll(f1.variables()))
			throw new IllegalArgumentException("Formula_Utils.replaceSubformula: formulas should have the same set of variables.");
		
		Random rand = new Random();
		// select randomly the fub formula of f0 to be replaced
		List<LabelledFormula> subformulas_f0 = subformulas(f0);
		LabelledFormula src = subformulas_f0.get(rand.nextInt(subformulas_f0.size()));
//		System.out.println("Selected source formula "+ src);
		
		// get randomly the sub formula of f1 to be used to replace in f0.
		List<LabelledFormula> subformulas_f1 = subformulas(f1);
		LabelledFormula target = subformulas_f1.get(rand.nextInt(subformulas_f1.size()));
//		System.out.println("Selected target formula "+target);
		
		LabelledFormula f0_copy = LabelledFormula.of(f0.formula(), f0.variables());
		//replaceSubformula(f0_copy.formula(), src.formula(), target.formula());
		SubformulaReplacer visitor = new SubformulaReplacer(src.formula(),target.formula());
		Formula m = f0.formula().accept(visitor);
		f0_copy = LabelledFormula.of(m, f0.variables());
		return f0_copy;
	}
	

}
