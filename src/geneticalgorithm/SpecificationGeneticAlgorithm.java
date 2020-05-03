package geneticalgorithm;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.lagodiuk.ga.Fitness;
import com.lagodiuk.ga.GeneticAlgorithm;
import com.lagodiuk.ga.IterartionListener;
import com.lagodiuk.ga.Population;

import geneticalgorithm.SpecificationChromosome.SPEC_STATUS;
import main.Settings;
import owl.ltl.*;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.visitors.FormulaWeakening;
import owl.ltl.visitors.GeneralFormulaMutator;
import owl.ltl.visitors.SubformulaReplacer;
import solvers.StrixHelper;
import solvers.StrixHelper.RealizabilitySolverResult;
import tlsf.Formula_Utils;
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
	public Instant searchExecutionTime = null;
	public Instant finalExecutionTime = null;


	public List<SpecificationChromosome> solutions = new LinkedList<>();
	public List<SpecificationChromosome> bestSolutions = new LinkedList<>();

	public void run(Tlsf spec) throws IOException, InterruptedException {
		run(spec, -1.0d, -1.0d, -1.0d);
	}


	public void run(Tlsf spec, double status_factor,  double syntactic_factor, double semantic_factor) throws IOException, InterruptedException{
		Settings.setFactors(status_factor,syntactic_factor,semantic_factor);
		System.out.println(Settings.print_settings()+"\n");
		initialExecutionTime = Instant.now();
		long initialTime = System.currentTimeMillis();
		Population<SpecificationChromosome> population = createInitialPopulation(spec);
//		Fitness<SpecificationChromosome, Double> fitness = new SpecificationFitness();
//		Fitness<SpecificationChromosome, Double> fitness = new PreciseModelCountingSpecificationFitness(spec);
//		ModelCountingSpecificationFitness fitness = new ModelCountingSpecificationFitness(spec);
		AutomataBasedModelCountingSpecificationFitness fitness = new AutomataBasedModelCountingSpecificationFitness(spec);
//		fitness.allowAssumptionGuaranteeRemoval(allowAssumptionGuaranteeRemoval);
//		fitness.setBound(Settings.MC_BOUND);
		//if (population.getChromosomeByIndex(0).status == SPEC_STATUS.REALIZABLE) {
		if (!Settings.check_STRONG_SAT && fitness.originalStatus ==  SPEC_STATUS.REALIZABLE) {
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
		System.out.println(print_config());
		fitness.print_config();
		System.out.println();
		ga.evolve(Settings.GA_GENERATIONS);
		searchExecutionTime = Instant.now();
		if (!Settings.check_REALIZABILITY || Settings.check_STRONG_SAT) {
			System.out.println("Checking for Realizability ..." );
			for (SpecificationChromosome c : bestSolutions) {
//				System.out.println(TLSF_Utils.adaptTLSFSpec(c.spec));
				System.out.print(".");
				RealizabilitySolverResult status = StrixHelper.checkRealizability(c.spec);
				if (status == RealizabilitySolverResult.REALIZABLE && !solutions.contains(c)) {
					System.out.print("R");
					solutions.add(c);
				}
			}
			System.out.println();
		}
		System.out.println("Realizable Specifications:" );
		for (int i = 0; i < solutions.size(); i++) {
			SpecificationChromosome s = solutions.get(i);
			System.out.println();
			System.out.println(String.format("Solution N: %s\tFitness: %.2f", i, s.fitness));
			System.out.println(TLSF_Utils.adaptTLSFSpec(s.spec));
		}
		finalExecutionTime = Instant.now();
		System.out.println(print_execution_time());
		System.out.println(print_config());
		fitness.print_config();

		System.out.println();
		System.out.println(Settings.print_settings());
	}
	
	public void runRandom(Tlsf spec) throws IOException, InterruptedException{
		System.out.println(Settings.print_settings()+"\n");
		initialExecutionTime = Instant.now();
		//create random population
		Population<SpecificationChromosome> population = new Population<>();
		SpecificationChromosome init = new SpecificationChromosome(spec);
		//population.addChromosome(init);
		System.out.println("Random mutation of the Specifications..." );
		for (int i = 0; i < Settings.GA_POPULATION_SIZE; i++) {
			SpecificationChromosome c = init.mutate();
			population.addChromosome(c);
		}

		searchExecutionTime = Instant.now();

		AutomataBasedModelCountingSpecificationFitness fitness = new AutomataBasedModelCountingSpecificationFitness(spec);
		System.out.println("Checking for realizability..." );
		for (SpecificationChromosome c : population) {
			Double f = fitness.calculate(c);
//			RealizabilitySolverResult status = StrixHelper.checkRealizability(c.spec);
//			System.out.print("." );
//			if (status == RealizabilitySolverResult.REALIZABLE) {
			if (c.fitness < Settings.GA_THRESHOLD) continue;
			if (c.status == SPEC_STATUS.REALIZABLE && !solutions.contains(c)){
				System.out.print("R" );
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
		finalExecutionTime = Instant.now();
		System.out.println(print_execution_time());
		System.out.println(print_config());
	}
	
	public String print_config() {
		return String.format("GEN: %s, Pop:%s, MAX:%s MR: %s, COR: %s", Settings.GA_GENERATIONS, Settings.GA_POPULATION_SIZE, (Settings.GA_MAX_NUM_INDIVIDUALS==Integer.MAX_VALUE)?"INF":Settings.GA_MAX_NUM_INDIVIDUALS, Settings.GA_MUTATION_RATE, Settings.GA_CROSSOVER_RATE);
	}

	public String print_execution_time() {
		Duration search = Duration.ZERO;
		if (initialExecutionTime!=null && searchExecutionTime!=null)
			search = Duration.between(initialExecutionTime, searchExecutionTime);
		Duration duration = Duration.ZERO;
		if (initialExecutionTime!=null && finalExecutionTime!=null)
			duration = Duration.between(initialExecutionTime, finalExecutionTime);

		String timeStr = String.format("GA Time:     %s",search.toSeconds()) + "\n" +
				String.format("Time:      %s", duration.toSeconds());
		return timeStr;
	}
	private Population<SpecificationChromosome> createInitialPopulation(Tlsf spec){
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
				List<Formula> assumes = Formula_Utils.splitConjunction(spec.assume());
				assumes.add(new_assumption);
				Tlsf input_spec = TLSF_Utils.change_assume(spec, assumes);
				population.addChromosome(new SpecificationChromosome(input_spec));
			}
			//add simple assumptions: //G(!(i_1 & i_2))
			List inputs = new LinkedList();
			for (int i = 0; i < spec.numberOfInputs(); i++) {
				inputs.add(Literal.of(i));
			}

			Formula new_assumption = GOperator.of(Conjunction.of(inputs).not());
			List<Formula> assumes = Formula_Utils.splitConjunction(spec.assume());
			assumes.add(new_assumption);
			Tlsf input_spec = TLSF_Utils.change_assume(spec, assumes);
			population.addChromosome(new SpecificationChromosome(input_spec));

			//add simple assumptions: //GF (i_1 & i_2)
			new_assumption = GOperator.of(FOperator.of(Conjunction.of(inputs)));
			assumes = Formula_Utils.splitConjunction(spec.assume());
			assumes.add(new_assumption);
			input_spec = TLSF_Utils.change_assume(spec, assumes);
			population.addChromosome(new SpecificationChromosome(input_spec));
		}

		//combine or replace sub formulas by one input
		if (Settings.GA_GUARANTEES_PREFERENCE_FACTOR < 100) {
			for (Formula as : Formula_Utils.splitConjunction(spec.assume())) {
				int i = Settings.RANDOM_GENERATOR.nextInt(spec.numberOfInputs());
				Literal input = Literal.of(i);
				if (Settings.RANDOM_GENERATOR.nextBoolean())
					input = input.not();
				Formula new_assumption = null;
				if (Settings.RANDOM_GENERATOR.nextBoolean())
					new_assumption = Formula_Utils.replaceSubformula(as,input);
				else
					new_assumption = Formula_Utils.combineSubformula(as,input);
				List<Formula> assumes = Formula_Utils.splitConjunction(spec.assume());
				assumes.remove(as);
				assumes.add(new_assumption);
				Tlsf input_spec = TLSF_Utils.change_assume(spec, assumes);
				population.addChromosome(new SpecificationChromosome(input_spec));
			}
		}

		// weaken some sub formula
		if (Settings.GA_GUARANTEES_PREFERENCE_FACTOR < 100) {
			for (Formula as : Formula_Utils.splitConjunction(spec.assume())) {
				Set<Formula> subformulas = Formula_Utils.subformulas(as);
				int n = subformulas.size();
				Formula to_replace = (Formula) subformulas.toArray()[Settings.RANDOM_GENERATOR.nextInt(n)];
				List<String> variables = spec.variables();
				if (Settings.only_inputs_in_assumptions)
					variables = variables.subList(0,spec.numberOfInputs());
				GeneralFormulaMutator formVisitor = new GeneralFormulaMutator(variables, n, 1);
				Formula mutated_subformula = to_replace.nnf().accept(formVisitor);
				SubformulaReplacer visitor = new SubformulaReplacer(to_replace,mutated_subformula);
				Formula mutated_assumption = as.accept(visitor);
				List<Formula> assumes = Formula_Utils.splitConjunction(spec.assume());
				assumes.remove(as);
				assumes.add(mutated_assumption);
				Tlsf input_spec = TLSF_Utils.change_assume(spec, assumes);
				population.addChromosome(new SpecificationChromosome(input_spec));
			}
		}

		if (Settings.GA_GUARANTEES_PREFERENCE_FACTOR > 0) {
			for (Formula g : spec.guarantee()) {
				int i = Settings.RANDOM_GENERATOR.nextInt(spec.variables().size());//spec.numberOfInputs() + Settings.RANDOM_GENERATOR.nextInt(spec.variables().size()-spec.numberOfInputs());
				Literal output = Literal.of(i);
				if (Settings.RANDOM_GENERATOR.nextBoolean())
					output = output.not();
				Formula new_guarantee = null;
				if (Settings.RANDOM_GENERATOR.nextBoolean())
					new_guarantee = Formula_Utils.replaceSubformula(g,output);
				else
					new_guarantee = Formula_Utils.combineSubformula(g,output);
				List<Formula> guarantees = new LinkedList<>(spec.guarantee());
				guarantees.remove(g);
				guarantees.add(new_guarantee);
				Tlsf input_spec = TLSF_Utils.change_guarantees(spec, guarantees);
				population.addChromosome(new SpecificationChromosome(input_spec));
			}
		}

		if (Settings.GA_GUARANTEES_PREFERENCE_FACTOR > 0) {
			for (Formula g : spec.guarantee()) {
				Set<Formula> subformulas = Formula_Utils.subformulas(g);
				int n = subformulas.size();
				Formula to_replace = (Formula) subformulas.toArray()[Settings.RANDOM_GENERATOR.nextInt(n)];
				List<String> variables = spec.variables();
				GeneralFormulaMutator formVisitor = new GeneralFormulaMutator(variables, n, 1);
				Formula mutated_subformula = to_replace.nnf().accept(formVisitor);
				SubformulaReplacer visitor = new SubformulaReplacer(to_replace,mutated_subformula);
				Formula mutated_guarantee = g.accept(visitor);
				List<Formula> guarantees = new LinkedList<>(spec.guarantee());
				guarantees.remove(g);
				guarantees.add(mutated_guarantee);
				Tlsf input_spec = TLSF_Utils.change_guarantees(spec, guarantees);
				population.addChromosome(new SpecificationChromosome(input_spec));
			}
		}


		return population;
	}
	
	
	private  void addListener(GeneticAlgorithm<SpecificationChromosome,Double> ga) {
		// just for pretty print
				System.out.println(String.format("%s\t%s\t%s\t%s\t%s", "iter", "fit", "chromosome","#Pop","#Sol"));

				// Lets add listener, which prints best chromosome after each iteration
				ga.addIterationListener(new IterartionListener<SpecificationChromosome, Double>() {

//					//TODO: select a reasonable threshold
//					private final double threshold = 0.0d;

					@Override
					public void update(GeneticAlgorithm<SpecificationChromosome, Double> ga) {

						SpecificationChromosome best = ga.getBest();
						double bestFit = ga.fitness(best);
						int iteration = ga.getIteration();
						if (bestFit > 1.0d ) {
							System.out.println(String.format("WRONG Fitness: %.2f",best.fitness));
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
						if (Settings.check_REALIZABILITY && !Settings.check_STRONG_SAT) {
							for (SpecificationChromosome c : ga.getPopulation()) {
								if (c.fitness < Settings.GA_THRESHOLD) break;
								if (c.status == SPEC_STATUS.REALIZABLE && !solutions.contains(c))
									solutions.add(c);
								// we can stop Genetic algorithm
								// ga.terminate();
							}
						}
						else if (Settings.check_REALIZABILITY && Settings.check_STRONG_SAT) {
							for (SpecificationChromosome c : ga.getPopulation()) {
								if (c.fitness < Settings.GA_THRESHOLD) break;
								if (c.status == SPEC_STATUS.REALIZABLE && !bestSolutions.contains(best))
									bestSolutions.add(best);
							}
						}
						else {
							for (SpecificationChromosome c : ga.getPopulation()) {
								if (c.fitness < Settings.GA_THRESHOLD) break;
								if (c.status.isSpecificationConsistent() && !bestSolutions.contains(best))
									bestSolutions.add(best);
							}
						}
						// Listener prints best achieved solution
						System.out.println();
						System.out.println(String.format("%s\t%.2f\t%s\t%s\t%s", iteration, bestFit, best, ga.getNumberOfVisitedIndividuals(), (Settings.check_REALIZABILITY && ! Settings.check_STRONG_SAT)?solutions.size():bestSolutions.size()));

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
