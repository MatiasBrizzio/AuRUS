package geneticalgorithm;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.lagodiuk.ga.Fitness;

import owl.ltl.parser.TlsfParser;
import owl.ltl.tlsf.Tlsf;
import tlsf.TLSF_Utils;

class SpecificationFitnessTest {
	
	  private static final String TLSF2 = "INFO {\n"
			    + "  TITLE:       \"Load Balancing - Environment - 2 Clients\"\n"
			    + "  DESCRIPTION: \"One of the Acacia+ Example files\"\n"
			    + "  SEMANTICS:   Moore\n"
			    + "  TARGET:      Mealy\n"
			    + "}\n"
			    + '\n'
			    + "MAIN {\n"
			    + '\n'
			    + "  INPUTS {\n"
			    + "    idle;\n"
			    + "    request0;\n"
			    + "    request1;\n"
			    + "  }\n"
			    + '\n'
			    + "  OUTPUTS {\n"
			    + "    grant0;\n"
			    + "    grant1;\n"
			    + "  }\n"
			    + '\n'
			    + "  ASSUMPTIONS {\n"
			    + "    G F idle;\n"
			    + "    G (!(idle && !grant0 && !grant1) || X idle);    \n"
			    + "    G (!grant0 || X ((!request0 && !idle) U (!request0 && idle)));\n"
			    + "  }\n"
			    + '\n'
			    + "  INVARIANTS {\n"
			    + "    !request0 || !grant1;\n"
			    + "    !grant0 || !grant1;\n"
			    + "    !grant1 || !grant0;\n"
			    + "    !grant0 || request0;\n"
			    + "    !grant1 || request1;\n"
			    + "    (!grant0 && !grant1) || idle;\n"
			    + "  }\n"
			    + '\n'
			    + "  GUARANTEES {\n"
			    + "    ! F G (request0 && !grant0);\n"
			    + "    ! F G (request1 && !grant1);\n"
			    + "  }\n"
			    + '\n'
			    + "}\n";
	  
		private static final String TLSF3 = "INFO {\n" + 
				"  TITLE:       \"Parameterized Collector\"\n" + 
				"  DESCRIPTION: \"Signals whether all input clients have delivered a token\"\n" + 
				"  SEMANTICS:   Mealy\n" + 
				"  TARGET:      Mealy\n" + 
				"}\n" + 
				"\n" + 
				"MAIN {\n" + 
				"  INPUTS {\n" + 
				"    finished_0;\n" + 
				"    finished_1;\n" + 
				"    finished_2;\n" + 
				"    finished_3;\n" + 
				"    finished_4;\n" + 
				"    finished_5;\n" + 
				"  }\n" + 
				"  OUTPUTS {\n" + 
				"    allFinished;\n" + 
				"  }\n" + 
				"  INITIALLY {\n" + 
				"    ((! (allFinished)) W (finished_0));\n" + 
				"    ((! (allFinished)) W (finished_1));\n" + 
				"    ((! (allFinished)) W (finished_2));\n" + 
				"    ((! (allFinished)) W (finished_3));\n" + 
				"    ((! (allFinished)) W (finished_4));\n" + 
				"    ((! (allFinished)) W (finished_5));\n" + 
				"  }\n" + 
				"  ASSERT {\n" + 
				"    (((((((G (F (finished_0))) && (G (F (finished_1)))) && (G (F (finished_2)))) && (G (F (finished_3)))) && (G (F (finished_4)))) && (G (F (finished_5)))) -> (G (F (allFinished))));\n" + 
				"    ((allFinished) -> (X ((! (allFinished)) W (finished_0))));\n" + 
				"    ((allFinished) -> (X ((! (allFinished)) W (finished_1))));\n" + 
				"    ((allFinished) -> (X ((! (allFinished)) W (finished_2))));\n" + 
				"    ((allFinished) -> (X ((! (allFinished)) W (finished_3))));\n" + 
				"    ((allFinished) -> (X ((! (allFinished)) W (finished_4))));\n" + 
				"    ((allFinished) -> (X ((! (allFinished)) W (finished_5))));\n" + 
				"  }\n" + 
				"}";
	  
	@Test
	void test() throws IOException, InterruptedException {
		Tlsf tlsf = TLSF_Utils.toBasicTLSF(TLSF2);
		SpecificationChromosome chromosome = new SpecificationChromosome(tlsf);
		Fitness<SpecificationChromosome, Double> fitnessFunc = new SpecificationFitness();  
		System.out.println(fitnessFunc.calculate(chromosome));
	}
	
	@Test
	void test2() throws IOException, InterruptedException {
		Tlsf tlsf = TLSF_Utils.toBasicTLSF(TLSF3);
		SpecificationChromosome chromosome = new SpecificationChromosome(tlsf);
		Fitness<SpecificationChromosome, Double> fitnessFunc = new SpecificationFitness();  
		System.out.println(fitnessFunc.calculate(chromosome));
	}
	
	@Test
	void test3() throws IOException, InterruptedException {
		String filename = "examples/round_robin_arbiter.tlsf";
		Tlsf tlsf = TLSF_Utils.toBasicTLSF(new File(filename));
		SpecificationChromosome chromosome = new SpecificationChromosome(tlsf);
		Fitness<SpecificationChromosome, Double> fitnessFunc = new SpecificationFitness();  
		System.out.println(fitnessFunc.calculate(chromosome));
	}

}
