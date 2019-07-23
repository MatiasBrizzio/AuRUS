package geneticalgorithm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import geneticalgorithm.SpecificationChromosome.SPEC_STATUS;
import owl.ltl.parser.TlsfParser;
import owl.ltl.tlsf.Tlsf;
import tlsf.TLSF_Utils;

class SpecificationMergerTest {
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

	  private static final String TLSF1 = "INFO {\n"
			    + "  TITLE:       \"LTL -> DBA  -  Example 12\"\n"
			    + "  DESCRIPTION: \"One of the Acacia+ example files\"\n"
			    + "  SEMANTICS:   Moore\n"
			    + "  TARGET:      Mealy\n"
			    + "}\n"
			    + "// TEST COMMENT\n"
			    + "MAIN {\n"
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
			    + "// TEST COMMENT\n"
			    + "  GUARANTEE {\n"
			    + "// TEST COMMENT\n"
			    + "    (G grant0 -> F grant1) && (G !grant1 <-> F !request1)\n"
			    + "      && G F idle;\n"
			    + "  }\n"
			    + "// TEST COMMENT\n"
			    + " }";
	  
	@Test
	void testMerge() {
		List<Tlsf> mergeRes = SpecificationMerger.merge(TlsfParser.parse(TLSF1), TlsfParser.parse(TLSF2));
		System.out.println(TLSF_Utils.toTLSF(mergeRes.get(0)));
		assertTrue(!mergeRes.get(0).equals(TlsfParser.parse(TLSF1)));
		assertTrue(!mergeRes.get(0).equals(TlsfParser.parse(TLSF2)));
	}
	  
	@Test
	void testMergeLevel1() {
		List<Tlsf> mergeRes = SpecificationMerger.merge(TlsfParser.parse(TLSF1), TlsfParser.parse(TLSF2),SPEC_STATUS.UNKNOWN, SPEC_STATUS.UNKNOWN, 1);
		System.out.println(TLSF_Utils.toTLSF(mergeRes.get(0)));
		assertTrue(!mergeRes.get(0).equals(TlsfParser.parse(TLSF1)));
		assertTrue(!mergeRes.get(0).equals(TlsfParser.parse(TLSF2)));
	}

}
