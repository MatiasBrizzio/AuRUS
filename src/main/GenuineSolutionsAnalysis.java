package main;

import geneticalgorithm.AutomataBasedModelCountingSpecificationFitness;
import geneticalgorithm.SpecificationChromosome;
import owl.ltl.Conjunction;
import owl.ltl.Formula;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.visitors.SolverSyntaxOperatorReplacer;
import solvers.LTLSolver;
import solvers.SolverUtils;
import tlsf.TLSF_Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GenuineSolutionsAnalysis {

    public static Set<Integer> genuineSolutionsFound = new HashSet<>();
    public static Set<Integer> moreGeneralSolutions = new HashSet<>();
    public static Set<Integer> lessGeneralSolutions = new HashSet<>();
    public static boolean computeFitness = true;

    public static void main(String[] args) throws IOException, InterruptedException {
        List<Tlsf> genuineSolutions = new LinkedList<>();
        List<Tlsf> solutions = new LinkedList<>();
        List<Double> sol_fitness = new LinkedList<>();
        String directoryName;
        Tlsf original = null;
        for (String arg : args) {
            if (arg.startsWith("-ref=")) {
                String ref_name = arg.replace("-ref=", "");
                Tlsf ref_sol = TLSF_Utils.toBasicTLSF(new File(ref_name));
                genuineSolutions.add(ref_sol);
            } else if (arg.startsWith("-o=")) {
                String orig_name = arg.replace("-o=", "");
                original = TLSF_Utils.toBasicTLSF(new File(orig_name));
            } else if (arg.startsWith("-noFit")) {
                computeFitness = false;
            } else {
                directoryName = arg;
                Stream<Path> walk = Files.walk(Paths.get(directoryName));
                List<String> specifications = walk.map(Path::toString)
                        .filter(f -> f.endsWith(".tlsf") && !f.endsWith("_basic.tlsf")).collect(Collectors.toList());
                for (String filename : specifications) {
                    System.out.println(filename);
                    Tlsf tlsf = TLSF_Utils.toBasicTLSF(new File(filename));
                    solutions.add(tlsf);
                    //read the fitness from file
                    if (!computeFitness) {
                        FileReader f = new FileReader(filename);
                        BufferedReader in = new BufferedReader(f);
                        String aux;
                        double value = 0.0d;
                        while ((aux = in.readLine()) != null) {
                            if ((aux.startsWith("//fitness"))) {
                                value = Double.parseDouble(aux.substring(10));
                            }
                        }
                        sol_fitness.add(value);
                    }
                }
            }
        }
        calculateGenuineStatistics(genuineSolutions, solutions);

        if (original != null) {
            if (computeFitness) {
                AutomataBasedModelCountingSpecificationFitness fitness = new AutomataBasedModelCountingSpecificationFitness(original);
                for (Tlsf sol : solutions) {
                    SpecificationChromosome c = new SpecificationChromosome(sol);
                    Double f = fitness.calculate(c);
                    sol_fitness.add(f);
                }
            }
            //compute statistics
            double bestFitness = 0.0d;
            double sumFitness = 0.0d;
            for (Double f : sol_fitness) {
                if (bestFitness < f)
                    bestFitness = f;
                sumFitness += f;
            }
            System.out.println();
            System.out.println("Num. of Solutions:        " + solutions.size() + "\n");
            System.out.printf("Best fitness: %.2f\n%n", bestFitness);
            System.out.printf("AVG fitness: %.2f\n%n", (sumFitness / (double) solutions.size()));

            double genuineBestFitness = 0.0d;
            double genuineAvgFitness;
            double moregeneralBestFitness = 0.0d;
            double moregeneralAvgFitness;
            double lessgeneralBestFitness = 0.0d;
            double lessgeneralAvgFitness;
            double genuinesSumFitness = 0.0d;
            double moregeneralSumFitness = 0.0d;
            double lessgeneralSumFitness = 0.0d;
            if (!genuineSolutions.isEmpty()) {
                System.out.println("Computing genuine statistics...");
                //check if some genuine solution has been found

                for (Integer index : GenuineSolutionsAnalysis.genuineSolutionsFound) {
                    genuinesSumFitness += sol_fitness.get(index);
                    if (sol_fitness.get(index) > genuineBestFitness)
                        genuineBestFitness = sol_fitness.get(index);
                }
                genuineAvgFitness = genuinesSumFitness / (double) GenuineSolutionsAnalysis.genuineSolutionsFound.size();
                for (Integer index : GenuineSolutionsAnalysis.moreGeneralSolutions) {
                    moregeneralSumFitness += sol_fitness.get(index);
                    if (sol_fitness.get(index) > moregeneralBestFitness)
                        moregeneralBestFitness = sol_fitness.get(index);
                }
                moregeneralAvgFitness = moregeneralSumFitness / (double) GenuineSolutionsAnalysis.moreGeneralSolutions.size();
                for (Integer index : GenuineSolutionsAnalysis.lessGeneralSolutions) {
                    lessgeneralSumFitness += sol_fitness.get(index);
                    if (sol_fitness.get(index) > lessgeneralBestFitness)
                        lessgeneralBestFitness = sol_fitness.get(index);
                }
                lessgeneralAvgFitness = lessgeneralSumFitness / (double) GenuineSolutionsAnalysis.lessGeneralSolutions.size();

                System.out.println("Genuine Solutions:       " + GenuineSolutionsAnalysis.genuineSolutionsFound.size() + "\n");
                System.out.println("Genuine Solutions found:   " + GenuineSolutionsAnalysis.genuineSolutionsFound.toString() + "\n");
                System.out.printf("Best Genuine fitness: %.2f\n%n", genuineBestFitness);
                System.out.printf("AVG Genuine fitness: %.2f\n%n", genuineAvgFitness);
                System.out.println("Weaker Solutions:       " + GenuineSolutionsAnalysis.moreGeneralSolutions.size() + "\n");
                System.out.println("Weaker Solutions found:   " + GenuineSolutionsAnalysis.moreGeneralSolutions.toString() + "\n");
                System.out.printf("Best Weaker fitness: %.2f\n%n", moregeneralBestFitness);
                System.out.printf("AVG Weaker fitness: %.2f\n%n", moregeneralAvgFitness);
                System.out.println("Stronger Solutions:       " + GenuineSolutionsAnalysis.lessGeneralSolutions.size() + "\n");
                System.out.println("Stronger Solutions found:   " + GenuineSolutionsAnalysis.lessGeneralSolutions.toString() + "\n");
                System.out.printf("Best Stronger fitness: %.2f\n%n", lessgeneralBestFitness);
                System.out.printf("AVG Stronger fitness: %.2f\n%n", lessgeneralAvgFitness);
                System.out.printf("Genuine precision: %.2f \n%n", ((double) GenuineSolutionsAnalysis.genuineSolutionsFound.size() / (double) genuineSolutions.size()));
            }
        }
        System.exit(0);
    }

    public static void calculateGenuineStatistics(List<Tlsf> genuineSolutions, List<Tlsf> solutions) throws IOException, InterruptedException {
        SolverSyntaxOperatorReplacer visitor = new SolverSyntaxOperatorReplacer();

        if (genuineSolutions.isEmpty() || solutions.isEmpty())
            return;
        //comparison with genuine solutions
        for (int i = 0; i < solutions.size(); i++) {
            Tlsf solution = solutions.get(i);
            System.out.print(".");
            if (genuineSolutions.contains(solution)) {
                genuineSolutionsFound.add(i);
            } else {
                for (Tlsf genuine : genuineSolutions) {
                    boolean isMoreGeneral = false;
                    boolean isLessGeneral = false;

                    Formula as_solution = solution.assume();
                    Formula g_solution = Conjunction.of(solution.guarantee());
                    Formula as_genuine = genuine.assume();
                    Formula g_genuine = Conjunction.of(genuine.guarantee());

                    //check isMoreGeneral?
                    //check as_solution => as_genuine = UNSAT(as_solution & !as_genuine)
                    LTLSolver.SolverResult sat = LTLSolver.isSAT(SolverUtils.toSolverSyntax(Conjunction.of(as_solution, as_genuine.not()).accept(visitor)));
                    if (!sat.inconclusive() && sat == LTLSolver.SolverResult.UNSAT) {
                        //check g_genuine => g_solution = UNSAT(g_genuine & !g_solution)
                        sat = LTLSolver.isSAT(SolverUtils.toSolverSyntax(Conjunction.of(g_genuine, g_solution.not()).accept(visitor)));
                        if (!sat.inconclusive() && sat == LTLSolver.SolverResult.UNSAT) {
                            isMoreGeneral = true;
                        }
                    }

                    //check isLessGeneral?
                    //check as_genuine => as_solution = UNSAT(as_genuine & !as_solution)
                    sat = LTLSolver.isSAT(SolverUtils.toSolverSyntax(Conjunction.of(as_genuine, as_solution.not()).accept(visitor)));
                    if (!sat.inconclusive() && sat == LTLSolver.SolverResult.UNSAT) {
                        //check g_solution => g_genuine = UNSAT(g_solution & !g_genuine)
                        sat = LTLSolver.isSAT(SolverUtils.toSolverSyntax(Conjunction.of(g_solution, g_genuine.not()).accept(visitor)));
                        if (!sat.inconclusive() && sat == LTLSolver.SolverResult.UNSAT) {
                            isLessGeneral = true;
                        }
                    }
                    if (isMoreGeneral && isLessGeneral && !genuineSolutionsFound.contains(i)) {
                        genuineSolutionsFound.add(i);
                        break;
                    } else if (isMoreGeneral && !moreGeneralSolutions.contains(i)) {
                        moreGeneralSolutions.add(i);
                    } else if (isLessGeneral) {
                        lessGeneralSolutions.add(i);
                    }
                }
            }
        }
        System.out.println();
    }

}
