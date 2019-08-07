package geneticalgorithm;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileReader;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.lagodiuk.ga.Population;

import owl.ltl.parser.TlsfParser;
import owl.ltl.tlsf.Tlsf;

class SpecificationGeneticAlgorithmTest {

	@Test
	void testRun() throws IOException {
		String filename = "examples/minepump.tlsf";
		FileReader f = new FileReader(filename);
		Tlsf tlsf = TlsfParser.parse(f);
		SpecificationGeneticAlgorithm ga = new SpecificationGeneticAlgorithm();
		ga.run(tlsf);
	}

}
