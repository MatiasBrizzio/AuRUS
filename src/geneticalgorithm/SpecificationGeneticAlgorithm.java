package geneticalgorithm;

import com.lagodiuk.ga.GeneticAlgorithm;
import com.lagodiuk.ga.Population;
import geneticalgorithm.SpecificationChromosome.SPEC_STATUS;
import main.Settings;
import owl.ltl.*;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.visitors.GeneralFormulaMutator;
import owl.ltl.visitors.SubformulaReplacer;
import solvers.StrixHelper;
import solvers.StrixHelper.RealizabilitySolverResult;
import utils.FormulaUtils;
import utils.TlsfUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Main entry point of the AuRUS search: a genetic algorithm that evolves
 * candidate repairs for an unrealisable assume-guarantee LTL specification.
 *
 * <p>Given an input {@link Tlsf} specification {@code S = (A, G)}, this class
 * orchestrates the complete evolutionary loop:</p>
 * <ol>
 *   <li><b>Initial population</b> — seeded from the original specification plus
 *       variants obtained by adding patterned assumptions over the input
 *       variables (e.g. {@code G F x}, {@code G !(x0 & ... & xn)}) and by
 *       lightly mutating individual assumptions and guarantees
 *       (see {@link #createInitialPopulation(Tlsf)}).</li>
 *   <li><b>Fitness evaluation</b> — each candidate is scored by
 *       {@link AutomataBasedModelCountingSpecificationFitness}, which combines
 *       the realisability status (checked with Strix), the syntactic similarity,
 *       and the semantic similarity (estimated via bounded model counting over
 *       automaton transfer matrices) with respect to the original specification.</li>
 *   <li><b>Evolution</b> — selection, crossover and mutation are applied over
 *       successive generations, with rates and budgets taken from
 *       {@link Settings} (population size, crossover/mutation rates, maximum
 *       number of individuals, generations, and overall timeout).</li>
 *   <li><b>Output</b> — every candidate that reaches full realisability is
 *       collected into {@link #solutions} and reported, ranked by fitness, in
 *       TLSF format.</li>
 * </ol>
 *
 * <p>This class implements the approach introduced in:
 * <i>M. Brizzio, M. Cordy, M. Papadakis, C. S&aacute;nchez, N. Aguirre,
 * R. Degiovanni. "Automated Repair of Unrealisable LTL Specifications Guided
 * by Model Counting", GECCO 2023
 * (<a href="https://doi.org/10.1145/3583131.3590454">doi:10.1145/3583131.3590454</a>).</i>
 * Please cite this paper if you build on the technique.</p>
 *
 * @author Mat&iacute;as Brizzio
 * @see SpecificationChromosome
 * @see AutomataBasedModelCountingSpecificationFitness
 * @see main.Settings
 */
public class SpecificationGeneticAlgorithm {

    /** Timestamp taken when the run starts, before building the initial population. */
    public Instant initialExecutionTime = null;

    /** Timestamp taken when the evolutionary search finishes (before the final realisability sweep). */
    public Instant searchExecutionTime = null;

    /** Timestamp taken at the very end of the run, after solutions have been printed. */
    public Instant finalExecutionTime = null;


    /**
     * Realisable repairs found during the search, i.e. candidates whose status
     * reached {@link SPEC_STATUS#REALIZABLE}. Ordered by discovery; printed
     * ranked by fitness at the end of the run.
     */
    public List<SpecificationChromosome> solutions = new LinkedList<>();

    /**
     * Best candidates collected during the search when realisability is not
     * checked inside the fitness (see {@code Settings.check_REALIZABILITY} and
     * {@code Settings.check_STRONG_SAT}). These are verified with Strix in a
     * final sweep and promoted to {@link #solutions} if realisable.
     */
    public List<SpecificationChromosome> bestSolutions = new LinkedList<>();

    /**
     * Runs the genetic search on the given specification using the fitness
     * weights currently configured in {@link Settings} (equivalent to calling
     * {@link #run(Tlsf, double, double, double)} with negative factors, which
     * leaves the configured values untouched).
     *
     * @param spec the (typically unrealisable) input specification to repair
     * @throws IOException          if an external solver invocation fails
     * @throws InterruptedException if an external solver call is interrupted
     */
    public void run(Tlsf spec) throws IOException, InterruptedException {
        run(spec, -1.0d, -1.0d, -1.0d);
    }

    /**
     * Runs the complete genetic search on the given specification.
     *
     * <p>The three factors set the weights of the fitness components
     * (realisability status, syntactic similarity, semantic similarity); they
     * are forwarded to {@link Settings#setFactors(double, double, double)} and
     * should sum to 1. Passing a negative value keeps the currently configured
     * weight for that component.</p>
     *
     * <p>The method aborts early (without searching) if the input specification
     * is already realisable, or if it is inconsistent — the approach requires a
     * consistent specification as input. On termination, all realisable repairs
     * found are printed in TLSF format together with timing and configuration
     * information.</p>
     *
     * @param spec             the (typically unrealisable) input specification to repair
     * @param status_factor    weight of the realisability-status component of the fitness
     * @param syntactic_factor weight of the syntactic-similarity component of the fitness
     * @param semantic_factor  weight of the semantic-similarity (model counting) component of the fitness
     * @throws IOException          if an external solver invocation fails
     * @throws InterruptedException if an external solver call is interrupted
     */
    public void run(Tlsf spec, double status_factor, double syntactic_factor, double semantic_factor) throws IOException, InterruptedException {
        Settings.setFactors(status_factor, syntactic_factor, semantic_factor);
        System.out.println(Settings.print_settings() + "\n");
        initialExecutionTime = Instant.now();
        Population<SpecificationChromosome> population = createInitialPopulation(spec);
        AutomataBasedModelCountingSpecificationFitness fitness = new AutomataBasedModelCountingSpecificationFitness(spec);
        if (!Settings.check_STRONG_SAT && fitness.originalStatus == SPEC_STATUS.REALIZABLE) {
            System.out.println();
            System.out.println("The specification is already realizable.");
            return;
        }
        if (!fitness.originalStatus.isSpecificationConsistent()) {
            System.out.println();
            System.out.println("The specification is inconsistent. The approach requires a consistent specification as input.");
            return;
        }

        GeneticAlgorithm<SpecificationChromosome, Double> ga = new GeneticAlgorithm<>(population, fitness);
        addListener(ga);
        ga.setCrossoverRate(Settings.GA_CROSSOVER_RATE);
        ga.setMutationRate(Settings.GA_MUTATION_RATE);
        ga.setParentChromosomesSurviveCount(Settings.GA_POPULATION_SIZE);
        ga.setMaximumNumberOfIndividuals(Settings.GA_MAX_NUM_INDIVIDUALS);
        ga.setTIMEOUT(Settings.GA_EXECUTION_TIMEOUT);
        System.out.println(print_config());
        fitness.print_config();
        System.out.println();
        ga.evolve(Settings.GA_GENERATIONS);
        searchExecutionTime = Instant.now();
        if (!Settings.check_REALIZABILITY || Settings.check_STRONG_SAT) {
            System.out.println("Checking for Realizability ...");
            for (SpecificationChromosome c : bestSolutions) {
                System.out.print(".");
                RealizabilitySolverResult status = StrixHelper.checkRealizability(c.spec);
                if (status == RealizabilitySolverResult.REALIZABLE && !solutions.contains(c)) {
                    System.out.print("R");
                    solutions.add(c);
                }
            }
            System.out.println();
        }
        System.out.println("Realizable Specifications:");
        for (int i = 0; i < solutions.size(); i++) {
            SpecificationChromosome s = solutions.get(i);
            System.out.println();
            System.out.printf("Solution N: %s\tFitness: %.2f%n", i, s.fitness);
            System.out.println(TlsfUtils.adaptTLSFSpec(s.spec));
        }
        finalExecutionTime = Instant.now();
        System.out.println(print_execution_time());
        System.out.println(print_config());
        fitness.print_config();

        System.out.println();
        System.out.println(Settings.print_settings());
    }

    /**
     * Baseline used in the experimental evaluation (flag {@code -random}):
     * instead of evolving the population, it generates
     * {@code Settings.GA_POPULATION_SIZE} random mutants of the input
     * specification and checks each one for realisability with Strix at the
     * end. No fitness guidance is used, so comparing this baseline against
     * {@link #run(Tlsf)} isolates the contribution of the guided search.
     *
     * @param spec the (typically unrealisable) input specification
     * @throws IOException          if an external solver invocation fails
     * @throws InterruptedException if an external solver call is interrupted
     */
    public void runRandom(Tlsf spec) throws IOException, InterruptedException {
        System.out.println(Settings.print_settings() + "\n");
        initialExecutionTime = Instant.now();
        //create random population
        Population<SpecificationChromosome> population = new Population<>();
        SpecificationChromosome init = new SpecificationChromosome(spec);
        System.out.println("Random mutation of the Specifications...");
        for (int i = 0; i < Settings.GA_POPULATION_SIZE; i++) {
            SpecificationChromosome c = init.mutate();
            population.addChromosome(c);
        }

        searchExecutionTime = Instant.now();

        System.out.println("Checking for realizability...");
        for (SpecificationChromosome c : population) {
            RealizabilitySolverResult status = StrixHelper.checkRealizability(c.spec);
            System.out.print(".");
            if (status == RealizabilitySolverResult.REALIZABLE && !solutions.contains(c)) {
                System.out.print("R");
                solutions.add(c);
            }
        }
        System.out.println("Realizable Specifications:");
        for (int i = 0; i < solutions.size(); i++) {
            SpecificationChromosome s = solutions.get(i);
            System.out.println();
            System.out.printf("Solution N: %s\tFitness: %.2f%n", i, s.fitness);
            System.out.println(TlsfUtils.adaptTLSFSpec(s.spec));
        }
        finalExecutionTime = Instant.now();
        System.out.println(print_execution_time());
        System.out.println(print_config());
    }

    /**
     * Returns a one-line summary of the current genetic-algorithm
     * configuration: generations, population size, individuals budget,
     * mutation rate and crossover rate.
     *
     * @return a human-readable configuration string
     */
    public String print_config() {
        return String.format("GEN: %s, Pop:%s, MAX:%s MR: %s, COR: %s", Settings.GA_GENERATIONS, Settings.GA_POPULATION_SIZE, (Settings.GA_MAX_NUM_INDIVIDUALS == Integer.MAX_VALUE) ? "INF" : Settings.GA_MAX_NUM_INDIVIDUALS, Settings.GA_MUTATION_RATE, Settings.GA_CROSSOVER_RATE);
    }

    /**
     * Returns a summary of the wall-clock time spent by the run: the search
     * time (initial population until the end of the evolution) and the total
     * time (until solutions were printed), both in seconds. Relies on the
     * timestamps {@link #initialExecutionTime}, {@link #searchExecutionTime}
     * and {@link #finalExecutionTime}.
     *
     * @return a human-readable timing string
     */
    public String print_execution_time() {
        Duration search = Duration.ZERO;
        if (initialExecutionTime != null && searchExecutionTime != null)
            search = Duration.between(initialExecutionTime, searchExecutionTime);
        Duration duration = Duration.ZERO;
        if (initialExecutionTime != null && finalExecutionTime != null)
            duration = Duration.between(initialExecutionTime, finalExecutionTime);

        return String.format("GA Time:     %s", search.toSeconds()) + "\n" +
                String.format("Time:      %s", duration.toSeconds());
    }

    /**
     * Builds the initial population for the search.
     *
     * <p>The population always contains the original specification itself.
     * Depending on the configuration ({@code Settings.allowAssumptionAddition}
     * and {@code Settings.GA_GUARANTEES_PREFERENCE_FACTOR}), it is enriched
     * with:</p>
     * <ul>
     *   <li>variants adding one patterned assumption over the inputs:
     *       {@code G F x_i} (an input holds infinitely often),
     *       {@code G !(x_0 & ... & x_n)} (the inputs never hold all at once),
     *       and {@code G F (x_0 & ... & x_n)} (all inputs hold simultaneously,
     *       infinitely often) — only input variables are used, to avoid
     *       trivially/vacuously realisable repairs;</li>
     *   <li>variants where one assumption has a random sub-formula replaced by,
     *       or combined with, an input literal;</li>
     *   <li>variants where a random sub-formula of an assumption or a guarantee
     *       is rewritten by the general formula mutator (restricted to input
     *       variables for assumptions when
     *       {@code Settings.only_inputs_in_assumptions} is set);</li>
     *   <li>when {@code Settings.allowGuaranteeRemoval} is set, one variant per
     *       guarantee with that guarantee dropped (only if more than one
     *       guarantee remains), mirroring the assumption-addition seeding.</li>
     * </ul>
     *
     * <p>The guarantee-preference factor steers which side is seeded: values
     * below 100 enable the assumption-side variants, values above 0 enable the
     * guarantee-side variants.</p>
     *
     * @param spec the input specification the population is derived from
     * @return the initial {@link Population} of candidate specifications
     */
    private Population<SpecificationChromosome> createInitialPopulation(Tlsf spec) {
        Population<SpecificationChromosome> population = new Population<>();
        SpecificationChromosome init = new SpecificationChromosome(spec);
        population.addChromosome(init);


        if (Settings.allowAssumptionAddition && Settings.GA_GUARANTEES_PREFERENCE_FACTOR < 100) {
            //add simple assumptions G F input
            for (int i = 0; i < spec.numberOfInputs(); i++) {
                Literal input = Literal.of(i);
                if (Settings.RANDOM_GENERATOR.nextBoolean())
                    input = input.not();
                Formula new_assumption = GOperator.of(FOperator.of(input));
                List<Formula> assumes = FormulaUtils.splitConjunction(spec.assume());
                assumes.add(new_assumption);
                Tlsf input_spec = TlsfUtils.change_assume(spec, assumes);
                population.addChromosome(new SpecificationChromosome(input_spec));
            }
            //add simple assumptions: //G(!(i_1 & i_2))
            List<Literal> inputs = new LinkedList<>();
            for (int i = 0; i < spec.numberOfInputs(); i++) {
                inputs.add(Literal.of(i));
            }

            Formula new_assumption = GOperator.of(Conjunction.of(inputs).not());
            List<Formula> assumes = FormulaUtils.splitConjunction(spec.assume());
            assumes.add(new_assumption);
            Tlsf input_spec = TlsfUtils.change_assume(spec, assumes);
            population.addChromosome(new SpecificationChromosome(input_spec));

            //add simple assumptions: //GF (i_1 & i_2)
            new_assumption = GOperator.of(FOperator.of(Conjunction.of(inputs)));
            assumes = FormulaUtils.splitConjunction(spec.assume());
            assumes.add(new_assumption);
            input_spec = TlsfUtils.change_assume(spec, assumes);
            population.addChromosome(new SpecificationChromosome(input_spec));
        }

        //combine or replace sub formulas by one input
        if (Settings.GA_GUARANTEES_PREFERENCE_FACTOR < 100) {
            for (Formula as : FormulaUtils.splitConjunction(spec.assume())) {
                int i = Settings.RANDOM_GENERATOR.nextInt(spec.numberOfInputs());
                Literal input = Literal.of(i);
                if (Settings.RANDOM_GENERATOR.nextBoolean())
                    input = input.not();
                Formula new_assumption;
                if (Settings.RANDOM_GENERATOR.nextBoolean())
                    new_assumption = FormulaUtils.replaceSubformula(as, input);
                else
                    new_assumption = FormulaUtils.combineSubformula(as, input);
                List<Formula> assumes = FormulaUtils.splitConjunction(spec.assume());
                assumes.remove(as);
                assumes.add(new_assumption);
                Tlsf input_spec = TlsfUtils.change_assume(spec, assumes);
                population.addChromosome(new SpecificationChromosome(input_spec));
            }
        }

        // weaken some sub formula
        if (Settings.GA_GUARANTEES_PREFERENCE_FACTOR < 100) {
            for (Formula as : FormulaUtils.splitConjunction(spec.assume())) {
                Set<Formula> subformulas = FormulaUtils.subformulas(as);
                int n = subformulas.size();
                Formula to_replace = (Formula) subformulas.toArray()[Settings.RANDOM_GENERATOR.nextInt(n)];
                List<String> variables = spec.variables();
                if (Settings.only_inputs_in_assumptions)
                    variables = variables.subList(0, spec.numberOfInputs());
                GeneralFormulaMutator formVisitor = new GeneralFormulaMutator(variables, n, 1);
                Formula mutated_subformula = to_replace.nnf().accept(formVisitor);
                SubformulaReplacer visitor = new SubformulaReplacer(to_replace, mutated_subformula);
                Formula mutated_assumption = as.accept(visitor);
                List<Formula> assumes = FormulaUtils.splitConjunction(spec.assume());
                assumes.remove(as);
                assumes.add(mutated_assumption);
                Tlsf input_spec = TlsfUtils.change_assume(spec, assumes);
                population.addChromosome(new SpecificationChromosome(input_spec));
            }
        }

        if (Settings.GA_GUARANTEES_PREFERENCE_FACTOR > 0) {
            for (Formula g : spec.guarantee()) {
                int i = Settings.RANDOM_GENERATOR.nextInt(spec.variables().size());
                Literal output = Literal.of(i);
                if (Settings.RANDOM_GENERATOR.nextBoolean())
                    output = output.not();
                Formula new_guarantee;
                if (Settings.RANDOM_GENERATOR.nextBoolean())
                    new_guarantee = FormulaUtils.replaceSubformula(g, output);
                else
                    new_guarantee = FormulaUtils.combineSubformula(g, output);
                List<Formula> guarantees = new LinkedList<>(spec.guarantee());
                guarantees.remove(g);
                guarantees.add(new_guarantee);
                Tlsf input_spec = TlsfUtils.change_guarantees(spec, guarantees);
                population.addChromosome(new SpecificationChromosome(input_spec));
            }
        }

        if (Settings.GA_GUARANTEES_PREFERENCE_FACTOR > 0) {
            for (Formula g : spec.guarantee()) {
                Set<Formula> subformulas = FormulaUtils.subformulas(g);
                int n = subformulas.size();
                Formula to_replace = (Formula) subformulas.toArray()[Settings.RANDOM_GENERATOR.nextInt(n)];
                List<String> variables = spec.variables();
                GeneralFormulaMutator formVisitor = new GeneralFormulaMutator(variables, n, 1);
                Formula mutated_subformula = to_replace.nnf().accept(formVisitor);
                SubformulaReplacer visitor = new SubformulaReplacer(to_replace, mutated_subformula);
                Formula mutated_guarantee = g.accept(visitor);
                List<Formula> guarantees = new LinkedList<>(spec.guarantee());
                guarantees.remove(g);
                guarantees.add(mutated_guarantee);
                Tlsf input_spec = TlsfUtils.change_guarantees(spec, guarantees);
                population.addChromosome(new SpecificationChromosome(input_spec));
            }
        }

        // Guarantee-removal seeding: the dual of the assumption-addition block above.
        // addAssumptions works on the SPLIT list (splitConjunction(spec.assume())) and
        // ADDS a conjunct; here we work on the split guarantee list and, per conjunct,
        // seed one variant with that conjunct REMOVED. Same skeleton, inverse operation.
        // remove(g) is remove(Object) over a copy, so it does not trip the
        // "List.remove() in loop" inspection (that targets remove(int)). Guarded by
        // size > 1 so the set never empties (an empty conjunction is TRUE, which the
        // fitness prunes). The stochastic 5% removal lives in SpecificationMutator.
        if (Settings.allowGuaranteeRemoval && Settings.GA_GUARANTEES_PREFERENCE_FACTOR > 0) {
            List<Formula> conjuncts = FormulaUtils.splitConjunctions(spec.guarantee());
            if (conjuncts.size() > 1) {
                for (Formula g : conjuncts) {
                    List<Formula> guarantees = new LinkedList<>(conjuncts);
                    guarantees.remove(g);
                    Tlsf input_spec = TlsfUtils.change_guarantees(spec, guarantees);
                    population.addChromosome(new SpecificationChromosome(input_spec));
                }
            }
        }

        // Assumption-removal seeding: dual of the guarantee-removal block above.
        // Works on the split assumption list (splitConjunction(spec.assume())); per
        // conjunct, seed one variant with that assumption removed. Same remove(Object)
        // over a copy idiom, guarded by size > 1 so at least one assumption remains.
        // The stochastic 5% removal lives in SpecificationMutator; with the flag off,
        // the fitness guard prunes any candidate that drops an assumption.
        if (Settings.allowAssumptionRemoval && Settings.GA_GUARANTEES_PREFERENCE_FACTOR < 100) {
            List<Formula> conjuncts = FormulaUtils.splitConjunction(spec.assume());
            if (conjuncts.size() > 1) {
                for (Formula a : conjuncts) {
                    List<Formula> assumes = new LinkedList<>(conjuncts);
                    assumes.remove(a);
                    Tlsf input_spec = TlsfUtils.change_assume(spec, assumes);
                    population.addChromosome(new SpecificationChromosome(input_spec));
                }
            }
        }
        return population;
    }


    /**
     * Attaches the per-generation listener to the underlying GA engine.
     *
     * <p>After every generation the listener: (i) prints a progress line with
     * the iteration number, best fitness, best chromosome, number of visited
     * individuals and number of solutions found so far; (ii) collects every
     * candidate above the fitness threshold whose status is
     * {@link SPEC_STATUS#REALIZABLE} into {@link #solutions} (or into
     * {@link #bestSolutions} when realisability is not being checked inside
     * the fitness, for later verification); and (iii) terminates the search
     * defensively if a fitness above the theoretical maximum is observed.</p>
     *
     * @param ga the genetic algorithm engine to instrument
     */
    private void addListener(GeneticAlgorithm<SpecificationChromosome, Double> ga) {
        // just for pretty print
        System.out.printf("%s\t%s\t%s\t%s\t%s%n", "iter", "fit", "chromosome", "#Pop", "#Sol");

        ga.addIterationListener(ga1 -> {

            SpecificationChromosome best = ga1.getBest();
            double bestFit = ga1.fitness(best);
            int iteration = ga1.getIteration();
            if (bestFit > Settings.MAX_FITNESS()) {
                System.out.printf("WRONG Fitness: %.2f%n", best.fitness);
                System.out.println(TlsfUtils.adaptTLSFSpec(best.spec));
                bestSolutions.add(best);
                ga1.terminate();
            }

            // save ALL the solutions
            if (Settings.check_REALIZABILITY && !Settings.check_STRONG_SAT) {
                for (SpecificationChromosome c : ga1.getPopulation()) {
                    if (c.fitness < Settings.GA_THRESHOLD) break;
                    if (c.status == SPEC_STATUS.REALIZABLE && !solutions.contains(c))
                        solutions.add(c);
                }
            } else if (Settings.check_REALIZABILITY) {
                for (SpecificationChromosome c : ga1.getPopulation()) {
                    if (c.fitness < Settings.GA_THRESHOLD) break;
                    if (c.status == SPEC_STATUS.REALIZABLE && !bestSolutions.contains(best))
                        bestSolutions.add(best);
                }
            } else {
                for (SpecificationChromosome c : ga1.getPopulation()) {
                    if (c.fitness < Settings.GA_THRESHOLD) break;
                    if (c.status.isSpecificationConsistent() && !bestSolutions.contains(best))
                        bestSolutions.add(best);
                }
            }
            // Listener prints best achieved solution
            System.out.println();
            System.out.printf("%s\t%.2f\t%s\t%s\t%s%n", iteration, bestFit, best, ga1.getNumberOfVisitedIndividuals(), (Settings.check_REALIZABILITY && !Settings.check_STRONG_SAT) ? solutions.size() : bestSolutions.size());
        });
    }

}