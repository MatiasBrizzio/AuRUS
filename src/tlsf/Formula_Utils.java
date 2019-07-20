package tlsf;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import owl.ltl.BooleanConstant;
import owl.ltl.Conjunction;
import owl.ltl.Formula;
import owl.ltl.Formula.LogicalOperator;
import owl.ltl.parser.LtlParser;
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
	
	public static List<LabelledFormula> splitConjunction (LabelledFormula f){
		List<LabelledFormula> conjuncts = new LinkedList<>();
		if (f.formula() instanceof Conjunction) {
		      Conjunction conjunction = (Conjunction) f.formula();
		      for (Formula c : conjunction.children)
		    	  conjuncts.add(LabelledFormula.of(c, f.variables()));
		      
	    }
		return conjuncts;
	}

	
	public static LabelledFormula replaceSubformula (LabelledFormula f0, LabelledFormula f1) {
		if (!f0.variables().containsAll(f1.variables()))
			throw new IllegalArgumentException("Formula_Utils.replaceSubformula: formulas should have the same set of variables.");
		
		Random rand = new Random();
		// select randomly the fub formula of f0 to be replaced
		List<LabelledFormula> subformulas_f0 = subformulas(f0);
		LabelledFormula src = subformulas_f0.get(rand.nextInt(subformulas_f0.size()));
		
		// get randomly the sub formula of f1 to be used to replace in f0.
		List<LabelledFormula> subformulas_f1 = subformulas(f1);
		LabelledFormula target = subformulas_f1.get(rand.nextInt(subformulas_f1.size()));
		
		LabelledFormula f0_copy = LabelledFormula.of(f0.formula(), f0.variables());
		replaceSubformula(f0_copy.formula(), src.formula(), target.formula());
		return f0_copy;
	}
	
	public static Formula replaceSubformula (Formula f0, Formula src, Formula target) {		
		if (f0 == src)
			return target;
		if(f0.children().contains(src)) {
			f0.children().remove(src);
			f0.children().add(target);
		}
		else {
			for (Formula child : f0.children()) {
				Formula res = replaceSubformula(child, src, target);
				if (!child.equals(res))
					return res;
			}
		}
		return f0;
	}

	public static LabelledFormula replaceSubformula(LabelledFormula f, LabelledFormula src, LabelledFormula target) {
		return LtlParser.parse(f.toString().replace(src.toString(), target.toString()),f.variables());
	}
}
