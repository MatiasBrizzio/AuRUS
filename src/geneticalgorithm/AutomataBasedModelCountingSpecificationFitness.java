package geneticalgorithm;

import com.google.common.collect.Sets;
import com.lagodiuk.ga.Fitness;
import geneticalgorithm.SpecificationChromosome.SPEC_STATUS;
import main.Settings;
import modelcounter.EmersonLeiAutomatonBasedModelCounting;
import owl.ltl.*;
import owl.ltl.rewriter.SyntacticSimplifier;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.visitors.SolverSyntaxOperatorReplacer;
import solvers.LTLSolver;
import solvers.LTLSolver.SolverResult;
import solvers.PotentiallyRealizabilityChecker;
import solvers.StrixHelper;
import solvers.StrixHelper.RealizabilitySolverResult;
import utils.FormulaUtils;
import utils.SolverUtils;
import utils.TlsfUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The multi-objective fitness function of AuRUS — the compass that guides the
 * genetic search — with the semantic component estimated via <b>approximate
 * bounded model counting over automaton transfer matrices</b>.
 *
 * <p>Given the original specification {@code S} and a candidate repair
 * {@code S'}, the fitness is the weighted sum</p>
 *
 * <pre>
 *   f(S') = STATUS_FACTOR      * status(S')
 *         + LOST_MODELS_FACTOR * lost(S, S')
 *         + WON_MODELS_FACTOR  * won(S, S')
 *         + SYNTACTIC_FACTOR   * synSim(S, S')
 * </pre>
 *
 * <p>where the weights come from {@link Settings} (flag {@code -factors}; the
 * semantic weight is split evenly between the lost-models and won-models
 * directions) and:</p>
 *
 * <ul>
 *   <li><b>{@code status(S')}</b> rewards progress towards a well-formed,
 *       realisable specification on a graded ladder, so partially fixed
 *       candidates still receive gradient
 *       (see {@link #getStatusFitness(SpecificationChromosome)});</li>
 *   <li><b>{@code lost(S, S')}</b> is the fraction of the original
 *       specification's behaviours <i>preserved</i> by the candidate,
 *       computed as {@code 1 - #(S &and; &not;S', k) / #(S, k)}
 *       (see {@link #compute_lost_models_porcentage(Tlsf, Tlsf)});</li>
 *   <li><b>{@code won(S, S')}</b> penalises the <i>new</i> behaviours the
 *       candidate introduces, computed as
 *       {@code 1 - #(&not;S &and; S', k) / #(S', k)}
 *       (see {@link #compute_won_models_porcentage(Tlsf, Tlsf)});</li>
 *   <li><b>{@code synSim(S, S')}</b> is the sub-formula overlap between the
 *       two specifications
 *       (see {@link #compute_syntactic_distance(Tlsf, Tlsf)}).</li>
 * </ul>
 *
 * <p>All model counts {@code #(&phi;, k)} are numbers of satisfying traces of
 * bounded length {@code k} ({@code Settings.MC_BOUND}, flag {@code -k}),
 * approximated by {@link EmersonLeiAutomatonBasedModelCounting}: the formula
 * is translated into an automaton, the automaton is encoded as a transfer
 * matrix {@code T} whose entries count the propositional valuations on each
 * transition, and the count is obtained by matrix exponentiation as
 * {@code I &middot; T^k &middot; F}. The approximation preserves the relative
 * ordering of candidates — which is all the search needs — at a small fraction
 * of the cost of exact counting.</p>
 *
 * <p>Note on formulation: the paper presents the semantic similarity in its
 * <i>positive</i> form (via the shared models {@code #(S &and; S', k)}); this
 * implementation computes the algebraically equivalent <i>complement</i> form
 * (via the lost and won models). The equivalence follows from partitioning the
 * models of each specification, and is documented in detail — with worked
 * examples and the design rationale — in {@code docs/FITNESS.md}.</p>
 *
 * <p>This fitness design (graded realisability status + syntactic similarity +
 * model-counting-based semantic similarity) is the central contribution of:
 * <i>M. Brizzio, M. Cordy, M. Papadakis, C. S&aacute;nchez, N. Aguirre,
 * R. Degiovanni. "Automated Repair of Unrealisable LTL Specifications Guided
 * by Model Counting", GECCO 2023
 * (<a href="https://doi.org/10.1145/3583131.3590454">doi:10.1145/3583131.3590454</a>).</i>
 * Please cite this paper if you reuse or reimplement the technique. The paper
 * presents the semantic similarity through the shared-models ratios
 * {@code #(S &and; S')/#(S)} and {@code #(S &and; S')/#(S')}; the complement
 * form computed here is algebraically equivalent — see
 * <a href="https://github.com/MatiasBrizzio/AuRUS/blob/master/docs/FITNESS.md">docs/FITNESS.md</a>
 * for the derivation and its numerical verification.</p>
 *
 * @author Mat&iacute;as Brizzio
 * @see SpecificationGeneticAlgorithm
 * @see SpecificationChromosome
 * @see EmersonLeiAutomatonBasedModelCounting
 * @see main.Settings
 */
public class AutomataBasedModelCountingSpecificationFitness implements Fitness<SpecificationChromosome, Double> {

    /** Rewrites formulas into the operator syntax expected by the external LTL solvers. */
    private final SolverSyntaxOperatorReplacer visitor = new SolverSyntaxOperatorReplacer();

    /** The original (typically unrealisable) specification every candidate is compared against. */
    public Tlsf originalSpecification;

    /** Atomic propositions of the specification (currently unused; kept for compatibility). */
    public List<String> alphabet = null;

    /** Status of the original specification, computed once at construction time. */
    public SPEC_STATUS originalStatus;

    /**
     * Bounded model count {@code #(S, k)} of the original specification,
     * computed once at construction time (only when the lost-models component
     * is active). Used as the denominator of the lost-models ratio for every
     * candidate.
     */
    public BigInteger originalNumOfModels;

    /**
     * Creates the fitness function for a given original specification.
     *
     * <p>Computes and caches the status of the original specification (printed
     * to standard output) and, if the lost-models component is enabled, its
     * bounded model count {@code #(S, k)} — so it is paid once instead of once
     * per candidate.</p>
     *
     * @param originalSpecification the specification the search will try to repair
     * @throws IOException          if an external solver invocation fails
     * @throws InterruptedException if an external solver call is interrupted
     */
    public AutomataBasedModelCountingSpecificationFitness(Tlsf originalSpecification) throws IOException, InterruptedException {
        this.originalSpecification = originalSpecification;
        SpecificationChromosome originalChromosome = new SpecificationChromosome(originalSpecification);
        compute_status(originalChromosome);
        this.originalStatus = originalChromosome.status;
        System.out.println("Initial specification is: " + originalStatus);
        if (Settings.LOST_MODELS_FACTOR > 0.0d)
            originalNumOfModels = countModels(originalSpecification.toFormula());
    }

    /**
     * Maps the status of a candidate to its graded fitness contribution.
     * The ladder rewards candidates the closer they get to a consistent,
     * realisable specification:
     *
     * <pre>
     *   0.00  UNKNOWN / BOTTOM   (assumptions and guarantees both unsatisfiable, or status not computable)
     *   0.05  GUARANTEES         (only the guarantees are satisfiable; the environment side is not)
     *   0.10  ASSUMPTIONS        (assumptions satisfiable, guarantees unsatisfiable)
     *   0.20  CONTRADICTORY      (each side satisfiable, but jointly unsatisfiable)
     *   0.50  UNREALIZABLE       (consistent, but no controller exists)
     *   1.00  REALIZABLE         (a controller exists — a repair)
     * </pre>
     *
     * @param chromosome the candidate whose status has already been computed
     * @return the status component of the fitness, in {@code [0, 1]}
     */
    private static double getStatusFitness(SpecificationChromosome chromosome) {
        double status_fitness = 0.0d;
        if (chromosome.status == SPEC_STATUS.UNKNOWN || chromosome.status == SPEC_STATUS.BOTTOM)
            status_fitness = 0.0d;
        else if (chromosome.status == SPEC_STATUS.GUARANTEES)
            status_fitness = 0.05d;
        else if (chromosome.status == SPEC_STATUS.ASSUMPTIONS)
            status_fitness = 0.1d;
        else if (chromosome.status == SPEC_STATUS.CONTRADICTORY)
            status_fitness = 0.2d;
        else if (chromosome.status == SPEC_STATUS.UNREALIZABLE)
            status_fitness = 0.5d;
        else if (chromosome.status == SPEC_STATUS.REALIZABLE)
            status_fitness = 1.0d;
        return status_fitness;
    }

    /**
     * Computes the fitness of a candidate repair (the weighted sum described
     * in the class documentation).
     *
     * <p>Degenerate candidates are pruned with fitness {@code 0}: candidates
     * identical to the original specification, candidates with a {@code false}
     * assumption (vacuously realisable), candidates whose guarantees collapsed
     * to {@code true} (the useless "do whatever you want" repair), and
     * candidates that removed guarantees or added assumptions when the
     * corresponding flags forbid it. The lost/won model ratios are only
     * evaluated when both specifications are consistent and the candidate is
     * not syntactically identical to the original; fitness values above the
     * theoretical maximum abort the run, as they indicate a configuration
     * bug.</p>
     *
     * <p>As side effects, the computed fitness, syntactic distance and
     * semantic distance are stored in the chromosome, and a previously
     * evaluated chromosome (status different from {@code UNKNOWN}) returns its
     * cached fitness immediately.</p>
     *
     * @param chromosome the candidate specification to score
     * @return the fitness value in {@code [0, 1]}
     */
    @Override
    public Double calculate(SpecificationChromosome chromosome) {
        // compute multi-objective fitness function
        if (chromosome.status != SPEC_STATUS.UNKNOWN)
            return chromosome.fitness;

        // remove trivial specifications
        if (originalSpecification.equals(chromosome.spec))
            return 0.0d;
        if (chromosome.spec.assume() == BooleanConstant.FALSE)
            return 0.0d;
        Formula guarantees = Conjunction.of(chromosome.spec.guarantee());
        if (guarantees == BooleanConstant.TRUE)
            return 0.0d;

        if (!Settings.allowGuaranteeRemoval || !Settings.allowAssumptionAddition) {
            boolean somethingRemoved = somethingHasBeenRemoved(originalSpecification, chromosome.spec);
            if (somethingRemoved)
                return 0.0d;
        }
        // First compute the status fitness
        try {
            compute_status(chromosome);
        } catch (Exception e) {
            e.printStackTrace();
        }

        double status_fitness = getStatusFitness(chromosome);

        double syntactic_distance = 0.0d;
        if (Settings.SYNTACTIC_FACTOR > 0.0d)
            syntactic_distance = compute_syntactic_distance(originalSpecification, chromosome.spec);
        System.out.printf("s%.2f ", syntactic_distance);


//		if (syntactic_distance < 1.0d) {
        //if the specifications are not syntactically equivalent
        // Second, compute the portion of loosing models with respect to the original specification
        double lost_models_fitness = 0.0d; // if the current specification is inconsistent, then it looses all the models (it maintains 0% of models of the original specification)
        if (syntactic_distance < 1.0d && Settings.LOST_MODELS_FACTOR > 0.0d && originalStatus.isSpecificationConsistent() && chromosome.status.isSpecificationConsistent()) {
            // if both specifications are consistent, then we will compute the percentage of models that are maintained after the refinement
            try {
                lost_models_fitness = compute_lost_models_porcentage(originalSpecification, chromosome.spec);
                System.out.printf("%.2f ", lost_models_fitness);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Third, compute the portion of winning models with respect to the original specification
        double won_models_fitness = 0.0d;
        if (syntactic_distance < 1.0d && Settings.WON_MODELS_FACTOR > 0.0d && originalStatus.isSpecificationConsistent() && chromosome.status.isSpecificationConsistent()) {
            // if both specifications are consistent, then we will compute the percentage of models that are added after the refinement (or removed from the complement of the original specifiction)
            try {
                won_models_fitness = compute_won_models_porcentage(originalSpecification, chromosome.spec);
                System.out.printf("%.2f ", won_models_fitness);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        double fitness = (Settings.STATUS_FACTOR * status_fitness) + (Settings.LOST_MODELS_FACTOR * lost_models_fitness) + (Settings.WON_MODELS_FACTOR * won_models_fitness) + (Settings.SYNTACTIC_FACTOR * syntactic_distance);
//		}
        System.out.printf("f%.2f ", fitness);
        chromosome.fitness = fitness;
        chromosome.syntactic_distance = syntactic_distance;
        chromosome.semantic_distance = (0.5d * lost_models_fitness) + (0.5d * won_models_fitness);

        if (fitness > Settings.MAX_FITNESS()) {
            System.out.printf("BROKEN Fitness: %.2f%n", fitness);
            System.out.println(TlsfUtils.adaptTLSFSpec(chromosome.spec));
            throw new RuntimeException();
        }

        return fitness;
    }

    /**
     * Computes and stores the {@link SPEC_STATUS} of a candidate, performing
     * the satisfiability/realisability analysis that feeds the status ladder.
     *
     * <p>The analysis proceeds bottom-up. The environment side
     * ({@code initially &and; G(require) &and; assume}) and the system side
     * ({@code preset &and; G(assert) &and; guarantees}) are first checked for
     * satisfiability in isolation, yielding {@code BOTTOM},
     * {@code GUARANTEES} or {@code ASSUMPTIONS} when one (or both) is
     * unsatisfiable. If both sides are satisfiable, their conjunction is
     * checked: an unsatisfiable conjunction yields {@code CONTRADICTORY}.
     * Finally, consistent candidates are checked for realisability — with
     * Strix, or with the potential-realisability (strong satisfiability)
     * check when {@code Settings.check_STRONG_SAT} is enabled — yielding
     * {@code REALIZABLE} or {@code UNREALIZABLE}. Inconclusive solver answers
     * (e.g. timeouts) leave the status as {@code UNKNOWN}.</p>
     *
     * <p>Chromosomes whose status was already computed are returned
     * untouched, so the (expensive) solver calls are paid at most once per
     * candidate.</p>
     *
     * @param chromosome the candidate whose status should be computed
     * @throws IOException          if an external solver invocation fails
     * @throws InterruptedException if an external solver call is interrupted
     */
    public void compute_status(SpecificationChromosome chromosome) throws IOException, InterruptedException {
        System.out.print(".");
        //check if status has been computed before
        if (chromosome.status != SPEC_STATUS.UNKNOWN)
            return;

        Tlsf spec = chromosome.spec;
        // Env = initially && G(require) & assume
        Formula environment = Conjunction.of(spec.initially(), GOperator.of(spec.require()), spec.assume());
        Formula environment2 = environment.accept(visitor);
        SolverResult env_sat = LTLSolver.isSAT(SolverUtils.toSolverSyntax(environment2));
        SPEC_STATUS status = SPEC_STATUS.UNKNOWN;

        if (!env_sat.inconclusive()) {
            // Sys = preset && G(assert_) & guarantees
            Formula system = Conjunction.of(spec.preset(), GOperator.of(Conjunction.of(spec.assert_())), Conjunction.of(spec.guarantee()));
            Formula system2 = system.accept(visitor);
            SolverResult sys_sat = LTLSolver.isSAT(SolverUtils.toSolverSyntax(system2));

            if (!sys_sat.inconclusive()) {
                if (env_sat == SolverResult.UNSAT && sys_sat == SolverResult.UNSAT) {
                    status = SPEC_STATUS.BOTTOM;
                } else if (env_sat == SolverResult.UNSAT) {
                    status = SPEC_STATUS.GUARANTEES;
                } else if (sys_sat == SolverResult.UNSAT) {
                    status = SPEC_STATUS.ASSUMPTIONS;
                } else { //env_sat == SolverResult.SAT && sys_sat == SolverResult.SAT
//					Formula env_sys = spec.toFormula().formula();
                    //check if initial states and safety properties are consistent
                    Formula env_sys = Conjunction.of(spec.initially(), GOperator.of(spec.require()), spec.preset(), GOperator.of(Conjunction.of(spec.assert_())), spec.assume(), Conjunction.of(spec.guarantee()));


//					System.out.println(env_sys);
                    Formula env_sys2 = env_sys.accept(visitor);
//					System.out.println(env_sys2);

                    SolverResult sat = LTLSolver.isSAT(SolverUtils.toSolverSyntax(env_sys2));
                    if (!sat.inconclusive()) {
                        if (sat == SolverResult.UNSAT)
                            status = SPEC_STATUS.CONTRADICTORY;
                        else {
                            status = SPEC_STATUS.UNREALIZABLE;
                            if (Settings.check_REALIZABILITY) {
                                RealizabilitySolverResult rel = RealizabilitySolverResult.UNREALIZABLE;
                                if (Settings.check_STRONG_SAT) {
                                    // check for strong satisfiability
                                    PotentiallyRealizabilityChecker strong_sat_solver = new PotentiallyRealizabilityChecker<>(spec.toFormula());
                                    Boolean strong_sat_res = strong_sat_solver.checkPotentiallyRealizability();
                                    if (strong_sat_res != null && strong_sat_res)
                                        rel = RealizabilitySolverResult.REALIZABLE;
                                } else {
                                    // check for realizability
                                    rel = StrixHelper.checkRealizability(spec);
                                }
                                if (!rel.inconclusive()) {
                                    if (rel == RealizabilitySolverResult.REALIZABLE) {
                                        System.out.print("R");
                                        status = SPEC_STATUS.REALIZABLE;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        chromosome.status = status;
    }

    /**
     * Approximate bounded model count {@code #(&phi;, k)} of a formula, with
     * {@code k = Settings.MC_BOUND}.
     *
     * <p>The formula is first syntactically simplified (a formula collapsing
     * to {@code false} has zero models by definition, with no counting
     * needed); the surviving formula is handed to
     * {@link EmersonLeiAutomatonBasedModelCounting}, which builds the
     * automaton, encodes its transfer matrix {@code T}, and evaluates
     * {@code I &middot; T^k &middot; F}.</p>
     *
     * @param formula the labelled formula to count
     * @return the approximate number of bounded models, or {@code null} if counting failed
     */
    private BigInteger countModels(LabelledFormula formula) {
        SyntacticSimplifier simp = new SyntacticSimplifier();
        Formula simplified = formula.formula().accept(simp);
        if (simplified == BooleanConstant.FALSE)
            return BigInteger.ZERO;
        LabelledFormula simp_formula = LabelledFormula.of(simplified, formula.variables());
        EmersonLeiAutomatonBasedModelCounting counter = new EmersonLeiAutomatonBasedModelCounting<>(simp_formula);
        return counter.count(Settings.MC_BOUND);
    }

    /**
     * Semantic similarity between two specifications, as the even average of
     * the lost-models and won-models components:
     * {@code 0.5 * lost + 0.5 * won}. Exposed for reporting purposes (e.g.
     * assessing final solutions against genuine reference repairs); inside the
     * search, {@link #calculate(SpecificationChromosome)} weights the two
     * directions independently.
     *
     * @param original the original specification {@code S}
     * @param refined  the candidate repair {@code S'}
     * @return the semantic similarity in {@code [0, 1]}
     */
    public double compute_semantic_distance(Tlsf original, Tlsf refined) {
        double lost_models_fitness = 0.0d;
        double won_models_fitness = 0.0d;
        try {
            if (Settings.LOST_MODELS_FACTOR > 0.0d && originalStatus.isSpecificationConsistent()) {
                lost_models_fitness = compute_lost_models_porcentage(original, refined);
                System.out.printf("%.2f ", lost_models_fitness);
            }

            if (Settings.WON_MODELS_FACTOR > 0.0d && originalStatus.isSpecificationConsistent()) {
                won_models_fitness = compute_won_models_porcentage(original, refined);
                System.out.printf("%.2f ", won_models_fitness);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return (0.5d * lost_models_fitness) + (0.5d * won_models_fitness);
    }

    /**
     * Fraction of the original specification's behaviour <i>preserved</i> by
     * the candidate: {@code 1 - #(S &and; &not;S', k) / #(S, k)}, where the
     * numerator counts the bounded models of {@code S} that {@code S'} lost.
     * A value of {@code 1} means every original model survives
     * ({@code S &rArr; S'} within the bound); {@code 0} means none does.
     *
     * <p>The division by {@code #(S, k)} normalises the raw count into a
     * proportion, and the {@code 1 -} inverts it from a penalty (fraction
     * lost) into a reward (fraction preserved), so that all fitness
     * components point in the same maximise direction. Computing the
     * preserved fraction via the complement has a further advantage under
     * approximate counting: when the candidate genuinely weakens the original
     * ({@code S &rArr; S'}), the conjunction {@code S &and; &not;S'} is
     * unsatisfiable, the simplifier collapses it to {@code false}, and the
     * method returns exactly {@code 1} — no approximation noise at the
     * boundary that matters most.</p>
     *
     * <p>Edge cases: a candidate equivalent to {@code true} preserves
     * everything (returns {@code 1}); one equivalent to {@code false}
     * preserves nothing (returns {@code 0}); a ratio above {@code 1} —
     * possible because the counts are approximations — triggers a warning
     * suggesting a larger bound {@code k} and is clamped.</p>
     *
     * @param original the original specification {@code S}
     * @param refined  the candidate repair {@code S'}
     * @return the preserved-models fraction in {@code [0, 1]}
     */
    private double compute_lost_models_porcentage(Tlsf original, Tlsf refined) {
        System.out.print("-");
        if (originalNumOfModels == null || originalNumOfModels.equals(BigInteger.ZERO))
            return 0.0d;

        Formula refined_formula = refined.toFormula().formula();
        if (refined_formula == BooleanConstant.TRUE)
            return 1.0d;
        if (refined_formula == BooleanConstant.FALSE)
            return 0.0d;
        Formula lostModels = Conjunction.of(original.toFormula().formula(), refined_formula.not());

        LabelledFormula formula = LabelledFormula.of(lostModels, original.variables());
        BigInteger form_count = countModels(formula);
        if (form_count == null)
            return 0.0d;
        BigDecimal numOfLostModels = new BigDecimal(form_count);
        //patch to avoid computing again this value;
//		commonNumOfModels = numOfLostModels;
        BigDecimal numOfModels = new BigDecimal(originalNumOfModels);
//        BigDecimal numOfModels = new BigDecimal(UNIVERSE);

        BigDecimal res = numOfLostModels.divide(numOfModels, 2, RoundingMode.HALF_UP);
        double value = 1.0d - res.doubleValue();
//		System.out.print(numOfLostModels + " " + numOfModels + " ");
        if (res.doubleValue() > 1.0d) {
            System.out.println("\nWARNING: increase the bound. ");
            return 1.0d;
        }
        return value;
    }

    /**
     * Penalty for the <i>new</i> behaviours introduced by the candidate:
     * {@code 1 - #(&not;S &and; S', k) / #(S', k)}, where the numerator counts
     * the bounded models of {@code S'} that the original specification did not
     * admit. A value of {@code 1} means the candidate adds no unexpected
     * behaviour ({@code S' &rArr; S} within the bound); lower values indicate
     * the repair opened the door to behaviours the engineer never asked for.
     *
     * <p>Symmetrically to the lost-models direction, the division by
     * {@code #(S', k)} normalises the count and the {@code 1 -} turns the
     * penalty into a reward — and when the candidate genuinely strengthens
     * the original ({@code S' &rArr; S}), the conjunction
     * {@code &not;S &and; S'} is unsatisfiable and the method returns exactly
     * {@code 1}, free of approximation noise.</p>
     *
     * <p>As with the lost-models direction, approximate counts can produce a
     * ratio above {@code 1}; a warning suggesting a larger bound {@code k} is
     * printed and the value is clamped.</p>
     *
     * @param original the original specification {@code S}
     * @param refined  the candidate repair {@code S'}
     * @return the no-new-behaviours fraction in {@code [0, 1]}
     */
    private double compute_won_models_porcentage(Tlsf original, Tlsf refined) {
        System.out.print("+");
        if (originalNumOfModels == null || originalNumOfModels.equals(BigInteger.ZERO))
            return 0.0d;

        BigInteger refinedNumOfModels = countModels(refined.toFormula());
        if (Objects.equals(refinedNumOfModels, BigInteger.ZERO))
            return 0.0d;

        Formula original_formula = original.toFormula().formula();
        Formula wonModels = Conjunction.of(original_formula.not(), refined.toFormula().formula());

        LabelledFormula formula = LabelledFormula.of(wonModels, original.variables());
        //patch to avoid computing again this value;
        BigInteger form_count = countModels(formula);
        if (form_count == null)
            return 0.0d;
        BigDecimal numOfWonModels = new BigDecimal(form_count);
        BigDecimal numOfRefinedModels = new BigDecimal(refinedNumOfModels);
        BigDecimal res = numOfWonModels.divide(numOfRefinedModels, 2, RoundingMode.HALF_UP);

        double value = 1.0d - res.doubleValue();
        if (res.doubleValue() > 1.0d) {
            System.out.println("\nWARNING: increase the bound. ");
            return 1.0d;
        }
        return value;
    }

    /**
     * Syntactic similarity between two specifications: the even average of the
     * two sub-formula overlap ratios,
     * {@code 0.5 * |SF(S) &cap; SF(S')| / |SF(S)| + 0.5 * |SF(S) &cap; SF(S')| / |SF(S')|}.
     * A value close to {@code 1} means the candidate shares most of its
     * sub-formulas with the original — it will look familiar to the engineer —
     * while a value close to {@code 0} means the syntactic overlap is minimal.
     *
     * @param original the original specification {@code S}
     * @param refined  the candidate repair {@code S'}
     * @return the syntactic similarity in {@code [0, 1]}
     */
    public double compute_syntactic_distance(Tlsf original, Tlsf refined) {
        List<LabelledFormula> sub_original = FormulaUtils.subformulas(original.toFormula());
        List<LabelledFormula> sub_refined = FormulaUtils.subformulas(refined.toFormula());
        Set<LabelledFormula> commonSubs = Sets.intersection(Sets.newHashSet(sub_original), Sets.newHashSet(sub_refined));
        double lost = ((double) commonSubs.size()) / ((double) sub_original.size());
        double won = ((double) commonSubs.size()) / ((double) sub_refined.size());
        return 0.5d * lost + 0.5d * won;
    }

    /**
     * Structural guard used to prune candidates that transgress the configured
     * search space: returns {@code true} if the candidate added an assumption
     * while {@code Settings.allowAssumptionAddition} is disabled, or removed a
     * guarantee while {@code Settings.allowGuaranteeRemoval} is disabled
     * (both detected by comparing conjunct counts against the original
     * specification).
     *
     * @param original the original specification
     * @param refined  the candidate repair
     * @return {@code true} iff the candidate violates the addition/removal restrictions
     */
    public boolean somethingHasBeenRemoved(Tlsf original, Tlsf refined) {
        boolean assumptionAdded = !Settings.allowAssumptionAddition && FormulaUtils.splitConjunction(original.assume()).size() < FormulaUtils.splitConjunction(refined.assume()).size();
        boolean guaranteeRemoved = !Settings.allowGuaranteeRemoval && FormulaUtils.splitConjunctions(original.guarantee()).size() > FormulaUtils.splitConjunctions(refined.guarantee()).size();
        return assumptionAdded || guaranteeRemoved;
    }

    /**
     * Prints the active fitness configuration to standard output: the four
     * component weights (status, lost models, won models, syntactic) and the
     * assumption-addition / guarantee-removal flags.
     */
    public void print_config() {
        System.out.printf("status: %s, lost: %s, won: %s, syn: %s, addA: %s, remG: %s%n", Settings.STATUS_FACTOR, Settings.LOST_MODELS_FACTOR, Settings.WON_MODELS_FACTOR, Settings.SYNTACTIC_FACTOR, Settings.allowAssumptionAddition, Settings.allowGuaranteeRemoval);
    }
}