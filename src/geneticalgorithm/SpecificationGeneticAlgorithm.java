package geneticalgorithm;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

import com.lagodiuk.ga.Fitness;
import com.lagodiuk.ga.GeneticAlgorithm;
import com.lagodiuk.ga.IterartionListener;
import com.lagodiuk.ga.Population;

import geneticalgorithm.SpecificationChromosome.SPEC_STATUS;
import main.Settings;
import owl.ltl.Formula;
import owl.ltl.tlsf.Tlsf;
import solvers.StrixHelper;
import solvers.StrixHelper.RealizabilitySolverResult;
import tlsf.TLSF_Utils;

public class SpecificationGeneticAlgorithm {
//	public static int BOUND = 0;
//	public static int GENERATIONS = 10;
//	public static int NUM_OF_INDIVIDUALS = Integer.MAX_VALUE;
//	public static int POPULATION_SIZE = 30;
//	public static int CROSSOVER_RATE = 10; // Percentage of chromosomes that will be selected for crossover
//	public static int MUTATION_RATE = 100; // Probability with which the mutation is applied to each chromosome
//	public static int EXECUTION_TIMEOUT = 0;//in seconds. No timeout by default.
	public Instant initialExecutionTime = null;
	public Instant finalExecutionTime = null;


	public List<SpecificationChromosome> solutions = new LinkedList<>();
	public List<SpecificationChromosome> bestSolutions = new LinkedList<>();

	public void run(Tlsf spec) throws IOException, InterruptedException {
		run(spec, -1.0d, -1.0d, -1.0d, false);
	}

	public void run(Tlsf spec, boolean allowAssumptionGuaranteeRemoval) throws IOException, InterruptedException {
		run(spec, -1.0d, -1.0d, -1.0d, allowAssumptionGuaranteeRemoval);
	}

	public void run(Tlsf spec, double status_factor,  double syntactic_factor, double semantic_factor, boolean allowAssumptionGuaranteeRemoval) throws IOException, InterruptedException{
		initialExecutionTime = Instant.now();
		long initialTime = System.currentTimeMillis();
		Population<SpecificationChromosome> population = createInitialPopulation(spec);
//		Fitness<SpecificationChromosome, Double> fitness = new SpecificationFitness();
//		Fitness<SpecificationChromosome, Double> fitness = new PreciseModelCountingSpecificationFitness(spec);
//		ModelCountingSpecificationFitness fitness = new ModelCountingSpecificationFitness(spec);
		AutomataBasedModelCountingSpecificationFitness fitness = new AutomataBasedModelCountingSpecificationFitness(spec);
		Settings.setFactors(status_factor,syntactic_factor,semantic_factor);
//		fitness.allowAssumptionGuaranteeRemoval(allowAssumptionGuaranteeRemoval);
//		fitness.setBound(Settings.MC_BOUND);
		//if (population.getChromosomeByIndex(0).status == SPEC_STATUS.REALIZABLE) {
		if (fitness.originalStatus ==  SPEC_STATUS.REALIZABLE) {
			System.out.println();
			System.out.println("The specification is already realizable.");
			return;
		}
		if (!fitness.originalStatus.isSpecificationConsistent()) {
			System.out.println();
			System.out.println("The specification is inconsistent. The approach requires a consistent specification as input.");
			return;
		}

		GeneticAlgorithm<SpecificationChromosome,Double> ga = new GeneticAlgorithm<SpecificationChromosome,Double>(population, fitness);
		addListener(ga);
		ga.setCrossoverRate(Settings.GA_CROSSOVER_RATE);
		ga.setMutationRate(Settings.GA_MUTATION_RATE);
		ga.setParentChromosomesSurviveCount(Settings.GA_POPULATION_SIZE);
		ga.setMaximumNumberOfIndividuals(Settings.GA_MAX_NUM_INDIVIDUALS);
		ga.setTIMEOUT(Settings.GA_EXECUTION_TIMEOUT);
		print_config();
		ga.evolve(Settings.GA_GENERATIONS);
		finalExecutionTime = Instant.now();
		
		System.out.println("Realizable Specifications:" );
		for (int i = 0; i < solutions.size(); i++) {
			SpecificationChromosome s = solutions.get(i);
			System.out.println();
			System.out.println(String.format("Solution N: %s\tFitness: %.2f", i, s.fitness));
			System.out.println(TLSF_Utils.adaptTLSFSpec(s.spec));
		}
		System.out.println(print_execution_time());
		System.out.println(print_config());
		fitness.print_config();
	}
	
	public void runRandom(Tlsf spec) throws IOException, InterruptedException{
		long initialTime = System.currentTimeMillis();
		//create random population
		Population<SpecificationChromosome> population = new Population<>();
		SpecificationChromosome init = new SpecificationChromosome(spec);
		//population.addChromosome(init);
		for (int i = 0; i < Settings.GA_POPULATION_SIZE; i++) {
			SpecificationChromosome c = init.mutate();
			population.addChromosome(c);
		}

		for (SpecificationChromosome c : population) {
			RealizabilitySolverResult status = StrixHelper.checkRealizability(c.spec);
			if (status == RealizabilitySolverResult.REALIZABLE) {
				solutions.add(c);
			}
		}
		System.out.println("Realizable Specifications:" );
		for (int i = 0; i < solutions.size(); i++) {
			SpecificationChromosome s = solutions.get(i);
			System.out.println();
			System.out.println(String.format("Solution N: %s\tFitness: %.2f", i, s.fitness));
			System.out.println(TLSF_Utils.adaptTLSFSpec(s.spec));
		}
		System.out.println(print_execution_time());
		System.out.println(print_config());
	}
	
	public String print_config() {
		return String.format("GEN: %s, Pop:%s, MAX:%s MR: %s, COR: %s", Settings.GA_GENERATIONS, Settings.GA_POPULATION_SIZE, (Settings.GA_MAX_NUM_INDIVIDUALS==Integer.MAX_VALUE)?"INF":Settings.GA_MAX_NUM_INDIVIDUALS, Settings.GA_MUTATION_RATE, Settings.GA_CROSSOVER_RATE);
	}

	public String print_execution_time() {
		Duration duration = Duration.between(initialExecutionTime, finalExecutionTime);
		String timeStr = String.format("Time: %s m  %s s",duration.toMinutes(), duration.toSecondsPart());
		return timeStr;
	}
	private Population<SpecificationChromosome> createInitialPopulation(Tlsf spec){
		Population<SpecificationChromosome> population = new Population<>();
		SpecificationChromosome init = new SpecificationChromosome(spec);
		population.addChromosome(init);
//		for (Formula g : spec.guarantee()) {
//			Tlsf g_spec = TLSF_Utils.fromSpec(spec);
//			g_spec = TLSF_Utils.change_guarantees(g_spec, g);
//			SpecificationChromosome g_init = new SpecificationChromosome(g_spec);
//			population.addChromosome(g_init);
//		}
		return population;
	}
	
	
	private  void addListener(GeneticAlgorithm<SpecificationChromosome,Double> ga) {
		// just for pretty print
				System.out.println(String.format("%s\t%s\t%s\t%s\t%s", "iter", "fit", "chromosome","#Pop","#Sol"));

				// Lets add listener, which prints best chromosome after each iteration
				ga.addIterationListener(new IterartionListener<SpecificationChromosome, Double>() {

					//TODO: select a reasonable threshold
					private final double threshold = 0.0d;

					@Override
					public void update(GeneticAlgorithm<SpecificationChromosome, Double> ga) {

						SpecificationChromosome best = ga.getBest();
						double bestFit = ga.fitness(best);
						int iteration = ga.getIteration();
						if (bestFit > 1.0d && !bestSolutions.contains(best)) {
							System.out.println(String.format("BEST Fitness: %.2f",best.fitness));
							System.out.println(TLSF_Utils.adaptTLSFSpec(best.spec));
							bestSolutions.add(best);
							ga.terminate();
						}
							
						// put current best in the list
//						if (!bestSolutions.contains(best.spec))
//							bestSolutions.add(best.spec);

						// If fitness is satisfying 
//						if (best.status == SPEC_STATUS.REALIZABLE && bestFit >= threshold) {
//							// we save the best solutions as one in the boundary
//							if (!solutions.contains(best))
//								solutions.add(best);
//							// we can stop Genetic algorithm
////							ga.terminate(); 
//						}
						
						// save ALL the solutions
						for (SpecificationChromosome c : ga.getPopulation()) {
							if (c.fitness < threshold) break;
							if (c.status == SPEC_STATUS.REALIZABLE && !solutions.contains(c))
								solutions.add(c);
							// we can stop Genetic algorithm
//							ga.terminate(); 
						}
						
						// Listener prints best achieved solution
						System.out.println();
						System.out.println(String.format("%s\t%.2f\t%s\t%s\t%s", iteration, bestFit, best, ga.getNumberOfVisitedIndividuals(),solutions.size()));

//						//check if timeout has been reached
//						if (EXECUTION_TIMEOUT > 0) {
//							Duration current = Duration.between(initialExecutionTime, Instant.now());
////							long currentIterationTime = System.currentTimeMillis();
////							long totalTime = currentIterationTime - initialExecutionTime;
////							int min = (int) (totalTime) / 60000;
////							int sec = (int) (totalTime - min * 60000) / 1000;
//
//							if (current.toSeconds() > EXECUTION_TIMEOUT) {
//								System.out.println("GENETIC ALGORITHM TIMEOUT REACHED. Terminating the execution...");
//								ga.terminate();
//							}
//						}
					}
				});
	}

}
