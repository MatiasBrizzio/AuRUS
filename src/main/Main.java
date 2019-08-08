package main;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import geneticalgorithm.SpecificationGeneticAlgorithm;
import owl.ltl.parser.TlsfParser;
import owl.ltl.tlsf.Tlsf;

public class Main {

	public static void main(String[] args) throws IOException, InterruptedException {
		int popSize = 0;
		int crossoverRate = 0;
		int mutationRate = 0;
		int generations = 0;
		String filename = "";
		for (int i = 0; i< args.length; i++ ){
			if(args[i].startsWith("-Gen=")){
				generations = Integer.parseInt(args[i].replace("-Gen=", ""));
			}
			else if(args[i].startsWith("-Pop=")){
				popSize = Integer.parseInt(args[i].replace("-Pop=", ""));
			}
			else if(args[i].startsWith("-COR=")){
				crossoverRate = Integer.parseInt(args[i].replace("-COR=", ""));
			}
			else if(args[i].startsWith("-MR=")){
				mutationRate = Integer.parseInt(args[i].replace("-MR=", ""));
			}
			else if(args[i].startsWith("-") || !args[i].endsWith(".tlsf")){
				correctUssage();
				return;
			}
			else {
				filename = args[i];
			}
		}
		
		FileReader f = new FileReader(filename);
		Tlsf tlsf = TlsfParser.parse(f);
		SpecificationGeneticAlgorithm ga = new SpecificationGeneticAlgorithm();
		if (popSize > 0) ga.POPULATION_SIZE = popSize;
		if (crossoverRate > 0) ga.CROSSOVER_RATE = crossoverRate;
		if (mutationRate > 0) ga.MUTATION_RATE = mutationRate;
		if (generations > 0) ga.GENERATIONS = generations;
		ga.run(tlsf);

	}
	
	private static void correctUssage(){
		System.out.println("Use ./unreal-repair.sh [-Pop=population_size | -Gen=num_of_generations | -COR=crossover_rate | -MR=mutation_rate] input-file.tlsf");
	}

}
