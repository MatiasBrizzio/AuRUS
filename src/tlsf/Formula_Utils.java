package tlsf;

import java.awt.*;
import java.util.*;
import java.util.List;

import automata.State;
import automata.fsa.FiniteStateAutomaton;
import main.Settings;
import owl.automaton.Automaton;
import owl.automaton.acceptance.BuchiAcceptance;
import owl.automaton.edge.Edge;
import owl.collections.ValuationSet;
import owl.grammar.LTLLexer;
import owl.grammar.LTLParser;
import owl.ltl.*;
import owl.ltl.Formula.LogicalOperator;
import owl.ltl.parser.LtlParser;
import owl.ltl.parser.TokenErrorListener;
import owl.ltl.visitors.SubformulaReplacer;
import owl.run.DefaultEnvironment;
import owl.translations.LTL2DAFunction;

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

	public static Set<Formula> subformulas (Formula f) {//, List<String> variables) {
		Set<Formula> s = new HashSet<>();

		for (Formula c : f.children()) {
			for(Formula e : subformulas(c))
				s.add(e);
		}
		s.add(f);
		return s;
	}

	public static int compare(Formula f0, Formula f1) {
		return Formulas.compare(subformulas(f0), subformulas(f1));
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
		      	if (c != BooleanConstant.TRUE)
		      		conjuncts.addAll(splitConjunction(c));
	    }
		else if (f != BooleanConstant.TRUE)
			conjuncts.add(f);
		return conjuncts;
	}

	public static List<Formula> splitConjunctions(List<Formula> formulas){
		List<Formula> conjuncts = new LinkedList<>();
		for (Formula f : formulas) {
			if (f instanceof Conjunction) {
				Conjunction conjunction = (Conjunction) f;
				for (Formula c : conjunction.children)
					if (c != BooleanConstant.TRUE)
						conjuncts.addAll(splitConjunction(c));
			} else if (f != BooleanConstant.TRUE)
				conjuncts.add(f);
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

	public static Formula replaceSubformula (Formula f0, Formula f1) {
		Random rand = Settings.RANDOM_GENERATOR;
		// select randomly the fub formula of f0 to be replaced
		Set subformulas_f0 = f0.subformulas(Formula.TemporalOperator.class);
		if (subformulas_f0.isEmpty())
			return null;
		Formula src = (Formula) subformulas_f0.toArray()[rand.nextInt(subformulas_f0.size())];
//		System.out.println("Selected source formula "+ src);

		// get randomly the sub formula of f1 to be used to replace in f0.
		Set subformulas_f1 = f1.subformulas(Formula.TemporalOperator.class);
		if (subformulas_f1.isEmpty())
			return null;
		Formula target = (Formula)subformulas_f1.toArray()[rand.nextInt(subformulas_f1.size())];
//		System.out.println("Selected target formula "+target);

		//replaceSubformula(f0_copy.formula(), src.formula(), target.formula());
		SubformulaReplacer visitor = new SubformulaReplacer(src,target);
		Formula replaced_formula = f0.accept(visitor);
		return replaced_formula;
	}

	public static Formula combineSubformula (Formula f0, Formula f1) {
		Random rand = Settings.RANDOM_GENERATOR;
		// select randomly the fub formula of f0 to be replaced
		Set subformulas_f0 = f0.subformulas(Formula.TemporalOperator.class);
		if (subformulas_f0.isEmpty())
			return null;
		Formula left = (Formula) subformulas_f0.toArray()[rand.nextInt(subformulas_f0.size())];
//		System.out.println("Selected source formula "+ src);

		// get randomly the sub formula of f1 to be used to replace in f0.
		Set subformulas_f1 = f1.subformulas(Formula.TemporalOperator.class);
		if (subformulas_f1.isEmpty())
			return null;
		Formula right = (Formula)subformulas_f1.toArray()[rand.nextInt(subformulas_f1.size())];
//		System.out.println("Selected target formula "+target);

		//replaceSubformula(f0_copy.formula(), src.formula(), target.formula());
		//0:& 1:| 2:U 3:W 4:M
		int op = Settings.RANDOM_GENERATOR.nextInt(4);
		if (op == 0)
			right = Conjunction.of(left, right);
		else if (op == 1)
			right = Disjunction.of(left, right);
		else if (op == 2) {
			if (Settings.RANDOM_GENERATOR.nextBoolean())
				right = UOperator.of(left, right);
			else
				right = UOperator.of(right, left);
		}
		else if (op == 3) {
			if (Settings.RANDOM_GENERATOR.nextBoolean())
				right = WOperator.of(left, right);
			else
				right = WOperator.of(right, left);
		}

		SubformulaReplacer visitor = new SubformulaReplacer(left,right);
		Formula replaced_formula = f0.accept(visitor);
		return replaced_formula;
	}

	public static int numOfTemporalOperators(Formula formula) {
		if (formula == null || formula instanceof Literal)
			return 0;
		if (formula instanceof Formula.TemporalOperator && !(formula instanceof  XOperator)) {
			int max = 0;
			for (Formula c : formula.children()) {
				int aux = numOfTemporalOperators(c);
				if (max < aux)
					max = aux;
			}
			return max + 1;
		}
		if (formula instanceof Formula.LogicalOperator) {
			int max = 0;
			for (Formula c : formula.children()) {
				int aux = numOfTemporalOperators(c);
				if (max < aux)
					max = aux;
			}
			return max;
		}
		return 0;
	}

}
