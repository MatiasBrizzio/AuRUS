package main;

import java.util.Random;

public class Settings {

	public static boolean USE_SPECTRA = false;
	public static boolean USE_DOCKER = true;
	public static Random RANDOM_GENERATOR = new Random(System.currentTimeMillis());

	//genetic algorithm setting
	public static int GA_GENERATIONS = 10;
	public static int GA_MAX_NUM_INDIVIDUALS = Integer.MAX_VALUE;
	public static int GA_POPULATION_SIZE = 100;
	public static int GA_CROSSOVER_RATE = 10; // Percentage of chromosomes that will be selected for crossover
	public static int GA_MUTATION_RATE = 100; // Probability with which the mutation is applied to each chromosome
	public static int GA_GENE_MUTATION_RATE = 0; // Probability with which the mutation is applied to each gene of the chromosome
												 // 0 means that the probability will be 1/size_of(formula)
	public static int GA_GENE_NUM_OF_MUTATIONS = 0; // Number of allowed genes to be mutated
													// 0 means that it will be allowed to apply size_of(formula) mutations

	public static int GA_EXECUTION_TIMEOUT = 0;//in seconds. No timeout by default.
	public static int GA_GUARANTEES_PREFERENCE_FACTOR = 50; // p is the probability to which the genetic operators will be applied to the guarantees.
															// (1-p) is the probability to which the genetic operators will be applied to the assumptions.
	public static boolean GA_RANDOM_SELECTOR = false;
	public static boolean only_inputs_in_assumptions = false;
	public static double GA_THRESHOLD = 0.0d;

	//fitness setting
	public static boolean check_REALIZABILITY = true;
	public static boolean check_STRONG_SAT = false;
	public static boolean allowAssumptionAddition = false;
	public static boolean allowGuaranteeRemoval = false;
	public static double STATUS_FACTOR = 0.7d;
	public static double LOST_MODELS_FACTOR = 0.1d;
	public static double WON_MODELS_FACTOR = 0.1d;
	//	public static final double SOLUTION = 0.8d;
	public static double SYNTACTIC_FACTOR = 0.1d;
	public static double MAX_FITNESS () {
		return STATUS_FACTOR + LOST_MODELS_FACTOR + WON_MODELS_FACTOR + SYNTACTIC_FACTOR;
	}

	public static void setFactors(double status_factor,  double syntactic_factor, double semantic_factor) {
		if (status_factor >= 0.0d)
			STATUS_FACTOR = status_factor;
		if (syntactic_factor >= 0.0d)
			SYNTACTIC_FACTOR = syntactic_factor;
		if (semantic_factor >= 0.0d) {
			double factor = semantic_factor/2.0d;
			LOST_MODELS_FACTOR = factor;
			WON_MODELS_FACTOR = factor;
		}
	}

	//parsing timeout
	public static int PARSING_TIMEOUT = 60;

	//model counting setting
	public static int MC_BOUND = 10;
	public static boolean MC_EXHAUSTIVE = true;
	public static int MC_TIMEOUT = 180;

	//Strix setting
	public static int STRIX_TIMEOUT = 180;
	public static String STRIX_PATH = "docker/";
	public static String SPECTRA_PATH = "docker-spectra/";

	public static void setStrixName(String outname) {
		STRIX_PATH = outname + "/";
	}
	//SAT solver setting
	public static int STRONG_SAT_TIMEOUT = 180;

	//SAT solver setting
	public static int SAT_TIMEOUT = 30;

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

//	public static String print_settings() {
//		return "Settings{" +
//				"USE_DOCKER=" + USE_DOCKER +
//				", RANDOM_GENERATOR=" + RANDOM_GENERATOR +
//				", GA_GENERATIONS=" + GA_GENERATIONS +
//				", GA_MAX_NUM_INDIVIDUALS=" + GA_MAX_NUM_INDIVIDUALS +
//				", GA_POPULATION_SIZE=" + GA_POPULATION_SIZE +
//				", GA_CROSSOVER_RATE=" + GA_CROSSOVER_RATE +
//				", GA_MUTATION_RATE=" + GA_MUTATION_RATE +
//				", GA_EXECUTION_TIMEOUT=" + GA_EXECUTION_TIMEOUT +
//				", GA_GUARANTEES_PREFERENCE_FACTOR=" + GA_GUARANTEES_PREFERENCE_FACTOR +
//				", GA_RANDOM_SELECTOR=" + GA_RANDOM_SELECTOR +
//				", only_inputs_in_assumptions=" + only_inputs_in_assumptions +
//				", GA_THRESHOLD=" + GA_THRESHOLD +
//				", check_REALIZABILITY=" + check_REALIZABILITY +
//				", check_STRONG_SAT=" + check_STRONG_SAT +
//				", allowAssumptionAddition=" + allowAssumptionAddition +
//				", allowGuaranteeRemoval=" + allowGuaranteeRemoval +
//				", STATUS_FACTOR=" + STATUS_FACTOR +
//				", LOST_MODELS_FACTOR=" + LOST_MODELS_FACTOR +
//				", WON_MODELS_FACTOR=" + WON_MODELS_FACTOR +
//				", SYNTACTIC_FACTOR=" + SYNTACTIC_FACTOR +
//				", PARSING_TIMEOUT=" + PARSING_TIMEOUT +
//				", MC_BOUND=" + MC_BOUND +
//				", MC_EXHAUSTIVE=" + MC_EXHAUSTIVE +
//				", MC_TIMEOUT=" + MC_TIMEOUT +
//				", STRIX_TIMEOUT=" + STRIX_TIMEOUT +
//				", STRONG_SAT_TIMEOUT=" + STRONG_SAT_TIMEOUT +
//				", SAT_TIMEOUT=" + SAT_TIMEOUT +
//				'}';
//	}
}

