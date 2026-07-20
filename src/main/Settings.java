package main;

import java.util.Random;

/**
 * Central, static configuration of AuRUS: every knob the genetic search, the
 * fitness function, the model counter and the external solvers read from.
 *
 * <p>Fields are grouped by concern below, each documented with the
 * command-line flag that sets it (see {@link Main}), its default, and — where
 * useful — a pointer to the class that reads it. This mirrors the
 * "Configuration reference" tables in the project README; the two should be
 * read together.</p>
 *
 * <p>All fields are public static and mutable by design: {@link Main} parses
 * the command line once at start-up and writes directly into this class, and
 * every other class reads from it for the rest of the run. There is
 * deliberately no per-run instance — AuRUS runs one search per process.</p>
 *
 * @author Mat&iacute;as Brizzio
 * @see Main
 * @see geneticalgorithm.SpecificationGeneticAlgorithm
 * @see geneticalgorithm.AutomataBasedModelCountingSpecificationFitness
 */
public class Settings {

    /** Treat the input as a Spectra specification instead of TLSF (flag {@code -use-spectra}, default {@code false}). */
    public static boolean USE_SPECTRA = false;

    /** Run Strix through the Docker image rather than the local install (flags {@code -docker}/{@code -no-docker}, default {@code false} — native). Read by {@link solvers.StrixHelper}. */
    public static boolean USE_DOCKER = false; // default: native Strix via lib/strix_tlsf.sh; enable the Docker image with -docker

    /** Shared random source for every stochastic decision in the search (selection coin flips, mutation targets, ...); seeded from {@link Math#random()} at class load, so runs are not reproducible by default. */
    public static Random RANDOM_GENERATOR = new Random(Double.doubleToLongBits(Math.random()));

    // ---- genetic algorithm setting ----

    /** Number of generations the search runs for (flag {@code -Gen}, default {@code 10}). Read by {@link geneticalgorithm.SpecificationGeneticAlgorithm}. */
    public static int GA_GENERATIONS = 10;

    /** Hard cap on the total number of individuals ever generated, across all generations (flag {@code -Max}, default: unbounded). */
    public static int GA_MAX_NUM_INDIVIDUALS = Integer.MAX_VALUE;

    /** Population size retained at each generation (flag {@code -Pop}, default {@code 100}). */
    public static int GA_POPULATION_SIZE = 100;

    /** Percentage of chromosomes selected for crossover at each generation (flag {@code -COR}, default {@code 10}). */
    public static int GA_CROSSOVER_RATE = 10; // Percentage of chromosomes that will be selected for crossover

    /** Probability (%) with which the mutation operator is applied to each chromosome (flag {@code -MR}, default {@code 100} — always). */
    public static int GA_MUTATION_RATE = 100; // Probability with which the mutation is applied to each chromosome

    /**
     * Probability (%) with which each gene (sub-formula) of a mutated
     * chromosome is itself mutated (flag {@code -geneMR}, default {@code 0}).
     * A value of {@code 0} means the rate is derived as {@code 1/size_of(formula)}
     * — the rule used in the paper's experimental setup — rather than a fixed
     * percentage; see {@link geneticalgorithm.SpecificationMutator}.
     */
    public static int GA_GENE_MUTATION_RATE = 0; // Probability with which the mutation is applied to each gene of the chromosome
    // 0 means that the probability will be 1/size_of(formula)

    /**
     * Maximum number of genes (sub-formulas) mutated per formula (flag
     * {@code -geneNUM}, default {@code 0}). A value of {@code 0} means no
     * cap — up to {@code size_of(formula)} mutations may be applied.
     */
    public static int GA_GENE_NUM_OF_MUTATIONS = 0; // Number of allowed genes to be mutated
    // 0 means that it will be allowed to apply size_of(formula) mutations

    /** Overall wall-clock budget for the genetic search, in seconds (flag {@code -GATO}, default {@code 0} — no timeout). */
    public static int GA_EXECUTION_TIMEOUT = 0;//in seconds. No timeout by default.

    /**
     * Probability (%, {@code p}) that a genetic operator (mutation or
     * crossover) targets the guarantees rather than the assumptions; the
     * assumptions are targeted with probability {@code 1-p} (flag
     * {@code -GPR}, default {@code 50}). {@code p = 0} protects the
     * guarantees entirely, {@code p = 100} protects the assumptions —
     * useful when one side encodes fixed, non-negotiable constraints.
     */
    public static int GA_GUARANTEES_PREFERENCE_FACTOR = 50; // p is the probability to which the genetic operators will be applied to the guarantees.
    // (1-p) is the probability to which the genetic operators will be applied to the assumptions.

    /** Use a random selector instead of the best-selector for the next generation's parents (flag {@code -GA_random_selector}, default {@code false}). Read by {@code com.lagodiuk.ga.GeneticAlgorithm}, the underlying GA engine. */
    public static boolean GA_RANDOM_SELECTOR = false;

    /** Restrict newly generated/transplanted assumption fragments to input variables only (flag {@code -onlyInputsA}, default {@code false}), preventing the environment from being "repaired" with constraints over outputs it cannot observe. */
    public static boolean only_inputs_in_assumptions = false;

    /** Minimum fitness a candidate must reach to be collected as a solution during the search (flag {@code -sol}, default {@code 0.0} — no threshold). */
    public static double GA_THRESHOLD = 0.0d;

    // ---- fitness setting ----

    /** Whether realisability (via Strix, or strong satisfiability when {@link #check_STRONG_SAT} is set) is checked inside the fitness function itself, rather than only on final candidates (flag {@code -onlySAT} sets this to {@code false}; default {@code true}). See {@link geneticalgorithm.AutomataBasedModelCountingSpecificationFitness#compute_status}. */
    public static boolean check_REALIZABILITY = true;

    /** Use the strong-satisfiability (potential realisability) check instead of a full Strix realisability query inside the fitness (flag {@code -strongSAT}, default {@code false}) — cheaper, less precise. */
    public static boolean check_STRONG_SAT = false;

    /** Allow the genetic operators to introduce brand-new assumptions rather than only modifying existing ones (flags {@code -addA}/{@code -addAssumptions}, default {@code false}). */
    public static boolean allowAssumptionAddition = false;

    /** Allow the genetic operators to drop guarantees entirely (flags {@code -removeG}/{@code -removeGuarantees}, default {@code false}). */
    public static boolean allowGuaranteeRemoval = false;

    /** Weight &alpha; of the realisability-status component in the fitness sum (flag {@code -factors}, first value; default {@code 0.7}). Set together with the other three factors via {@link #setFactors}. */
    public static double STATUS_FACTOR = 0.7d;

    /** Weight of the lost-models direction of the semantic-similarity component (half of &gamma; from {@code -factors}; default {@code 0.1}). See {@code docs/FITNESS.md}. */
    public static double LOST_MODELS_FACTOR = 0.1d;

    /** Weight of the won-models direction of the semantic-similarity component (the other half of &gamma;; default {@code 0.1}). See {@code docs/FITNESS.md}. */
    public static double WON_MODELS_FACTOR = 0.1d;

    /** Weight &beta; of the syntactic-similarity component in the fitness sum (flag {@code -factors}, second value; default {@code 0.1}). */
    public static double SYNTACTIC_FACTOR = 0.1d;

    // ---- parsing timeout ----

    /** Timeout, in seconds, for translating a formula into its automaton (default {@code 60}). Read by {@link modelcounter.EmersonLeiAutomatonBasedModelCounting}. */
    public static int PARSING_TIMEOUT = 60;

    // ---- model counting setting ----

    /** Bound {@code k} for the bounded model-counting approach (flag {@code -k}, default {@code 10}). See {@code docs/FITNESS.md} for what the bound controls. */
    public static int MC_BOUND = 10;

    /**
     * Intended to select an exact model counter instead of the automata/matrix
     * approximation (flag {@code -precise} sets this to {@code false}; default
     * {@code true}). <b>Currently unused:</b> no counting code path reads this
     * field, so {@code -precise} has no effect on the search — see the
     * project {@code TODO.md}.
     */
    public static boolean MC_EXHAUSTIVE = true;

    /** Timeout, in seconds, for a single bounded model-counting query (flag {@code -MCTO}, default {@code 180}). */
    public static int MC_TIMEOUT = 180;

    // ---- Strix setting ----

    /** Timeout, in seconds, for a single Strix realisability query (flag {@code -RTO}, default {@code 20}). */
    public static int STRIX_TIMEOUT = 20;

    /** Working directory Strix (native or Docker) reads/writes its temporary TLSF files in; overridden to the output directory via {@link #setStrixName} when {@code -out} is given (default {@code "docker/"}). Read by {@link utils.TlsfUtils}. */
    public static String STRIX_PATH = "docker/";

    /** Working directory used when checking realisability of Spectra specifications ({@code -use-spectra}, default {@code "docker-spectra/"}). Read by {@link solvers.StrixHelper}. */
    public static String SPECTRA_PATH = "docker-spectra/";

    // ---- SAT solver setting ----

    /** Timeout, in seconds, for a strong-satisfiability query (default {@code 180}). */
    public static int STRONG_SAT_TIMEOUT = 180;

    /** Timeout, in seconds, for a single LTL satisfiability query (flag {@code -SatTO}, default {@code 30}). */
    public static int SAT_TIMEOUT = 30;

    /**
     * Theoretical maximum value the fitness function can return: the sum of
     * all four component weights. Used as a sanity ceiling — a computed
     * fitness above this value indicates a configuration or arithmetic bug
     * (see {@link geneticalgorithm.AutomataBasedModelCountingSpecificationFitness#calculate}).
     *
     * @return {@code STATUS_FACTOR + LOST_MODELS_FACTOR + WON_MODELS_FACTOR + SYNTACTIC_FACTOR}
     */
    public static double MAX_FITNESS() {
        return STATUS_FACTOR + LOST_MODELS_FACTOR + WON_MODELS_FACTOR + SYNTACTIC_FACTOR;
    }

    /**
     * Sets the three top-level fitness weights (realisability status,
     * syntactic similarity, semantic similarity), splitting the semantic
     * weight evenly between {@link #LOST_MODELS_FACTOR} and
     * {@link #WON_MODELS_FACTOR}. A negative argument leaves the
     * corresponding setting(s) untouched, so callers can update a subset of
     * the weights (this is how {@code -factors} is applied, and how
     * {@link geneticalgorithm.SpecificationGeneticAlgorithm#run(owl.ltl.tlsf.Tlsf)}
     * — which passes all three as {@code -1.0} — keeps the currently
     * configured weights).
     *
     * @param status_factor    new value for {@link #STATUS_FACTOR}, or negative to keep the current one
     * @param syntactic_factor new value for {@link #SYNTACTIC_FACTOR}, or negative to keep the current one
     * @param semantic_factor  new combined weight to split across {@link #LOST_MODELS_FACTOR} and {@link #WON_MODELS_FACTOR}, or negative to keep the current ones
     */
    public static void setFactors(double status_factor, double syntactic_factor, double semantic_factor) {
        if (status_factor >= 0.0d)
            STATUS_FACTOR = status_factor;
        if (syntactic_factor >= 0.0d)
            SYNTACTIC_FACTOR = syntactic_factor;
        if (semantic_factor >= 0.0d) {
            double factor = semantic_factor / 2.0d;
            LOST_MODELS_FACTOR = factor;
            WON_MODELS_FACTOR = factor;
        }
    }

    /**
     * Redirects {@link #STRIX_PATH} to the given output directory (called
     * when {@code -out} is supplied), so Strix's temporary files land next to
     * the repairs rather than in the default {@code docker/} directory.
     *
     * @param outname the output directory (as passed to {@code -out})
     */
    public static void setStrixName(String outname) {
        STRIX_PATH = outname + "/";
    }

    /**
     * Serialises every setting relevant to reproducing a run into a single
     * line, printed at the start and end of {@code Main} and written to
     * {@code out.txt}.
     *
     * @return a human-readable, single-line summary of the current configuration
     */
    public static String print_settings() {
        return "Settings{" +
                "USE_DOCKER=" + USE_DOCKER +
                ", RANDOM_GENERATOR=" + RANDOM_GENERATOR +
                ", GA_GENERATIONS=" + GA_GENERATIONS +
                ", GA_MAX_NUM_INDIVIDUALS=" + GA_MAX_NUM_INDIVIDUALS +
                ", GA_POPULATION_SIZE=" + GA_POPULATION_SIZE +
                ", GA_CROSSOVER_RATE=" + GA_CROSSOVER_RATE +
                ", GA_MUTATION_RATE=" + GA_MUTATION_RATE +
                ", GA_GENE_MUTATION_RATE=" + GA_GENE_MUTATION_RATE +
                ", GA_GENE_NUM_OF_MUTATIONS=" + GA_GENE_NUM_OF_MUTATIONS +
                ", GA_EXECUTION_TIMEOUT=" + GA_EXECUTION_TIMEOUT +
                ", GA_GUARANTEES_PREFERENCE_FACTOR=" + GA_GUARANTEES_PREFERENCE_FACTOR +
                ", GA_RANDOM_SELECTOR=" + GA_RANDOM_SELECTOR +
                ", only_inputs_in_assumptions=" + only_inputs_in_assumptions +
                ", GA_THRESHOLD=" + GA_THRESHOLD +
                ", check_REALIZABILITY=" + check_REALIZABILITY +
                ", check_STRONG_SAT=" + check_STRONG_SAT +
                ", allowAssumptionAddition=" + allowAssumptionAddition +
                ", allowGuaranteeRemoval=" + allowGuaranteeRemoval +
                ", STATUS_FACTOR=" + STATUS_FACTOR +
                ", LOST_MODELS_FACTOR=" + LOST_MODELS_FACTOR +
                ", WON_MODELS_FACTOR=" + WON_MODELS_FACTOR +
                ", SYNTACTIC_FACTOR=" + SYNTACTIC_FACTOR +
                ", PARSING_TIMEOUT=" + PARSING_TIMEOUT +
                ", MC_BOUND=" + MC_BOUND +
                ", MC_EXHAUSTIVE=" + MC_EXHAUSTIVE +
                ", MC_TIMEOUT=" + MC_TIMEOUT +
                ", STRIX_TIMEOUT=" + STRIX_TIMEOUT +
                ", STRIX_PATH='" + STRIX_PATH + '\'' +
                ", STRONG_SAT_TIMEOUT=" + STRONG_SAT_TIMEOUT +
                ", SAT_TIMEOUT=" + SAT_TIMEOUT +
                '}';
    }
}