package tlsf;
/*
 * Copyright (C) 2016 - 2018  (See AUTHORS)
 *
 * This file is part of Owl.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */



import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static owl.automaton.output.HoaPrinter.HoaOption.SIMPLE_TRANSITION_LABELS;

import java.io.*;
import java.util.*;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import geneticalgorithm.SpecificationMerger;
import owl.automaton.Automaton;
import owl.automaton.acceptance.BuchiAcceptance;
import owl.automaton.acceptance.GeneralizedBuchiAcceptance;
import owl.automaton.acceptance.OmegaAcceptance;
import owl.automaton.edge.Edge;
import owl.automaton.edge.Edges;
import owl.automaton.output.HoaPrinter;
import owl.collections.Collections3;
import owl.collections.ValuationSet;
import owl.grammar.LTLParserBaseVisitor;
import owl.ltl.*;
import owl.ltl.Formula.LogicalOperator;
import owl.ltl.Formula.ModalOperator;
import owl.ltl.Formula.TemporalOperator;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.util.FormulaIsomorphism;
import owl.ltl.visitors.FormulaMutator;
import owl.ltl.visitors.FormulaStrengthening;
import owl.ltl.visitors.FormulaWeakening;
import owl.run.DefaultEnvironment;
import owl.run.modules.Transformers;
import owl.translations.LTL2DAFunction;
import owl.translations.LTL2DAModule;
import owl.translations.canonical.BreakpointState;
import owl.translations.ltl2nba.SymmetricNBAConstruction;
import solvers.StrixHelper;
import solvers.StrixHelper.RealizabilitySolverResult;
import tlsf.TLSF_Utils;
import owl.ltl.parser.*;
import owl.ltl.rewriter.*;

import owl.ltl.spectra.Spectra;

class TlsfParserTest {
	
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
	
  private static final String TLSF1 = "INFO {\n"
    + "  TITLE:       \"LTL -> DBA  -  Example 12\"\n"
    + "  DESCRIPTION: \"One of the Acacia+ example files\"\n"
    + "  SEMANTICS:   Moore\n"
    + "  TARGET:      Mealy\n"
    + "}\n"
    + "// TEST COMMENT\n"
    + "MAIN {\n"
    + "// TEST COMMENT\n"
    + "  INPUTS {\n"
    + "    p;\n"
    + "    q;\n"
    + "  }\n"
    + "// TEST COMMENT\n"
    + "  OUTPUTS {\n"
    + "    acc;\n"
    + "  }\n"
    + "// TEST COMMENT\n"
    + "  GUARANTEE {\n"
    + "// TEST COMMENT\n"
    + "    (G p -> F q) && (G !p <-> F !q)\n"
    + "      && G F acc;\n"
    + "  }\n"
    + "// TEST COMMENT\n"
    + " }";

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

  private static final String LILY = "INFO {\n"
    + "  TITLE:       \"Lily Demo V1\"\n"
    + "  DESCRIPTION: \"One of the Lily demo files\"\n"
    + "  SEMANTICS:   Moore\n"
    + "  TARGET:      Mealy\n"
    + "}\n"
    + '\n'
    + "MAIN {\n"
    + "  INPUTS {\n"
    + "    go;\n"
    + "    cancel;\n"
    + "    req;\n"
    + "  }\n"
    + "  OUTPUTS {\n"
    + "    grant;\n"
    + "  }\n"
    + "  ASSERT {\n"
    + "    ((req) -> (X ((grant) && (X ((grant) && (X (grant)))))));\n"
    + "    ((grant) -> (X (! (grant))));\n"
    + "    ((cancel) -> (X ((! (grant)) U (go))));\n"
    + "  }\n"
    + '}';

  private static final String LILY_LTL = "(G ((((X req) -> (X (grant && (X ((grant) && (X (gran"
    + "t))))))) && (grant -> (X (! (grant))))) && ((X (cancel)) -> (X ((! (grant)) U (X (go)))))))";

  private static final String UPPER_CASE = "INFO {\n"
    + "  TITLE:       \"Lily Demo V1\"\n"
    + "  DESCRIPTION: \"One of the Lily demo files\"\n"
    + "  SEMANTICS:   Moore\n"
    + "  TARGET:      Mealy\n"
    + "}\n"
    + '\n'
    + "MAIN {\n"
    + "  INPUTS {\n"
    + "    GO;\n"
    + "    CANCEL;\n"
    + "    REQ;\n"
    + "  }\n"
    + "  OUTPUTS {\n"
    + "    GRANT;\n"
    + "  }\n"
    + "  ASSERT {\n"
    + "    ((REQ) -> (X ((GRANT) && (X ((GRANT) && (X (GRANT)))))));\n"
    + "    ((GRANT) -> (X (! (GRANT))));\n"
    + "    ((CANCEL) -> (X ((! (GRANT)) U (GO))));\n"
    + "  }\n"
    + '}';

  private static final String UPPER_CASE_DIFFICULT = "INFO {\n"
    + "  TITLE:       \"Lily Demo V1\"\n"
    + "  DESCRIPTION: \"One of the Lily demo files\"\n"
    + "  SEMANTICS:   Moore\n"
    + "  TARGET:      Mealy\n"
    + "}\n"
    + '\n'
    + "MAIN {\n"
    + "  INPUTS {\n"
    + "    BARFOO;\n"
    + "    FOO;\n"
    + "    BAR;\n"
    + "  }\n"
    + "  OUTPUTS {\n"
    + "    FOOBAR;\n"
    + "  }\n"
    + "  ASSERT {\n"
    + "    ((BARFOO) -> (X ((FOO) && (X ((BAR) && (X (FOOBAR)))))));\n"
    + "  }\n"
    + '}';

  private static final String UPPER_CASE_FAULTY = "INFO {\n"
    + "  TITLE:       \"Lily Demo V1\"\n"
    + "  DESCRIPTION: \"One of the Lily demo files\"\n"
    + "  SEMANTICS:   Moore\n"
    + "  TARGET:      Mealy\n"
    + "}\n"
    + '\n'
    + "MAIN {\n"
    + "  INPUTS {\n"
    + "    Foo;\n"
    + "    fOO;\n"
    + "    foo;\n"
    + "  }\n"
    + '}';

  private static final String TLSF_COMPLETE = "INFO {\n"
    + "  TITLE:       \"TLSF - Test Specification\"\n"
    + "  DESCRIPTION: \"Test Test Test\"\n"
    + "  SEMANTICS:   Mealy\n"
    + "  TARGET:      Mealy\n"
    + "}\n"
    + '\n'
    + "MAIN {\n"
    + '\n'
    + "  INPUTS {\n"
    + "    a1;\n"
    + "    b2;\n"
    + "    c3;\n"
    + "    d4;\n"
    + "    e5;\n"
    + "  } \n"
    + '\n'
    + "  OUTPUTS {\n"
    + "    f6;\n"
    + "    g7;\n"
    + "    h8;\n"
    + "    i9;\n"
    + "  }\n"
    + '\n'
    + "  INITIALLY {\n"
    + "    a1;\n"
    + "  }\n"
    + '\n'
    + "  PRESET {\n"
    + "    b2;\n"
    + "  }\n"
    + '\n'
    + "  REQUIRE {\n"
    + "    c3;\n"
    + "  }\n"
    + '\n'
    + "  ASSERT {\n"
    + "    d4;\n"
    + "  }\n"
    + '\n'
    + "  INVARIANTS {\n"
    + "    e5;\n"
    + "  }\n"
    + '\n'
    + "  ASSUME {\n"
    + "    f6;\n"
    + "  }\n"
    + '\n'
    + "  ASSUMPTIONS {\n"
    + "    g7;\n"
    + "  }\n"
    + '\n'
    + "  GUARANTEE {\n"
    + "    h8;\n"
    + "  }\n"
    + '\n'
    + "  GUARANTEES {\n"
    + "    i9;\n"
    + "  }  \n"
    + '}';

  private static final String LTL_COMPLETE =
    "((a1) -> ((b2) && ((((G (c3)) && (f6)) && (g7)) -> (((G ((d4) && (e5))) && (h8)) && (i9)))))";

  @Test
  void testParse1() {
    Tlsf tlsf = TlsfParser.parse(TLSF1);

    assertEquals(Tlsf.Semantics.MOORE, tlsf.semantics());
    assertEquals(Tlsf.Semantics.MEALY, tlsf.target());

    assertEquals(2, tlsf.inputs().cardinality());
    assertEquals(1, tlsf.outputs().cardinality());

    assertEquals(0, tlsf.variables().indexOf("p"));
    assertEquals(1, tlsf.variables().indexOf("q"));
    assertEquals(2, tlsf.variables().indexOf("acc"));
  }

  @Test
  void testParse2() {
    Tlsf tlsf = TlsfParser.parse(TLSF2);

    assertEquals(Tlsf.Semantics.MOORE, tlsf.semantics());
    assertEquals(Tlsf.Semantics.MEALY, tlsf.target());

    assertEquals(3, tlsf.inputs().cardinality());
    assertEquals(2, tlsf.outputs().cardinality());
  }

  @Test
  void testParseLily() {
    Tlsf lily = TlsfParser.parse(LILY);
    LabelledFormula expectedFormula = LtlParser.parse(LILY_LTL, lily.toFormula().variables());
    assertEquals(expectedFormula.split(Set.of("go", "cancel", "req")), lily.toFormula());
  }

  @Test
  void testCompParseLily() {
    Tlsf lily = TlsfParser.parse(LILY);
    assertEquals(3, lily.assert_().size());
  }

  @Test
  void testParseUpperCase() {
    Tlsf lily = TlsfParser.parse(LILY);
    Tlsf upperCase = TlsfParser.parse(UPPER_CASE);
    assertEquals(lily.toFormula().formula(), upperCase.toFormula().formula());
  }

  @Test
  void testParseUpperCaseDifficult() {
    Tlsf upperCaseDifficult = TlsfParser.parse(UPPER_CASE_DIFFICULT);
    assertEquals(List.of("BARFOO", "FOO", "BAR", "FOOBAR"), upperCaseDifficult.variables());
  }

  @Test
  void testParseUpperCaseFaulty() {
    assertThrows(ParseCancellationException.class, () -> TlsfParser.parse(UPPER_CASE_FAULTY));
  }

  @Test
  void testTlsfComplete() {
    Tlsf tlsf = TlsfParser.parse(TLSF_COMPLETE);
    assertEquals(LtlParser.syntax(LTL_COMPLETE, tlsf.variables()), tlsf.toFormula().formula());
  }
  
  @Test
  void testTlsfFile() throws IOException {
	  String filename = "examples/round_robin_arbiter.tlsf";
//		System.out.println(TLSF_COMPLETE);

	  FileReader f = new FileReader(filename);
    
    
    Assertions.assertThrows(ParseCancellationException.class, () -> TlsfParser.parse(f));
  }
  
  @Test
  void testTlsfFile2() throws IOException, InterruptedException {
	  String filename = "examples/round_robin_arbiter.tlsf";
	  Tlsf tlsf = TLSF_Utils.toBasicTLSF(new File(filename));
	  //Tlsf tlsf2 = TlsfParser.parse(TLSF_Utils.toTLSF(tlsf));
	  System.out.println("TLSF TEST2 \n "+TLSF_Utils.toTLSF(tlsf)+" \n end TLSF TEST2");
	  
  }
  
  @Test
  void testTlsfMinepump() throws IOException, InterruptedException {
	  String filename = "examples/minepump.tlsf";
//	  FileReader f = new FileReader(filename);
//	    Tlsf tlsf = TlsfParser.parse(f);
	  Tlsf tlsf = TLSF_Utils.toBasicTLSF(new File(filename));
//	  Tlsf tlsf2 = TlsfParser.parse(TLSF_Utils.toTLSF(tlsf));
	  System.out.println(tlsf.toFormula());
	  
	  System.out.println(tlsf.assert_());
	  System.out.println(tlsf.guarantee());
	  System.out.println(tlsf.toAssertGuaranteeConjuncts());
	  System.out.println(tlsf.toFormula());
	  
  }
  
  @Test
  void testTlsfInitially() throws IOException, InterruptedException {
	  String filename = "examples/collector_v4_6.tlsf";
	  Tlsf tlsf = TLSF_Utils.toBasicTLSF(new File(filename));
	  Tlsf tlsf2 = TlsfParser.parse(TLSF_Utils.toTLSF(tlsf));
	  System.out.println(TLSF_Utils.toTLSF(tlsf2));
	  
  }
  
  @Test
  void testTlsfInitiallyBasic() throws IOException, InterruptedException {
	  String filename = "examples/collector_v4_6_basic.tlsf";
	  Tlsf tlsf2 = TlsfParser.parse(new FileReader(filename));
	  System.out.println(TLSF_Utils.toTLSF(tlsf2));
	  
  }
  
  @Test
  void testTlsfEmpty() throws IOException {
    Tlsf tlsf = TlsfParser.parse(TLSF_Utils.TLSF_EXAMPLE_SPEC);
    System.out.println(tlsf);
  }
  
  @Test
  void testTlsfFullString() throws IOException, InterruptedException {
	  Tlsf tlsf = TLSF_Utils.toBasicTLSF(TLSFFULL);
	  System.out.println("TLSF FULL TEST STRING \n "+TLSF_Utils.toTLSF(tlsf)+" \n END TLSF FULL TEST STRING");
  }
  
  @Test
  void testTlsfChangeSpec() throws IOException {
    Tlsf tlsf = TlsfParser.parse(TLSF_Utils.TLSF_EXAMPLE_SPEC);
    Tlsf tlsf1 = TLSF_Utils.change_assume(tlsf, LtlParser.syntax("G(a)"));
    Tlsf tlsf2 = TLSF_Utils.change_initially(tlsf, LtlParser.syntax("b & c"));
    List<Tlsf> res = SpecificationMerger.merge(tlsf1, tlsf2);
    System.out.println(res);
  }
	@Test
	void testIsomorphism() throws IOException {
		List<String> vars = List.of("a", "b", "c");
		Formula f0 = LtlParser.syntax("a & b | c", vars);
		Formula f1 = LtlParser.syntax("a | b | c", vars);
		System.out.println(f0);
		System.out.println(f0.height());
		System.out.println(f1);
		int[] iso = FormulaIsomorphism.compute(f0, f1);
		for (int i=0; i < iso.length; i++)
			System.out.print(iso[i]+", ");
	}

	@Test
	void testFormulas() throws IOException {
		List<String> vars = List.of("a", "b", "c");
		Formula f0 = LtlParser.syntax("F(!a & b & c)", vars);
		Formula f1 = LtlParser.syntax("a | b & c", vars);
		System.out.println(f0.subformulas(Formula.TemporalOperator.class));
		System.out.println(f1.subformulas(Formula.TemporalOperator.class));
		int diff = Formulas.compare(f0.subformulas(Formula.TemporalOperator.class), f1.subformulas(Formula.TemporalOperator.class));
		System.out.println(diff);
	}

	@Test
	void testFragments0() throws IOException {
		List<String> vars = List.of("a", "b", "c");
		Formula f0 = LtlParser.syntax("!a & b & c", vars);
		System.out.println(f0);
		System.out.println(SyntacticFragments.isAlmostAll(f0));
		System.out.println(SyntacticFragments.isFgSafety(f0));
		System.out.println(SyntacticFragments.isFSafety(f0));
		System.out.println(SyntacticFragments.isInfinitelyOften(f0));
		System.out.println(SyntacticFragments.isGCoSafety(f0));
		System.out.println(SyntacticFragments.isSafety(Set.of(f0)));
	}
	@Test
	void testFragments1() throws IOException {
		List<String> vars = List.of("a", "b", "c");
		Formula f0 = LtlParser.syntax("G(!a & b & c)", vars);
		System.out.println(f0);
		System.out.println(SyntacticFragments.isAlmostAll(f0));
		System.out.println(SyntacticFragments.isFgSafety(f0));
		System.out.println(SyntacticFragments.isFSafety(f0));
		System.out.println(SyntacticFragments.isInfinitelyOften(f0));
		System.out.println(SyntacticFragments.isGCoSafety(f0));
		System.out.println(SyntacticFragments.isSafety(Set.of(f0)));
	}

	@Test
	void testFragments1b() throws IOException {
		List<String> vars = List.of("a", "b", "c");
		Formula f0 = LtlParser.syntax("a U c", vars);
		System.out.println(f0);
		System.out.println(SyntacticFragments.isAlmostAll(f0));
		System.out.println(SyntacticFragments.isFgSafety(f0));
		System.out.println(SyntacticFragments.isFSafety(f0));
		System.out.println(SyntacticFragments.isInfinitelyOften(f0));
		System.out.println(SyntacticFragments.isGCoSafety(f0));
		System.out.println(SyntacticFragments.isSafety(Set.of(f0)));
		System.out.println(SyntacticFragments.isCoSafety(Set.of(f0)));
	}

	@Test
	void testFragments2() throws IOException {
		List<String> vars = List.of("a", "b", "c");
		Formula f0 = LtlParser.syntax("F(!a & b & c)", vars);
		System.out.println(f0);
		System.out.println(SyntacticFragments.isAlmostAll(f0));
		System.out.println(SyntacticFragments.isFgSafety(f0));
		System.out.println(SyntacticFragments.isFSafety(f0));
		System.out.println(SyntacticFragments.isInfinitelyOften(f0));
		System.out.println(SyntacticFragments.isGCoSafety(f0));
		System.out.println(SyntacticFragments.isSafety(Set.of(f0)));
	}
	@Test
	void testFragments3() throws IOException {
		List<String> vars = List.of("a", "b", "c");
		Formula f0 = LtlParser.syntax("F(!a | c)", vars);
		System.out.println(f0);
		System.out.println(SyntacticFragments.isAlmostAll(f0));
		System.out.println(SyntacticFragments.isFgSafety(f0));
		System.out.println(SyntacticFragments.isFSafety(f0));
		System.out.println(SyntacticFragments.isInfinitelyOften(f0));
		System.out.println(SyntacticFragments.isGCoSafety(f0));
		System.out.println(SyntacticFragments.isSafety(Set.of(f0)));
	}

	@Test
	void testFragments4() throws IOException {
		List<String> vars = List.of("a", "b", "c");
		Formula f0 = LtlParser.syntax("G (a -> F(c))", vars);
		System.out.println(f0);
		System.out.println(SyntacticFragments.isAlmostAll(f0));
		System.out.println(SyntacticFragments.isFgSafety(f0));
		System.out.println(SyntacticFragments.isFSafety(f0));
		System.out.println(SyntacticFragments.isInfinitelyOften(f0));
		System.out.println(SyntacticFragments.isGCoSafety(f0));
		System.out.println(SyntacticFragments.isSafety(Set.of(f0)));
	}
	@Test
	void testFragments5() throws IOException {
		List<String> vars = List.of("a", "b", "c");
		Formula f0 = LtlParser.syntax("G (a  U c)", vars);
		System.out.println(f0);
		System.out.println(SyntacticFragments.isAlmostAll(f0));
		System.out.println(SyntacticFragments.isFgSafety(f0));
		System.out.println(SyntacticFragments.isFSafety(f0));
		System.out.println(SyntacticFragments.isInfinitelyOften(f0));
		System.out.println(SyntacticFragments.isGCoSafety(f0));
		System.out.println(SyntacticFragments.isSafety(Set.of(f0)));
	}

	@Test
	void testFragments6() throws IOException {
		List<String> vars = List.of("a", "b", "c");
		Formula f0 = LtlParser.syntax("F G (a  -> c)", vars);
		System.out.println(f0);
		System.out.println(SyntacticFragments.isAlmostAll(f0));
		System.out.println(SyntacticFragments.isFgSafety(f0));
		System.out.println(SyntacticFragments.isFSafety(f0));
		System.out.println(SyntacticFragments.isInfinitelyOften(f0));
		System.out.println(SyntacticFragments.isGCoSafety(f0));
		System.out.println(SyntacticFragments.isSafety(Set.of(f0)));
	}

	@Test
	void testFragments7() throws IOException {
		List<String> vars = List.of("a", "b", "c");
		Formula f0 = LtlParser.syntax("F (a  U c)", vars);
		System.out.println(f0);
		System.out.println(SyntacticFragments.isAlmostAll(f0));
		System.out.println(SyntacticFragments.isFgSafety(f0));
		System.out.println(SyntacticFragments.isFSafety(f0));
		System.out.println(SyntacticFragments.isInfinitelyOften(f0));
		System.out.println(SyntacticFragments.isGCoSafety(f0));
		System.out.println(SyntacticFragments.isSafety(Set.of(f0)));
	}

	@Test
	void testFormulasCompare() throws IOException {
		List<String> vars = List.of("a", "b", "c");
		Formula f0 = LtlParser.syntax("G(a | b)", vars);
		Formula f1 = LtlParser.syntax("X(a & b)", vars);
		System.out.print(f0+ " ");
		System.out.print(f0.height()+ " ");
		System.out.println(Formula_Utils.formulaSize(f0));
		System.out.print(f1+ " ");
		System.out.print(f1.height()+ " ");
		System.out.println(Formula_Utils.formulaSize(f1));
		int diff = Formula_Utils.compare(f0,f1);
		System.out.println(diff);
		int diff2 = Collections3.compare(Formula_Utils.subformulas(f0), Formula_Utils.subformulas(f1));
		System.out.println(diff2);
		int diffc = Formulas.compare(Set.of(f0), Set.of(f1));
		System.out.println(diffc);
		int diffs = Formulas.compare(Formula_Utils.subformulas(f0), Formula_Utils.subformulas(f1));
		System.out.println(diffs);
		System.out.println(f0.compareTo(f1));
	}
  @Test
  void testAutomata() throws IOException {
	  List<String> vars = List.of("a", "b", "c");
	  LabelledFormula f0 =  LtlParser.parse("G(a -> (b))",vars);

	  System.out.println(f0);

//	  LTL2DAFunction translator = new LTL2DAFunction(DefaultEnvironment.standard(),
//			  true, EnumSet.of(
//			  LTL2DAFunction.Constructions.SAFETY,
//			  LTL2DAFunction.Constructions.CO_SAFETY,
//			  LTL2DAFunction.Constructions.BUCHI,
//			  LTL2DAFunction.Constructions.CO_BUCHI));
//	  LTL2DAFunction translator = new LTL2DAFunction(DefaultEnvironment.standard(),
//			  false, EnumSet.allOf(LTL2DAFunction.Constructions.class));
	  SymmetricNBAConstruction translator = (SymmetricNBAConstruction) SymmetricNBAConstruction.of(DefaultEnvironment.standard(), BuchiAcceptance.class);

	  Automaton<?, BuchiAcceptance> automaton = translator.apply(f0);
	  System.out.println(HoaPrinter.toString(automaton, EnumSet.of(SIMPLE_TRANSITION_LABELS)));
//	  System.out.println(automaton.initialStates());
//	  System.out.println(automaton.acceptance().acceptanceSets());
//	  System.out.println(automaton.acceptance().acceptingSet());
//	  System.out.println(automaton.acceptance().booleanExpression());


  }
  
  @Test
  void testSplitConjunction() throws IOException {
	  List<String> vars = List.of("a", "b", "c");
	  LabelledFormula f =  LtlParser.parse("a",vars);
	  System.out.println(f);
	  System.out.println(f.formula().children());
      System.out.println(NormalForms.toCnf(f.formula()));
  }
  
	@Test
	void testSpectra1() throws IOException, InterruptedException {
		 Spectra spectra = SpectraParser.parse(new FileReader("examples/icse2019/Simple/RG1.spectra"));	 
		 Tlsf spec = TLSF_Utils.fromSpectra(spectra);
		 System.out.println(spec);
	}
	
	@Test
	void testSpectra2() throws IOException, InterruptedException {
		 Spectra spectra = SpectraParser.parse(new FileReader("examples/icse2019/SYNTECH15/ColorSortLTLUnrealizable2_791_ColorSort_unrealizable.spectra"));	 
		 Tlsf spec = TLSF_Utils.fromSpectra(spectra);
		 System.out.println(spec);
	}

	@Test
	void testLTL2PL() throws IOException, InterruptedException {
		BufferedReader br = new BufferedReader(new FileReader("lib/ltl-model-counter/result/numofmodels-k3.pl"));
		String formula = br.readLine();
		System.out.println(formula);
		Formula f = LtlParser.syntax(formula);

		System.out.println(f);
		System.out.println(SyntacticSimplifier.INSTANCE.apply(NormalForms.toCnfFormula(f.nnf())));
	}

	@Test
	void testLTL2PL2() throws IOException {
		List<String> vars = List.of("a", "b", "c");
		LabelledFormula f =  LtlParser.parse(" G(a | !b)",vars);
		System.out.println(f);
		BitSet set = new BitSet();
		set.set(2);
		System.out.println(f.formula().temporalStep(set));
		System.out.println(NormalForms.toCnf(f.formula().unfold()));
	}
}