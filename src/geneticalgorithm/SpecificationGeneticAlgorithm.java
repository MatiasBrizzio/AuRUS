package geneticalgorithm;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.lagodiuk.ga.Fitness;
import com.lagodiuk.ga.GeneticAlgorithm;
import com.lagodiuk.ga.IterartionListener;
import com.lagodiuk.ga.Population;

import geneticalgorithm.SpecificationChromosome.SPEC_STATUS;
import owl.ltl.Formula;
import owl.ltl.tlsf.Tlsf;
import solvers.StrixHelper;
import solvers.StrixHelper.RealizabilitySolverResult;
import tlsf.TLSF_Utils;

public class SpecificationGeneticAlgorithm {
	
	public static int GENERATIONS = 10;
	public static int POPULATION_SIZE = 30;
	public static int CROSSOVER_RATE = 10; // Percentage of chromosomes that will be selected for crossover
	public static int MUTATION_RATE = 100; // Probability with which the mutation is applied to each chromosome
	
	public List<SpecificationChromosome> solutions = new LinkedList<>();
	public List<SpecificationChromosome> bestSolutions = new LinkedList<>();
	
	public void run(Tlsf spec) throws IOException, InterruptedException{
		long initialTime = System.currentTimeMillis();
		Population<SpecificationChromosome> population = createInitialPopulation(spec);
//		Fitness<SpecificationChromosome, Double> fitness = new SpecificationFitness();
//		Fitness<SpecificationChromosome, Double> fitness = new PreciseModelCountingSpecificationFitness(spec);
//		ModelCountingSpecificationFitness fitness = new ModelCountingSpecificationFitness(spec);
		AutomataBasedModelCountingSpecificationFitness fitness = new AutomataBasedModelCountingSpecificationFitness(spec);
		//if (population.getChromosomeByIndex(0).status == SPEC_STATUS.REALIZABLE) {
		if (fitness.originalStatus ==  SPEC_STATUS.REALIZABLE) {	
			System.out.println("The specification is already realizable.");
			return;
		}

		GeneticAlgorithm<SpecificationChromosome,Double> ga = new GeneticAlgorithm<SpecificationChromosome,Double>(population, fitness);
		addListener(ga);
		ga.setCrossoverRate(CROSSOVER_RATE);
		ga.setMutationRate(MUTATION_RATE);
		ga.setParentChromosomesSurviveCount(POPULATION_SIZE);
		print_config();
		ga.evolve(GENERATIONS);
		long finalTime = System.currentTimeMillis();
		
		System.out.println("Realizable Specifications:" );
		for (int i = 0; i < solutions.size(); i++) {
			SpecificationChromosome s = solutions.get(i);
			System.out.println();
			System.out.println(String.format("Solution N: %s\tFitness: %.2f", i, s.fitness));
			System.out.println(TLSF_Utils.adaptTLSFSpec(s.spec));
		}
		long totalTime = finalTime-initialTime;
		int min = (int) (totalTime)/60000;
		int sec = (int) (totalTime - min*60000)/1000;
		System.out.println(String.format("Time: %s m  %s s",min, sec));
		print_config();
		fitness.print_config();
	}
	
	public void runRandom(Tlsf spec) throws IOException, InterruptedException{
		long initialTime = System.currentTimeMillis();
		//create random population
		Population<SpecificationChromosome> population = new Population<>();
		SpecificationChromosome init = new SpecificationChromosome(spec);
		population.addChromosome(init);
		for (int i = 0; i < POPULATION_SIZE; i++) {
			SpecificationChromosome c = init.mutate();
			population.addChromosome(c);
		}
		ModelCountingSpecificationFitness fitness = new ModelCountingSpecificationFitness(spec);
		GeneticAlgorithm<SpecificationChromosome,Double> ga = new GeneticAlgorithm<SpecificationChromosome,Double>(population, fitness);
		for (SpecificationChromosome c : ga.getPopulation()) {
			if (c.status == SPEC_STATUS.REALIZABLE) {
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
		long finalTime = System.currentTimeMillis();
		long totalTime = finalTime-initialTime;
		int min = (int) (totalTime)/60000;
		int sec = (int) (totalTime - min*60000)/1000;
		System.out.println(String.format("Time: %s m  %s s",min, sec));
		print_config();
		fitness.print_config();
	}
	
	public void print_config() {
		System.out.println(String.format("GEN: %s, Pop:%s MR: %s, COR: %s", GENERATIONS, POPULATION_SIZE, MUTATION_RATE, CROSSOVER_RATE));
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
					private final double threshold = 0.9d;

					@Override
					public void update(GeneticAlgorithm<SpecificationChromosome, Double> ga) {

						SpecificationChromosome best = ga.getBest();
						double bestFit = ga.fitness(best);
						int iteration = ga.getIteration();
						if (bestFit >= 1.0d && !bestSolutions.contains(best)) {
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
						System.out.println(String.format("%s\t%.2f\t%s\t%s\t%s", iteration, bestFit, best, ga.getPopulationSize(),solutions.size()));
					}
				});
	}

}
