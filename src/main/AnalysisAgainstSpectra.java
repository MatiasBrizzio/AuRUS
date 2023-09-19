package main;

import owl.ltl.Conjunction;
import owl.ltl.Formula;
import owl.ltl.GOperator;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.visitors.SolverSyntaxOperatorReplacer;
import solvers.LTLSolver;
import solvers.LTLSolver.SolverResult;
import solvers.SolverUtils;
import tlsf.TLSF_Utils;

import java.io.File;
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

public class AnalysisAgainstSpectra {

    private static final Set<Integer> genuineSolutionsFound = new HashSet<>();
    private static final Set<Integer> moreGeneralSolutions = new HashSet<>();
    private static final Set<Integer> lessGeneralSolutions = new HashSet<>();
    private static Tlsf original;
    private static boolean rmSpecsWithsameGuarantees;
    private static int originalTargetSizeWORemoveGuaratees = 0;
    private static int originalSourceSizeWORemoveGuaratees = 0;

    public static void main(String[] args) throws IOException, InterruptedException {
        List<Tlsf> sourceSolutions = new LinkedList<>();
        List<Tlsf> targetSolutions = new LinkedList<>();
        String directoryName;
        for (String arg : args) {

            if (arg.startsWith("-ref=")) {
                directoryName = arg.replace("-ref=", "");
                @SuppressWarnings("resource")
                Stream<Path> walk = Files.walk(Paths.get(directoryName));
                List<String> specifications = walk.map(Path::toString)
                        .filter(f -> f.endsWith(".spectra")).collect(Collectors.toList());

                if (specifications.isEmpty()) {
                    @SuppressWarnings("resource")
                    Stream<Path> walk2 = Files.walk(Paths.get(directoryName));
                    specifications = walk2.map(Path::toString)
                            .filter(f -> f.endsWith(".tlsf")).collect(Collectors.toList());
                }

                for (String filename : specifications) {
                    Tlsf tlsf = TLSF_Utils.toBasicTLSF(new File(filename));
                    targetSolutions.add(tlsf);
                }
                System.out.println("target Solutions: " + targetSolutions.size());
            } else if (arg.startsWith("-original=")) {
                original = TLSF_Utils.toBasicTLSF(new File(arg.replace("-original=", "")));
            } else if (arg.startsWith("-remove-same-guarantees")) {
                rmSpecsWithsameGuarantees = true;
            } else if (arg.startsWith("-satTO=")) {
                int timeout = Integer.parseInt(arg.replace("-satTO=", ""));
                if (timeout > 0) Settings.SAT_TIMEOUT = timeout;
            } else if (arg.startsWith("-source=")) {
                directoryName = arg.replace("-source=", "");
                @SuppressWarnings("resource")
                Stream<Path> walk = Files.walk(Paths.get(directoryName));
                List<String> specifications = walk.map(Path::toString)
                        .filter(f -> f.endsWith(".tlsf") && !f.endsWith("_basic.tlsf")).collect(Collectors.toList());
                if (specifications.isEmpty()) {
                    @SuppressWarnings("resource")
                    Stream<Path> walk2 = Files.walk(Paths.get(directoryName));
                    specifications = walk2.map(Path::toString)
                            .filter(f -> f.endsWith(".spectra")).collect(Collectors.toList());
                }
                for (String filename : specifications) {
                    Tlsf tlsf = TLSF_Utils.toBasicTLSF(new File(filename));
                    sourceSolutions.add(tlsf);
                }
                System.out.println("source Solutions: " + sourceSolutions.size());
            }
        }
        System.out.println("SAT TO: " + Settings.SAT_TIMEOUT);
        calculateGenuineStatistics(sourceSolutions, targetSolutions);

        int equivalentSolutions = AnalysisAgainstSpectra.genuineSolutionsFound.size();
        int moreGeneralSolutions = AnalysisAgainstSpectra.moreGeneralSolutions.size();
        int lessGeneralSolutions = AnalysisAgainstSpectra.lessGeneralSolutions.size();

        System.out.println("Computing statistics...");
        System.out.println("source Solutions found: " + sourceSolutions.size());
        System.out.println("target solutions found: " + targetSolutions.size());
        if (rmSpecsWithsameGuarantees) {
            System.out.println("Amount of source repairs with guarantees modified:       " + (originalSourceSizeWORemoveGuaratees - sourceSolutions.size()) + "\n");
            System.out.println("Amount of target repairs with guarantees modified:       " + (originalTargetSizeWORemoveGuaratees - targetSolutions.size()) + "\n");
        }
        System.out.println("Percentage of Genuine Solutions:       " + (double) equivalentSolutions / (double) sourceSolutions.size() + "\n");
        System.out.println("Percentage of Weaker Solutions:       " + (double) moreGeneralSolutions / (double) sourceSolutions.size() + "\n");
        System.out.println("Percentage of Stronger Solutions:       " + (double) lessGeneralSolutions / (double) sourceSolutions.size() + "\n");
        System.out.println("Genuine Solutions:       " + AnalysisAgainstSpectra.genuineSolutionsFound.toString() + "\n");
        System.out.println("Weaker Solutions:       " + AnalysisAgainstSpectra.moreGeneralSolutions.toString() + "\n");
        System.out.println("Stronger Solutions:       " + AnalysisAgainstSpectra.lessGeneralSolutions.toString() + "\n");
        System.out.println("Genuine Solutions:       " + equivalentSolutions + "\n");
        System.out.println("Weaker Solutions:       " + moreGeneralSolutions + "\n");
        System.out.println("Stronger Solutions:       " + lessGeneralSolutions + "\n");

    }

    public static void calculateGenuineStatistics(List<Tlsf> sourceSolutions, List<Tlsf> targetSolutions) throws IOException, InterruptedException {
        SolverSyntaxOperatorReplacer visitor = new SolverSyntaxOperatorReplacer();
        if (sourceSolutions.isEmpty() || targetSolutions.isEmpty())
            return;
        if (rmSpecsWithsameGuarantees) {
            List<Tlsf> aux;
            originalSourceSizeWORemoveGuaratees = sourceSolutions.size();
            originalTargetSizeWORemoveGuaratees = targetSolutions.size();
            System.out.println(originalSourceSizeWORemoveGuaratees + "  " + originalTargetSizeWORemoveGuaratees);
            aux = removeSameGuarantees(sourceSolutions);
            sourceSolutions.clear();
            sourceSolutions.addAll(aux);
            aux = removeSameGuarantees(targetSolutions);
            targetSolutions.clear();
            targetSolutions.addAll(aux);
        }

        for (int i = 0; i < sourceSolutions.size(); i++) {
            Tlsf solution = sourceSolutions.get(i);
            System.out.print(".");
            if (targetSolutions.contains(solution)) {
                genuineSolutionsFound.add(i);
            } else {
                int nOfEquivalentSolutions = 0;
                int nOfWeakerSolutions = 0;
                int nOfStrongerSolutions = 0;
                for (Tlsf genuine : targetSolutions) {
                    System.out.print("-");
                    boolean isMoreGeneral = false;
                    boolean isLessGeneral = false;

                    // Env = initially && G(require) & assume
                    Formula as_solution = Conjunction.of(solution.assume(), solution.initially(), GOperator.of(solution.require()));
                    // Env = initially && G(require) & assume
                    Formula as_genuine = Conjunction.of(genuine.assume(), genuine.initially(), GOperator.of(genuine.require()));

                    //check isLessGeneral?
                    //check as_solution => as_genuine = UNSAT(as_solution & !as_genuine)
                    LTLSolver.SolverResult sat = LTLSolver.isSAT(SolverUtils.toSolverSyntax(Conjunction.of(as_solution, as_genuine.not()).accept(visitor)));
                    if (sat.inconclusive()) System.out.print(sat);
                    if (!sat.inconclusive() && sat == LTLSolver.SolverResult.UNSAT)
                        isLessGeneral = true;

                    //check isMoreGeneral?
                    //check as_genuine => as_solution = UNSAT(as_genuine & !as_solution)
                    sat = LTLSolver.isSAT(SolverUtils.toSolverSyntax(Conjunction.of(as_genuine, as_solution.not()).accept(visitor)));
                    if (sat.inconclusive()) System.out.print(sat);
                    if (!sat.inconclusive() && sat == LTLSolver.SolverResult.UNSAT)
                        isMoreGeneral = true;

                    if (isMoreGeneral && isLessGeneral && !genuineSolutionsFound.contains(i)) {
                        genuineSolutionsFound.add(i);
                        System.out.print("E");
                        nOfEquivalentSolutions++;
                    } else if (isMoreGeneral && !moreGeneralSolutions.contains(i)) {
                        moreGeneralSolutions.add(i);
                        System.out.print("W");
                        nOfWeakerSolutions++;
                    } else if (isLessGeneral && !lessGeneralSolutions.contains(i)) {
                        lessGeneralSolutions.add(i);
                        System.out.print("S");
                        nOfStrongerSolutions++;
                    }
                }
                System.out.println("\n Source repair " + i + " is equivalent to, weaker and stronger than: " + nOfEquivalentSolutions + " " + nOfWeakerSolutions + " " + nOfStrongerSolutions);
            }
        }
        System.out.println();
    }

    private static boolean guaranteesHaveBeenModified(Formula f1, Formula f2) throws IOException, InterruptedException {
        SolverSyntaxOperatorReplacer visitor = new SolverSyntaxOperatorReplacer();
        // check equivalence
        // f1 => f2
        SolverResult sat = LTLSolver.isSAT(SolverUtils.toSolverSyntax(Conjunction.of(f1, f2.not()).accept(visitor)));
        if (sat == SolverResult.UNSAT) {
            // f2 => f1
            sat = LTLSolver.isSAT(SolverUtils.toSolverSyntax(Conjunction.of(f2, f1.not()).accept(visitor)));
            if (sat == SolverResult.UNSAT)
                return false; // are equivalent so the guarantees have no changes.
            else return true;
        }
        return true;
    }

    public static List<Tlsf> removeSameGuarantees(List<Tlsf> sourceSols) throws IOException, InterruptedException {
        //Remove all specifications which have modified the guarantees
        List<Tlsf> woMod = new LinkedList<Tlsf>();
        Formula r_sys;
        Formula o_sys;
        for (Tlsf repair : sourceSols) {
            r_sys = Conjunction.of(repair.preset(), GOperator.of(Conjunction.of(repair.assert_())), Conjunction.of(repair.guarantee()));
            o_sys = Conjunction.of(original.preset(), GOperator.of(Conjunction.of(original.assert_())), Conjunction.of(original.guarantee()));
            if (guaranteesHaveBeenModified(r_sys, o_sys)) {
                System.out.print("M");
                continue;
            } else woMod.add(repair);
        }
        return woMod;
    }
}
