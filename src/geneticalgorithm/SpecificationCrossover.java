package geneticalgorithm;

import main.Settings;
import owl.ltl.BooleanConstant;
import owl.ltl.Formula;
import owl.ltl.Literal;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.visitors.PropositionVariablesExtractor;
import utils.FormulaUtils;
import utils.TlsfUtils;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * The <b>crossover operator</b> of AuRUS: recombines two parent
 * specifications into offspring that mix their assumptions and guarantees,
 * so that promising fragments discovered in different individuals can meet in
 * a single candidate.
 *
 * <p>Each side (assumptions, guarantees) is recombined independently
 * according to a <i>level</i> drawn by the caller
 * ({@link SpecificationChromosome#crossover}):</p>
 * <ul>
 *   <li><b>Level 0 — wholesale swap.</b> The whole side is inherited from one
 *       parent chosen by a coin flip. No mixing; the coarsest form of
 *       recombination.</li>
 *   <li><b>Level 1 — conjunct merge.</b> The side is built as the union of a
 *       random subset of each parent's conjuncts (each conjunct kept with
 *       probability &frac12;, duplicates and {@code true} skipped — see
 *       {@link #selectRandomly(List)}). Conjuncts travel whole between
 *       parents; the offspring may also <i>lose</i> conjuncts both parents
 *       had.</li>
 *   <li><b>Level 2 (default branch) — sub-formula merge.</b> The deepest form
 *       of mixing: the side starts as parent 0's conjuncts, one random
 *       conjunct &phi;<sub>0</sub> is removed, a random conjunct
 *       &phi;<sub>1</sub> of parent 1 is picked, and the two are fused at the
 *       syntax-tree level — either {@code replaceSubformula} (a random
 *       sub-tree of &phi;<sub>0</sub> is replaced by &phi;<sub>1</sub>) or
 *       {@code combineSubformula} (they are joined under a random binary
 *       connective), with equal probability.</li>
 * </ul>
 *
 * <p><b>Bloat control.</b> A level-2 merged conjunct is accepted only if it
 * contains at most <i>two temporal operators</i>; this guards the search
 * against the classic genetic-programming pathology of ever-growing formulas.
 * Note the side effect: when the merge fails the guard, the removed conjunct
 * &phi;<sub>0</sub> is <i>not</i> restored, so level-2 crossover can shrink a
 * specification by one conjunct.</p>
 *
 * <p><b>Restrictions.</b> The guarantee-preference factor gates which sides
 * may mix at levels 1–2 (assumptions only when {@code -GPR &lt; 100},
 * guarantees only when {@code -GPR &gt; 0}; a frozen side is copied verbatim
 * from parent 0). When {@code -onlyInputsA} is set, the fragment transplanted
 * into an assumption is first restricted to a sub-formula over input
 * variables only (falling back to {@code true} if none exists), preventing
 * the environment from being "repaired" with constraints over outputs.</p>
 *
 * <p>The operator makes <b>no consistency promise</b>: offspring may well be
 * unsatisfiable or unrealisable. Judging them is deliberately left to the
 * fitness function, whose graded status ladder rewards partially consistent
 * candidates.</p>
 *
 * <p>Part of the reference implementation of: <i>Brizzio, Cordy, Papadakis,
 * S&aacute;nchez, Aguirre, Degiovanni. "Automated Repair of Unrealisable LTL
 * Specifications Guided by Model Counting", GECCO 2023
 * (<a href="https://doi.org/10.1145/3583131.3590454">doi:10.1145/3583131.3590454</a>).</i></p>
 *
 * @author Mat&iacute;as Brizzio
 * @see SpecificationChromosome
 * @see SpecificationMutator
 */
public class SpecificationCrossover {

    /**
     * Recombines the two parents, applying the given level to each side
     * independently (levels are described in the class documentation; any
     * value other than 0 or 1 selects the sub-formula merge).
     *
     * <p>The offspring inherits everything else (inputs, outputs, initial
     * conditions, ...) from {@code spec0}. At most one offspring is produced;
     * the list is empty when the resulting guarantee side would be empty.</p>
     *
     * @param spec0            the first parent (also the template for the offspring)
     * @param spec1            the second parent
     * @param assumption_level recombination level for the assumption side
     * @param guarantee_level  recombination level for the guarantee side
     * @return a singleton list with the offspring, or an empty list
     */
    public static List<Tlsf> apply(Tlsf spec0, Tlsf spec1, int assumption_level, int guarantee_level) {
        List<Tlsf> merged_specifications = new LinkedList<>();
        List<Formula> assumptionConjuncts = new LinkedList<>();
        List<Formula> guaranteeConjuncts = new LinkedList<>();
        List<Formula> assumesspec0 = FormulaUtils.splitConjunction(spec0.assume());
        List<Formula> assumesspec1 = FormulaUtils.splitConjunction(spec1.assume());
        if (assumption_level == 0) {
            // set assume
            if (Settings.RANDOM_GENERATOR.nextBoolean())
                assumptionConjuncts.addAll(assumesspec0);
            else
                assumptionConjuncts.addAll(assumesspec1);
        } else if (assumption_level == 1) {
            // set assume
            //if the assumptions can be modified
            if (Settings.GA_GUARANTEES_PREFERENCE_FACTOR < 100) {
                assumptionConjuncts.addAll(selectRandomly(assumesspec0));
                for (Formula f : selectRandomly(assumesspec1))
                    if (!assumptionConjuncts.contains(f))
                        assumptionConjuncts.add(f);
            } else
                assumptionConjuncts.addAll(assumesspec0);
        } else { // sub-formula merge (default branch)
            // set assume
            //if assumptions can be modified
            if (Settings.GA_GUARANTEES_PREFERENCE_FACTOR < 100 && !assumesspec0.isEmpty() && !assumesspec1.isEmpty()) {
                assumptionConjuncts.addAll(assumesspec0);
                int size = assumptionConjuncts.size();
                Formula merge_ass0 = assumptionConjuncts.remove(Settings.RANDOM_GENERATOR.nextInt(size));
                Formula merge_ass1 = assumesspec1.get(Settings.RANDOM_GENERATOR.nextInt(assumesspec1.size()));
                // merge ass0 and ass1
                if (merge_ass0 != null && merge_ass1 != null) {
                    if (Settings.only_inputs_in_assumptions) {
                        Set<Formula> subformulas = FormulaUtils.subformulas(merge_ass1);
                        Set<Formula> to_remove = new LinkedHashSet<>();
                        for (Formula f : subformulas) {
                            PropositionVariablesExtractor prop_visitor = new PropositionVariablesExtractor();
                            Set<Literal> props = f.accept(prop_visitor);
                            for (Literal l : props) {
                                if (l.getAtom() >= spec0.numberOfInputs()) {
                                    to_remove.add(f);
                                    break;
                                }
                            }
                        }
                        subformulas.removeAll(to_remove);
                        if (!subformulas.isEmpty())
                            merge_ass1 = (Formula) subformulas.toArray()[Settings.RANDOM_GENERATOR.nextInt(subformulas.size())];
                        else
                            merge_ass1 = BooleanConstant.TRUE;
                    }

                    Formula merged_assumption;
                    if (Settings.RANDOM_GENERATOR.nextBoolean())
                        merged_assumption = FormulaUtils.replaceSubformula(merge_ass0, merge_ass1);
                    else {
                        merged_assumption = FormulaUtils.combineSubformula(merge_ass0, merge_ass1);
                    }
                    if (merged_assumption != null && FormulaUtils.numOfTemporalOperators(merged_assumption) <= 2)
                        assumptionConjuncts.add(merged_assumption);
                }
            } else
                assumptionConjuncts.addAll(assumesspec0);

        }

        if (guarantee_level == 0) {
            // set guarantees
            if (Settings.RANDOM_GENERATOR.nextBoolean())
                guaranteeConjuncts.addAll(spec0.guarantee());
            else
                guaranteeConjuncts.addAll(spec1.guarantee());
        } else if (guarantee_level == 1) {
            // set guarantee
            //if the guarantees can be modified
            if (Settings.GA_GUARANTEES_PREFERENCE_FACTOR > 0) {
                guaranteeConjuncts.addAll(selectRandomly(spec0.guarantee()));
                for (Formula f : selectRandomly(spec1.guarantee()))
                    if (!guaranteeConjuncts.contains(f))
                        guaranteeConjuncts.add(f);
            } else
                guaranteeConjuncts.addAll(spec0.guarantee());

        } else { // sub-formula merge (default branch)
            // set guarantee
            //if guarantees can be modified
            if (Settings.GA_GUARANTEES_PREFERENCE_FACTOR > 0 && !spec0.guarantee().isEmpty() && !spec1.guarantee().isEmpty()) {
                guaranteeConjuncts.addAll(spec0.guarantee());
                int size_g = guaranteeConjuncts.size();
                if (size_g >= 1) {
                    Formula merge_g0 = guaranteeConjuncts.remove(Settings.RANDOM_GENERATOR.nextInt(size_g));
                    Formula merge_g1 = spec1.guarantee().get(Settings.RANDOM_GENERATOR.nextInt(spec1.guarantee().size()));
                    // merge g0 and g1
                    if (merge_g0 != null && merge_g1 != null) {
                        Formula merged_g;
                        if (Settings.RANDOM_GENERATOR.nextBoolean())
                            merged_g = FormulaUtils.replaceSubformula(merge_g0, merge_g1);
                        else {
                            merged_g = FormulaUtils.combineSubformula(merge_g0, merge_g1);
                        }
                        if (merged_g != null && FormulaUtils.numOfTemporalOperators(merged_g) <= 2)
                            guaranteeConjuncts.add(merged_g);
                    }
                }
            } else
                guaranteeConjuncts.addAll(spec0.guarantee());
        }
        if (!guaranteeConjuncts.isEmpty()) {
            Tlsf new_spec = TlsfUtils.change_assume(spec0, assumptionConjuncts);
            new_spec = TlsfUtils.change_guarantees(new_spec, guaranteeConjuncts);
            merged_specifications.add(new_spec);
        }

        return merged_specifications;
    }

    /**
     * Draws a random subset of the given conjuncts: each formula is kept with
     * probability &frac12;, skipping {@code true} (which carries no content)
     * and duplicates. Used by the level-1 conjunct merge.
     *
     * @param formulas the conjuncts to sample from
     * @return a freshly allocated random subset
     */
    private static List<Formula> selectRandomly(List<Formula> formulas) {
        List<Formula> selectedFormulas = new LinkedList<>();
        for (Formula f : formulas) {
            if (Settings.RANDOM_GENERATOR.nextBoolean() && f != BooleanConstant.TRUE && !selectedFormulas.contains(f))
                selectedFormulas.add(f);
        }
        return selectedFormulas;
    }
}