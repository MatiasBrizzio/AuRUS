package geneticalgorithm;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Sets;
import com.lagodiuk.ga.Fitness;

import geneticalgorithm.SpecificationChromosome.SPEC_STATUS;
import modelcounter.Count;
import owl.ltl.BooleanConstant;
import owl.ltl.Conjunction;
import owl.ltl.Disjunction;
import owl.ltl.Formula;
import owl.ltl.GOperator;
import owl.ltl.LabelledFormula;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.visitors.SolverSyntaxOperatorReplacer;
import owl.util.StringUtil;
import solvers.LTLModelCounter;
import solvers.LTLSolver;
import solvers.StrixHelper;
import solvers.LTLSolver.SolverResult;
import solvers.StrixHelper.RealizabilitySolverResult;
import tlsf.Formula_Utils;

public class PreciseModelCountingSpecificationFitness implements Fitness<SpecificationChromosome, Double> {

	public static final int BOUND = 5;
	public static final double STATUS_FACTOR = 0.5d;
	public static final double LOST_MODELS_FACTOR = 0.2d;
	public static final double WON_MODELS_FACTOR = 0.2d;
//	public static final double SOLUTION = STATUS_FACTOR * d;
	public static final double SYNTACTIC_FACTOR = 0.1d;
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
		if (chromosome.status != SPEC_STATUS.UNKNOWN)
			return chromosome.fitness;
		
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
			status_fitness = 0.15d;
		else if (chromosome.status == SPEC_STATUS.ASSUMPTIONS)
			status_fitness = 0.25d;
		else if (chromosome.status == SPEC_STATUS.CONTRADICTORY)
			status_fitness = 0.5d;
				else if (chromosome.status == SPEC_STATUS.UNREALIZABLE)
			status_fitness = 0.9d;
		else
			status_fitness = 1.0d;
		
		double fitness = STATUS_FACTOR * status_fitness;
		
		double syntactic_distance = 0.0d;
		if (originalStatus.isSpecificationConsistent() && chromosome.status.isSpecificationConsistent() && originalSpecification.hashCode()!=chromosome.spec.hashCode()) {
			syntactic_distance = compute_syntactic_distance2(originalSpecification, chromosome.spec);
			System.out.printf("s%.2f ", syntactic_distance);
		}
		
		if (syntactic_distance < 1.0d) { 
			//if the specifications are not syntactically equivalent 
			// Second, compute the portion of loosing models with respect to the original specification
			double lost_models_fitness = 0.0d; // if the current specification is inconsistent, then it looses all the models (it maintains 0% of models of the original specification)
			if (originalStatus.isSpecificationConsistent() && chromosome.status.isSpecificationConsistent() && originalSpecification.hashCode()!=chromosome.spec.hashCode()) {
				// if both specifications are consistent, then we will compute the percentage of models that are maintained after the refinement
				try {
					lost_models_fitness = compute_lost_models_porcentage(originalSpecification, chromosome.spec);
					System.out.print(lost_models_fitness + " ");
				}
				catch (Exception e) { e.printStackTrace(); }
			}
			
			// Third, compute the portion of winning models with respect to the original specification
			double won_models_fitness = 0.0d;
			if (originalStatus.isSpecificationConsistent() && chromosome.status.isSpecificationConsistent() && originalSpecification.hashCode()!=chromosome.spec.hashCode()) {
				// if both specifications are consistent, then we will compute the percentage of models that are added after the refinement (or removed from the complement of the original specifiction)
				try {
					won_models_fitness = compute_won_models_porcentage(originalSpecification, chromosome.spec);
					System.out.print(won_models_fitness + " ");
				}
				catch (Exception e) { e.printStackTrace(); }
			}
			
			fitness += LOST_MODELS_FACTOR * lost_models_fitness + WON_MODELS_FACTOR * won_models_fitness + SYNTACTIC_FACTOR * syntactic_distance;
		}
		
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
		Formula refined_formula = refined.toFormula().formula();
		if (refined_formula == BooleanConstant.TRUE)
			return 1d;
		if (refined_formula == BooleanConstant.FALSE)
			return 0d;
		Formula lostModels = Conjunction.of(original.toFormula().formula(), refined_formula);
		if (lostModels == BooleanConstant.TRUE)
			return 1d;
		if (lostModels == BooleanConstant.FALSE)
			return 0d;
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
		Formula refined_negated_formula = refined.toFormula().formula().not();
		if (refined_negated_formula == BooleanConstant.TRUE)
			return 1d;
		if (refined_negated_formula == BooleanConstant.FALSE)
			return 0d;
		Formula wonModels = Conjunction.of(original.toFormula().formula().not(), refined_negated_formula);
		if (wonModels == BooleanConstant.TRUE)
			return 1d;
		if (wonModels == BooleanConstant.FALSE)
			return 0d;
		BigDecimal numOfWonModels = new BigDecimal(LTLModelCounter.count(wonModels, numOfVars));
		
		BigDecimal numOfNegationModels = new BigDecimal(originalNegationNumOfModels);
		
		BigDecimal res = numOfWonModels.divide(numOfNegationModels, 2, RoundingMode.HALF_UP);
		double value = res.doubleValue();
//		System.out.print(numOfWonModels + " " + numOfNegationModels + " ");
		return value;
	}
	
	public double compute_syntactic_distance(Tlsf original, Tlsf refined) {
		List<LabelledFormula> sub_original = Formula_Utils.subformulas(original.toFormula());
		List<LabelledFormula> sub_refined = Formula_Utils.subformulas(refined.toFormula());
		
		Set<LabelledFormula> lostSubs = Sets.difference(Sets.newHashSet(sub_original), Sets.newHashSet(sub_refined));
		Set<LabelledFormula> wonSubs = Sets.difference(Sets.newHashSet(sub_refined), Sets.newHashSet(sub_original));
//		String originalStr = original.toFormula().toString();
//		String refinedStr = refined.toFormula().toString();
//		String diffLost = StringUtils.difference(originalStr, refinedStr);
//		System.out.println(lostSubs.size() +" " + sub_original.size());
//		String diffWon = StringUtils.difference(refinedStr, originalStr);
//		System.out.println(wonSubs.size()  +" " + sub_refined.size());
		double lost = ((double) lostSubs.size()) / ((double) sub_original.size());
		double won = ((double) wonSubs.size()) / ((double) sub_refined.size());
		double syntactic_distance = ((double) 1.0d - (0.5d * lost + 0.5d * won));
		return syntactic_distance;
	}
	
	private String toSolverSyntax(Formula f) {
		String LTLFormula = f.toString();
		LTLFormula = LTLFormula.replaceAll("\\!", "~");
		LTLFormula = LTLFormula.replaceAll("([A-Z])", " $1 ");
		return new String(LTLFormula); 
	}
	
	public double compute_syntactic_distance2(Tlsf original, Tlsf refined) {
		
		List<Formula> og = original.guarantee();
		Formula oa = original.assume();
		
		List<Formula> rg = refined.guarantee();
		Formula ra = refined.assume();
			
		List<LabelledFormula> sub_original = Formula_Utils.subformulas(original.toFormula());
		List<LabelledFormula> sub_refined = Formula_Utils.subformulas(refined.toFormula());
		
		
		Set<LabelledFormula> lostSubs = Sets.difference(Sets.newHashSet(sub_original), Sets.newHashSet(sub_refined));
		Set<LabelledFormula> wonSubs = Sets.difference(Sets.newHashSet(sub_refined), Sets.newHashSet(sub_original));

		List<LabelledFormula> guarantees = new ArrayList<LabelledFormula>();
		List<Formula> assume = Formula_Utils.splitConjunction(oa);
		for (Formula fo : og)
			guarantees.add(LabelledFormula.of(fo, original.variables())) ;
		

		List<LabelledFormula> guaranteesr = new ArrayList<LabelledFormula>();
		List<Formula> assumer = Formula_Utils.splitConjunction(ra);
		for (Formula fo : rg)
			guaranteesr.add(LabelledFormula.of(fo, refined.variables()));
		
		
		double lost = ((double) lostSubs.size()) / ((double) sub_original.size());
		double won = ((double) wonSubs.size()) / ((double) sub_refined.size());
		double syntactic_distance = 0.5d * lost + 0.5d * won;
		
		double diff_guar = guaranteesr.size() - guarantees.size();
		double diff_assum = assumer.size() - assume.size();
		System.out.println("syntac:" + diff_guar + " " +diff_assum+ " " + assumer.size() + " " + guaranteesr.size());
		
			
		syntactic_distance += diff_assum*0.5d + diff_guar*0.3d;
		//syntactic_distance += ((( ) + ((double)diff_guar*0.3d))/ (sub_assumer.size() + sub_guaranteesr.size())) );
		return syntactic_distance;
	}
	
	
}
