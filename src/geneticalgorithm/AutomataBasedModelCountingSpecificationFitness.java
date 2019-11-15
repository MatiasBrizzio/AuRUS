package geneticalgorithm;

import com.google.common.collect.Sets;
import com.lagodiuk.ga.Fitness;
import geneticalgorithm.SpecificationChromosome.SPEC_STATUS;
import gov.nasa.ltl.trans.ParseErrorException;
import main.Settings;
import modelcounter.AutomataBasedModelCounting;
import modelcounter.CountRltlConv;
import modelcounter.EmersonLeiAutomatonBasedModelCounting;
import owl.ltl.*;
import owl.ltl.parser.LtlParser;
import owl.ltl.rewriter.NormalForms;
import owl.ltl.rewriter.SyntacticSimplifier;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.visitors.SolverSyntaxOperatorReplacer;
import solvers.LTLSolver;
import solvers.LTLSolver.SolverResult;
import solvers.StrixHelper;
import solvers.StrixHelper.RealizabilitySolverResult;
import tlsf.Formula_Utils;
import tlsf.TLSF_Utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class AutomataBasedModelCountingSpecificationFitness implements Fitness<SpecificationChromosome, Double> {

//	public int BOUND = 10;
//	public boolean EXHAUSTIVE = true;
//	public double STATUS_FACTOR = 0.7d;
//	public double LOST_MODELS_FACTOR = 0.1d;
//	public double WON_MODELS_FACTOR = 0.1d;
//	//	public static final double SOLUTION = 0.8d;
//	public  double SYNTACTIC_FACTOR = 0.1d;
	public Tlsf originalSpecification = null;
	public List<String> alphabet = null;
	public SPEC_STATUS originalStatus = SPEC_STATUS.UNKNOWN;
//    public boolean allowAssumptionGuaranteeRemoval = false;
	public BigInteger originalNumOfModels;
//	public BigInteger originalNegationNumOfModels;
//    public BigInteger UNIVERSE;

	public AutomataBasedModelCountingSpecificationFitness(Tlsf originalSpecification) throws IOException, InterruptedException {
		this.originalSpecification = originalSpecification;
		SpecificationChromosome originalChromosome = new SpecificationChromosome(originalSpecification);
		compute_status(originalChromosome);
		this.originalStatus = originalChromosome.status;
		System.out.println("Initial specification is: " + originalStatus);
		originalNumOfModels = countModels(originalSpecification.toFormula());
//        originalNegationNumOfModels = countModels(originalSpecification.toFormula().not());
//        UNIVERSE = countModels(LtlParser.parse("true",originalSpecification.variables()));
    }

//	public void setFactors(double status_factor,  double syntactic_factor, double semantic_factor) {
//		if (status_factor >= 0.0d)
//			this.STATUS_FACTOR = status_factor;
//		if (syntactic_factor >= 0.0d)
//			this.SYNTACTIC_FACTOR = syntactic_factor;
//		if (semantic_factor >= 0.0d) {
//			double factor = semantic_factor/2.0d;
//			this.LOST_MODELS_FACTOR = factor;
//			this.WON_MODELS_FACTOR = factor;
//		}
//	}

//	public void setBound (int bound) {
//		if (bound > 0)
//			 this.BOUND = bound;
//	}

//	public void allowAssumptionGuaranteeRemoval(boolean value) {
//		this.allowAssumptionGuaranteeRemoval = value;
//	}

	private void generateAlphabet () {
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

		if (!Settings.allowAssumptionGuaranteeRemoval) {
			boolean somethingRemoved = somethingHasBeenRemoved(originalSpecification, chromosome.spec);
			if (somethingRemoved)
				return 0.0d;
		}
		// First compute the status fitness
		try {
			compute_status(chromosome);
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		double status_fitness = 0.0d;
		if (chromosome.status == SPEC_STATUS.UNKNOWN || chromosome.status == SPEC_STATUS.BOTTOM)
			status_fitness = 0.0d;
		else if (chromosome.status == SPEC_STATUS.GUARANTEES)
			status_fitness = 0.05d;
		else if (chromosome.status == SPEC_STATUS.ASSUMPTIONS)
			status_fitness = 0.1d;
		else if (chromosome.status == SPEC_STATUS.CONTRADICTORY)
			status_fitness = 0.2d;
		else if (chromosome.status == SPEC_STATUS.UNREALIZABLE)
			status_fitness = 0.5d;
		else if (chromosome.status == SPEC_STATUS.REALIZABLE)
			status_fitness = 1.0d;

		

		double syntactic_distance = 0.0d;
		if (Settings.SYNTACTIC_FACTOR > 0.0d)
			syntactic_distance = compute_syntactic_distance(originalSpecification, chromosome.spec);
		System.out.printf("s%.2f ", syntactic_distance);


//		if (syntactic_distance < 1.0d) {
		//if the specifications are not syntactically equivalent
		// Second, compute the portion of loosing models with respect to the original specification
		double lost_models_fitness = 0.0d; // if the current specification is inconsistent, then it looses all the models (it maintains 0% of models of the original specification)
		if (syntactic_distance < 1.0d &&  Settings.LOST_MODELS_FACTOR > 0.0d && originalStatus.isSpecificationConsistent() && chromosome.status.isSpecificationConsistent()) {
			// if both specifications are consistent, then we will compute the percentage of models that are maintained after the refinement
			try {
				lost_models_fitness = compute_lost_models_porcentage(originalSpecification, chromosome.spec);
				System.out.print(lost_models_fitness + " ");
			}
			catch (Exception e) { e.printStackTrace(); }
		}

		// Third, compute the portion of winning models with respect to the original specification
		double won_models_fitness = 0.0d;
		if (syntactic_distance < 1.0d &&  Settings.WON_MODELS_FACTOR > 0.0d && originalStatus.isSpecificationConsistent() && chromosome.status.isSpecificationConsistent()) {
			// if both specifications are consistent, then we will compute the percentage of models that are added after the refinement (or removed from the complement of the original specifiction)
			try {
				won_models_fitness = compute_won_models_porcentage(originalSpecification, chromosome.spec);
				System.out.print(won_models_fitness + " ");
			}
			catch (Exception e) { e.printStackTrace(); }
		}

		double fitness = (Settings.STATUS_FACTOR * status_fitness) + (Settings.LOST_MODELS_FACTOR * lost_models_fitness) + (Settings.WON_MODELS_FACTOR * won_models_fitness) + (Settings.SYNTACTIC_FACTOR * syntactic_distance);
//		}
		System.out.printf("f%.2f ",fitness);
		chromosome.fitness = fitness;
		if (fitness > 1.0d) {
			System.out.println(String.format("BROKEN Fitness: %.2f",fitness));
			System.out.println(TLSF_Utils.adaptTLSFSpec(chromosome.spec));
			throw new RuntimeException();
		}
			
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
//					Formula env_sys = spec.toFormula().formula();
					//check if initial states and safety properties are consistent
					Formula env_sys = Conjunction.of(spec.initially(), GOperator.of(spec.require()), spec.preset(), GOperator.of(Conjunction.of(spec.assert_())), spec.assume(), Conjunction.of(spec.guarantee()));


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
									System.out.print("R" );
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
		SyntacticSimplifier simp = new SyntacticSimplifier();
		Formula simplified = formula.formula().accept(simp);
		if (simplified == BooleanConstant.FALSE)
			return BigInteger.ZERO;
		LabelledFormula simp_formula = LabelledFormula.of(simplified, formula.variables());
//		AutomataBasedModelCounting counter = new AutomataBasedModelCounting(simp_formula, Settings.MC_EXHAUSTIVE);
		EmersonLeiAutomatonBasedModelCounting counter = new EmersonLeiAutomatonBasedModelCounting(simp_formula);
		BigInteger numOfModels = counter.count(Settings.MC_BOUND);
		return numOfModels;
	}

	private double compute_lost_models_porcentage(Tlsf original, Tlsf refined) throws IOException, InterruptedException {
		System.out.print("-");
		if (originalNumOfModels == BigInteger.ZERO)
			return 0.0d;

		Formula refined_formula = refined.toFormula().formula();
		if (refined_formula == BooleanConstant.TRUE)
			return 0.0d;
		if (refined_formula == BooleanConstant.FALSE)
			return 1.0d;
		Formula lostModels = Conjunction.of(original.toFormula().formula(), refined_formula);
		if (lostModels == BooleanConstant.TRUE)
			return 0.0d;
		if (lostModels == BooleanConstant.FALSE)
			return 1.0d;

		LabelledFormula formula = LabelledFormula.of(lostModels, original.variables());
		BigDecimal numOfLostModels = new BigDecimal(countModels(formula));
		//patch to avoid computing again this value;
		commonNumOfModels = numOfLostModels;
		BigDecimal numOfModels = new BigDecimal(originalNumOfModels);
//        BigDecimal numOfModels = new BigDecimal(UNIVERSE);

		BigDecimal res = numOfLostModels.divide(numOfModels, 2, RoundingMode.HALF_UP);
		double value = res.doubleValue();
		System.out.print(numOfLostModels + " " + numOfModels + " ");
		if (res.doubleValue() > 1.0d) {
//			System.out.println("\nBROKEN formula: " + formula);
//			throw new RuntimeException("lost models major than 1.0: " + refined.toFormula());
			System.out.println("\nWARNING: increase the bound. ");
			return 1.0d;
		}
		return value;
	}

	private BigDecimal commonNumOfModels = null;
	private double compute_won_models_porcentage(Tlsf original, Tlsf refined) throws IOException, InterruptedException {
		System.out.print("+");
		if (refined.toFormula().formula() == BooleanConstant.FALSE)
			return 0.0d;

		BigInteger refinedNumOfModels = countModels(refined.toFormula());
		if (refinedNumOfModels == BigInteger.ZERO)
			return 0.0d;

//		int numOfVars = original.variables().size();
		Formula original_formula = original.toFormula().formula();
//		if (original_formula == BooleanConstant.TRUE)
//			return 1.0d;
//		if (original_formula == BooleanConstant.FALSE)
//			return 0.0d;
		Formula wonModels = Conjunction.of(original_formula, refined.toFormula().formula());
		if (wonModels == BooleanConstant.TRUE)
			return 0.0d;
		if (wonModels == BooleanConstant.FALSE)
			return 1.0d;
		LabelledFormula formula = LabelledFormula.of(wonModels, original.variables());
//		System.out.println("WON: "+formula);
		//patch to avoid computing again this value;
		BigDecimal numOfWonModels = null;
		if (commonNumOfModels != null)
			numOfWonModels = commonNumOfModels;
		else
			numOfWonModels = new BigDecimal(countModels(formula));
		commonNumOfModels = null;
		BigDecimal numOfRefinedModels = new BigDecimal(refinedNumOfModels);
//        BigDecimal numOfRefinedModels = new BigDecimal(UNIVERSE);
//        BigDecimal numOfRefinedModels = new BigDecimal(originalNegationNumOfModels);
		BigDecimal res = numOfWonModels.divide(numOfRefinedModels, 2, RoundingMode.HALF_UP);

		double value = res.doubleValue();
		System.out.print(numOfWonModels + " " + numOfRefinedModels + " ");
		if (res.doubleValue() > 1.0d) {
			System.out.println("\nWARNING: increase the bound. ");
			return 1.0d;
		}
		return value;
	}


	public double compute_syntactic_distance_size(Tlsf original, Tlsf refined) {
		Formula f0 = original.toFormula().formula();
	    Formula f1 = refined.toFormula().formula();
		double orig_size = Formula_Utils.formulaSize(f0);
		double ref_size = Formula_Utils.formulaSize(f1);
		double orig_constraints_size = original.toAssertGuaranteeConjuncts().size();
		double ref_constraints_size = refined.toAssertGuaranteeConjuncts().size();
		
		double size_diff = Math.abs(orig_size - ref_size);
		double constraints_diff = Math.abs(orig_constraints_size - ref_constraints_size);
		double syntactic_distance = (double) (1.0d - 0.5d*(size_diff / orig_size) - 0.5d*(constraints_diff / orig_constraints_size));
		return syntactic_distance;
	}

    public double compute_syntactic_distance_ast(Tlsf original, Tlsf refined) {
        Formula f0 = original.toFormula().formula();
        Formula f1 = refined.toFormula().formula();
        double d = 3.0d;
        if (f0.height() != f1.height())
            d--;
        int orig_size = Formula_Utils.formulaSize(f0);
        int ref_size = Formula_Utils.formulaSize(f1);
        if (orig_size != ref_size)
            d--;
        int diff_compare = Formulas.compare(Set.of(f0), Set.of(f1));
        if (diff_compare != 0)
            d--;
        double syntactic_distance = (double) (d / 3.0d);
        return syntactic_distance;
    }

	public double compute_syntactic_distance(Tlsf original, Tlsf refined) {
		List<LabelledFormula> sub_original = Formula_Utils.subformulas(original.toFormula());
//		sub_original.remove(original.toFormula());
		List<LabelledFormula> sub_refined = Formula_Utils.subformulas(refined.toFormula());
//		sub_refined.remove(refined.toFormula());

//		Set<LabelledFormula> lostSubs = Sets.difference(Sets.newHashSet(sub_original), Sets.newHashSet(sub_refined));
//		Set<LabelledFormula> wonSubs = Sets.difference(Sets.newHashSet(sub_refined), Sets.newHashSet(sub_original));
		Set<LabelledFormula> commonSubs = Sets.intersection(Sets.newHashSet(sub_original), Sets.newHashSet(sub_refined));
//		String originalStr = original.toFormula().toString();
//		String refinedStr = refined.toFormula().toString();
//		String diffLost = StringUtils.difference(originalStr, refinedStr);
//		System.out.println(lostSubs.size() +" " + sub_original.size());
//		String diffWon = StringUtils.difference(refinedStr, originalStr);
//		System.out.println(wonSubs.size()  +" " + sub_refined.size());
		double lost = ((double) commonSubs.size()) / ((double) sub_original.size());
		double won = ((double) commonSubs.size()) / ((double) sub_refined.size());
		double size = compute_syntactic_distance_size(original, refined);
		double syntactic_distance =  0.5d * size +  0.25d * lost + 0.25d * won;
		return syntactic_distance;
	}

	public boolean somethingHasBeenRemoved(Tlsf original, Tlsf refined) {
		boolean assumptionRemoved = Formula_Utils.splitConjunction(original.assume()).size() > Formula_Utils.splitConjunction(refined.assume()).size() ;
		boolean guaranteeRemoved =  original.guarantee().size() > refined.guarantee().size();
		return assumptionRemoved || guaranteeRemoved;
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

	public  void print_config() {
		System.out.println(String.format("status: %s, lost: %s, won: %s, syn: %s, rem: %s", Settings.STATUS_FACTOR, Settings.LOST_MODELS_FACTOR, Settings.WON_MODELS_FACTOR, Settings.SYNTACTIC_FACTOR,Settings.allowAssumptionGuaranteeRemoval));
	}
}
