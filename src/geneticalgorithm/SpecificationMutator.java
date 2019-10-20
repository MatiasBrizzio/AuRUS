package geneticalgorithm;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import geneticalgorithm.SpecificationChromosome.SPEC_STATUS;
import owl.grammar.LTLLexer;
import owl.grammar.LTLParser;
import owl.ltl.BooleanConstant;
import owl.ltl.Conjunction;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.Literal;
import owl.ltl.parser.TokenErrorListener;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.visitors.FormulaMutator;
import owl.ltl.visitors.FormulaStrengthening;
import owl.ltl.visitors.FormulaWeakening;
import tlsf.Formula_Utils;
import tlsf.TLSF_Utils;

public class SpecificationMutator {

	public static Tlsf mutate(Tlsf spec, SPEC_STATUS status) {
		//create empty specification
		Tlsf new_spec = TLSF_Utils.fromSpec(spec);

		if (Settings.RANDOM_GENERATOR.nextBoolean()) {
		// mutate assumptions	
			List<Formula> assumptions = Formula_Utils.splitConjunction(spec.assume());
			int index_to_mutate = Settings.RANDOM_GENERATOR.nextInt(assumptions.size());
			Formula assumption_to_mutate = assumptions.get(index_to_mutate);
			Formula new_assumption = BooleanConstant.TRUE;
			int modification =  Settings.RANDOM_GENERATOR.nextInt(3);
			if (modification == 0) {
				// arbitrary mutation
				new_assumption = mutateFormula(assumption_to_mutate, spec.variables());
			}
			else if (modification == 1) {
				// weaken mutation
				new_assumption = weakenFormula(assumption_to_mutate, spec.variables());
			}
			else {
				// strengthen mutation
				new_assumption = strengthenFormula(assumption_to_mutate, spec.variables());
			}
			if (new_assumption != BooleanConstant.FALSE) {
				assumptions.remove(index_to_mutate);
				assumptions.add(index_to_mutate, new_assumption);
				new_spec = TLSF_Utils.change_assume(new_spec, assumptions);
			}
		}
		else {
			List<Formula> guarantees = new LinkedList<>(spec.guarantee());
			int index_to_mutate = Settings.RANDOM_GENERATOR.nextInt(guarantees.size());
			Formula guarantee_to_mutate = guarantees.get(index_to_mutate);
			Formula new_guarantee = BooleanConstant.TRUE;
			int modification =  Settings.RANDOM_GENERATOR.nextInt(3);
			if (modification == 0) {
				// arbitrary mutation
				new_guarantee = mutateFormula(guarantee_to_mutate, spec.variables());
			}
			else if (modification == 1){
				// weaken mutation
				new_guarantee = weakenFormula(guarantee_to_mutate, spec.variables());
			}
			else {
				// weaken mutation
				new_guarantee = strengthenFormula(guarantee_to_mutate, spec.variables());
			}

			if (new_guarantee != BooleanConstant.FALSE) {
				guarantees.remove(index_to_mutate);
				guarantees.add(index_to_mutate, new_guarantee);
				Formula new_guarantees = Conjunction.of(guarantees);
				new_spec = TLSF_Utils.change_guarantees(new_spec, new_guarantees);
			}

		}
		
		return new_spec;
	}


	public static Formula mutateFormula (Formula f, List<String> variables) {
		int n = Formula_Utils.formulaSize(f);
		FormulaMutator formVisitor = new FormulaMutator(variables, n, n);
		Formula m = f.nnf().accept(formVisitor);
		return m;
	}

	public static Formula weakenFormula (Formula f, List<String> variables) {
		int n = Formula_Utils.formulaSize(f);
		FormulaWeakening formVisitor = new FormulaWeakening(variables, n, n);
		Formula m = f.nnf().accept(formVisitor);
		return m;
	}

	public static Formula strengthenFormula (Formula f, List<String> variables) {
		int n = Formula_Utils.formulaSize(f);
		FormulaStrengthening formVisitor = new FormulaStrengthening(variables, n, n);
		Formula m = f.nnf().accept(formVisitor);
		return m;
	}
}
