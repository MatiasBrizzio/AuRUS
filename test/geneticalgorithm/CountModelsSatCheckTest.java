package geneticalgorithm;

import main.Settings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import owl.ltl.BooleanConstant;
import owl.ltl.Conjunction;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.LtlParser;
import owl.ltl.parser.TlsfParser;
import owl.ltl.rewriter.SyntacticSimplifier;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.visitors.SolverSyntaxOperatorReplacer;
import solvers.LTLSolver;
import solvers.LTLSolver.SolverResult;
import utils.SolverUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * The satisfiability check that {@code countModels} runs before building the
 * automaton. Expected counts were measured on the code without the check, with
 * {@code k = 10}. The counting methods are private, so they are reached by
 * reflection rather than widening production visibility for the tests.
 */
class CountModelsSatCheckTest {

    private static final List<String> VARS = List.of("a", "b");

    /** Unsatisfiable, and counted as 13122 without the check. */
    private static final String MAIN_UNSAT = "G(a -> X b) & !G(a -> F b)";

    private boolean savedRealizability;
    private int savedBound;
    private int savedSatTimeout;

    private Tlsf s;
    private Tlsf sPrime;
    private AutomataBasedModelCountingSpecificationFitness fitness;

    private static Tlsf spec(String guarantee) {
        return TlsfParser.parse("INFO {\n  TITLE: \"t\"\n  DESCRIPTION: \"d\"\n"
                + "  SEMANTICS: Mealy\n  TARGET: Mealy\n}\n"
                + "MAIN {\n  INPUTS { a; }\n  OUTPUTS { b; }\n"
                + "  GUARANTEES { " + guarantee + "; }\n}\n");
    }

    private static LabelledFormula parse(String formula) {
        return LtlParser.parse(formula, VARS);
    }

    @BeforeEach
    void setUp() throws Exception {
        savedRealizability = Settings.check_REALIZABILITY;
        savedBound = Settings.MC_BOUND;
        savedSatTimeout = Settings.SAT_TIMEOUT;
        // Only the SAT checks and the counts matter here: no synthesis tool needed.
        Settings.check_REALIZABILITY = false;
        Settings.MC_BOUND = 10;
        s = spec("G (a -> X b)");
        sPrime = spec("G (a -> F b)");
        fitness = new AutomataBasedModelCountingSpecificationFitness(s);
    }

    @AfterEach
    void tearDown() {
        Settings.check_REALIZABILITY = savedRealizability;
        Settings.MC_BOUND = savedBound;
        Settings.SAT_TIMEOUT = savedSatTimeout;
    }

    private BigInteger count(String formula) throws Exception {
        Method m = AutomataBasedModelCountingSpecificationFitness.class
                .getDeclaredMethod("countModels", LabelledFormula.class);
        m.setAccessible(true);
        BigInteger models = (BigInteger) invoke(m, parse(formula));
        Formula simplified = parse(formula).formula().accept(new SyntacticSimplifier());
        System.out.printf("%nformula:     %s%nsimplified:  %s%naalta input: %s%nmodels(k=%d): %s%n",
                formula,
                LabelledFormula.of(simplified, VARS),
                SolverUtils.toSolverSyntax(simplified.accept(new SolverSyntaxOperatorReplacer())),
                Settings.MC_BOUND,
                models);
        return models;
    }

    private double ratio(String method) throws Exception {
        Method m = AutomataBasedModelCountingSpecificationFitness.class
                .getDeclaredMethod(method, Tlsf.class, Tlsf.class);
        m.setAccessible(true);
        double value = (double) invoke(m, s, sPrime);
        System.out.printf("%n%s%n  S:  %s%n  S': %s%n  value: %s%n", method, s.toFormula(), sPrime.toFormula(), value);
        return value;
    }

    private static SolverResult isSat(String input) throws Exception {
        SolverResult result = LTLSolver.isSAT(input);
        System.out.printf("%naalta input: %s%nresult:      %s%n", input, result);
        return result;
    }

    private Object invoke(Method m, Object... args) throws Exception {
        try {
            return m.invoke(fitness, args);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    // 1. Regression on the string aalta receives.

    @Test
    void solverSyntaxMakesAaltaAnswerUnsat() throws Exception {
        Formula f = parse(MAIN_UNSAT).formula();
        assertEquals(SolverResult.UNSAT, isSat(SolverUtils.toSolverSyntax(f)));
    }

    @Test
    void rawToStringIsWronglyReportedSat() throws Exception {
        // Documents the trap: toString() yields "(F(p0&G!p1)&G(!p0|Xp1))". aalta
        // reads "Xp1" as a variable name, so it answers sat about a different
        // formula, with no error. Always use toSolverSyntax.
        Formula f = parse(MAIN_UNSAT).formula();
        assertEquals(SolverResult.SAT, isSat(f.toString()));
    }

    @Test
    void formulaAndLabelledSyntaxAgree() throws Exception {
        LabelledFormula lf = parse(MAIN_UNSAT);
        assertEquals(isSat(SolverUtils.toSolverSyntax(lf.formula())),
                isSat(SolverUtils.toSolverSyntax(lf)));
    }

    // 2. The main case.

    @Test
    void mainUnsatFormulaCountsZero() throws Exception {
        assertEquals(BigInteger.ZERO, count(MAIN_UNSAT));
    }

    // 3. Other unsatisfiable formulas the simplifier does not reduce to false.
    // Every one counted above zero without the check (value in the comment).

    @Test
    void unsatFormulasSurvivingTheSimplifierCountZero() throws Exception {
        List<String> formulas = List.of(
                "G(a -> X X b) & !G(a -> F b)",         // 20412
                "G(b -> X a) & !G(b -> F a)",           // 13122
                "G(a -> X b) & F(a & G !b)",            // 13122
                "G(a -> X b) & X F(a & G !b)",          // 78732
                "G(a -> (b | X b)) & F(a & G !b)",      // 79647
                "G(a -> X X X b) & F(a & G !b)");       // 27864
        for (String formula : formulas) {
            Formula simplified = parse(formula).formula().accept(new SyntacticSimplifier());
            assertNotSame(BooleanConstant.FALSE, simplified, formula);
            assertEquals(BigInteger.ZERO, count(formula), formula);
        }
    }

    @Test
    void unsatFormulaWithWeakUntilCountsZero() throws Exception {
        // aalta has no W: without SolverSyntaxOperatorReplacer the check would
        // end in a syntax error and the spurious count (6561) would remain.
        String formula = "(b W a) & G(a -> X b) & F(a & G !b)";
        Formula simplified = parse(formula).formula().accept(new SyntacticSimplifier());
        assertNotSame(BooleanConstant.FALSE, simplified);
        assertEquals(BigInteger.ZERO, count(formula));
    }

    // 4. Satisfiable formulas keep their counts.

    @Test
    void satisfiableCountsAreUnchanged() throws Exception {
        assertEquals(BigInteger.valueOf(1024), count("G a"));
        assertEquals(BigInteger.valueOf(1023), count("G a & F b"));
        assertEquals(BigInteger.valueOf(1), count("!(G a & F b) & G a"));
    }

    @Test
    void approximationOfEquivalentFormulasIsNotFixedByTheCheck() throws Exception {
        // Known limitation: both formulas are equivalent, yet they count
        // differently. The automaton approximation produces this, and a
        // satisfiability check cannot remove it because both are satisfiable.
        assertEquals(BigInteger.valueOf(65610), count("G (a -> X b) & G (a -> F b)"));
        assertEquals(BigInteger.valueOf(78732), count("G (a -> X b)"));
    }

    // 5. lost and won.

    private static String solverInput(Formula f) {
        return SolverUtils.toSolverSyntax(f.accept(new SolverSyntaxOperatorReplacer()));
    }

    @Test
    void sPrimeIsAStrictWeakeningOfS() throws Exception {
        Formula fS = s.toFormula().formula();
        Formula fSPrime = sPrime.toFormula().formula();
        // S => S' holds iff S & !S' has no model.
        assertEquals(SolverResult.UNSAT, isSat(solverInput(Conjunction.of(fS, fSPrime.not()))),
                "S => S' must hold");
        // S' => S fails iff S' & !S has a model, so the weakening is strict.
        assertEquals(SolverResult.SAT, isSat(solverInput(Conjunction.of(fSPrime, fS.not()))),
                "S' => S must not hold");

        // Model counts for the same comparison (k = 10). Measured without the
        // check, S & !S' counted 13122; the other three are unchanged by it.
        assertEquals(BigInteger.valueOf(78732), count("G (a -> X b)"));
        assertEquals(BigInteger.valueOf(699051), count("G (a -> F b)"));
        assertEquals(BigInteger.ZERO, count("(G (a -> X b)) & !(G (a -> F b))"));
        assertEquals(BigInteger.valueOf(633441), count("(G (a -> F b)) & !(G (a -> X b))"));
    }

    @Test
    void lostIsExactlyOneWhenSImpliesSPrime() throws Exception {
        // S => S', so S & !S' is unsatisfiable. Without the check this was
        // 1 - round(13122 / 78732, 2) = 0.83.
        assertEquals(1.0d, ratio("compute_lost_models_porcentage"));
    }

    @Test
    void wonIsUnchangedBecauseItsFormulaIsSatisfiable() throws Exception {
        assertEquals(0.09d, ratio("compute_won_models_porcentage"), 1e-9);
    }

    // 6. A timeout never means zero models.

    @Test
    void satTimeoutFallsBackToTheAutomatonCount() throws Exception {
        Settings.SAT_TIMEOUT = 0;
        assertEquals(SolverResult.TIMEOUT,
                isSat(SolverUtils.toSolverSyntax(parse(MAIN_UNSAT).formula())));
        assertEquals(BigInteger.valueOf(13122), count(MAIN_UNSAT));
    }
}
