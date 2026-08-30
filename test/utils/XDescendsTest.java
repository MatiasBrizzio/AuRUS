package utils;

import org.junit.jupiter.api.Test;
import owl.ltl.Formula;
import owl.ltl.parser.LtlParser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The X branch: an XOperator adds no nesting level, but must not hide the
 * nesting beneath it either. Before the fix the walk returned 0 at an X
 * without descending, so any depth under an X was invisible.
 */
class XDescendsTest {

    private static final List<String> VARS =
            List.of("a", "b", "c", "high_water", "pump_on", "methane");

    private static int depth(String ltl) {
        Formula f = LtlParser.syntax(ltl, VARS);
        int d = FormulaUtils.temporalNestingDepth(f);
        System.out.printf("  %-42s -> %s   (depth %d)%n", ltl, f, d);
        return d;
    }

    @Test
    void xStillAddsNoLevel() {
        // Unchanged by the fix: X on a literal is still 0.
        assertEquals(0, depth("X a"));
        assertEquals(1, depth("G(a -> X b)"));
    }

    @Test
    void xNoLongerHidesWhatIsBeneathIt() {
        // These all read 0 before the fix.
        assertEquals(1, depth("X F a"));
        assertEquals(2, depth("X (G (F (G a)))"));   // Owl normalises to X F G a
        assertEquals(2, depth("X (a U (b U c))"));
    }

    @Test
    void theMinepumpRepairNowReadsTwo() {
        // The repair found in the wild: read 1 before the fix, 2 after.
        assertEquals(2, depth("G(!methane | X(F !high_water | F !pump_on))"));
    }

    @Test
    void aFormulaThatUsedToSlipUnderTheCapNowCrossesIt() {
        // The point of the fix. Read 2 before (passed a cap of 2), reads 3 now.
        assertEquals(3, depth("G(F a | F b | F c | X(F a) | X(G(F b)))"));
    }

    @Test
    void formulaeWithoutAnXAreUnaffected() {
        // Regression: the fix must not move anything that has no X in it.
        assertEquals(0, depth("a"));
        assertEquals(1, depth("G(a | b)"));
        assertEquals(2, depth("G F a"));
        assertEquals(2, depth("G(F a | F b)"));
        assertEquals(3, depth("G(a U (b U c))"));
    }

    @Test
    void xDoesNotAddALevelButDoesNotHideEither() {
        // X shifts by one step rather than introducing a loop, so it contributes
        // no level of its own -- but the walk descends past it and counts what
        // lies below.
        assertEquals(1, depth("X F a"));
        assertEquals(2, depth("X (G (F (G a)))"));   // Owl builds it as X F G a
        assertEquals(2, depth("X (a U (b U c))"));
    }


    @Test
    void theGeneratedMinepumpRepairReadsTwo() {
        // Four temporal operators, two of them under an X. The X adds no level,
        // but the two F's below it are counted, so the guard at 2 does fire.
        assertEquals(2, depth("G(!methane | X(F !high_water | F !pump_on))"));
    }

    @Test
    void nestingUnderAnXStillCounts() {
        // The X does not add a level, but G(F b) beneath it does: two, plus the
        // outer G, gives three. A formula can be large and shallow, but it cannot
        // hide depth behind an X.
        assertEquals(3, depth("G(F a | F b | F c | X(F a) | X(G(F b)))"));
    }

}