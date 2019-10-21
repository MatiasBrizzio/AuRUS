package geneticalgorithm;

import com.lagodiuk.ga.Fitness;
import org.junit.jupiter.api.Test;
import owl.ltl.parser.TlsfParser;
import owl.ltl.tlsf.Tlsf;

import java.io.FileReader;
import java.io.IOException;

class AutomabaBasedModelCountingSpecificationFitnessTest {

	@Test
	void testMinePump() throws IOException, InterruptedException {
		String filename = "examples/minepump.tlsf";
		FileReader f = new FileReader(filename);
		Tlsf tlsf = TlsfParser.parse(f);
//		Settings.USE_DOCKER = false;
		SpecificationChromosome chromosome = new SpecificationChromosome(tlsf);
		Fitness<SpecificationChromosome, Double> fitnessFunc = new AutomataBasedModelCountingSpecificationFitness(tlsf);
		System.out.printf("%.2f ",fitnessFunc.calculate(chromosome));
	}

	@Test
	void testMinePump2() throws IOException, InterruptedException {
		FileReader f = new FileReader("examples/minepump.tlsf");
		Tlsf spec1 = TlsfParser.parse(f);
		FileReader f2 = new FileReader("examples/minepump-2.tlsf");
		Tlsf spec2 = TlsfParser.parse(f2);
//		Settings.USE_DOCKER = false;
		AutomataBasedModelCountingSpecificationFitness fitnessFunc = new AutomataBasedModelCountingSpecificationFitness(spec1);
		fitnessFunc.allowAssumptionGuaranteeRemoval(true);
		SpecificationChromosome chromosome = new SpecificationChromosome(spec2);
		System.out.printf("%.2f ",fitnessFunc.calculate(chromosome));
	}
	
	@Test
	void testMinePump3() throws IOException, InterruptedException {
		FileReader f = new FileReader("examples/minepump.tlsf");
		Tlsf spec1 = TlsfParser.parse(f);
		f = new FileReader("examples/minepump-3.tlsf");
		Tlsf spec2 = TlsfParser.parse(f);
//		Settings.USE_DOCKER = false;
		AutomataBasedModelCountingSpecificationFitness fitnessFunc = new AutomataBasedModelCountingSpecificationFitness(spec1);
		fitnessFunc.allowAssumptionGuaranteeRemoval(true);
		SpecificationChromosome chromosome = new SpecificationChromosome(spec2);
		System.out.printf("%.2f ",fitnessFunc.calculate(chromosome));
		
	}

	@Test
	void testMinePump4() throws IOException, InterruptedException {
		FileReader f = new FileReader("examples/minepump.tlsf");
		Tlsf spec1 = TlsfParser.parse(f);
		f = new FileReader("examples/minepump-4.tlsf");
		Tlsf spec2 = TlsfParser.parse(f);
//		Settings.USE_DOCKER = false;
		AutomataBasedModelCountingSpecificationFitness fitnessFunc = new AutomataBasedModelCountingSpecificationFitness(spec1);
		fitnessFunc.allowAssumptionGuaranteeRemoval(true);
		SpecificationChromosome chromosome = new SpecificationChromosome(spec2);
		System.out.printf("%.2f ",fitnessFunc.calculate(chromosome));
		
	}

	@Test
	void testMinePumpBroken() throws IOException, InterruptedException {
		FileReader f = new FileReader("examples/minepump.tlsf");
		Tlsf spec1 = TlsfParser.parse(f);
		f = new FileReader("examples/minepump-broken.tlsf");
		Tlsf spec2 = TlsfParser.parse(f);
//		Settings.USE_DOCKER = false;
		Fitness fitnessFunc = new AutomataBasedModelCountingSpecificationFitness(spec1);
		SpecificationChromosome chromosome = new SpecificationChromosome(spec2);
		System.out.printf("%.2f ",fitnessFunc.calculate(chromosome));

	}
}
