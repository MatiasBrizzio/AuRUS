package solvers;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.SpectraParser;
import owl.ltl.parser.TlsfParser;
import owl.ltl.tlsf.Tlsf;
import solvers.StrixHelper.RealizabilitySolverResult;
import tlsf.TLSF_Utils;
import owl.ltl.spectra.*;

class StrixHelperTest {
	 private static final String TLSFFULL = "INFO {\n" 
			  + "  TITLE:       \"Parameterized Load Balancer\"\n" 
			  + "  DESCRIPTION: \"Parameterized Load Balancer (generalized version of the Acacia+ benchmark)\"\n" 
			  + "  SEMANTICS:   Moore\n" 
			  + "  TARGET:      Mealy\n" 
			  + "}\n" 
			  + "\n" 
			  + "GLOBAL {\n" 
			  + "  PARAMETERS {\n" 
			  + "    n = 2;\n" 
			  + "  }\n" 
			  + "\n" 
			  + "  DEFINITIONS {\n" 
			  + "    // ensures mutual exclusion on an n-ary bus\n" 
			  + "    mutual_exclusion(bus) =\n" 
			  + "     mone(bus,0,(SIZEOF bus) - 1);\n" 
			  + "\n" 
			  + "    // ensures that none of the signals\n" 
			  + "    // bus[i] - bus[j] is HIGH\n" 
			  + "    none(bus,i,j) =\n" 
			  + "      &&[i <= t <= j]\n" 
			  + "        !bus[t];\n" 
			  + "\n" 
			  + "    // ensures that at most one of the signals\n" 
			  + "    // bus[i] - bus[j] is HIGH\n" 
			  + "    mone(bus,i,j) =\n" 
			  + "    i > j : false\n" 
			  + "    i == j : true\n" 
			  + "    i < j :\n" 
			  + "      // either no signal of the lower half is HIGH and at \n" 
			  + "      // most one signal of the upper half is HIGH\n" 
			  + "      (none(bus, i, m(i,j)) && mone(bus, m(i,j) + 1, j)) ||\n" 
			  + "      // or at most one signal of the lower half is HIGH\n" 
			  + "      // and no signal in of the upper half is HIGH\n" 
			  + "      (mone(bus, i, m(i,j)) && none(bus, m(i,j) + 1, j));\n" 
			  + "\n" 
			  + "    // returns the position between i and j\n" 
			  + "    m(i,j) = (i + j) / 2;\n" 
			  + "  }   \n" 
			  + "}\n" 
			  + "\n" 
			  + "MAIN {\n" 
			  + "\n" 
			  + "  INPUTS {\n" 
			  + "    idle;\n" 
			  + "    request[n];\n" 
			  + "  }\n" 
			  + "\n" 
			  + "  OUTPUTS {\n" 
			  + "    grant[n];\n" 
			  + "  }\n" 
			  + "\n" 
			  + "  ASSUMPTIONS {\n" 
			  + "    G F idle;\n" 
			  + "    G (idle && X &&[0 <= i < n] !grant[i] -> X idle);\n" 
			  + "    G (X !grant[0] || X ((!request[0] && !idle) U (!request[0] && idle)));\n" 
			  + "  }\n" 
			  + "\n" 
			  + "  INVARIANTS {\n" 
			  + "    X mutual_exclusion(grant);    \n" 
			  + "    &&[0 <= i < n] (X grant[i] -> request[i]);\n" 
			  + "    &&[0 < i < n] (request[0] -> grant[i]);\n" 
			  + "    !idle -> X &&[0 <= i < n] !grant[i];\n" 
			  + "  }\n"
			  + "\n" 
			  + "  GUARANTEES {\n" 
			  + "    &&[0 <= i < n] ! F G (request[i] && X !grant[i]);\n" 
			  + "  }\n" 
			  + "\n" 
			  + "}";
	 
	  private static final String TLSF2 = "INFO {\n" + 
	  		"  TITLE:       \"Parameterized Load Balancer, unrealizable variant 1\"\n" + 
	  		"  DESCRIPTION: \"Parameterized Load Balancer (generalized version of the Acacia+ benchmark)\"\n" + 
	  		"  SEMANTICS:   Mealy\n" + 
	  		"  TARGET:      Mealy\n" + 
	  		"}\n" + 
	  		"\n" + 
	  		"GLOBAL {\n" + 
	  		"  PARAMETERS {\n" + 
	  		"    n = 2;\n" + 
	  		"    u = 12;\n" + 
	  		"  }\n" + 
	  		"\n" + 
	  		"  DEFINITIONS {\n" + 
	  		"    // ensures mutual exclusion on an n-ary bus\n" + 
	  		"    mutual_exclusion(bus) =\n" + 
	  		"     mone(bus,0,(SIZEOF bus) - 1);\n" + 
	  		"\n" + 
	  		"    // ensures that none of the signals\n" + 
	  		"    // bus[i] - bus[j] is HIGH\n" + 
	  		"    none(bus,i,j) =\n" + 
	  		"      &&[i <= t <= j]\n" + 
	  		"        !bus[t];\n" + 
	  		"\n" + 
	  		"    // ensures that at most one of the signals\n" + 
	  		"    // bus[i] - bus[j] is HIGH\n" + 
	  		"    mone(bus,i,j) =\n" + 
	  		"    i > j : false\n" + 
	  		"    i == j : true\n" + 
	  		"    i < j :\n" + 
	  		"      // either no signal of the lower half is HIGH and at \n" + 
	  		"      // most one signal of the upper half is HIGH\n" + 
	  		"      (none(bus, i, m(i,j)) && mone(bus, m(i,j) + 1, j)) ||\n" + 
	  		"      // or at most one signal of the lower half is HIGH\n" + 
	  		"      // and no signal in of the upper half is HIGH\n" + 
	  		"      (mone(bus, i, m(i,j)) && none(bus, m(i,j) + 1, j));\n" + 
	  		"\n" + 
	  		"    // returns the position between i and j\n" + 
	  		"    m(i,j) = (i + j) / 2;\n" + 
	  		"  }   \n" + 
	  		"}\n" + 
	  		"\n" + 
	  		"MAIN {\n" + 
	  		"\n" + 
	  		"  INPUTS {\n" + 
	  		"    idle;\n" + 
	  		"    request[n];\n" + 
	  		"  }\n" + 
	  		"\n" + 
	  		"  OUTPUTS {\n" + 
	  		"    grant[n];\n" + 
	  		"  }\n" + 
	  		"\n" + 
	  		"  ASSUMPTIONS {\n" + 
	  		"    G F idle;\n" + 
	  		"    G (idle && X &&[0 <= i < n] !grant[i] -> X idle);\n" + 
	  		"    G (X !grant[0] || X ((!request[0] && !idle) U (!request[0] && idle)));\n" + 
	  		"  }\n" + 
	  		"\n" + 
	  		"  INVARIANTS {\n" + 
	  		"    X mutual_exclusion(grant);    \n" + 
	  		"    &&[0 <= i < n] (X grant[i] -> request[i]);\n" + 
	  		"    &&[0 < i < n] (request[0] -> grant[i]);\n" + 
	  		"    !idle -> X &&[0 <= i < n] !grant[i];    \n" + 
	  		"      \n" + 
	  		"    /* Making the benchmark unrealizable: ask for two grants at the same time, \n" + 
	  		"     * after u steps.\n" + 
	  		"     */\n" + 
	  		"    &&[0 <= i <n] ( &&[i < j < n] (request[i] && X request[j] -> \n" + 
	  		"      X[u] (grant[i] && grant[j])) ); \n" + 
	  		"  }\n" + 
	  		"\n" + 
	  		"  GUARANTEES {\n" + 
	  		"    &&[0 <= i < n] ! F G (request[i] && X !grant[i]);\n" + 
	  		"  }\n" + 
	  		"\n" + 
	  		"}\n" + 
	  		"//#!SYNTCOMP\n" + 
	  		"//STATUS : unrealizable\n" + 
	  		"//REF_SIZE : 0\n" + 
	  		"//#.";	
	  
	  
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
	void testCheckRealizability() throws IOException, InterruptedException {
		 assertTrue(StrixHelper.checkRealizability(TLSFFULL).equals(RealizabilitySolverResult.REALIZABLE));
	}
	 
	@Test
	void testCheckRealizability2() throws IOException, InterruptedException {
		assertTrue(StrixHelper.checkRealizability(TLSF2).equals(RealizabilitySolverResult.UNREALIZABLE));
	}
	
	@Test
	void testCheckRealizability3() throws IOException, InterruptedException {
		Tlsf tlsf = TLSF_Utils.toBasicTLSF(TLSF2);
		assertTrue(StrixHelper.checkRealizability(tlsf).equals(RealizabilitySolverResult.UNREALIZABLE));
	}
	
	@Test
	void testCheckRealizability4() throws IOException, InterruptedException {
		assertTrue(StrixHelper.checkRealizability(new File("examples/collector_v4_6_basic.tlsf")).equals(RealizabilitySolverResult.REALIZABLE));
	}
	
	 @Test
	void testCheckRealizability5() throws IOException, InterruptedException {
		assertTrue(StrixHelper.checkRealizability(TLSF3).equals(RealizabilitySolverResult.REALIZABLE));
	}
	
	@Test
	void testCheckRealizability6() throws IOException, InterruptedException {
		assertTrue(StrixHelper.checkRealizability(new File("examples/collector_v4_6_basic2.tlsf")).equals(RealizabilitySolverResult.ERROR));
	}
	 
	@Test
	void testCheckRealizability7() throws IOException, InterruptedException {
		assertTrue(StrixHelper.checkRealizability(new File("examples/minepump.tlsf")).equals(RealizabilitySolverResult.UNREALIZABLE));
	}

	@Test
	void testCheckRealizability8() throws IOException, InterruptedException {
		 Tlsf tlsf = TlsfParser.parse(TLSF3);
		assertTrue(StrixHelper.checkRealizability(tlsf).equals(RealizabilitySolverResult.REALIZABLE));
	}
	
	 @Test
	void testCheckRealizabilit95() throws IOException, InterruptedException {
		assertTrue(StrixHelper.checkRealizability(new File("examples/tictactoe.tlsf")).equals(RealizabilitySolverResult.REALIZABLE));
	}
	
	 @Test
	void testSpectra() throws IOException, InterruptedException {
		 Spectra spectra = SpectraParser.parse(new FileReader("examples/HumanoidLTL_458_Humanoid_fixed_unrealizable.spectra"));	 
		 assertTrue(StrixHelper.checkRealizability(spectra).equals(RealizabilitySolverResult.UNREALIZABLE));
	}
	 
	void testSpectra2() throws IOException, InterruptedException {
		 Spectra spectra = SpectraParser.parse(new FileReader("examples/PCarLTL_Unrealizable_V_2_unrealizable.0_888_PCar_fixed_unrealizable.spectra"));	 
		 assertTrue(StrixHelper.checkRealizability(spectra).equals(RealizabilitySolverResult.UNREALIZABLE));
	}
	 


}
