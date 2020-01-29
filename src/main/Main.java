package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import geneticalgorithm.SpecificationChromosome;
import geneticalgorithm.SpecificationGeneticAlgorithm;
import owl.ltl.tlsf.Tlsf;
import tlsf.TLSF_Utils;

public class Main {

	public static void main(String[] args) throws IOException, InterruptedException {
		int popSize = 0;
		int maxNumOfInd = 0;
		int crossoverRate = 0;
		int mutationRate = 0;
		int guaranteePreferenceRate = 0;
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
		int sat_timeout = 0;
		int mc_timeout = 0;
		String filename = "";
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
			else if(args[i].startsWith("-SatTO=")){
				sat_timeout = Integer.parseInt(args[i].replace("-SatTO=", ""));
			}
			else if(args[i].startsWith("-MCTO=")){
				mc_timeout = Integer.parseInt(args[i].replace("-MCTO=", ""));
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
		if (guaranteePreferenceRate > 0) Settings.GA_GUARANTEES_PREFERENCE_FACTOR = guaranteePreferenceRate;
		if (generations > 0) Settings.GA_GENERATIONS = generations;
		if (ga_timeout > 0) Settings.GA_EXECUTION_TIMEOUT = ga_timeout;
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
		if (randomGen)
			ga.runRandom(tlsf);
		else
			ga.run(tlsf,status_factor,syntactic_factor,semantic_factor);
		
		if (ga.solutions.isEmpty())
			return;
		
		String directoryName = filename.substring(0, filename.lastIndexOf('.'));
		File outfolder = new File(directoryName);
		if (!outfolder.exists())
			outfolder.mkdir();
		for (int i = 0; i < ga.solutions.size(); i++) {
			SpecificationChromosome sol = ga.solutions.get(i);
			File file = new File(directoryName + "/spec"+i+".tlsf");
	        FileWriter fw = new FileWriter(file.getAbsoluteFile());
	        BufferedWriter bw = new BufferedWriter(fw);
	        bw.write(TLSF_Utils.adaptTLSFSpec(sol.spec));
	        bw.write("\n//fitness: " + sol.fitness);
	        bw.close(); 
		}

		//saving the time execution and configuration details

		File file = new File(directoryName + "/out.txt");
		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(ga.print_execution_time()+"\n");
		bw.write(ga.print_config()+"\n");
		bw.close();

	}
	
	private static void correctUssage(){
		System.out.println("Use ./unreal-repair.sh \n" +
								"\t[ -onlySAT | -strongSAT | -no-docker | -random | \n" +
								"\t -Max=max_num_of_individuals | -Gen=num_of_generations | \n" +
								"\t -Pop=population_size | -COR=crossover_rate | -MR=mutation_rate | \n" +
								"\t -removeGuarantees | -addAssumptions | -GA_random_selector | -GPR=guarantee_preference_rate | \n" +
								"\t -k=bound | -precise | -factors=STATUS_factor,MC_factor,SYN_factor | \n" +
								"\t -GATO=GA_timeout | -SatTO=sat_timeout | MCTO=model_counting_timeout] \n" +
								"\tinput-file.tlsf");
	}

}
