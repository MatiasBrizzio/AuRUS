package geneticalgorithm;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileReader;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.lagodiuk.ga.Fitness;

import owl.ltl.parser.TlsfParser;
import owl.ltl.tlsf.Tlsf;

class ModelCountingSpecificationFitnessTest {

	@Test
	void testMinePump() throws IOException, InterruptedException {
		String filename = "examples/minepump.tlsf";
		FileReader f = new FileReader(filename);
		Tlsf tlsf = TlsfParser.parse(f);
		Settings.USE_DOCKER = false;
		SpecificationChromosome chromosome = new SpecificationChromosome(tlsf);
		Fitness<SpecificationChromosome, Double> fitnessFunc = new ModelCountingSpecificationFitness(tlsf);
		System.out.printf("%.2f ",fitnessFunc.calculate(chromosome));
	}

	@Test
	void testMinePump2() throws IOException, InterruptedException {
		FileReader f = new FileReader("examples/minepump.tlsf");
		Tlsf spec1 = TlsfParser.parse(f);
		FileReader f2 = new FileReader("examples/minepump-2.tlsf");
		Tlsf spec2 = TlsfParser.parse(f2);
		Settings.USE_DOCKER = false;
		Fitness<SpecificationChromosome, Double> fitnessFunc = new ModelCountingSpecificationFitness(spec1);
		SpecificationChromosome chromosome = new SpecificationChromosome(spec2);
		System.out.printf("%.2f ",fitnessFunc.calculate(chromosome));
	}
	
	@Test
	void testMinePump3() throws IOException, InterruptedException {
		FileReader f = new FileReader("examples/minepump.tlsf");
		Tlsf spec1 = TlsfParser.parse(f);
		f = new FileReader("examples/minepump-3.tlsf");
		Tlsf spec2 = TlsfParser.parse(f);
		Settings.USE_DOCKER = false;
		ModelCountingSpecificationFitness fitnessFunc = new ModelCountingSpecificationFitness(spec1);
		SpecificationChromosome chromosome = new SpecificationChromosome(spec2);
		System.out.printf("%.2f ",fitnessFunc.calculate(chromosome));
		
	}

	@Test
	void testMinePump4() throws IOException, InterruptedException {
		FileReader f = new FileReader("examples/minepump.tlsf");
		Tlsf spec1 = TlsfParser.parse(f);
		f = new FileReader("examples/minepump-4.tlsf");
		Tlsf spec2 = TlsfParser.parse(f);
		Settings.USE_DOCKER = false;
		ModelCountingSpecificationFitness fitnessFunc = new ModelCountingSpecificationFitness(spec1);
		SpecificationChromosome chromosome = new SpecificationChromosome(spec2);
		System.out.printf("%.2f ",fitnessFunc.calculate(chromosome));
		
	}
}
