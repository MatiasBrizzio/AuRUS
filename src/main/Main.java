package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import geneticalgorithm.Settings;
import geneticalgorithm.SpecificationChromosome;
import geneticalgorithm.SpecificationGeneticAlgorithm;
import owl.ltl.parser.TlsfParser;
import owl.ltl.tlsf.Tlsf;
import tlsf.TLSF_Utils;

public class Main {

	public static void main(String[] args) throws IOException, InterruptedException {
		int popSize = 0;
		int maxNumOfInd = 0;
		int crossoverRate = 0;
		int mutationRate = 0;
		int generations = 0;
		boolean randomGen = false;
		double status_factor = 0.0d;
		double syntactic_factor = 0.0d;
		double semantic_factor = 0.0d;
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
			else if(args[i].startsWith("-MR=")){
				mutationRate = Integer.parseInt(args[i].replace("-MR=", ""));
			}
			else if(args[i].startsWith("-no-docker")){
				Settings.USE_DOCKER = false;
			}
			else if(args[i].startsWith("-random")){
				randomGen = true;
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
		if (popSize > 0) ga.POPULATION_SIZE = popSize;
		if (maxNumOfInd > 0) ga.NUM_OF_INDIVIDUALS = maxNumOfInd;
		if (crossoverRate > 0) ga.CROSSOVER_RATE = crossoverRate;
		if (mutationRate > 0) ga.MUTATION_RATE = mutationRate;
		if (generations > 0) ga.GENERATIONS = generations;
		
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
	}
	
	private static void correctUssage(){
		System.out.println("Use ./unreal-repair.sh [-Pop=population_size | -Max=max_num_of_individuals |  -Gen=num_of_generations | -COR=crossover_rate | -MR=mutation_rate | -no-docker | -random] input-file.tlsf");
	}

}
