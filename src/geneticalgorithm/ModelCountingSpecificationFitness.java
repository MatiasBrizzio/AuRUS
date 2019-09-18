package geneticalgorithm;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.List;

import com.lagodiuk.ga.Fitness;

import geneticalgorithm.SpecificationChromosome.SPEC_STATUS;
import modelcounter.ABC;
import modelcounter.Count;
import owl.ltl.*;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.visitors.SolverSyntaxOperatorReplacer;
import solvers.LTLSolver;
import solvers.StrixHelper;
import solvers.LTLSolver.SolverResult;
import solvers.StrixHelper.RealizabilitySolverResult;
import tlsf.CountREModels;
import tlsf.Formula_Utils;

public class ModelCountingSpecificationFitness implements Fitness<SpecificationChromosome, Double> {

	public static final int BOUND = 5;
	public static boolean EXHAUSTIVE = true;
	public static final double STATUS_FACTOR = 0.65d;
	public static final double LOST_MODELS_FACTOR = 0.15d;
	public static final double WON_MODELS_FACTOR = 0.15d;
	//	public static final double SOLUTION = 0.8d;
	public static final double SYNTACTIC_FACTOR = 0.05d;
	public static Tlsf originalSpecification = null;
	public static List<String> alphabet = null;
	public static SPEC_STATUS originalStatus = SPEC_STATUS.UNKNOWN;
	public static BigInteger originalNumOfModels;
	public static BigInteger originalNegationNumOfModels;
	
	public ModelCountingSpecificationFitness(Tlsf originalSpecification) throws IOException, InterruptedException {
		this.originalSpecification = originalSpecification;
		generateAlphabet();
		SpecificationChromosome originalChromosome = new SpecificationChromosome(originalSpecification);
		compute_status(originalChromosome);
		this.originalStatus = originalChromosome.status;
		originalNumOfModels = countModels(originalSpecification.toFormula());
		originalNegationNumOfModels = countModels(originalSpecification.toFormula().not());
	}
	
	private static void generateAlphabet () {
		if (originalSpecification.variables().size() <= 26) {
			alphabet = new LinkedList();
			for (int i = 0; i < originalSpecification.variables().size(); i++) {
				String v = ""+Character.toChars(97+i)[0];
				alphabet.add(v);
			}
			System.out.println(alphabet);
		}
	}
	
	private SolverSyntaxOperatorReplacer visitor  = new SolverSyntaxOperatorReplacer();
	
	@Override
	public Double calculate(SpecificationChromosome chromosome) {
		// compute multi-objective fitness function
		if (chromosome.status != SPEC_STATUS.UNKNOWN)
			return chromosome.fitness;

		// remove trivial specifications
		if (originalSpecification.equals(chromosome.spec))
			return 0.0d;
		if (chromosome.spec.assume() == BooleanConstant.FALSE)
			return 0.0d;
		Formula guarantees = Conjunction.of(chromosome.spec.guarantee());
		if (guarantees == BooleanConstant.TRUE)
			return 0.0d;

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
		syntactic_distance = compute_syntactic_distance_size(originalSpecification, chromosome.spec);
		System.out.printf("s%.2f ", syntactic_distance);


//		if (syntactic_distance < 1.0d) {
		//if the specifications are not syntactically equivalent
		// Second, compute the portion of loosing models with respect to the original specification
		double lost_models_fitness = 0.0d; // if the current specification is inconsistent, then it looses all the models (it maintains 0% of models of the original specification)
		if (originalStatus.isSpecificationConsistent() && chromosome.status.isSpecificationConsistent()) {
			// if both specifications are consistent, then we will compute the percentage of models that are maintained after the refinement
			try {
				lost_models_fitness = compute_lost_models_porcentage(originalSpecification, chromosome.spec);
				System.out.print(lost_models_fitness + " ");
			}
			catch (Exception e) { e.printStackTrace(); }
		}

		// Third, compute the portion of winning models with respect to the original specification
		double won_models_fitness = 0.0d;
		if (originalStatus.isSpecificationConsistent() && chromosome.status.isSpecificationConsistent()) {
			// if both specifications are consistent, then we will compute the percentage of models that are added after the refinement (or removed from the complement of the original specifiction)
			try {
				won_models_fitness = compute_won_models_porcentage(originalSpecification, chromosome.spec);
				System.out.print(won_models_fitness + " ");
			}
			catch (Exception e) { e.printStackTrace(); }
		}

		fitness += LOST_MODELS_FACTOR * lost_models_fitness + WON_MODELS_FACTOR * won_models_fitness + SYNTACTIC_FACTOR * syntactic_distance;
//		}

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
	private BigInteger countModels (LabelledFormula formula) throws IOException, InterruptedException {
		LinkedList<LabelledFormula> formulas = new LinkedList<>();
		formulas.add(formula);
		BigInteger numOfModels = CountREModels.count(formulas, this.BOUND, this.EXHAUSTIVE, true);

		return numOfModels;
	}

	private BigInteger countModels (List<LabelledFormula> formulas) throws IOException, InterruptedException {
		BigInteger numOfModels = CountREModels.count(formulas, this.BOUND, this.EXHAUSTIVE, true);
		return numOfModels;
	}
//	private BigInteger countModels (Formula formula, boolean positive) throws IOException, InterruptedException {
//		List<String> formulas = new LinkedList<String>();
//		formulas.add(toLambConvSyntax(formula));
//		String alph = null;
//		if (this.alphabet != null)
//			alph = this.alphabet.toString();
//		BigInteger numOfModels = Count.count(formulas, alph, this.BOUND, this.EXHAUSTIVE, positive);
//
//		return numOfModels;
//	}
//
//	private BigInteger countModels (List<Formula> constraints, boolean positive) throws IOException, InterruptedException {
//		List<String> formulas = new LinkedList<String>();
//		for (Formula f : constraints)
//			formulas.add(toLambConvSyntax(f));
////		String alph = "[";
////		for (int i = 0; i < originalSpecification.variables().size()-1; i++) {
////			alph += "p"+i+",";
////		}
////		alph += "p"+(originalSpecification.variables().size()-1)+"]";
//		String alph = null;
//		if (this.alphabet != null)
//			alph = this.alphabet.toString();
//		BigInteger numOfModels = Count.count(formulas, alph, this.BOUND, this.EXHAUSTIVE, positive);
//
//		return numOfModels;
//	}

	private double compute_lost_models_porcentage(Tlsf original, Tlsf refined) throws IOException, InterruptedException {
		System.out.print("-");
		if (originalNumOfModels == BigInteger.ZERO)
			return 1.0d;

		int numOfVars = original.variables().size();
		Formula refined_formula = refined.toFormula().formula();
		if (refined_formula == BooleanConstant.TRUE)
			return 1.0d;
		if (refined_formula == BooleanConstant.FALSE)
			return 0.0d;
		Formula lostModels = Conjunction.of(original.toFormula().formula(), refined_formula);
		if (lostModels == BooleanConstant.TRUE)
			return 1.0d;
		if (lostModels == BooleanConstant.FALSE)
			return 0.0d;
		LabelledFormula formula = LabelledFormula.of(lostModels, original.variables());
		BigDecimal numOfLostModels = new BigDecimal(countModels(formula));

		BigDecimal numOfModels = new BigDecimal(originalNumOfModels);

		BigDecimal res = numOfLostModels.divide(numOfModels, 2, RoundingMode.HALF_UP);
		double value = res.doubleValue();
		System.out.print(numOfLostModels + " " + numOfModels + " ");
		return value;
	}
	
	private double compute_won_models_porcentage(Tlsf original, Tlsf refined) throws IOException, InterruptedException {
		System.out.print("+");
		if (originalNegationNumOfModels == BigInteger.ZERO)
			return 1.0d;
		int numOfVars = original.variables().size();
		Formula refined_negated_formula = refined.toFormula().formula().not();
		if (refined_negated_formula == BooleanConstant.TRUE)
			return 1.0d;
		if (refined_negated_formula == BooleanConstant.FALSE)
			return 0.0d;
		Formula wonModels = Conjunction.of(original.toFormula().formula().not(), refined_negated_formula);
		if (wonModels == BooleanConstant.TRUE)
			return 1.0d;
		if (wonModels == BooleanConstant.FALSE)
			return 0.0d;
		LabelledFormula formula = LabelledFormula.of(wonModels, original.variables());
		BigDecimal numOfWonModels = new BigDecimal(countModels(formula));

		BigDecimal numOfNegationModels = new BigDecimal(originalNegationNumOfModels);

		BigDecimal res = numOfWonModels.divide(numOfNegationModels, 2, RoundingMode.HALF_UP);
		double value = res.doubleValue();
		System.out.print(numOfWonModels + " " + numOfNegationModels + " ");
		return value;
	}

	public double compute_syntactic_distance_size(Tlsf original, Tlsf refined) {
		double orig_size = Formula_Utils.formulaSize(original.toFormula().formula());
		double ref_size = Formula_Utils.formulaSize(refined.toFormula().formula());
		double diff = Math.abs(orig_size - ref_size);
		double syntactic_distance = (double) (1.0d - (diff / orig_size));
		return syntactic_distance;
	}
	
	private String toSolverSyntax(Formula f) {
		String LTLFormula = f.toString();
		LTLFormula = LTLFormula.replaceAll("\\!", "~");
		LTLFormula = LTLFormula.replaceAll("([A-Z])", " $1 ");
		return new String(LTLFormula); 
	}
	
	private String toLambConvSyntax(Formula f) {
		String LTLFormula = LabelledFormula.of(f, this.alphabet).toString();
		LTLFormula = LTLFormula.replaceAll("&", "&&");
		LTLFormula = LTLFormula.replaceAll("\\|", "||");
		return new String(LTLFormula); 
	}

	public static void print_config() {
		System.out.println(String.format("status: %s, lost%s, won: %s, syn: %s", STATUS_FACTOR, LOST_MODELS_FACTOR, WON_MODELS_FACTOR, SYNTACTIC_FACTOR));
	}
}
