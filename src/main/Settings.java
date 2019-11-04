package main;

import java.util.Random;

public class Settings {

	public static boolean USE_DOCKER = true;
	public static Random RANDOM_GENERATOR = new Random(System.currentTimeMillis());

	//genetic algorithm setting
	public static int GA_GENERATIONS = 10;
	public static int GA_MAX_NUM_INDIVIDUALS = Integer.MAX_VALUE;
	public static int GA_POPULATION_SIZE = 30;
	public static int GA_CROSSOVER_RATE = 10; // Percentage of chromosomes that will be selected for crossover
	public static int GA_MUTATION_RATE = 100; // Probability with which the mutation is applied to each chromosome
	public static int GA_EXECUTION_TIMEOUT = 0;//in seconds. No timeout by default.

	//fitness setting
	public static boolean allowAssumptionGuaranteeRemoval = false;
	public static double STATUS_FACTOR = 0.7d;
	public static double LOST_MODELS_FACTOR = 0.1d;
	public static double WON_MODELS_FACTOR = 0.1d;
	//	public static final double SOLUTION = 0.8d;
	public static double SYNTACTIC_FACTOR = 0.1d;
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

	//model counting setting
	public static int MC_BOUND = 10;
	public static boolean MC_EXHAUSTIVE = true;
	public static int MC_TIMEOUT = 60;

	//Strix setting
	public static int STRIX_TIMEOUT = 180;


	//SAT solver setting
	public static int SAT_TIMEOUT = 30;

}

