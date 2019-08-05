package geneticalgorithm;

import java.util.LinkedList;
import java.util.List;

import com.lagodiuk.ga.Fitness;
import com.lagodiuk.ga.GeneticAlgorithm;
import com.lagodiuk.ga.IterartionListener;
import com.lagodiuk.ga.Population;

import owl.ltl.tlsf.Tlsf;

public class SpecificationGeneticAlgorithm {
	
	public static int GENERATIONS = 10;
	public static int POPULATION_SIZE = 100;
	public static int CROSSOVER_RATE = 10; // Percentage of chromosomes that will be selected for crossover
	public static int MUTATION_RATE = 100; // Probability with which the mutation is applied to each chromosome
	
	public static List<Tlsf> solutions = new LinkedList<>();
	
	public void run(Tlsf spec){
		
		Population<SpecificationChromosome> population = createInitialPopulation(spec);
		Fitness<SpecificationChromosome, Double> fitness = new SpecificationFitness();
		GeneticAlgorithm<SpecificationChromosome,Double> ga = new GeneticAlgorithm<SpecificationChromosome,Double>(population, fitness);
		addListener(ga);

		ga.evolve(GENERATIONS);
		
		System.out.println("Realizable Specifications:" );
		System.out.println(solutions.toString());
	}
	
	private static Population<SpecificationChromosome> createInitialPopulation(Tlsf spec){
		Population<SpecificationChromosome> population = new Population<>();
		SpecificationChromosome init = new SpecificationChromosome(spec);
		population.addChromosome(init);
		return population;
	}
	
	private static void addListener(GeneticAlgorithm<SpecificationChromosome,Double> ga) {
		// just for pretty print
				System.out.println(String.format("%s\t%s\t%s", "iter", "fit", "chromosome"));

				// Lets add listener, which prints best chromosome after each iteration
				ga.addIterationListener(new IterartionListener<SpecificationChromosome, Double>() {

					//TODO: select a reasonable threshold
					private final double threshold = SpecificationFitness.SOLUTION;

					@Override
					public void update(GeneticAlgorithm<SpecificationChromosome, Double> ga) {

						SpecificationChromosome best = ga.getBest();
						double bestFit = ga.fitness(best);
						int iteration = ga.getIteration();
						
						// put current best in the list
						if (!solutions.contains(best.spec))
							solutions.add(best.spec);

						// Listener prints best achieved solution
						System.out.println(String.format("%s\t%s\t%s", iteration, bestFit, best));

						// If fitness is satisfying 
						if (bestFit >= this.threshold) {
							// we save the best solutions as one in the boundary
							solutions.add(best.spec);
							// we can stop Genetic algorithm
//							ga.terminate(); 
						}
					}
				});
	}
}
