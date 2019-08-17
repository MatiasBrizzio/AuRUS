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
import tlsf.TLSF_Utils;

public class SpecificationGeneticAlgorithm {
	
	public static int GENERATIONS = 10;
	public static int POPULATION_SIZE = 30;
	public static int CROSSOVER_RATE = 10; // Percentage of chromosomes that will be selected for crossover
	public static int MUTATION_RATE = 100; // Probability with which the mutation is applied to each chromosome
	
	public static List<Tlsf> solutions = new LinkedList<>();
	public static List<Tlsf> bestSolutions = new LinkedList<>();
	
	public void run(Tlsf spec) throws IOException, InterruptedException{
		long initialTime = System.currentTimeMillis();
		Population<SpecificationChromosome> population = createInitialPopulation(spec);
//		Fitness<SpecificationChromosome, Double> fitness = new SpecificationFitness();
		Fitness<SpecificationChromosome, Double> fitness = new PreciseModelCountingSpecificationFitness(spec);
		GeneticAlgorithm<SpecificationChromosome,Double> ga = new GeneticAlgorithm<SpecificationChromosome,Double>(population, fitness);
		addListener(ga);
		ga.setCrossoverRate(CROSSOVER_RATE);
		ga.setMutationRate(MUTATION_RATE);
		ga.setParentChromosomesSurviveCount(POPULATION_SIZE);
		print_config();
		ga.evolve(GENERATIONS);
		long finalTime = System.currentTimeMillis();
		
		System.out.println("Realizable Specifications:" );
		for (Tlsf tlsf : solutions)
			System.out.println(TLSF_Utils.toTLSF(tlsf));
		
		long totalTime = finalTime-initialTime;
		int min = (int) (totalTime)/60000;
		int sec = (int) (totalTime - min*60000)/1000;
		System.out.println(String.format("Time: %s m  %s s",min, sec));
		print_config();
		PreciseModelCountingSpecificationFitness.print_config();
	}
	
	private void print_config() {
		System.out.println(String.format("GEN: %s, Pop:%s MR: %s, COR: %s", GENERATIONS, POPULATION_SIZE, MUTATION_RATE, CROSSOVER_RATE));
	}
	private static Population<SpecificationChromosome> createInitialPopulation(Tlsf spec){
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
	
	private static void addListener(GeneticAlgorithm<SpecificationChromosome,Double> ga) {
		// just for pretty print
				System.out.println(String.format("%s\t%s\t%s\t%s\t%s", "iter", "fit", "chromosome","#Pop","#Sol"));

				// Lets add listener, which prints best chromosome after each iteration
				ga.addIterationListener(new IterartionListener<SpecificationChromosome, Double>() {

					//TODO: select a reasonable threshold
					private final double threshold = 0.95d;

					@Override
					public void update(GeneticAlgorithm<SpecificationChromosome, Double> ga) {

						SpecificationChromosome best = ga.getBest();
						double bestFit = ga.fitness(best);
						int iteration = ga.getIteration();
						
						// put current best in the list
//						if (!bestSolutions.contains(best.spec))
//							bestSolutions.add(best.spec);

						// Listener prints best achieved solution
						System.out.println();
						System.out.println(String.format("%s\t%.2f\t%s\t%s\t%s", iteration, bestFit, best, ga.getPopulationSize(),solutions.size()));

						// If fitness is satisfying 
						if (best.status == SPEC_STATUS.REALIZABLE && bestFit >= threshold) {
							// we save the best solutions as one in the boundary
							if (!solutions.contains(best.spec))
								solutions.add(best.spec);
							// we can stop Genetic algorithm
//							ga.terminate(); 
						}
					}
				});
	}

}
