package geneticalgorithm;

import java.io.IOException;

import owl.ltl.Conjunction;
import owl.ltl.Formula;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.visitors.SolverSyntaxOperatorReplacer;
import solvers.LTLSolver;
import utils.SolverUtils;

public class SpecificationWeakness {
    private static final SolverSyntaxOperatorReplacer visitor = new SolverSyntaxOperatorReplacer();

    public static boolean is_weakening_of_original(Tlsf original, SpecificationChromosome chromosome) throws IOException, InterruptedException {
        Tlsf candidate = chromosome.spec;
        Formula as_candidate = candidate.assume();
        Formula g_candidate = Conjunction.of(candidate.guarantee());
        Formula as_original = original.assume();
        Formula g_original = Conjunction.of(original.guarantee());
        LTLSolver.SolverResult sat = LTLSolver.isSAT(
            SolverUtils.toSolverSyntax(
                Conjunction.of(as_candidate, as_original.not()).accept(visitor)));
        //check as_candidate => as_original = UNSAT(as_candidate & !as_original)
        if (sat.inconclusive() || sat != LTLSolver.SolverResult.UNSAT) {
            return false;
        }
        //check g_original => g_candidate = UNSAT(g_original & !g_candidate)
        sat = LTLSolver.isSAT(
            SolverUtils.toSolverSyntax(
                Conjunction.of(g_original, g_candidate.not()).accept(visitor)));
        if (sat.inconclusive() || sat != LTLSolver.SolverResult.UNSAT) {
            return false;
        }
        return true;
    }
}
