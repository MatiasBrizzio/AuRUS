package geneticalgorithm;

import java.util.List;
import java.util.Random;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.tree.ParseTree;

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
		
//		// assumptions must be weaken/mutated
//		boolean weakAssumptions = !status.areAssumptionsSAT() || (status == SPEC_STATUS.CONTRADICTORY);
//		// assumptions must be strengthen/mutated
//		boolean strengthenAssumptions = false;
//		
//		// guarantees must be weaken/mutated
//		boolean weakGuarantees = !status.areGuaranteesSAT();
//	
//		// consider the particular situation when specification is sat but unrealizable		
//		if (status == SPEC_STATUS.UNREALIZABLE) {
//			weakAssumptions = false;
//			strengthenAssumptions = true;
//			weakGuarantees = true;
//		}
		
		if (Settings.RANDOM_GENERATOR.nextBoolean()) {
		// mutate assumptions	
			
//			if (weakAssumptions) {	
				Formula new_assume = BooleanConstant.TRUE;
				int modification =  Settings.RANDOM_GENERATOR.nextInt(3);
				if (modification == 0) {
					// arbitrary mutation
					new_assume = mutateFormula(spec.assume(), spec.variables());
				}
				else if (modification == 1) {
					// weaken mutation
					new_assume = weakenFormula(spec.assume(), spec.variables());
				}
				else {
					// strengthen mutation
					new_assume = strengthenFormula(spec.assume(), spec.variables());
				}
				
				new_spec = TLSF_Utils.change_assume(new_spec, new_assume);
//			}
//			
//			if (strengthenAssumptions) {
//				Formula new_assumptions = BooleanConstant.FALSE;
//				int modification =  Settings.RANDOM_GENERATOR.nextInt(2);
//				if (modification == 0) {
//					// arbitrary mutation
//					new_assumptions = mutateFormula(spec.assume(), spec.variables());
//				}
//				else {
//					// weaken mutation
//					new_assumptions = strengthenFormula(spec.assume(), spec.variables());
//				}
//				new_spec = TLSF_Utils.change_assume(new_spec, new_assumptions);
//			}
		}
		else {
			//mutate guarantees
//			if (weakGuarantees) {
				Formula new_guarantees = BooleanConstant.TRUE;
				int modification =  Settings.RANDOM_GENERATOR.nextInt(3);
				if (modification == 0) {
					// arbitrary mutation
					new_guarantees = mutateFormula(Conjunction.of(spec.guarantee()), spec.variables());
				}
				else if (modification == 1){
					// weaken mutation
					new_guarantees = weakenFormula(Conjunction.of(spec.guarantee()), spec.variables());
				}
				else {
					// weaken mutation
					new_guarantees = strengthenFormula(Conjunction.of(spec.guarantee()), spec.variables());
				}
				
				if (new_guarantees != BooleanConstant.FALSE)
					new_spec = TLSF_Utils.change_guarantees(new_spec, Formula_Utils.splitConjunction(new_guarantees));
//			}
			
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
