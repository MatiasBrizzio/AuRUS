package geneticalgorithm;

import com.google.common.collect.Sets;
import com.lagodiuk.ga.Fitness;
import geneticalgorithm.SpecificationChromosome.SPEC_STATUS;
import main.Settings;
import modelcounter.CountRltlConv;
import owl.ltl.*;
import owl.ltl.rewriter.NormalForms;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.visitors.SolverSyntaxOperatorReplacer;
import solvers.LTLSolver;
import solvers.LTLSolver.SolverResult;
import solvers.SolverUtils;
import solvers.StrixHelper;
import solvers.StrixHelper.RealizabilitySolverResult;
import tlsf.Formula_Utils;
import tlsf.TLSF_Utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

public class ModelCountingSpecificationFitness implements Fitness<SpecificationChromosome, Double> {
    private final SolverSyntaxOperatorReplacer visitor = new SolverSyntaxOperatorReplacer();
    public Tlsf originalSpecification;
    public List<String> alphabet = null;
    public SPEC_STATUS originalStatus;
    public BigInteger originalNumOfModels;

    public ModelCountingSpecificationFitness(Tlsf originalSpecification) throws IOException, InterruptedException {
        this.originalSpecification = originalSpecification;
//		generateAlphabet();
        SpecificationChromosome originalChromosome = new SpecificationChromosome(originalSpecification);
        compute_status(originalChromosome);
        this.originalStatus = originalChromosome.status;
        originalNumOfModels = countModels(originalSpecification.toFormula(), null);
    }

    private static double getStatusFitness(SpecificationChromosome chromosome) {
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
        return status_fitness;
    }

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
        } catch (Exception e) {
            e.printStackTrace();
        }

        double status_fitness = getStatusFitness(chromosome);


        double syntactic_distance;
        syntactic_distance = compute_syntactic_distance(originalSpecification, chromosome.spec);
        System.out.printf("s%.2f ", syntactic_distance);


//		if (syntactic_distance < 1.0d) {
        //if the specifications are not syntactically equivalent
        // Second, compute the portion of loosing models with respect to the original specification
        double lost_models_fitness = 0.0d; // if the current specification is inconsistent, then it looses all the models (it maintains 0% of models of the original specification)
        if (syntactic_distance < 1.0d && originalStatus.isSpecificationConsistent() && chromosome.status.isSpecificationConsistent()) {
            // if both specifications are consistent, then we will compute the percentage of models that are maintained after the refinement
            try {
                lost_models_fitness = compute_lost_models_porcentage(originalSpecification, chromosome.spec);
                System.out.print(lost_models_fitness + " ");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Third, compute the portion of winning models with respect to the original specification
        double won_models_fitness = 0.0d;
        if (syntactic_distance < 1.0d && originalStatus.isSpecificationConsistent() && chromosome.status.isSpecificationConsistent()) {
            // if both specifications are consistent, then we will compute the percentage of models that are added after the refinement (or removed from the complement of the original specifiction)
            try {
                won_models_fitness = compute_won_models_porcentage(originalSpecification, chromosome.spec);
                System.out.print(won_models_fitness + " ");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        double fitness = (Settings.STATUS_FACTOR * status_fitness) + (Settings.LOST_MODELS_FACTOR * lost_models_fitness) + (Settings.WON_MODELS_FACTOR * won_models_fitness) + (Settings.SYNTACTIC_FACTOR * syntactic_distance);
//		}
        System.out.printf("f%.2f ", fitness);
        chromosome.fitness = fitness;
        if (fitness > 1.0d) {
            System.out.printf("BEST Fitness: %.2f%n", fitness);
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

//					System.out.println(env_sys);
                    Formula env_sys2 = env_sys.accept(visitor);
//					System.out.println(env_sys2);

                    SolverResult sat = LTLSolver.isSAT(SolverUtils.toSolverSyntax(env_sys2));
                    if (!sat.inconclusive()) {
                        if (sat == SolverResult.UNSAT)
                            status = SPEC_STATUS.CONTRADICTORY;
                        else {
                            // check for realizability
                            RealizabilitySolverResult rel = StrixHelper.checkRealizability(spec);
                            if (!rel.inconclusive()) {
                                if (rel == RealizabilitySolverResult.REALIZABLE) {
                                    System.out.print("R");
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


    private BigInteger countModels(LabelledFormula formula1, LabelledFormula formula2) throws IOException, InterruptedException {
        LabelledFormula form;
        if (formula2 == null)
            form = LabelledFormula.of(formula1.formula(), formula1.variables());
        else {
            Formula cnf = NormalForms.toCnfFormula(Conjunction.of(formula1.formula(), formula2.formula()));
            form = LabelledFormula.of(cnf, formula1.variables());
        }
        CountRltlConv counter = new CountRltlConv();
        return counter.countPrefixes(form, Settings.MC_BOUND);
    }

    private double compute_lost_models_porcentage(Tlsf original, Tlsf refined) throws IOException, InterruptedException {
        System.out.print("-");
        BigDecimal numOfLostModels = new BigDecimal(countModels(original.toFormula(), refined.toFormula()));
        BigDecimal numOfModels = new BigDecimal(originalNumOfModels);
        BigDecimal res = numOfLostModels.divide(numOfModels, 2, RoundingMode.HALF_UP);
        double value = res.doubleValue();
        System.out.print(numOfLostModels + " " + numOfModels + " ");
        return value;
    }

    private double compute_won_models_porcentage(Tlsf original, Tlsf refined) throws IOException, InterruptedException {
        System.out.print("+");
        BigInteger refinedNumOfModels = countModels(refined.toFormula(), null);
        BigDecimal numOfWonModels = new BigDecimal(countModels(original.toFormula(), refined.toFormula()));
        BigDecimal numOfRefinedModels = new BigDecimal(refinedNumOfModels);
        BigDecimal res = numOfWonModels.divide(numOfRefinedModels, 2, RoundingMode.HALF_UP);
        double value = res.doubleValue();
        System.out.print(numOfWonModels + " " + numOfRefinedModels + " ");
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
        return 1.0d - 0.5d * (size_diff / orig_size) - 0.5d * (constraints_diff / orig_constraints_size);
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
        return 0.5d * size + 0.25d * lost + 0.25d * won;
    }


}
