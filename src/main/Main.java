package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import geneticalgorithm.AutomataBasedModelCountingSpecificationFitness;
import geneticalgorithm.SpecificationChromosome;
import geneticalgorithm.SpecificationGeneticAlgorithm;
import owl.ltl.tlsf.Tlsf;
import tlsf.TLSF_Utils;

public class Main {

	public static void main(String[] args) throws IOException, InterruptedException {
		List<Tlsf> referenceSolutions = new LinkedList<>();
		int popSize = 0;
		int maxNumOfInd = 0;
		int crossoverRate = 0;
		int mutationRate = 0;
		int gene_mutationRate = 0;
		int gene_num_of_mutations = 0;
		int guaranteePreferenceRate = -1;
		boolean random_GA_selector = false;
		int generations = 0;
		boolean randomGen = false;
		double status_factor = -1.0d;
		double syntactic_factor = -1.0d;
		double semantic_factor = -1.0d;
		boolean allowGuaranteesRemoval = false;
		boolean allowAssumptionsAddition = false;
		boolean onlyInputsInAssumptions = false;
		boolean no_check_realizability = false;
		boolean strong_SAT = false;
		int bound = 0;
		boolean precise = false;
		int ga_timeout = 0;
		int real_timeout = 0;
		int sat_timeout = 0;
		int mc_timeout = 0;
		double threshold = 0.0d;
		String filename = "";
		String outname = "";
		for (int i = 0; i< args.length; i++ ){
			if(args[i].startsWith("-Gen=")){
				generations = Integer.parseInt(args[i].replace("-Gen=", ""));
			}
			else if(args[i].startsWith("-Pop=")){
				popSize = Integer.parseInt(args[i].replace("-Pop=", ""));
			}
			else if(args[i].startsWith("-Max=")){
				maxNumOfInd = Integer.parseInt(args[i].replace("-Max=", ""));
			}
			else if(args[i].startsWith("-COR=")){
				crossoverRate = Integer.parseInt(args[i].replace("-COR=", ""));
			}
			else if(args[i].startsWith("-GPR=")){
				guaranteePreferenceRate = Integer.parseInt(args[i].replace("-GPR=", ""));
			}
			else if(args[i].startsWith("-MR=")){
				mutationRate = Integer.parseInt(args[i].replace("-MR=", ""));
			}
			else if(args[i].startsWith("-geneMR=")){
				gene_mutationRate = Integer.parseInt(args[i].replace("-geneMR=", ""));
			}
			else if(args[i].startsWith("-geneNUM=")){
				gene_num_of_mutations = Integer.parseInt(args[i].replace("-geneNUM=", ""));
			}
			else if(args[i].startsWith("-k=")){
				bound = Integer.parseInt(args[i].replace("-k=", ""));
			}
			else if(args[i].startsWith("-precise")){
				precise = true;
			}
			else if(args[i].startsWith("-no-docker")){
				Settings.USE_DOCKER = false;
			}
			else if(args[i].startsWith("-random")){
				randomGen = true;
			}
			else if(args[i].startsWith("-GA_random_selector")){
				random_GA_selector = true;
			}
			else if(args[i].startsWith("-onlySAT")){
				no_check_realizability = true;
			}
			else if(args[i].startsWith("-strongSAT")){
				strong_SAT = true;
			}
			else if(args[i].startsWith("-removeG")){
				allowGuaranteesRemoval = true;
			}
			else if(args[i].startsWith("-addA")){
				allowAssumptionsAddition = true;
			}
			else if(args[i].startsWith("-onlyInputsA")){
				onlyInputsInAssumptions = true;
			}
			else if(args[i].startsWith("-GATO=")){
				ga_timeout = Integer.parseInt(args[i].replace("-GATO=", ""));
			}
			else if(args[i].startsWith("-RTO=")){
				real_timeout = Integer.parseInt(args[i].replace("-RTO=", ""));
			}
			else if(args[i].startsWith("-SatTO=")){
				sat_timeout = Integer.parseInt(args[i].replace("-SatTO=", ""));
			}
			else if(args[i].startsWith("-MCTO=")){
				mc_timeout = Integer.parseInt(args[i].replace("-MCTO=", ""));
			}
			else if(args[i].startsWith("-sol=")){
				threshold = Double.valueOf(args[i].replace("-sol=", ""));
			}
			else if(args[i].startsWith("-factors")){
				String[] factors = args[i].replace("-factors=", "").split(",");
				if (factors == null || factors.length != 3) {
					correctUssage();
					return;
				}
				status_factor = Double.valueOf(factors[0]);
				syntactic_factor = Double.valueOf(factors[1]);
				semantic_factor = Double.valueOf(factors[2]);
			}
			else if(args[i].startsWith("-ref=")){
				String ref_name = args[i].replace("-ref=","");
				Tlsf ref_sol = TLSF_Utils.toBasicTLSF(new File(ref_name));
				referenceSolutions.add(ref_sol);
			}
			else if(args[i].startsWith("-out=")){
				outname = args[i].replace("-out=","");
			}
			else if(args[i].startsWith("-") || (!args[i].endsWith(".tlsf") && !args[i].endsWith(".spectra"))){
				correctUssage();
				return;
			}
			else {
				filename = args[i];
			}
		}
		if (filename == null || filename == "") {
			correctUssage();
			return;
		}
		//FileReader f = new FileReader(filename);
		Tlsf tlsf = TLSF_Utils.toBasicTLSF(new File(filename));
		SpecificationGeneticAlgorithm ga = new SpecificationGeneticAlgorithm();
		if (popSize > 0) Settings.GA_POPULATION_SIZE = popSize;
		if (maxNumOfInd > 0) Settings.GA_MAX_NUM_INDIVIDUALS = maxNumOfInd;
		if (crossoverRate > 0) Settings.GA_CROSSOVER_RATE = crossoverRate;
		if (mutationRate > 0) Settings.GA_MUTATION_RATE = mutationRate;
		if (gene_mutationRate > 0) Settings.GA_GENE_MUTATION_RATE = gene_mutationRate;
		if (gene_num_of_mutations > 0) Settings.GA_GENE_NUM_OF_MUTATIONS = gene_num_of_mutations;
		if (threshold > 0.0d) Settings.GA_THRESHOLD = threshold;
		if (guaranteePreferenceRate >= 0) Settings.GA_GUARANTEES_PREFERENCE_FACTOR = guaranteePreferenceRate;
		if (generations > 0) Settings.GA_GENERATIONS = generations;
		if (ga_timeout > 0) Settings.GA_EXECUTION_TIMEOUT = ga_timeout;
		if (real_timeout > 0) Settings.STRIX_TIMEOUT = real_timeout;
		if (sat_timeout > 0) Settings.SAT_TIMEOUT = sat_timeout;
		if (mc_timeout > 0) Settings.MC_TIMEOUT = mc_timeout;
		if (bound > 0) Settings.MC_BOUND = bound;
		if (precise) Settings.MC_EXHAUSTIVE = false;
		if (allowAssumptionsAddition) Settings.allowAssumptionAddition = true;
		if (allowGuaranteesRemoval) Settings.allowGuaranteeRemoval = true;
		if (onlyInputsInAssumptions) Settings.only_inputs_in_assumptions = true;
		if (no_check_realizability) Settings.check_REALIZABILITY = false;
		if (strong_SAT) Settings.check_STRONG_SAT = true;
		if (random_GA_selector) Settings.GA_RANDOM_SELECTOR = true;

		if (outname != null && outname != "") Settings.setStrixName(outname);

		if (randomGen)
			ga.runRandom(tlsf);
		else
			ga.run(tlsf,status_factor,syntactic_factor,semantic_factor);
		
		if (ga.solutions.isEmpty())
			return;

		//compute statistics
		double bestFitness = 0.0d;
		double sumFitness = 0.0d;

		String directoryName = filename.substring(0, filename.lastIndexOf('.'));
		if (outname != null && outname != "")
			directoryName = outname;
		File outfolder = new File(directoryName);
		if (!outfolder.exists())
			outfolder.mkdir();
		List<Tlsf> solutions = new LinkedList<>();
		for (int i = 0; i < ga.solutions.size(); i++) {
			SpecificationChromosome sol = ga.solutions.get(i);
			String sol_name = directoryName + "/spec"+i+".tlsf";
			File file = new File(sol_name);
	        FileWriter fw = new FileWriter(file.getAbsoluteFile());
	        BufferedWriter bw = new BufferedWriter(fw);
	        bw.write(TLSF_Utils.adaptTLSFSpec(sol.spec));
	        bw.write("\n//fitness: " + sol.fitness);
	        bw.close();

	        if (bestFitness < sol.fitness)
	        	bestFitness = sol.fitness;

			sumFitness += sol.fitness;

			solutions.add(sol.spec);
		}
		System.out.println("Num. of Solutions:" + solutions.size() + "\n");
		System.out.println(String.format("Best fitness: %.2f\n", bestFitness));
		System.out.println(String.format("AVG fitness: %.2f\n", (sumFitness / (double)ga.solutions.size())));
		double genuineBestFitness = 0.0d;
		double genuineAvgFitness = 0.0d;
		double moregeneralBestFitness = 0.0d;
		double moregeneralAvgFitness = 0.0d;
		double lessgeneralBestFitness = 0.0d;
		double lessgeneralAvgFitness = 0.0d;
		double genuinesSumFitness = 0.0d;
		double moregeneralSumFitness = 0.0d;
		double lessgeneralSumFitness = 0.0d;
		if (!referenceSolutions.isEmpty()) {
			System.out.println("Computing genuine statistics...");
			//check if some genuine solution has been found
			GenuineSolutionsAnalysis.calculateGenuineStatistics(referenceSolutions,solutions);

			for(Integer index : GenuineSolutionsAnalysis.genuineSolutionsFound) {
				SpecificationChromosome c = ga.solutions.get(index);
				genuinesSumFitness += c.fitness;
				if (c.fitness > genuineBestFitness)
					genuineBestFitness = c.fitness;
			}
			genuineAvgFitness = genuinesSumFitness / (double) GenuineSolutionsAnalysis.genuineSolutionsFound.size();
			for(Integer index : GenuineSolutionsAnalysis.moreGeneralSolutions) {
				SpecificationChromosome c = ga.solutions.get(index);
				moregeneralSumFitness += c.fitness;
				if (c.fitness > moregeneralBestFitness)
					moregeneralBestFitness = c.fitness;
			}
			moregeneralAvgFitness = moregeneralSumFitness / (double)GenuineSolutionsAnalysis.moreGeneralSolutions.size();
			for(Integer index : GenuineSolutionsAnalysis.lessGeneralSolutions) {
				SpecificationChromosome c = ga.solutions.get(index);
				lessgeneralSumFitness += c.fitness;
				if (c.fitness > lessgeneralBestFitness)
					lessgeneralBestFitness = c.fitness;
			}
			lessgeneralAvgFitness = lessgeneralSumFitness / (double)GenuineSolutionsAnalysis.lessGeneralSolutions.size();

			System.out.println("Genuine Solutions: " + GenuineSolutionsAnalysis.genuineSolutionsFound.size() + "\n");
			System.out.println("Genuine Solutions found: " + GenuineSolutionsAnalysis.genuineSolutionsFound.toString() + "\n");
			System.out.println(String.format("Best Genuine fitness: %.2f\n", genuineBestFitness));
			System.out.println(String.format("AVG Genuine fitness: %.2f\n", genuineAvgFitness));
			System.out.println("Weaker Solutions:" + GenuineSolutionsAnalysis.moreGeneralSolutions.size() + "\n");
			System.out.println("Weaker Solutions found:" + GenuineSolutionsAnalysis.moreGeneralSolutions.toString() + "\n");
			System.out.println(String.format("Best Weaker fitness: %.2f\n", moregeneralBestFitness));
			System.out.println(String.format("AVG Weaker fitness: %.2f\n", moregeneralAvgFitness));
			System.out.println("Stronger Solutions:" + GenuineSolutionsAnalysis.lessGeneralSolutions.size() + "\n");
			System.out.println("Stronger Solutions found:" + GenuineSolutionsAnalysis.lessGeneralSolutions.toString() + "\n");
			System.out.println(String.format("Best Stronger fitness: %.2f\n", lessgeneralBestFitness));
			System.out.println(String.format("AVG Stronger fitness: %.2f\n", lessgeneralAvgFitness));
			System.out.println(String.format("Genuine precision: %.2f \n",  ((double)GenuineSolutionsAnalysis.genuineSolutionsFound.size() / (double)referenceSolutions.size())));

		}

		//saving the time execution and configuration details
		File file = new File(directoryName + "/out.txt");
		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write("Num. of Solutions:   " + solutions.size() + "\n");
		bw.write(String.format("Best fitness: %.2f\n", bestFitness));
		bw.write(String.format("AVG fitness: %.2f\n", (sumFitness / (double)ga.solutions.size())));
		if (!referenceSolutions.isEmpty()) {
			bw.write("Genuine Solutions:   " + GenuineSolutionsAnalysis.genuineSolutionsFound.size() + "\n");
			bw.write("Genuine Solutions found:" + GenuineSolutionsAnalysis.genuineSolutionsFound.toString() + "\n");
			bw.write(String.format("Best Genuine fitness: %.2f\n", genuineBestFitness));
			bw.write(String.format("AVG Genuine fitness: %.2f\n", genuineAvgFitness));
			bw.write("Weaker Solutions:   " + GenuineSolutionsAnalysis.moreGeneralSolutions.size() + "\n");
			bw.write("Weaker Solutions found:" + GenuineSolutionsAnalysis.moreGeneralSolutions.toString() + "\n");
			bw.write(String.format("Best Weaker fitness: %.2f\n", moregeneralBestFitness));
			bw.write(String.format("AVG Weaker fitness: %.2f\n", moregeneralAvgFitness));
			bw.write("Stronger Solutions:    " + GenuineSolutionsAnalysis.lessGeneralSolutions.size() + "\n");
			bw.write("Stronger Solutions found:" + GenuineSolutionsAnalysis.lessGeneralSolutions.toString() + "\n");
			bw.write(String.format("Best Stronger fitness: %.2f\n", lessgeneralBestFitness));
			bw.write(String.format("AVG Stronger fitness: %.2f\n", lessgeneralAvgFitness));
			bw.write(String.format("Genuine precision: %.2f \n",  ((double)GenuineSolutionsAnalysis.genuineSolutionsFound.size() / (double)referenceSolutions.size())));
		}

		bw.write(ga.print_execution_time()+"\n");
		bw.write(ga.print_config()+"\n");
		bw.write("\n");
		bw.write(Settings.print_settings()+"\n");
		bw.close();

		System.exit(0);
	}
	
	private static void correctUssage(){
		System.out.println("Use ./unreal-repair.sh \n" +
								"\t[ -onlySAT | -strongSAT | -no-docker | -random | -GA_random_selector | \n" +
								"\t -Max=max_num_of_individuals | -Gen=num_of_generations | -sol=THRESHOLD | \n" +
								"\t -Pop=population_size | -COR=crossover_rate | -MR=mutation_rate | \n" +
								"\t -geneMR=gene_mutation_rate | -geneNUM=num_of_genes_to_mutate | \n" +
								"\t -removeGuarantees | -addAssumptions | -onlyInputsA | -GPR=guarantee_preference_rate | \n" +
								"\t -k=bound | -precise | -factors=STATUS_factor,MC_factor,SYN_factor | \n" +
								"\t -RTO=strix_timeout -GATO=GA_timeout | -SatTO=sat_timeout | -MCTO=model_counting_timeout | \n" +
								"\t -ref=TLSF_reference_solution]\n" +
								"\tinput-file.tlsf");
	}

}
