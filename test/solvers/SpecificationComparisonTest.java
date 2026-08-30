package solvers;

import org.junit.jupiter.api.Test;
import owl.ltl.Conjunction;
import owl.ltl.Disjunction;
import owl.ltl.Formula;
import owl.ltl.parser.LtlParser;
import owl.ltl.visitors.SolverSyntaxOperatorReplacer;
import utils.SolverUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two ways of comparing specifications, and where they disagree.
 * <p>
 * Section 4.4.3 defines the comparison at the level of the formula: B is
 * weaker than A when A implies B. calculateGenuineStatistics checks something
 * else -- the two components separately, contravariantly on assumptions and
 * covariantly on guarantees. The componentwise check is sound: when it holds,
 * the formula-level implication holds too. It is not complete, and these cases
 * show the gap.
 */
class SpecificationComparisonTest {

    private static final List<String> VARS = List.of("p", "q", "r");

    private static Formula f(String ltl) {
        return LtlParser.syntax(ltl, VARS);
    }

    /** Does {@code left} entail {@code right}? Checked as UNSAT(left & !right). */
    private static boolean entails(Formula left, Formula right)
            throws Exception {
        SolverSyntaxOperatorReplacer visitor = new SolverSyntaxOperatorReplacer();
        LTLSolver.SolverResult sat = LTLSolver.isSAT(
                SolverUtils.toSolverSyntax(
                        Conjunction.of(left, right.not()).accept(visitor)));
        assertTrue(!sat.inconclusive(), "solver was inconclusive");
        return sat == LTLSolver.SolverResult.UNSAT;
    }

    /** The check the code performs: A |= A' and G' |= G. */
    private static boolean isMoreGeneralComponentwise(
            Formula a, Formula g, Formula a2, Formula g2) throws Exception {
        return entails(a, a2) && entails(g2, g);
    }

    /** The check the text describes: (A' -> G') |= (A -> G). */
    private static boolean isWeakerAsFormula(
            Formula a, Formula g, Formula a2, Formula g2) throws Exception {
        Formula contract  = Disjunction.of(a.not(), g);     // A -> G
        Formula contract2 = Disjunction.of(a2.not(), g2);   // A' -> G'
        return entails(contract2, contract);
    }

    // ---- where the two agree ------------------------------------------------

    @Test
    void componentwiseImpliesFormulaLevel() throws Exception {
        // A = p,  G = q      vs      A' = p & r,  G' = q & r
        // Assumptions: p |= p & r?  no. So componentwise says nothing here.
        // Take instead one that does hold:
        //   A = p & r,  G = q      vs      A' = p,  G' = q & r
        Formula a  = f("p & r"), g  = f("q");
        Formula a2 = f("p"),     g2 = f("q & r");

        assertTrue(isMoreGeneralComponentwise(a, g, a2, g2),
                "p & r |= p, and q & r |= q");
        assertTrue(isWeakerAsFormula(a, g, a2, g2),
                "soundness: componentwise ordering implies formula ordering");
    }

    // ---- where they disagree: the case C4.7 is about ------------------------

    @Test
    void movingAConstraintBetweenAssumptionAndGuarantee() throws Exception {
        // Same contract written two ways.
        //   Spec 1:  assume p,      guarantee q        ->  p -> q
        //   Spec 2:  assume nothing, guarantee p -> q  ->  p -> q
        Formula a  = f("p"),    g  = f("q");
        Formula a2 = f("true"), g2 = f("p -> q");

        // As formulas they are equivalent, in both directions.
        assertTrue(isWeakerAsFormula(a, g, a2, g2));
        assertTrue(isWeakerAsFormula(a2, g2, a, g));

        // Componentwise, neither direction holds.
        assertEquals(false, isMoreGeneralComponentwise(a, g, a2, g2),
                "q is not entailed by p -> q");
        assertEquals(false, isMoreGeneralComponentwise(a2, g2, a, g),
                "true is not entailed by p");

        // So calculateGenuineStatistics records these as unrelated, and the
        // repair is counted as unique, when it is the same specification.
    }

    @Test
    void strengtheningBothComponents() throws Exception {
        // A repair that assumes more and guarantees more. Componentwise the
        // two conditions point in opposite directions, so neither holds --
        // reported as incomparable regardless of how the contracts relate.
        Formula a  = f("p"),     g  = f("q");
        Formula a2 = f("p & r"), g2 = f("q & r");

        assertEquals(false, isMoreGeneralComponentwise(a, g, a2, g2));
        assertEquals(false, isMoreGeneralComponentwise(a2, g2, a, g));

        // Print what the formula-level check says, whichever way it goes.
        System.out.println("formula-level, 1 weaker than 2: "
                + isWeakerAsFormula(a, g, a2, g2));
        System.out.println("formula-level, 2 weaker than 1: "
                + isWeakerAsFormula(a2, g2, a, g));
    }

    // ---- sanity: identical specifications -----------------------------------

    @Test
    void identicalSpecificationsAreComparableBothWays() throws Exception {
        Formula a = f("p"), g = f("q");

        assertTrue(isMoreGeneralComponentwise(a, g, a, g));
        assertTrue(isWeakerAsFormula(a, g, a, g));
    }
}