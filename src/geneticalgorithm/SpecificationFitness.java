package geneticalgorithm;

import java.io.IOException;
import java.util.Set;

import com.lagodiuk.ga.Fitness;

import geneticalgorithm.SpecificationChromosome.SPEC_STATUS;
import owl.ltl.Conjunction;
import owl.ltl.Disjunction;
import owl.ltl.FOperator;
import owl.ltl.Formula;
import owl.ltl.GOperator;
import owl.ltl.LabelledFormula;
import owl.ltl.rewriter.NormalForms;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.visitors.StrongReleaseReplacer;
import solvers.LTLSolver;
import solvers.StrixHelper;
import solvers.StrixHelper.RealizabilitySolverResult;
import solvers.LTLSolver.SolverResult;

public class SpecificationFitness implements Fitness<SpecificationChromosome, Double> {
	
	static double LOGICAL_INCONSISTENCY = 1;
	private StrongReleaseReplacer visitor  = new StrongReleaseReplacer();
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
		Formula environment2 = environment.accept(visitor);
		SolverResult env_sat = LTLSolver.isSAT(separateLTLOperator(environment2.toString()));
		SPEC_STATUS status = SPEC_STATUS.UNKNOWN;
		
		if (!env_sat.inconclusive()) {
			Formula system = Conjunction.of(spec.preset(), GOperator.of(Conjunction.of(spec.assert_())), Conjunction.of(spec.guarantee()));
			Formula system2 = system.accept(visitor);
			SolverResult sys_sat = LTLSolver.isSAT(separateLTLOperator(system2.toString()));
			
			if (!sys_sat.inconclusive()) {
				if (env_sat == SolverResult.UNSAT && sys_sat == SolverResult.UNSAT) {
					status = SPEC_STATUS.BOTTOM;
				}
				else if (env_sat == SolverResult.UNSAT) {
					status = SPEC_STATUS.GUARANTEES;
				}
				else if (sys_sat == SolverResult.UNSAT) {
					status = SPEC_STATUS.ASSUMPTIONS;
				}
				else { //env_sat == SolverResult.SAT && sys_sat == SolverResult.SAT
					Formula env_sys = spec.toFormula().formula();
					
					System.out.println(env_sys);
					Formula env_sys2 = env_sys.accept(visitor);
					System.out.println(env_sys2);
					
					SolverResult sat = LTLSolver.isSAT(separateLTLOperator(env_sys2.toString()));
					if (!sat.inconclusive()) {
						if (sat == SolverResult.UNSAT)
							status = SPEC_STATUS.CONTRADICTORY;
						else {
							// check for realizability
							RealizabilitySolverResult rel = StrixHelper.checkRealizability(spec);
							if (!rel.inconclusive()) {
								if (rel == RealizabilitySolverResult.REALIZABLE) {
									status = SPEC_STATUS.REALIZABLE;
									System.out.println("asdasdasd");
								}
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
	
	private String separateLTLOperator(String LTLFormula) {
		return new String(LTLFormula.replaceAll("([A-Z])", " $1 ")); 
	}
}
