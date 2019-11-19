package geneticalgorithm;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import main.Settings;
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
		Settings.GA_GENERATIONS = 10;
		Settings.GA_POPULATION_SIZE = 30;
		Settings.GA_MUTATION_RATE = 100;
		Settings.GA_MAX_NUM_INDIVIDUALS = 200;
		Settings.allowAssumptionGuaranteeRemoval = true;
		ga.run(tlsf);
	}

	@Test
	void testRunSimple() throws IOException, InterruptedException {
		//example taken from paper: Minimal Assumptions Refinement for GR(1) Specifications
		String filename = "examples/simple2.tlsf";
		FileReader f = new FileReader(filename);
		Tlsf tlsf = TlsfParser.parse(f);
//		Settings.USE_DOCKER = false;
		SpecificationGeneticAlgorithm ga = new SpecificationGeneticAlgorithm();
		Settings.GA_GENERATIONS = 10	;
		Settings.GA_POPULATION_SIZE = 30;
		Settings.GA_MUTATION_RATE = 100;
		Settings.GA_MAX_NUM_INDIVIDUALS = 200;
		Settings.GA_EXECUTION_TIMEOUT = 600;
		ga.run(tlsf);
	}

	@Test
	void testRuHenzingerPaper() throws IOException, InterruptedException {
		//example taken from paper: Minimal Assumptions Refinement for GR(1) Specifications
		String filename = "examples/HENZINGER/simple.tlsf";
		FileReader f = new FileReader(filename);
		Tlsf tlsf = TlsfParser.parse(f);
//		Settings.USE_DOCKER = false;
		SpecificationGeneticAlgorithm ga = new SpecificationGeneticAlgorithm();
		Settings.GA_GENERATIONS = 10	;
		Settings.GA_POPULATION_SIZE = 30;
		Settings.GA_MUTATION_RATE = 100;
		Settings.GA_MAX_NUM_INDIVIDUALS = 200;
		ga.run(tlsf);
	}

	@Test
	void testRunUnreal() throws IOException, InterruptedException {
		String filename = "examples/unreal-paper.tlsf";
		FileReader f = new FileReader(filename);
		Tlsf tlsf = TlsfParser.parse(f);
		SpecificationGeneticAlgorithm ga = new SpecificationGeneticAlgorithm();
		Settings.GA_GENERATIONS = 10;
		Settings.GA_POPULATION_SIZE = 30;
		Settings.GA_MUTATION_RATE = 100;
		//Settings.GA_MAX_NUM_INDIVIDUALS = 1000;
		ga.run(tlsf,-1,0,0);
	}

	@Test
	void testRunSafralessMaxReal() throws IOException, InterruptedException {
		String filename = "examples/safraless-maximal-real.tlsf";
		FileReader f = new FileReader(filename);
		Tlsf tlsf = TlsfParser.parse(f);
		SpecificationGeneticAlgorithm ga = new SpecificationGeneticAlgorithm();
		Settings.GA_GENERATIONS = 10;
		Settings.GA_POPULATION_SIZE = 30;
		Settings.GA_MUTATION_RATE = 100;
		Settings.GA_MAX_NUM_INDIVIDUALS = 100;
		ga.run(tlsf);
	}

	@Test
	void testRunStrongSAT() throws IOException, InterruptedException {
		String filename = "examples/strong-sat.tlsf";
		FileReader f = new FileReader(filename);
		Tlsf tlsf = TlsfParser.parse(f);
		SpecificationGeneticAlgorithm ga = new SpecificationGeneticAlgorithm();
		Settings.GA_GENERATIONS = 10;
		Settings.GA_POPULATION_SIZE = 30;
		Settings.GA_MUTATION_RATE = 100;
		Settings.GA_MAX_NUM_INDIVIDUALS = 100;
		ga.run(tlsf);
	}

	@Test
	void testRunLiftController() throws IOException, InterruptedException {
		String filename = "examples/lift-controller.tlsf";
		FileReader f = new FileReader(filename);
		Tlsf tlsf = TlsfParser.parse(f);
		SpecificationGeneticAlgorithm ga = new SpecificationGeneticAlgorithm();
		Settings.GA_GENERATIONS = 10;
		Settings.GA_POPULATION_SIZE = 30;
		Settings.GA_MUTATION_RATE = 100;
		//Settings.GA_MAX_NUM_INDIVIDUALS = 1000;
		ga.run(tlsf,0,0,0);
	}

	@Test
	void testRunRRCS() throws IOException, InterruptedException {
		String filename = "examples/rrcs.tlsf";
		FileReader f = new FileReader(filename);
		Tlsf tlsf = TlsfParser.parse(f);
		SpecificationGeneticAlgorithm ga = new SpecificationGeneticAlgorithm();
		Settings.GA_GENERATIONS = 1;
		Settings.GA_POPULATION_SIZE = 30;
		Settings.GA_MUTATION_RATE = 100;
		Settings.GA_MAX_NUM_INDIVIDUALS = 50;
		Settings.MC_BOUND = 10;
		Settings.GA_EXECUTION_TIMEOUT = 600;
		Settings.allowAssumptionGuaranteeRemoval = true;
		ga.run(tlsf);
	}
}
