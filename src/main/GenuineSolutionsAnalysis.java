package main;

import geneticalgorithm.AutomataBasedModelCountingSpecificationFitness;
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
        for (int i = 0; i< args.length; i++ ){
            if(args[i].startsWith("-ref=")){
                String ref_name = args[i].replace("-ref=","");
                Tlsf ref_sol = TLSF_Utils.toBasicTLSF(new File(ref_name));
                genuineSolutions.add(ref_sol);
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
                    if (isMoreGeneral && isLessGeneral) {
                        genuineSolutionsFound.add(i);
                    }
                    else if (isMoreGeneral) {
                        moreGeneralSolutions.add(i);
                    }
                    else if (isLessGeneral) {
                        lessGeneralSolutions.add(i);
                    }

                    if (isLessGeneral || isMoreGeneral)
                        break;
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
