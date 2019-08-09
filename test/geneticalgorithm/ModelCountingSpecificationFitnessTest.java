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
		SpecificationChromosome chromosome = new SpecificationChromosome(tlsf);
		Fitness<SpecificationChromosome, Double> fitnessFunc = new ModelCountingSpecificationFitness(tlsf);
		System.out.println(fitnessFunc.calculate(chromosome));
	}

	@Test
	void testMinePump2() throws IOException, InterruptedException {
		FileReader f = new FileReader("examples/minepump.tlsf");
		Tlsf spec1 = TlsfParser.parse(f);
		f = new FileReader("examples/minepump-2.tlsf");
		Tlsf spec2 = TlsfParser.parse(f);
		Fitness<SpecificationChromosome, Double> fitnessFunc = new ModelCountingSpecificationFitness(spec1);
		SpecificationChromosome chromosome = new SpecificationChromosome(spec2);
		System.out.println(fitnessFunc.calculate(chromosome));
	}
}
