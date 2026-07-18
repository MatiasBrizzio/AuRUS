package geneticalgorithm;

import main.Settings;
import owl.ltl.BooleanConstant;
import owl.ltl.Formula;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.visitors.FormulaStrengthening;
import owl.ltl.visitors.FormulaWeakening;
import owl.ltl.visitors.GeneralFormulaMutator;
import owl.ltl.visitors.SubformulaReplacer;
import utils.FormulaUtils;
import utils.TlsfUtils;

import java.util.List;
import java.util.Set;

/**
 * The <b>mutation operator</b> of AuRUS: produces a syntactic variant of a
 * specification by rewriting one randomly chosen sub-formula of one of its
 * assumptions or guarantees.
 *
 * <p>The operator proceeds in three steps:</p>
 * <ol>
 *   <li><b>Pick a side.</b> With probability
 *       {@code GA_GUARANTEES_PREFERENCE_FACTOR}% (flag {@code -GPR}) a
 *       guarantee is mutated; otherwise an assumption. Unrealisability most
 *       often originates in the guarantees, so biasing the factor upward
 *       focuses the search there; setting it to 100 protects the assumptions
 *       entirely (useful when they encode fixed environment constraints).</li>
 *   <li><b>Pick a target.</b> A conjunct of the chosen side is selected
 *       uniformly at random, and inside it a random <i>sub-formula</i> — not
 *       necessarily the root — so that surgical changes deep within a complex
 *       formula are possible without restructuring its top level.</li>
 *   <li><b>Pick a mode.</b> One of three rewritings is applied to the
 *       selected sub-formula &phi;, each drawn with equal probability:
 *       a <i>general</i> syntactic mutation (undirected exploration), a
 *       <i>weakening</i> (producing &phi;<sub>w</sub> with
 *       &phi; &#8872; &phi;<sub>w</sub> — no model of &phi; is lost), or a
 *       <i>strengthening</i> (producing &phi;<sub>s</sub> with
 *       &phi;<sub>s</sub> &#8872; &phi; — no new model is added). The
 *       rewriting rules per operator are implemented by the
 *       {@code FormulaWeakening} / {@code FormulaStrengthening} visitors.</li>
 * </ol>
 *
 * <p><b>Deliberate asymmetry.</b> The mapping from the random draw to the
 * mode differs per side: for <i>assumptions</i> the draw {@code 0} yields the
 * general mutation, while for <i>guarantees</i> it yields a
 * <i>strengthening</i>. Strengthening a guarantee tightens (and can thereby
 * untangle) the system's obligations — e.g. strengthening the antecedent of
 * an implication makes it harder to trigger — whereas for assumptions the
 * productive direction is harder to predict a priori, so undirected
 * exploration is preferred and the fitness function is left to judge. This
 * mirrors the design discussion of the mutation operator in the paper and
 * thesis.</p>
 *
 * <p>Mutants whose rewritten formula collapses to {@code false} are rejected:
 * the unmodified copy of the specification is returned instead (the caller's
 * deduplication then discards it as identical to its parent), since a
 * {@code false} assumption makes the specification vacuously realisable and a
 * {@code false} guarantee makes it hopeless — neither conveys intent.</p>
 *
 * <p>Part of the reference implementation of: <i>Brizzio, Cordy, Papadakis,
 * S&aacute;nchez, Aguirre, Degiovanni. "Automated Repair of Unrealisable LTL
 * Specifications Guided by Model Counting", GECCO 2023
 * (<a href="https://doi.org/10.1145/3583131.3590454">doi:10.1145/3583131.3590454</a>).</i></p>
 *
 * @author Mat&iacute;as Brizzio
 * @see SpecificationChromosome#mutate()
 * @see SpecificationCrossover
 */
public class SpecificationMutator {

    /**
     * Applies the three-step mutation operator described in the class
     * documentation to the given specification.
     *
     * <p>Mode mapping per side (draw &isin; {0, 1, 2}, uniform):</p>
     * <pre>
     *   draw   assumptions      guarantees
     *   0      general          strengthening   (default modes)
     *   1      weakening        weakening
     *   2      strengthening    general
     * </pre>
     *
     * <p>When {@code Settings.only_inputs_in_assumptions} is set, mutations
     * on the assumption side draw fresh literals from the input variables
     * only, preventing the search from "repairing" the environment with
     * constraints over outputs it cannot observe.</p>
     *
     * @param spec the specification to mutate
     * @return a fresh specification carrying the mutation — or an unmodified
     *         copy of the input when the rewriting collapsed to {@code false}
     */
    public static Tlsf mutate(Tlsf spec) {
        //create empty specification
        Tlsf new_spec = TlsfUtils.fromSpec(spec);
        int random = Settings.RANDOM_GENERATOR.nextInt(100);
        if (random >= Settings.GA_GUARANTEES_PREFERENCE_FACTOR) {
            // mutate assumptions
            List<Formula> assumptions = FormulaUtils.splitConjunction(spec.assume());
            if (assumptions.isEmpty())
                assumptions.add(BooleanConstant.TRUE);
            int index_to_mutate = Settings.RANDOM_GENERATOR.nextInt(assumptions.size());
            Formula assumption_to_mutate = assumptions.get(index_to_mutate);

            List<String> vars = spec.variables();
            if (Settings.only_inputs_in_assumptions)
                vars = vars.subList(0, spec.numberOfInputs());

            //select subformula to mutate
            Set<Formula> subformulas = FormulaUtils.subformulas(assumption_to_mutate);
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
                new_spec = TlsfUtils.change_assume(new_spec, assumptions);
            }
        } else {
            List<Formula> guarantees = FormulaUtils.splitConjunctions(spec.guarantee());
            if (guarantees.isEmpty())
                guarantees.add(BooleanConstant.TRUE);
            int index_to_mutate = Settings.RANDOM_GENERATOR.nextInt(guarantees.size());
            Formula guarantee_to_mutate = guarantees.get(index_to_mutate);

            //select subformula to mutate
            Set<Formula> subformulas = FormulaUtils.subformulas(guarantee_to_mutate);
            int n = subformulas.size();
            Formula to_mutate = (Formula) subformulas.toArray()[Settings.RANDOM_GENERATOR.nextInt(n)];

            Formula mutated_subformula;
            int modification = Settings.RANDOM_GENERATOR.nextInt(3);
            if (modification == 0) {
                // strengthening mutation (the default mode for guarantees)
                mutated_subformula = strengthenFormula(to_mutate, spec.variables());
            } else if (modification == 1) {
                // weaken mutation
                mutated_subformula = weakenFormula(to_mutate, spec.variables());
            } else {
                // arbitrary mutation
                mutated_subformula = applyGeneralMutation(to_mutate, spec.variables());
            }

            SubformulaReplacer visitor = new SubformulaReplacer(to_mutate, mutated_subformula);
            Formula new_guarantee = guarantee_to_mutate.accept(visitor);


            if (new_guarantee != BooleanConstant.FALSE) {
                guarantees.remove(index_to_mutate);
                guarantees.add(index_to_mutate, new_guarantee);
                new_spec = TlsfUtils.change_guarantees(new_spec, guarantees);
            }
        }
        return new_spec;
    }

    /**
     * Applies the undirected <i>general</i> mutation to a formula: atoms can
     * be flipped or swapped, unary operators removed, replaced or stacked,
     * and binary connectives exchanged, exploring the syntactic
     * neighbourhood without directional bias. The formula is first put in
     * negation normal form.
     *
     * <p>The per-node mutation probability is {@code 1/MR}, where {@code MR}
     * is derived from {@code Settings.GA_GENE_MUTATION_RATE}: with the
     * default rate ({@code 0}) it evaluates to the formula size {@code n},
     * yielding the {@code 1/|formula|} rule of the experimental setup.
     * <b>Implementation note:</b> the derivation uses integer division, so
     * any explicitly set rate in {@code 1..100} collapses {@code MR} to
     * {@code 1} — i.e. every node becomes a mutation candidate, subject to
     * the {@code Settings.GA_GENE_NUM_OF_MUTATIONS} cap.</p>
     *
     * @param f         the formula to mutate
     * @param variables the variable names fresh literals may be drawn from
     * @return the mutated formula
     */
    public static Formula applyGeneralMutation(Formula f, List<String> variables) {
        int n = FormulaUtils.formulaSize(f);
        int MR = Math.max(1, ((100 - Settings.GA_GENE_MUTATION_RATE) / 100) * n);
        int num_of_mut = n;
        if (Settings.GA_GENE_NUM_OF_MUTATIONS > 0)
            num_of_mut = Math.min(n, Settings.GA_GENE_NUM_OF_MUTATIONS);
        GeneralFormulaMutator formVisitor = new GeneralFormulaMutator(variables, MR, num_of_mut);
        return f.nnf().accept(formVisitor);
    }

    /**
     * Applies the semantically directed <i>weakening</i> mutation, producing
     * a formula &phi;<sub>w</sub> such that &phi; &#8872; &phi;<sub>w</sub>:
     * every model of the original sub-formula survives (e.g.
     * {@code p &rarr; p | q}, {@code X &phi; &rarr; F &phi;},
     * {@code &phi; U &psi; &rarr; &phi; W &psi;}). Weakening an assumption
     * relaxes a constraint on the environment. Same {@code 1/MR} probability
     * scheme (and integer-division caveat) as
     * {@link #applyGeneralMutation(Formula, List)}.
     *
     * @param f         the formula to weaken
     * @param variables the variable names fresh literals may be drawn from
     * @return the weakened formula
     */
    public static Formula weakenFormula(Formula f, List<String> variables) {
        int n = FormulaUtils.formulaSize(f);
        int MR = Math.max(1, ((100 - Settings.GA_GENE_MUTATION_RATE) / 100) * n);
        int num_of_mut = n;
        if (Settings.GA_GENE_NUM_OF_MUTATIONS > 0)
            num_of_mut = Math.min(n, Settings.GA_GENE_NUM_OF_MUTATIONS);
        FormulaWeakening formVisitor = new FormulaWeakening(variables, MR, num_of_mut);
        return f.nnf().accept(formVisitor);
    }

    /**
     * Applies the semantically directed <i>strengthening</i> mutation,
     * producing a formula &phi;<sub>s</sub> such that
     * &phi;<sub>s</sub> &#8872; &phi;: no new model is introduced (e.g.
     * {@code p &rarr; p & q}, {@code F &phi; &rarr; &phi;},
     * {@code &phi; W &psi; &rarr; G &phi;}). Strengthening a guarantee
     * tightens the system's obligations — the default move on the guarantee
     * side. Same {@code 1/MR} probability scheme (and integer-division
     * caveat) as {@link #applyGeneralMutation(Formula, List)}.
     *
     * @param f         the formula to strengthen
     * @param variables the variable names fresh literals may be drawn from
     * @return the strengthened formula
     */
    public static Formula strengthenFormula(Formula f, List<String> variables) {
        int n = FormulaUtils.formulaSize(f);
        int MR = Math.max(1, ((100 - Settings.GA_GENE_MUTATION_RATE) / 100) * n);
        int num_of_mut = n;
        if (Settings.GA_GENE_NUM_OF_MUTATIONS > 0)
            num_of_mut = Math.min(n, Settings.GA_GENE_NUM_OF_MUTATIONS);
        FormulaStrengthening formVisitor = new FormulaStrengthening(variables, MR, num_of_mut);
        return f.nnf().accept(formVisitor);
    }
}