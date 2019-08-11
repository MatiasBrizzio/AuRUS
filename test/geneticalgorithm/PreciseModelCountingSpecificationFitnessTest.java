package geneticalgorithm;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileReader;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.lagodiuk.ga.Fitness;

import owl.ltl.parser.TlsfParser;
import owl.ltl.tlsf.Tlsf;

class PreciseModelCountingSpecificationFitnessTest {

	@Test
	void testMinePump() throws IOException, InterruptedException {
		String filename = "examples/minepump.tlsf";
		FileReader f = new FileReader(filename);
		Tlsf tlsf = TlsfParser.parse(f);
		SpecificationChromosome chromosome = new SpecificationChromosome(tlsf);
		Fitness<SpecificationChromosome, Double> fitnessFunc = new PreciseModelCountingSpecificationFitness(tlsf);
		System.out.println(fitnessFunc.calculate(chromosome));
	}

	@Test
	void testMinePump2() throws IOException, InterruptedException {
		FileReader f = new FileReader("examples/minepump.tlsf");
		Tlsf spec1 = TlsfParser.parse(f);
		f = new FileReader("examples/minepump-2.tlsf");
		Tlsf spec2 = TlsfParser.parse(f);
		PreciseModelCountingSpecificationFitness fitnessFunc = new PreciseModelCountingSpecificationFitness(spec1);
		System.out.println(fitnessFunc.compute_lost_models_porcentage(spec1, spec2));
		System.out.println(fitnessFunc.compute_won_models_porcentage(spec1, spec2));
		System.out.println(fitnessFunc.compute_syntactic_distance(spec1, spec2));
	}
	
	@Test
	void testMinePump3() throws IOException, InterruptedException {
		FileReader f = new FileReader("examples/minepump.tlsf");
		Tlsf spec1 = TlsfParser.parse(f);
		f = new FileReader("examples/minepump-3.tlsf");
		Tlsf spec2 = TlsfParser.parse(f);
		PreciseModelCountingSpecificationFitness fitnessFunc = new PreciseModelCountingSpecificationFitness(spec1);
		System.out.println(fitnessFunc.compute_lost_models_porcentage(spec1, spec2));
		System.out.println(fitnessFunc.compute_won_models_porcentage(spec1, spec2));
		
	}
}
