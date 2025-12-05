package geneticalgorithm;

import java.io.IOException;

import owl.ltl.BooleanConstant;
import owl.ltl.Conjunction;
import owl.ltl.Formula;
import owl.ltl.GOperator;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.visitors.SolverSyntaxOperatorReplacer;
import solvers.LTLSolver;
import solvers.StrixHelper;
import utils.SolverUtils;
import utils.TlsfUtils;

import java.util.function.Predicate;

public class SpecificationFiltering {
    private Tlsf original;
    private final SolverSyntaxOperatorReplacer visitor = new SolverSyntaxOperatorReplacer();

    public SpecificationFiltering(Tlsf original) {
        this.original = original;
    }

    private boolean is_weakening_of_original(SpecificationChromosome chromosome) throws IOException, InterruptedException {
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

    private boolean is_well_separated(SpecificationChromosome chromosome) throws IOException, InterruptedException {
        Tlsf spec = chromosome.spec;
        Formula env_sys = Conjunction.of(
            spec.initially(),
            GOperator.of(spec.require()),
            spec.preset(),
            GOperator.of(
                Conjunction.of(spec.assert_())),
                spec.assume(),
                Conjunction.of(spec.guarantee())
        );
        SolverSyntaxOperatorReplacer visitor = new SolverSyntaxOperatorReplacer();
        Formula env_sys2 = env_sys.accept(visitor);
        LTLSolver.SolverResult res = LTLSolver.isSAT(SolverUtils.toSolverSyntax(env_sys2));
        if (res != LTLSolver.SolverResult.SAT) {
            return false;
        }
        Tlsf wellSeparatedSpec = TlsfUtils.change_guarantees(spec, BooleanConstant.FALSE);
        StrixHelper.RealizabilitySolverResult rel = StrixHelper.checkRealizability(wellSeparatedSpec);
        return rel != StrixHelper.RealizabilitySolverResult.REALIZABLE;
    }

    public Predicate<SpecificationChromosome> get_predicate() {
        return chromosome -> {
            try {
                return this.is_weakening_of_original(chromosome) && this.is_well_separated(chromosome);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        };
    }
}
