package utils;

import org.junit.jupiter.api.Test;
import owl.ltl.Formula;
import owl.ltl.parser.LtlParser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * What numOfTemporalOperators actually measures. Reading the code it is a
 * nesting depth (max over children, +1 per non-X temporal node), not a count,
 * and it returns 0 at an XOperator without descending. These cases pin both.
 */
class NumOfTemporalOperatorsTest {

    private static final List<String> VARS = List.of("a", "b", "c", "high_water", "pump_on", "methane");

    private static int depth(String ltl) {
        Formula f = LtlParser.syntax(ltl, VARS);
        int d = FormulaUtils.temporalNestingDepth(f);
        System.out.printf("%-40s -> %s   (depth %d)%n", ltl, f, d);
        return d;
    }

    // ---- baseline: one temporal node over propositional content -------------

    @Test
    void propositionalContentDoesNotNest() {
        assertEquals(1, depth("G(a & b)"));
        assertEquals(1, depth("G(a | b)"));
        assertEquals(0, depth("a & b"));
        assertEquals(0, depth("a"));
    }

    // ---- the max-vs-sum question: siblings are not additive -----------------

    @Test
    void siblingTemporalOperatorsCountAsOne() {
        // If this were a count, both would be 3. As a depth, both are 2.
        assertEquals(2, depth("G(F a | F b)"));
        assertEquals(2, depth("G(F a & F b)"));
        // And a third sibling changes nothing.
        assertEquals(2, depth("G(F a | F b | F c)"));
    }

    @Test
    void genuineNestingDoesAccumulate() {
        assertEquals(2, depth("G F a"));
        assertEquals(2, depth("F G a"));
        assertEquals(1, depth("a U b"));
        assertEquals(2, depth("G(a U b)"));
    }

    @Test
    void owlNormalisesAtConstruction() {
        // Not the metric: the parser rewrites before it is ever called.
        assertEquals("FGp0", LtlParser.syntax("G F G a", VARS).toString());   // G F G φ = F G φ
        assertEquals(2, depth("G F G a"));
        // G distributes over conjunction, so the root stops being a G.
        assertEquals(2, depth("G(F a & F b)"));   // -> (G F a & G F b)
        assertEquals(1, depth("G(a & b)"));       // -> (G a & G b)
        assertEquals(1, depth("G(a | b)"));       // no distribuye sobre |
    }

    @Test
    void depthThreeNeedsNestingOwlWillNotCollapse() {
        // `a U (b U c)` is depth 2 and Owl has no identity to flatten it, so the
        // G on top should be the one case that actually reaches the cap.
        assertEquals(2, depth("a U (b U c)"));
        assertEquals(3, depth("G(a U (b U c))"));
    }
    // ---- the X wall ---------------------------------------------------------

    @Test
    void xIsNotCountedAsNesting() {
        // Deliberate: X does not add unbounded structure the way G/F/U do.
        assertEquals(0, depth("X a"));
        assertEquals(1, depth("G(a -> X b)"));   // minepump's original guarantee
    }

    @Test
    void xHidesEverythingBeneathIt() {
        // This is the part that does not match "bound the nesting": whatever
        // sits under an X is invisible, however deeply nested it is.
        assertEquals(1, depth("X F a"));          // nesting 1 under the X
        assertEquals(2, depth("X (G (F (G a)))")); // nesting 3 under the X
        assertEquals(2, depth("X (a U (b U c))"));
    }

    @Test
    void theGeneratedMinepumpRepairReadsOne() {
        // The repair we saw in the wild. Four temporal operators, two of them
        // under an X, and the guard is set at 2 — so it never fires.
        assertEquals(2, depth("G(!methane | X(F !high_water | F !pump_on))"));
    }

    // ---- how far the guard can be pushed without firing ---------------------

    @Test
    void anArbitrarilyLargeFormulaCanStayUnderTheCap() {
        // Everything below is depth <= 2, so no guard anywhere rejects it,
        // regardless of how many operators or how large the formula gets.
        assertEquals(3, depth("G(F a | F b | F c | X(F a) | X(G(F b)))"));
    }
}