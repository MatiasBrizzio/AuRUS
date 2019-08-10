package geneticalgorithm;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.List;

import com.lagodiuk.ga.Fitness;

import geneticalgorithm.SpecificationChromosome.SPEC_STATUS;
import modelcounter.Count;
import owl.ltl.Conjunction;
import owl.ltl.Disjunction;
import owl.ltl.Formula;
import owl.ltl.GOperator;
import owl.ltl.LabelledFormula;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.visitors.SolverSyntaxOperatorReplacer;
import solvers.LTLModelCounter;
import solvers.LTLSolver;
import solvers.StrixHelper;
import solvers.LTLSolver.SolverResult;
import solvers.StrixHelper.RealizabilitySolverResult;

public class PreciseModelCountingSpecificationFitness implements Fitness<SpecificationChromosome, Double> {

	public static final int BOUND = 5;
	public static final double STATUS_FACTOR = 1d;
	public static final double LOST_MODELS_FACTOR = 0.25d;
	public static final double WON_MODELS_FACTOR = 0.25d;
	public static final double SOLUTION = STATUS_FACTOR * 5d;
	Tlsf originalSpecification = null;
	SPEC_STATUS originalStatus = SPEC_STATUS.UNKNOWN;
	BigInteger originalNumOfModels;
	BigInteger originalNegationNumOfModels;
	
	public PreciseModelCountingSpecificationFitness(Tlsf originalSpecification) throws IOException, InterruptedException {
		this.originalSpecification = originalSpecification;
		SpecificationChromosome originalChromosome = new SpecificationChromosome(originalSpecification);
		compute_status(originalChromosome);
		this.originalStatus = originalChromosome.status;
		originalNumOfModels = LTLModelCounter.count(originalSpecification.toFormula().formula(), originalSpecification.variables().size());
		originalNegationNumOfModels = LTLModelCounter.count(originalSpecification.toFormula().formula().not(), originalSpecification.variables().size());
	}
	
	private SolverSyntaxOperatorReplacer visitor  = new SolverSyntaxOperatorReplacer();
	
	@Override
	public Double calculate(SpecificationChromosome chromosome) {
		// compute multi-objective fitness function
		
		// First compute the status fitness
		try { 
			compute_status(chromosome);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		double status_fitness = 0d;
		if (chromosome.status == SPEC_STATUS.UNKNOWN)
			status_fitness = 0d;
		else if (chromosome.status == SPEC_STATUS.GUARANTEES)
			status_fitness = 1d;
		else if (chromosome.status == SPEC_STATUS.ASSUMPTIONS)
			status_fitness = 2d;
		else if (chromosome.status == SPEC_STATUS.CONTRADICTORY)
			status_fitness = 3d;
		else if (chromosome.status == SPEC_STATUS.UNREALIZABLE)
			status_fitness = 4d;
		else
			status_fitness = 5d;
		
		
		// Second, compute the portion of loosing models with respect to the original specification
		double lost_models_fitness = 0d; // if the current specification is inconsistent, then it looses all the models (it maintains 0% of models of the original specification)
		if (originalStatus.isSpecificationConsistent() && chromosome.status.isSpecificationConsistent() && originalSpecification.hashCode()!=chromosome.spec.hashCode()) {
			// if both specifications are consistent, then we will compute the percentage of models that are maintained after the refinement
			try {
				lost_models_fitness = compute_lost_models_porcentage(originalSpecification, chromosome.spec);
				System.out.print(lost_models_fitness + " ");
			}
			catch (Exception e) { e.printStackTrace(); }
		}
		
		// Third, compute the portion of winning models with respect to the original specification
		double won_models_fitness = 0d;
		if (originalStatus.isSpecificationConsistent() && chromosome.status.isSpecificationConsistent() && originalSpecification.hashCode()!=chromosome.spec.hashCode()) {
			// if both specifications are consistent, then we will compute the percentage of models that are added after the refinement (or removed from the complement of the original specifiction)
			try {
				won_models_fitness = compute_won_models_porcentage(originalSpecification, chromosome.spec);
				System.out.print(won_models_fitness + " ");
			}
			catch (Exception e) { e.printStackTrace(); }
		}
		
		double fitness = STATUS_FACTOR * status_fitness + LOST_MODELS_FACTOR * lost_models_fitness + WON_MODELS_FACTOR * won_models_fitness;
		chromosome.fitness = fitness;
		return fitness;
	}
	
	public void compute_status(SpecificationChromosome chromosome) throws IOException, InterruptedException {
		System.out.print(".");
		//check if status has been computed before
		if (chromosome.status != SPEC_STATUS.UNKNOWN)
			return;

		Tlsf spec = chromosome.spec;
		// Env = initially && G(require) & assume
		Formula environment = Conjunction.of(spec.initially(), GOperator.of(spec.require()), spec.assume());
		Formula environment2 = environment.accept(visitor);
		SolverResult env_sat = LTLSolver.isSAT(toSolverSyntax(environment2));
		SPEC_STATUS status = SPEC_STATUS.UNKNOWN;
		
		if (!env_sat.inconclusive()) {
			// Sys = preset && G(assert_) & guarantees
			Formula system = Conjunction.of(spec.preset(), GOperator.of(Conjunction.of(spec.assert_())), Conjunction.of(spec.guarantee()));
			Formula system2 = system.accept(visitor);
			SolverResult sys_sat = LTLSolver.isSAT(toSolverSyntax(system2));
			
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
					
//					System.out.println(env_sys);
					Formula env_sys2 = env_sys.accept(visitor);
//					System.out.println(env_sys2);
					
					SolverResult sat = LTLSolver.isSAT(toSolverSyntax(env_sys2));
					if (!sat.inconclusive()) {
						if (sat == SolverResult.UNSAT)
							status = SPEC_STATUS.CONTRADICTORY;
						else {
							// check for realizability
							RealizabilitySolverResult rel = StrixHelper.checkRealizability(spec);
							if (!rel.inconclusive()) {
								if (rel == RealizabilitySolverResult.REALIZABLE) {
									status = SPEC_STATUS.REALIZABLE;
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
	
	public double compute_lost_models_porcentage(Tlsf original, Tlsf refined) throws IOException, InterruptedException {
		System.out.print("-");
		int numOfVars = original.variables().size();
		
		Formula lostModels = Conjunction.of(original.toFormula().formula(), refined.toFormula().formula());
		BigDecimal numOfLostModels = new BigDecimal(LTLModelCounter.count(lostModels, numOfVars));
		
		BigDecimal numOfModels = new BigDecimal(originalNumOfModels);
		
		BigDecimal res = numOfLostModels.divide(numOfModels, 2, RoundingMode.HALF_UP);
		double value = res.doubleValue();
//		System.out.print(numOfLostModels + " " + numOfModels + " ");
		return value;
	}
	
	public double compute_won_models_porcentage(Tlsf original, Tlsf refined) throws IOException, InterruptedException {
		System.out.print("+");
		int numOfVars = original.variables().size();
		
		Formula wonModels = Conjunction.of(original.toFormula().formula().not(), refined.toFormula().formula().not());
		BigDecimal numOfWonModels = new BigDecimal(LTLModelCounter.count(wonModels, numOfVars));
		
		BigDecimal numOfNegationModels = new BigDecimal(originalNegationNumOfModels);
		
		BigDecimal res = numOfWonModels.divide(numOfNegationModels, 2, RoundingMode.HALF_UP);
		double value = res.doubleValue();
//		System.out.print(numOfWonModels + " " + numOfNegationModels + " ");
		return value;
	}
	
	private String toSolverSyntax(Formula f) {
		String LTLFormula = f.toString();
		LTLFormula = LTLFormula.replaceAll("\\!", "~");
		LTLFormula = LTLFormula.replaceAll("([A-Z])", " $1 ");
		return new String(LTLFormula); 
	}

}
