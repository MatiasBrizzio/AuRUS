package geneticalgorithm;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
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
import solvers.LTLSolver;
import solvers.StrixHelper;
import solvers.LTLSolver.SolverResult;
import solvers.StrixHelper.RealizabilitySolverResult;

public class ModelCountingSpecificationFitness implements Fitness<SpecificationChromosome, Double> {

	public static final int BOUND = 1000;
	public static final double STATUS_FACTOR = 0.9d;
	public static final double LOST_MODELS_FACTOR = 0.05d;
	public static final double WON_MODELS_FACTOR = 0.05d;
	public static final double SOLUTION = STATUS_FACTOR * 5d;
	Tlsf originalSpecification = null;
	SPEC_STATUS originalStatus = SPEC_STATUS.UNKNOWN;
	
	public ModelCountingSpecificationFitness(Tlsf originalSpecification) throws IOException, InterruptedException {
		this.originalSpecification = originalSpecification;
		SpecificationChromosome originalChromosome = new SpecificationChromosome(originalSpecification);
		compute_status(originalChromosome);
		this.originalStatus = originalChromosome.status;
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
		if (originalStatus.isSpecificationConsistent() && chromosome.status.isSpecificationConsistent()) {
			// if both specifications are consistent, then we will compute the percentage of models that are maintained after the refinement
			try {
				lost_models_fitness = compute_lost_models_porcentage(originalSpecification, chromosome.spec);
			}
			catch (Exception e) { e.printStackTrace(); }
		}
		
		// Third, compute the portion of winning models with respect to the original specification
		double won_models_fitness = 0d;
		if (originalStatus.isSpecificationConsistent() && chromosome.status.isSpecificationConsistent()) {
			// if both specifications are consistent, then we will compute the percentage of models that are added after the refinement (or removed from the complement of the original specifiction)
			try {
				won_models_fitness = compute_won_models_porcentage(originalSpecification, chromosome.spec);
			}
			catch (Exception e) { e.printStackTrace(); }
		}
		
		double fitness = STATUS_FACTOR * status_fitness + LOST_MODELS_FACTOR * lost_models_fitness + WON_MODELS_FACTOR * won_models_fitness;
		chromosome.fitness = fitness;
		return fitness;
	}
	
	public void compute_status(SpecificationChromosome chromosome) throws IOException, InterruptedException {
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
	
	private double compute_lost_models_porcentage(Tlsf original, Tlsf refined) throws IOException, InterruptedException {
		// #(original & !refined) = #(Ie, Is, []Se, Ae -> ([]Ss & G), Ae', []Ss -> !G'
		List<LabelledFormula> constraints = new LinkedList<>();
		List<String> variables = original.variables();
		// Ie
		constraints.add(LabelledFormula.of(original.initially(), variables));
		// Is
		constraints.add(LabelledFormula.of(original.preset(), variables));
		// []Se
		constraints.add(LabelledFormula.of(GOperator.of(original.require()), variables));
		//Ae -> ([]Ss & G)
		constraints.add(LabelledFormula.of(Disjunction.of(original.assume().not(), 
														  Conjunction.of(GOperator.of(Conjunction.of(original.assert_())), 
																  		 Conjunction.of(original.guarantee())))
						, variables));
		
		//Ae'
		constraints.add(LabelledFormula.of(refined.assume(), variables));
		//[]Ss -> !G' = ![]Ss | !G'
		constraints.add(LabelledFormula.of(Disjunction.of(GOperator.of(Conjunction.of(original.assert_())).not(), 
						  		 Conjunction.of(refined.guarantee()).not() )
						, variables));
		
		List<String> formulas = new LinkedList<String>();
		for (LabelledFormula f : constraints)
			formulas.add(f.toString());
		
		BigDecimal numOfLostModels = new BigDecimal(Count.count(formulas, null, BOUND, false));
		
		formulas = new LinkedList<String>();
		formulas.add(original.toFormula().toString());
		BigDecimal numOfModels = new BigDecimal(Count.count(formulas, null, BOUND, false));
		
		BigDecimal res = numOfLostModels.divide(numOfModels);
		double value = res.doubleValue();
		
		return value;
	}
	
	private double compute_won_models_porcentage(Tlsf original, Tlsf refined) throws IOException, InterruptedException {
		// #(original & !refined) = #(Ie, Is, []Se, Ae' -> ([]Ss & G'), Ae, []Ss -> !G
		List<LabelledFormula> constraints = new LinkedList<>();
		List<String> variables = original.variables();
		// Ie
		constraints.add(LabelledFormula.of(original.initially(), variables));
		// Is
		constraints.add(LabelledFormula.of(original.preset(), variables));
		// []Se
		constraints.add(LabelledFormula.of(GOperator.of(original.require()), variables));
		//Ae' -> ([]Ss & G')
		constraints.add(LabelledFormula.of(Disjunction.of(refined.assume().not(), 
														  Conjunction.of(GOperator.of(Conjunction.of(original.assert_())), 
																  		 Conjunction.of(refined.guarantee())))
						, variables));
		
		//Ae
		constraints.add(LabelledFormula.of(original.assume(), variables));
		//[]Ss -> !G = ![]Ss | !G
		constraints.add(LabelledFormula.of(Disjunction.of(GOperator.of(Conjunction.of(original.assert_())).not(), 
						  		 Conjunction.of(original.guarantee()).not() )
						, variables));
		
		List<String> formulas = new LinkedList<String>();
		for (LabelledFormula f : constraints)
			formulas.add(f.toString());
		
		BigDecimal numOfWonModels = new BigDecimal(Count.count(formulas, null, BOUND, false));
		
		formulas = new LinkedList<String>();
		formulas.add(original.toFormula().not().toString());
		BigDecimal numOfModels = new BigDecimal(Count.count(formulas, null, BOUND, false));
		
		BigDecimal res = numOfWonModels.divide(numOfModels);
		double value = res.doubleValue();
		
		return value;
	}
	
	private String toSolverSyntax(Formula f) {
		String LTLFormula = f.toString();
		LTLFormula = LTLFormula.replaceAll("\\!", "~");
		LTLFormula = LTLFormula.replaceAll("([A-Z])", " $1 ");
		return new String(LTLFormula); 
	}

}
