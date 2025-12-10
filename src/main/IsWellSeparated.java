package main;

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

import java.io.File;
import java.io.IOException;

public class IsWellSeparated {

    public static void main(String[] args) throws IOException, InterruptedException {
        String filename = "";
        for (String arg : args) {
            if (arg.startsWith("-f=")) {
                filename = arg.replace("-f=", "");
            }
        }
        if (filename.isEmpty()) {
            System.out.println("filename name is missing.");
            System.exit(0);
        }
        if (!filename.endsWith(".tlsf")) {
            System.out.println("filename must be a .tlsf file.");
            System.exit(0);
        }

        Tlsf spec = TlsfUtils.toBasicTLSF(new File(filename));
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
        if (res == null) {
            System.out.println("Timeout");
            return;
        }
        if (res == LTLSolver.SolverResult.SAT) {
            Tlsf wellSeparatedSpec = TlsfUtils.change_guarantees(spec, BooleanConstant.FALSE);
            StrixHelper.RealizabilitySolverResult rel = StrixHelper.checkRealizability(wellSeparatedSpec);
            if (rel == null) {
                System.out.println("Timeout");
                return;
            }
            else if (rel == StrixHelper.RealizabilitySolverResult.REALIZABLE) {
                System.out.println("Not well-separated");
                return;
            }
        } else if (res == LTLSolver.SolverResult.UNSAT) {
            System.out.println("Unsatisfiable");
            return;
        }
        System.out.println("Well-separated");
    }
}
