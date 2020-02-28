package main;

import geneticalgorithm.AutomataBasedModelCountingSpecificationFitness;
import geneticalgorithm.SpecificationChromosome;
import owl.ltl.Conjunction;
import owl.ltl.Disjunction;
import owl.ltl.Formula;
import owl.ltl.GOperator;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.visitors.SolverSyntaxOperatorReplacer;
import solvers.LTLSolver;
import tlsf.TLSF_Utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GenuineSolutionsAnalysis {

    public static void main(String[] args) throws IOException, InterruptedException {
        List<Tlsf> genuineSolutions = new LinkedList<>();
        List<Tlsf> solutions = new LinkedList<>();
        String directoryName = "";
        Tlsf original = null;
        for (int i = 0; i< args.length; i++ ){
            if(args[i].startsWith("-ref=")){
                String ref_name = args[i].replace("-ref=","");
                Tlsf ref_sol = TLSF_Utils.toBasicTLSF(new File(ref_name));
                genuineSolutions.add(ref_sol);
            }
            else if (args[i].startsWith("-o=")){
                String orig_name = args[i].replace("-o=","");
                original= TLSF_Utils.toBasicTLSF(new File(orig_name));
            }
            else {
                directoryName = args[i];
                Stream<Path> walk = Files.walk(Paths.get(directoryName));
                List<String> specifications = walk.map(x -> x.toString())
                        .filter(f -> f.endsWith(".tlsf") && !f.endsWith("_basic.tlsf")).collect(Collectors.toList());
                for (String filename : specifications) {
                    System.out.println(filename);
                    FileReader f = new FileReader(filename);
                    Tlsf tlsf = TLSF_Utils.toBasicTLSF(new File(filename));
                    solutions.add(tlsf);
                }
            }
        }
        calculateGenuineStatistics(genuineSolutions, solutions);
        List<Double> sol_fitness = new LinkedList<>();
        if (original != null) {
            AutomataBasedModelCountingSpecificationFitness fitness = new AutomataBasedModelCountingSpecificationFitness(original);
            //compute statistics
            double bestFitness = 0.0d;
            double sumFitness = 0.0d;
            for (Tlsf sol : solutions) {
                SpecificationChromosome c = new SpecificationChromosome(sol);
                Double f = fitness.calculate(c);
                sol_fitness.add(f);
                if (bestFitness < f)
                    bestFitness = f;
                sumFitness += f;
            }

            System.out.println("Num. of Solutions:" + solutions.size() + "\n");
            System.out.println(String.format("Best fitness: %.2f\n", bestFitness));
            System.out.println(String.format("AVG fitness: %.2f\n", (sumFitness / (double)solutions.size())));

            double genuineBestFitness = 0.0d;
            double genuineAvgFitness = 0.0d;
            double moregeneralBestFitness = 0.0d;
            double moregeneralAvgFitness = 0.0d;
            double lessgeneralBestFitness = 0.0d;
            double lessgeneralAvgFitness = 0.0d;
            double genuinesSumFitness = 0.0d;
            double moregeneralSumFitness = 0.0d;
            double lessgeneralSumFitness = 0.0d;
            if (!genuineSolutions.isEmpty()) {
                System.out.println("Computing genuine statistics...");
                //check if some genuine solution has been found

                for(Integer index : GenuineSolutionsAnalysis.genuineSolutionsFound) {
                    genuinesSumFitness += sol_fitness.get(index);
                    if (sol_fitness.get(index) > genuineBestFitness)
                        genuineBestFitness = sol_fitness.get(index);
                }
                genuineAvgFitness = genuinesSumFitness / (double) GenuineSolutionsAnalysis.genuineSolutionsFound.size();
                for(Integer index : GenuineSolutionsAnalysis.moreGeneralSolutions) {
                    moregeneralSumFitness += sol_fitness.get(index);
                    if (sol_fitness.get(index) > moregeneralBestFitness)
                        moregeneralBestFitness = sol_fitness.get(index);
                }
                moregeneralAvgFitness = moregeneralSumFitness / (double)GenuineSolutionsAnalysis.moreGeneralSolutions.size();
                for(Integer index : GenuineSolutionsAnalysis.lessGeneralSolutions) {
                    lessgeneralSumFitness += sol_fitness.get(index);
                    if (sol_fitness.get(index) > lessgeneralBestFitness)
                        lessgeneralBestFitness = sol_fitness.get(index);
                }
                lessgeneralAvgFitness = lessgeneralSumFitness / (double)GenuineSolutionsAnalysis.lessGeneralSolutions.size();

                System.out.println("Genuine Solutions: " + GenuineSolutionsAnalysis.genuineSolutionsFound.size() + "\n");
                System.out.println("Genuine Solutions found: " + GenuineSolutionsAnalysis.genuineSolutionsFound.toString() + "\n");
                System.out.println(String.format("Best Genuine fitness: %.2f\n", genuineBestFitness));
                System.out.println(String.format("AVG Genuine fitness: %.2f\n", genuineAvgFitness));
                System.out.println("Weaker Solutions:" + GenuineSolutionsAnalysis.moreGeneralSolutions.size() + "\n");
                System.out.println("Weaker Solutions found:" + GenuineSolutionsAnalysis.moreGeneralSolutions.toString() + "\n");
                System.out.println(String.format("Best Weaker fitness: %.2f\n", moregeneralBestFitness));
                System.out.println(String.format("AVG Weaker fitness: %.2f\n", moregeneralAvgFitness));
                System.out.println("Stronger Solutions:" + GenuineSolutionsAnalysis.lessGeneralSolutions.size() + "\n");
                System.out.println("Stronger Solutions found:" + GenuineSolutionsAnalysis.lessGeneralSolutions.toString() + "\n");
                System.out.println(String.format("Best Stronger fitness: %.2f\n", lessgeneralBestFitness));
                System.out.println(String.format("AVG Stronger fitness: %.2f\n", lessgeneralAvgFitness));
                System.out.println(String.format("Genuine precision: %.2f \n",  ((double)GenuineSolutionsAnalysis.genuineSolutionsFound.size() / (double)genuineSolutions.size())));

            }
        }
    }

    public static Set<Integer> genuineSolutionsFound = new HashSet<>();

    public static Set<Integer> moreGeneralSolutions = new HashSet<>();

    public static Set<Integer> lessGeneralSolutions = new HashSet<>();


    public static void calculateGenuineStatistics(List<Tlsf> genuineSolutions, List<Tlsf> solutions) throws IOException, InterruptedException {
        SolverSyntaxOperatorReplacer visitor  = new SolverSyntaxOperatorReplacer();

        if (genuineSolutions.isEmpty() || solutions.isEmpty())
            return;
        //comparison with genuine solutions
        for (int i = 0; i < solutions.size(); i++) {
            Tlsf solution = solutions.get(i);
            System.out.print(".");
            if (genuineSolutions.contains(solution)) {
                genuineSolutionsFound.add(i);
            }
            else {
                for (Tlsf genuine : genuineSolutions) {
                    boolean isMoreGeneral = false;
                    boolean isLessGeneral = false;

                    Formula as_solution = solution.assume();
                    Formula g_solution = Conjunction.of(solution.guarantee());
                    Formula as_genuine = genuine.assume();
                    Formula g_genuine = Conjunction.of(genuine.guarantee());

                    //check isMoreGeneral?
                    //check as_solution => as_genuine = UNSAT(as_solution & !as_genuine)
                    LTLSolver.SolverResult sat = LTLSolver.isSAT(toSolverSyntax(Conjunction.of(as_solution, as_genuine.not()).accept(visitor)));
                    if (!sat.inconclusive() && sat == LTLSolver.SolverResult.UNSAT) {
                        //check g_genuine => g_solution = UNSAT(g_genuine & !g_solution)
                        sat = LTLSolver.isSAT(toSolverSyntax(Conjunction.of(g_genuine, g_solution.not()).accept(visitor)));
                        if (!sat.inconclusive() && sat == LTLSolver.SolverResult.UNSAT) {
                            isMoreGeneral = true;
                        }
                    }

                    //check isLessGeneral?
                    //check as_genuine => as_solution = UNSAT(as_genuine & !as_solution)
                    sat = LTLSolver.isSAT(toSolverSyntax(Conjunction.of(as_genuine,as_solution.not()).accept(visitor)));
                    if (!sat.inconclusive() && sat == LTLSolver.SolverResult.UNSAT) {
                        //check g_solution => g_genuine = UNSAT(g_solution & !g_genuine)
                        sat = LTLSolver.isSAT(toSolverSyntax(Conjunction.of(g_solution,g_genuine.not()).accept(visitor)));
                        if (!sat.inconclusive() && sat == LTLSolver.SolverResult.UNSAT) {
                            isLessGeneral = true;
                        }
                    }
                    if (isMoreGeneral && isLessGeneral && !genuineSolutionsFound.contains(i)) {
                        genuineSolutionsFound.add(i);
                        break;
                    }
                    else if (isMoreGeneral && !moreGeneralSolutions.contains(i)) {
                        moreGeneralSolutions.add(i);
                    }
                    else if (isLessGeneral && !lessGeneralSolutions.contains(i)) {
                        lessGeneralSolutions.add(i);
                    }
                }
            }
        }
        System.out.println();
    }

    private static String toSolverSyntax(Formula f) {
        String LTLFormula = f.toString();
        LTLFormula = LTLFormula.replaceAll("\\!", "~");
        LTLFormula = LTLFormula.replaceAll("([A-Z])", " $1 ");
        return new String(LTLFormula);
    }

}
