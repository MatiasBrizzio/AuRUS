package solvers;

import owl.ltl.*;
import owl.ltl.parser.LtlParser;

import org.junit.jupiter.api.Test;
import owl.ltl.visitors.FormulaStrengthening;
import owl.ltl.visitors.FormulaWeakening;
import owl.ltl.visitors.SolverSyntaxOperatorReplacer;
import utils.SolverUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
// Adjust this import to wherever SolverResult actually lives in your project:
import static solvers.LTLSolver.SolverResult.SAT;
import static solvers.LTLSolver.SolverResult.UNSAT;

/**
 * Verifies that every rule listed in Table 4.1 (as implemented by
 * {@link FormulaWeakening} / {@link FormulaStrengthening}) is a genuine
 * entailment in the claimed direction.
 *
 * <p>Semantics used:
 * <ul>
 *   <li>A <b>weakening</b> {@code φ ⤳ φ_w} must satisfy {@code φ ⊨ φ_w}
 *       (every model of the original survives).</li>
 *   <li>A <b>strengthening</b> {@code φ ⤳ φ_s} must satisfy {@code φ_s ⊨ φ}
 *       (no new model is introduced).</li>
 * </ul>
 *
 * <p>Entailment {@code a ⊨ b} is checked as validity of {@code a → b},
 * i.e. UNSAT of {@code a ∧ ¬b}. This is why {@code isSAT} of the raw
 * implication is <i>not</i> enough — that is always satisfiable and proves
 * nothing.</p>
 *
 * <p>The branches marked with † in the table (which combine the formula with
 * a drawn, not-logically-fresh literal) are asserted to be <b>NOT</b> sound
 * entailments — the tests document exactly why they are heuristic.</p>
 */
public class MutationRulesEntailmentTest {

    /** Alphabet: a,b are φ1,φ2; c is the "fresh" drawn literal; p,q,r,s spare. */
    private static final List<String> VARS = List.of("a", "b", "c", "p", "q", "r", "s");

    private static final Formula TRUE = BooleanConstant.TRUE;
    private static final Formula FALSE = BooleanConstant.FALSE;

    // ---- helpers -----------------------------------------------------------

    private static Formula f(String s) {
        SolverSyntaxOperatorReplacer replacer = new SolverSyntaxOperatorReplacer();
        return LtlParser.parse(s, VARS).formula().accept(replacer);
    }

    /**
     * SAT status of the entailment counterexample {@code a ∧ ¬b}.
     * {@code a ⊨ b} holds iff this is {@link LTLSolver.SolverResult#UNSAT}.
     * Returned raw so callers can assert against the exact enum value —
     * a {@code TIMEOUT}/{@code ERROR} then never masquerades as a proof.
     */
    private static LTLSolver.SolverResult counterexampleStatus(Formula a, Formula b) throws Exception {
        Formula counterexample = Conjunction.of(a, b.not());
        return LTLSolver.isSAT(SolverUtils.toSolverSyntax(counterexample.toString()));
    }

    /** original ⊨ weaker  <=>  (original ∧ ¬weaker) is UNSAT. */
    private void assertWeakens(String original, String weaker) throws Exception {
        assertEquals(UNSAT, counterexampleStatus(f(original), f(weaker)),
                "WEAKENING should hold: (" + original + ")  |=  (" + weaker + ")");
    }

    private void assertWeakens(Formula original, String weaker) throws Exception {
        assertEquals(UNSAT, counterexampleStatus(original, f(weaker)),
                "WEAKENING should hold: (" + original + ")  |=  (" + weaker + ")");
    }

    /** stronger ⊨ original  <=>  (stronger ∧ ¬original) is UNSAT. */
    private void assertStrengthens(String original, String stronger) throws Exception {
        assertEquals(UNSAT, counterexampleStatus(f(stronger), f(original)),
                "STRENGTHENING should hold: (" + stronger + ")  |=  (" + original + ")");
    }

    private void assertStrengthens(String original, Formula stronger) throws Exception {
        assertEquals(UNSAT, counterexampleStatus(stronger, f(original)),
                "STRENGTHENING should hold: (" + stronger + ")  |=  (" + original + ")");
    }

    /** a does NOT entail b: the counterexample (a ∧ ¬b) must be genuinely SAT. */
    private void assertNotEntails(String a, String b) throws Exception {
        assertEquals(SAT, counterexampleStatus(f(a), f(b)),
                "expected NON-entailment (counterexample must be SAT): (" + a + ")  |/=  (" + b + ")");
    }

    // ========================================================================
    //  The marquee case: G(a|b)  vs  G a | G b
    // ========================================================================

    @Test
    public void marquee_G_distributes_over_or() throws Exception {
        // (G a | G b) is STRONGER: it entails G(a|b), but not vice versa.
        assertEquals(UNSAT, counterexampleStatus(f("G a | G b"), f("G(a | b)")),
                "(G a | G b) |= G(a | b) must hold");
        assertEquals(SAT,   counterexampleStatus(f("G(a | b)"), f("G a | G b")),
                "G(a | b) must NOT entail (G a | G b)");
    }

    @Test
    public void marquee_matches_black_raw_sat() throws Exception {
        // Replicates the two `black solve` checks:
        // black -f "G(a || b) && ~(G a || G b)"  -> SAT
        assertEquals(SAT,   LTLSolver.isSAT(SolverUtils.toSolverSyntax(f("G(a | b) & !(G a | G b)").toString())),
                "G(a|b) & !(G a | G b) should be SAT");
        // black -f "~G(a || b) && (G a || G b)"  -> UNSAT
        assertEquals(UNSAT, LTLSolver.isSAT(SolverUtils.toSolverSyntax(f("!G(a | b) & (G a | G b)").toString())),
                "!G(a|b) & (G a | G b) should be UNSAT");
    }

    // ========================================================================
    //  WEAKENING rules  (original |= result)
    // ========================================================================

    @Test
    public void weakening_constants_and_literals() throws Exception {
        assertWeakens(FALSE, "true");             // false ⤳ true
        assertWeakens("p", "true");
        assertWeakens("p", "p | q");              // add disjunct
        assertWeakens("p", "F p");                // now ⤳ eventually
    }

    @Test
    public void weakening_next() throws Exception {
        assertWeakens("X p", "true");
        assertWeakens("X p", "F p");              // drop next ⤳ eventually
        assertWeakens("X(F p)", "F p");           // X F ⤳ F
    }

    @Test
    public void weakening_eventually() throws Exception {
        assertWeakens("F(a & b)", "F a & F b");   // distribute over conjunction
        assertWeakens("F(G a)", "G(F a)");        // F G ⤳ G F  (classic)
        assertWeakens("F(X a)", "F a");           // drop the extra step
        assertWeakens("F(G a)", "F a");           // discard the "forever"
    }

    @Test
    public void weakening_globally() throws Exception {
        assertWeakens("G a", "a");
        assertWeakens("G a", "F a");
        assertWeakens("G a", "X a");
        assertWeakens("G a", "G(F a)");
        assertWeakens("G a", "F(G a)");
        assertWeakens("G a", "X(G a)");
    }

    @Test
    public void weakening_conjunction_disjunction() throws Exception {
        assertWeakens("a & b", "a");              // drop conjunct
        assertWeakens("a & b", "a | b");          // conjunction ⤳ disjunction
        assertWeakens("a & b", "F(a & b)");
        assertWeakens("a | b", "a | b | c");      // add disjunct
    }

    @Test
    public void weakening_until_weakuntil() throws Exception {
        assertWeakens("a U b", "a W b");
        assertWeakens("a U b", "(a | c) U b");    // widen the "while waiting" side
        assertWeakens("a U b", "F b");

        assertWeakens("a W b", "F a | (a U b)");  // relax the "always" disjunct
        assertWeakens("a W b", "G a | F b");      // relax the "until" disjunct
        assertWeakens("a W b", "(a | c) W b");
    }

    @Test
    public void weakening_release_operators() throws Exception {
        assertWeakens("a M b", "b W (a & b)");
        assertWeakens("a M b", "F(a & b)");

        assertWeakens("a R b", "F b | (b U (a & b))");
        assertWeakens("a R b", "G b | F(a & b)");
    }

    // ========================================================================
    //  STRENGTHENING rules  (result |= original)
    // ========================================================================

    @Test
    public void strengthening_constants_and_literals() throws Exception {
        assertStrengthens("p", FALSE);            // true ⤳ false / p ⤳ false
        assertStrengthens("p", "p & q");          // add conjunct
        assertStrengthens("p", "G p");            // now ⤳ always
    }

    @Test
    public void strengthening_next() throws Exception {
        assertStrengthens("X p", FALSE);
        assertStrengthens("X p", "G p");          // strength(X a) = G a
        assertStrengthens("X(G a)", "G a");       // X G ⤳ G
    }

    @Test
    public void strengthening_eventually() throws Exception {
        assertStrengthens("F a", FALSE);
        assertStrengthens("F a", "a");            // require it now
        assertStrengthens("F a", "X a");
        assertStrengthens("F a", "G a");
        assertStrengthens("F a", "F(X a)");
        assertStrengthens("F a", "F(G a)");
        assertStrengthens("F a", "G(F a)");
    }

    @Test
    public void strengthening_globally() throws Exception {
        assertStrengthens("G(a | b)", "G a | G b"); // <-- the confirmed rule
        assertStrengthens("G(F a)", "F(G a)");      // G F ⤳ F G
        assertStrengthens("G(X a)", "G a");         // G X ⤳ G
        assertStrengthens("G(G a)", "G a");         // idempotence
    }

    @Test
    public void strengthening_conjunction_disjunction() throws Exception {
        assertStrengthens("a & b", FALSE);
        assertStrengthens("a & b", "a & b & c");    // add conjunct
        assertStrengthens("a & b", "G(a & b)");

        assertStrengthens("a | b", FALSE);
        assertStrengthens("a | b", "a & b");        // disjunction ⤳ conjunction
        assertStrengthens("a | b", "a");            // remove disjunct
        assertStrengthens("a | b", "G(a | b)");
    }

    @Test
    public void strengthening_until_weakuntil() throws Exception {
        assertStrengthens("a U b", FALSE);
        assertStrengthens("a U b", "b");
        assertStrengthens("a U b", "a & !b & X(a U b)"); // one-step unroll

        assertStrengthens("a W b", FALSE);
        assertStrengthens("a W b", "G a");          // keep only the "always" disjunct
        assertStrengthens("a W b", "a U b");        // keep only the "until" disjunct
    }

    @Test
    public void strengthening_release_operators() throws Exception {
        assertStrengthens("a M b", FALSE);
        assertStrengthens("a M b", "a & b");
        assertStrengthens("a M b", "b & !(a & b) & X(a M b)");

        assertStrengthens("a R b", FALSE);
        assertStrengthens("a R b", "G b");
        assertStrengthens("a R b", "b U (a && b)");
    }

    // ========================================================================
    //  The † heuristic branches: these are NOT sound entailments.
    //  The tests assert the entailment FAILS, documenting why they are
    //  exploration-only (they use a drawn, not logically-fresh literal c).
    // ========================================================================

    @Test
    public void heuristic_weakening_F_to_cWa_is_not_sound() throws Exception {
        // FormulaWeakening.visit(FOperator) fallback: weak(F a) = c W a
        // F a  does NOT entail  c W a  (e.g. a holds only later, c false).
        assertNotEntails("F a", "c W a");
    }

    @Test
    public void heuristic_strengthening_F_to_aUc_is_not_sound() throws Exception {
        // FormulaStrengthening.visit(FOperator) option 7: strength(F a) = a U c
        // a U c  does NOT entail  F a  (c may hold at t=0 with a false forever).
        assertNotEntails("a U c", "F a");
    }
}