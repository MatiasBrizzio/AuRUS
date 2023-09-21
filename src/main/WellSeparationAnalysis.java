package main;

import owl.ltl.BooleanConstant;
import owl.ltl.Conjunction;
import owl.ltl.Formula;
import owl.ltl.GOperator;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.visitors.SolverSyntaxOperatorReplacer;
import solvers.LTLSolver;
import solvers.SolverUtils;
import solvers.StrixHelper;
import tlsf.TLSF_Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WellSeparationAnalysis {

    public static void main(String[] args) throws IOException, InterruptedException {
        String directoryName = "";
        String outFile = "";
        for (String arg : args) {
            if (arg.startsWith("-d=")) {
                directoryName = arg.replace("-d=", "");
                System.out.println("directory: " + directoryName);
            } else if (arg.startsWith("-out=")) {
                outFile = arg.replace("-out=", "");
                System.out.println("out: " + outFile);
            }
        }
        if (directoryName.isEmpty()) {
            System.out.println("directory name is missing.");
            System.exit(0);
        }

        Stream<Path> walk = Files.walk(Paths.get(directoryName));
        List<String> specifications = walk.map(Path::toString)
                .filter(f -> f.endsWith(".tlsf") && !f.endsWith("_basic.tlsf")).collect(Collectors.toList());

        List<String> noWellSeparated = new LinkedList<>();
        int errors = 0;
        int numOfTimeout = 0;
        int numOfSAT = 0;
        int numOfNoWellSeparated = 0;
        int numOfUNSAT = 0;

        for (String filename : specifications) {
            System.out.println(filename);
            try {
                Tlsf spec = TLSF_Utils.toBasicTLSF(new File(filename));
                Formula env_sys = Conjunction.of(spec.initially(), GOperator.of(spec.require()), spec.preset(), GOperator.of(Conjunction.of(spec.assert_())), spec.assume(), Conjunction.of(spec.guarantee()));
                SolverSyntaxOperatorReplacer visitor = new SolverSyntaxOperatorReplacer();
                Formula env_sys2 = env_sys.accept(visitor);
                LTLSolver.SolverResult res = LTLSolver.isSAT(SolverUtils.toSolverSyntax(env_sys2));
                System.out.println(res);
                if (res == null)
                    numOfTimeout++;
                else if (res == LTLSolver.SolverResult.SAT) {
                    numOfSAT++;
                    Tlsf wellSeparatedSpec = TLSF_Utils.change_guarantees(spec, BooleanConstant.FALSE);
                    StrixHelper.RealizabilitySolverResult rel = StrixHelper.checkRealizability(wellSeparatedSpec);
                    if (rel == null)
                        numOfTimeout++;
                    else if (rel == StrixHelper.RealizabilitySolverResult.REALIZABLE) {
                        numOfNoWellSeparated++;
                        noWellSeparated.add(filename);
                        System.out.println(rel);
                    }
                } else if (res == LTLSolver.SolverResult.UNSAT) {
                    numOfUNSAT++;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                errors++;
            }

        }
        System.out.println("SATISFIABLE: " + numOfSAT);
        System.out.println("NO WELL SEPARATED: " + numOfNoWellSeparated);
        System.out.println("WELL SEPARATED: " + (numOfSAT - numOfNoWellSeparated));
        System.out.println("UNSATISFIABLE: " + numOfUNSAT);
        System.out.println("TIMEOUTS: " + numOfTimeout);
        System.out.println("ERRORS: " + errors);
        System.out.println();
        System.out.println(noWellSeparated);

        if (outFile.isEmpty())
            System.exit(0);
        //saving the time execution and configuration details
        File file = new File(outFile);
        FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
        BufferedWriter bw = new BufferedWriter(fw);
        boolean empty = !file.exists() || file.length() == 0;
        if (empty)
            bw.write("sat,nowellsep,wellsep,unsat,to,err\n");
        bw.write(numOfSAT + "," + numOfNoWellSeparated + "," + (numOfSAT - numOfNoWellSeparated) + "," + numOfUNSAT + "," + numOfTimeout + "," + errors + "\n");
        bw.flush();
        bw.close();
    }

}
