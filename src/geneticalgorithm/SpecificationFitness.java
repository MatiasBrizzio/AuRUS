package geneticalgorithm;

import java.io.IOException;

import com.lagodiuk.ga.Fitness;

import geneticalgorithm.SpecificationChromosome.SPEC_STATUS;
import owl.ltl.Conjunction;
import owl.ltl.Formula;
import owl.ltl.GOperator;
import owl.ltl.tlsf.Tlsf;
import solvers.LTLSolver;
import solvers.StrixHelper;
import solvers.StrixHelper.RealizabilitySolverResult;
import solvers.LTLSolver.SolverResult;

public class SpecificationFitness implements Fitness<SpecificationChromosome, Double> {
	
	static double LOGICAL_INCONSISTENCY = 1;
	
	@Override
	public Double calculate(SpecificationChromosome chromosome) {
		try { 
			compute_status(chromosome);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		double fitness = 0d;
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
		
		return fitness;
	}
	
	private void compute_status(SpecificationChromosome chromosome) throws IOException, InterruptedException {
		Tlsf spec = chromosome.spec;
		// ENv = initially && G(require) & assume
		Formula environment = Conjunction.of(spec.initially(), GOperator.of(spec.require()), spec.assume());
		SolverResult env_sat = LTLSolver.isSAT(environment.toString());
		SPEC_STATUS status = SPEC_STATUS.UNKNOWN;
		if (!env_sat.inconclusive()) {
			Formula system = Conjunction.of(spec.preset(), GOperator.of(Conjunction.of(spec.assert_())), Conjunction.of(spec.guarantee()));
			SolverResult sys_sat = LTLSolver.isSAT(system.toString());
			if (!sys_sat.inconclusive()) {
				if (env_sat == SolverResult.UNSAT && sys_sat == SolverResult.UNSAT)
					status = SPEC_STATUS.BOTTOM;
				else if (env_sat == SolverResult.UNSAT)
					status = SPEC_STATUS.GUARANTEES;
				else if (sys_sat == SolverResult.UNSAT)
					status = SPEC_STATUS.ASSUMPTIONS;
				else { //env_sat == SolverResult.SAT && sys_sat == SolverResult.SAT
					Formula env_sys = spec.toFormula().formula();
					SolverResult sat = LTLSolver.isSAT(env_sys.toString());
					if (!sat.inconclusive()) {
						if (sat == SolverResult.UNSAT)
							status = SPEC_STATUS.CONTRADICTORY;
						else {
							// check for realizability
							RealizabilitySolverResult rel = StrixHelper.checkRealizability(spec);
							if (!rel.inconclusive()) {
								if (rel == RealizabilitySolverResult.REALIZABLE)
									status = SPEC_STATUS.REALIZABLE;
								else
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
