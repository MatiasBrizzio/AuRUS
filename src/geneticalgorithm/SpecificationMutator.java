package geneticalgorithm;

import geneticalgorithm.SpecificationChromosome.SPEC_STATUS;
import main.Settings;
import owl.ltl.BooleanConstant;
import owl.ltl.Formula;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.visitors.FormulaStrengthening;
import owl.ltl.visitors.FormulaWeakening;
import owl.ltl.visitors.GeneralFormulaMutator;
import owl.ltl.visitors.SubformulaReplacer;
import tlsf.Formula_Utils;
import tlsf.TLSF_Utils;

import java.util.List;
import java.util.Set;

public class SpecificationMutator {

    public static Tlsf mutate(Tlsf spec, SPEC_STATUS status) {
        //create empty specification
        Tlsf new_spec = TLSF_Utils.fromSpec(spec);
        int random = Settings.RANDOM_GENERATOR.nextInt(100);
        if (random >= Settings.GA_GUARANTEES_PREFERENCE_FACTOR) {
            // mutate assumptions
            List<Formula> assumptions = Formula_Utils.splitConjunction(spec.assume());
            if (assumptions.isEmpty())
                assumptions.add(BooleanConstant.TRUE);
            int index_to_mutate = Settings.RANDOM_GENERATOR.nextInt(assumptions.size());
            Formula assumption_to_mutate = assumptions.get(index_to_mutate);

            List<String> vars = spec.variables();
            if (Settings.only_inputs_in_assumptions)
                vars = vars.subList(0, spec.numberOfInputs());

            //select subformula to mutate
            Set<Formula> subformulas = Formula_Utils.subformulas(assumption_to_mutate);
            int n = subformulas.size();
            Formula to_mutate = (Formula) subformulas.toArray()[Settings.RANDOM_GENERATOR.nextInt(n)];


            Formula mutated_subformula;
            int modification = Settings.RANDOM_GENERATOR.nextInt(3);
            if (modification == 0) {
                // arbitrary mutation
                mutated_subformula = applyGeneralMutation(to_mutate, vars);
            } else if (modification == 1) {
                // weaken mutation
                mutated_subformula = weakenFormula(to_mutate, vars);
            } else {
                // strengthen mutation
                mutated_subformula = strengthenFormula(to_mutate, vars);
            }
            SubformulaReplacer visitor = new SubformulaReplacer(to_mutate, mutated_subformula);
            Formula new_assumption = assumption_to_mutate.accept(visitor);

//			Formula new_assumption = applyGeneralMutation(assumption_to_mutate, vars);
            if (new_assumption != BooleanConstant.FALSE) {
                assumptions.remove(index_to_mutate);
                assumptions.add(index_to_mutate, new_assumption);
                new_spec = TLSF_Utils.change_assume(new_spec, assumptions);
            }
        } else {
            List<Formula> guarantees = Formula_Utils.splitConjunctions(spec.guarantee());
            if (guarantees.isEmpty())
                guarantees.add(BooleanConstant.TRUE);
            int index_to_mutate = Settings.RANDOM_GENERATOR.nextInt(guarantees.size());
            Formula guarantee_to_mutate = guarantees.get(index_to_mutate);

            //select subformula to mutate
            Set<Formula> subformulas = Formula_Utils.subformulas(guarantee_to_mutate);
            int n = subformulas.size();
            Formula to_mutate = (Formula) subformulas.toArray()[Settings.RANDOM_GENERATOR.nextInt(n)];

            Formula mutated_subformula;
            int modification = Settings.RANDOM_GENERATOR.nextInt(3);
            if (modification == 0) {
                // arbitrary mutation
                mutated_subformula = strengthenFormula(to_mutate, spec.variables());
            } else if (modification == 1) {
                // weaken mutation
                mutated_subformula = weakenFormula(to_mutate, spec.variables());
            } else {
                // weaken mutation
                mutated_subformula = applyGeneralMutation(to_mutate, spec.variables());
            }

            SubformulaReplacer visitor = new SubformulaReplacer(to_mutate, mutated_subformula);
            Formula new_guarantee = guarantee_to_mutate.accept(visitor);


            if (new_guarantee != BooleanConstant.FALSE) {
                guarantees.remove(index_to_mutate);
                guarantees.add(index_to_mutate, new_guarantee);
                new_spec = TLSF_Utils.change_guarantees(new_spec, guarantees);
            }
        }
        return new_spec;
    }

    public static Formula applyGeneralMutation(Formula f, List<String> variables) {
        int n = Formula_Utils.formulaSize(f);
        int MR = Math.max(1, ((100 - Settings.GA_GENE_MUTATION_RATE) / 100) * n);
        int num_of_mut = n;
        if (Settings.GA_GENE_NUM_OF_MUTATIONS > 0)
            num_of_mut = Math.min(n, Settings.GA_GENE_NUM_OF_MUTATIONS);
        GeneralFormulaMutator formVisitor = new GeneralFormulaMutator(variables, MR, num_of_mut);
        return f.nnf().accept(formVisitor);
    }

    public static Formula weakenFormula(Formula f, List<String> variables) {
        int n = Formula_Utils.formulaSize(f);
        int MR = Math.max(1, ((100 - Settings.GA_GENE_MUTATION_RATE) / 100) * n);
        int num_of_mut = n;
        if (Settings.GA_GENE_NUM_OF_MUTATIONS > 0)
            num_of_mut = Math.min(n, Settings.GA_GENE_NUM_OF_MUTATIONS);
        FormulaWeakening formVisitor = new FormulaWeakening(variables, MR, num_of_mut);
        return f.nnf().accept(formVisitor);
    }

    public static Formula strengthenFormula(Formula f, List<String> variables) {
        int n = Formula_Utils.formulaSize(f);
        int MR = Math.max(1, ((100 - Settings.GA_GENE_MUTATION_RATE) / 100) * n);
        int num_of_mut = n;
        if (Settings.GA_GENE_NUM_OF_MUTATIONS > 0)
            num_of_mut = Math.min(n, Settings.GA_GENE_NUM_OF_MUTATIONS);
        FormulaStrengthening formVisitor = new FormulaStrengthening(variables, MR, num_of_mut);
        return f.nnf().accept(formVisitor);
    }
}
