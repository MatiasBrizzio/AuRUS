package solvers;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import owl.ltl.parser.TlsfParser;
import owl.ltl.tlsf.Tlsf;
import tlsf.TLSF_Utils;

class LTLModelCounterTest {

	@Test
	void testMinePump() throws IOException, InterruptedException {
		 String filename = "examples/minepump.tlsf";
		 FileReader f = new FileReader(filename);
		 Tlsf tlsf = TlsfParser.parse(f);
		 List<String> vars = new LinkedList<String>();
		 for (int i = 0; i < tlsf.variables().size(); i++)
			 vars.add("p"+i);
		 System.out.println(vars);
		 System.out.println(tlsf.toFormula().formula());
		 BigInteger res = PreciseLTLModelCounter.count(tlsf.toFormula().formula().toString(), vars);
		 System.out.println(res);
	}

}
