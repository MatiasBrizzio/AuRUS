package modelcounter;

import owl.ltl.BooleanConstant;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.rewriter.SyntacticSimplifier;
import owl.ltl.visitors.SolverSyntaxOperatorReplacer;
import solvers.LTLSolver;
import solvers.LTLSolver.SolverResult;
import utils.SolverUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Optional;

/**
 * Public entry point of AuRUS's approximate bounded model counter, and the
 * path the fitness function counts through.
 *
 * <p>A formula is simplified, checked for satisfiability with
 * {@code Settings.SAT_SOLVER}, and only then translated and counted. The count
 * is over the variables declared in the {@link LabelledFormula}.</p>
 */
public final class ModelCounter {

    private ModelCounter() {
    }

    private enum SatCheck {
        SATISFIABLE, UNSATISFIABLE, TIMEOUT, ERROR
    }

    /**
     * Counts the words of length {@code bound} accepted by the formula's
     * automaton.
     *
     * @param formula the formula, with its declared variables
     * @param bound   the word length {@code k}
     * @return the count, a proof of unsatisfiability, or a failure
     */
    public static CountResult count(LabelledFormula formula, int bound) {
        Formula simplified = simplify(formula);
        if (simplified == BooleanConstant.FALSE)
            return new CountResult(CountResult.Status.UNSAT_SIMPLIFIED, BigInteger.ZERO);

        SatCheck check = checkSatisfiability(simplified);
        if (check == SatCheck.UNSATISFIABLE)
            return new CountResult(CountResult.Status.UNSAT_SOLVER, BigInteger.ZERO);

        EmersonLeiAutomatonBasedModelCounting<?> counter =
                new EmersonLeiAutomatonBasedModelCounting<>(LabelledFormula.of(simplified, formula.variables()));
        BigInteger models = counter.count(bound);
        if (models == null)
            return new CountResult(statusOf(counter.getFailure()), null);

        return switch (check) {
            case TIMEOUT -> new CountResult(CountResult.Status.SOLVER_TIMEOUT, models);
            case ERROR -> new CountResult(CountResult.Status.ERROR, models);
            default -> new CountResult(CountResult.Status.OK, models);
        };
    }

    /**
     * Builds the automaton {@link #count} would use, without counting.
     *
     * @param formula the formula, with its declared variables
     * @return the automaton, or empty if the simplified formula is {@code false}
     * @throws IllegalStateException    if the translation failed
     * @throws IllegalArgumentException if the alphabet is too large to dump
     */
    public static Optional<AutomatonDump> dump(LabelledFormula formula) {
        Formula simplified = simplify(formula);
        if (simplified == BooleanConstant.FALSE)
            return Optional.empty();
        return Optional.of(new EmersonLeiAutomatonBasedModelCounting<>(
                LabelledFormula.of(simplified, formula.variables())).dump());
    }

    private static Formula simplify(LabelledFormula formula) {
        return formula.formula().accept(new SyntacticSimplifier());
    }

    /**
     * Only a conclusive {@code UNSAT} proves there are no models.
     * {@link LTLSolver} also reports {@code UNSAT} when aalta dies with a
     * non-zero exit and an empty stderr, so {@code numOfError} is checked too.
     */
    private static SatCheck checkSatisfiability(Formula formula) {
        int errorsBefore = LTLSolver.numOfError;
        try {
            // The visitor rewrites W and M, which aalta rejects with a syntax
            // error, into operators it accepts. toSolverSyntax then separates
            // the operators, without which aalta reads "Xp1" as a variable.
            SolverResult result = LTLSolver.isSAT(
                    SolverUtils.toSolverSyntax(formula.accept(new SolverSyntaxOperatorReplacer())));
            if (LTLSolver.numOfError != errorsBefore)
                return SatCheck.ERROR;
            return switch (result) {
                case SAT -> SatCheck.SATISFIABLE;
                case UNSAT -> SatCheck.UNSATISFIABLE;
                case TIMEOUT -> SatCheck.TIMEOUT;
                default -> SatCheck.ERROR;
            };
        } catch (IOException e) {
            return SatCheck.ERROR;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SatCheck.ERROR;
        }
    }

    private static CountResult.Status statusOf(EmersonLeiAutomatonBasedModelCounting.Failure failure) {
        return switch (failure) {
            case TRANSLATION_TIMEOUT -> CountResult.Status.TRANSLATION_TIMEOUT;
            case COUNT_TIMEOUT -> CountResult.Status.COUNT_TIMEOUT;
            default -> CountResult.Status.ERROR;
        };
    }
}
