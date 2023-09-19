package geneticalgorithm;

import com.lagodiuk.ga.Fitness;
import geneticalgorithm.SpecificationChromosome.SPEC_STATUS;
import owl.ltl.Conjunction;
import owl.ltl.Formula;
import owl.ltl.GOperator;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.visitors.SolverSyntaxOperatorReplacer;
import solvers.LTLSolver;
import solvers.LTLSolver.SolverResult;
import solvers.SolverUtils;
import solvers.StrixHelper;
import solvers.StrixHelper.RealizabilitySolverResult;

import java.io.IOException;

public class SpecificationFitness implements Fitness<SpecificationChromosome, Double> {

    public static double SOLUTION = 5d;
    private final SolverSyntaxOperatorReplacer visitor = new SolverSyntaxOperatorReplacer();

    public SpecificationFitness() {

    }

    @Override
    public Double calculate(SpecificationChromosome chromosome) {
        try {
            compute_status(chromosome);
        } catch (Exception e) {
            e.printStackTrace();
        }

        double fitness;
        if (chromosome.status == SPEC_STATUS.UNKNOWN)
            fitness = 0d;
        else if (chromosome.status == SPEC_STATUS.GUARANTEES)
            fitness = 1d;
        else if (chromosome.status == SPEC_STATUS.ASSUMPTIONS)
            fitness = 2d;
        else if (chromosome.status == SPEC_STATUS.CONTRADICTORY)
            fitness = 3d;
        else if (chromosome.status == SPEC_STATUS.UNREALIZABLE)
            fitness = 4d;
        else
            fitness = 5d;

        chromosome.fitness = fitness;
        return fitness;
    }

    private void compute_status(SpecificationChromosome chromosome) throws IOException, InterruptedException {
        //check if status has been computed before
        if (chromosome.status != SPEC_STATUS.UNKNOWN)
            return;

        Tlsf spec = chromosome.spec;
        // Env = initially && G(require) & assume
        Formula environment = Conjunction.of(spec.initially(), GOperator.of(spec.require()), spec.assume());
        Formula environment2 = environment.accept(visitor);
        SolverResult env_sat = LTLSolver.isSAT(SolverUtils.toSolverSyntax(environment2));
        SPEC_STATUS status = SPEC_STATUS.UNKNOWN;

        if (!env_sat.inconclusive()) {
            // Sys = preset && G(assert_) & guarantees
            Formula system = Conjunction.of(spec.preset(), GOperator.of(Conjunction.of(spec.assert_())), Conjunction.of(spec.guarantee()));
            Formula system2 = system.accept(visitor);
            SolverResult sys_sat = LTLSolver.isSAT(SolverUtils.toSolverSyntax(system2));

            if (!sys_sat.inconclusive()) {
                if (env_sat == SolverResult.UNSAT && sys_sat == SolverResult.UNSAT) {
                    status = SPEC_STATUS.BOTTOM;
                } else if (env_sat == SolverResult.UNSAT) {
                    status = SPEC_STATUS.GUARANTEES;
                } else if (sys_sat == SolverResult.UNSAT) {
                    status = SPEC_STATUS.ASSUMPTIONS;
                } else { //env_sat == SolverResult.SAT && sys_sat == SolverResult.SAT
                    Formula env_sys = spec.toFormula().formula();
                    Formula env_sys2 = env_sys.accept(visitor);
                    SolverResult sat = LTLSolver.isSAT(SolverUtils.toSolverSyntax(env_sys2));
                    if (!sat.inconclusive()) {
                        if (sat == SolverResult.UNSAT)
                            status = SPEC_STATUS.CONTRADICTORY;
                        else {
                            // check for realizability
                            RealizabilitySolverResult rel = StrixHelper.checkRealizability(spec);
                            if (!rel.inconclusive()) {
                                if (rel == RealizabilitySolverResult.REALIZABLE) {
                                    status = SPEC_STATUS.REALIZABLE;
                                } else
                                    status = SPEC_STATUS.UNREALIZABLE;
                            }
                        }
                    }
                }
            }
        }
        chromosome.status = status;
    }
}
