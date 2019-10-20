package geneticalgorithm;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.lagodiuk.ga.Population;

import owl.ltl.parser.TlsfParser;
import owl.ltl.tlsf.Tlsf;
import tlsf.TLSF_Utils;

class SpecificationGeneticAlgorithmTest {

	@Test
	void testRunMinePump() throws IOException, InterruptedException {
		String filename = "examples/minepump.tlsf";
		FileReader f = new FileReader(filename);
		Tlsf tlsf = TlsfParser.parse(f);
//		Settings.USE_DOCKER = false;
		SpecificationGeneticAlgorithm ga = new SpecificationGeneticAlgorithm();
		ga.GENERATIONS = 10;
		ga.POPULATION_SIZE = 30;
		ga.MUTATION_RATE = 100;
//		ga.NUM_OF_INDIVIDUALS = 100;
		ga.run(tlsf);
	}

	@Test
	void testRunSimple() throws IOException, InterruptedException {
		//example taken from paper: Minimal Assumptions Refinement for GR(1) Specifications
		String filename = "examples/simple.tlsf";
		FileReader f = new FileReader(filename);
		Tlsf tlsf = TlsfParser.parse(f);
//		Settings.USE_DOCKER = false;
		SpecificationGeneticAlgorithm ga = new SpecificationGeneticAlgorithm();
		ga.GENERATIONS = 20	;
		ga.POPULATION_SIZE = 100;
		ga.MUTATION_RATE = 100;
		//ga.NUM_OF_INDIVIDUALS = 1000;
		ga.run(tlsf,0,0,0,false);
	}

	@Test
	void testRunUnreal() throws IOException, InterruptedException {
		String filename = "examples/unreal-paper.tlsf";
		File f = new File(filename);
		Tlsf tlsf = TLSF_Utils.toBasicTLSF(f);
		SpecificationGeneticAlgorithm ga = new SpecificationGeneticAlgorithm();
		ga.GENERATIONS = 10;
		ga.POPULATION_SIZE = 30;
		ga.MUTATION_RATE = 100;
		//ga.NUM_OF_INDIVIDUALS = 1000;
		ga.run(tlsf,0,0,0,false);
	}

	@Test
	void testRunLiftController() throws IOException, InterruptedException {
		String filename = "examples/lift-controller.tlsf";
		File f = new File(filename);
		Tlsf tlsf = TLSF_Utils.toBasicTLSF(f);
		SpecificationGeneticAlgorithm ga = new SpecificationGeneticAlgorithm();
		ga.GENERATIONS = 10;
		ga.POPULATION_SIZE = 30;
		ga.MUTATION_RATE = 100;
		//ga.NUM_OF_INDIVIDUALS = 1000;
		ga.run(tlsf,0,0,0,false);
	}

	@Test
	void testRunRRCS() throws IOException, InterruptedException {
		String filename = "examples/rrcs.tlsf";
		File f = new File(filename);
		Tlsf tlsf = TLSF_Utils.toBasicTLSF(f);
		SpecificationGeneticAlgorithm ga = new SpecificationGeneticAlgorithm();
		ga.GENERATIONS = 10;
		ga.POPULATION_SIZE = 30;
		ga.MUTATION_RATE = 100;
		//ga.NUM_OF_INDIVIDUALS = 1000;
		ga.run(tlsf,0,0,0,false);
	}
}
