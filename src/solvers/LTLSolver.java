package solvers;

import main.Settings;
import utils.SolverUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static utils.SolverUtils.toSolverSyntax;

public class LTLSolver {

    public static int numOfTimeout = 0;
    public static int numOfError = 0;
    public static int numOfCalls = 0;

    private static String getCommand() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("mac") ? "./lib/aalta" : "./lib/aalta_linux";
    }

    private static boolean usesBlack() {
        return "black".equalsIgnoreCase(Settings.SAT_SOLVER);
    }

    private static List<String> buildCommand(String formula) {
        if (usesBlack()) {
            String bin = Settings.SAT_BIN.isEmpty() ? "black" : Settings.SAT_BIN;
            // Infinite-trace semantics is BLACK's default and has no flag of its
            // own: it holds as long as --finite is never passed. The backend is
            // pinned so that macOS and the Linux package use the same one.
            // W and M must be rewritten first (SolverSyntaxOperatorReplacer):
            // BLACK 26.05.0 accepts them but answers wrongly.
            return List.of(bin, "solve", "-B", "z3", "-f", SolverUtils.toBlackSyntax(formula));
        }
        String bin = Settings.SAT_BIN.isEmpty() ? getCommand() : Settings.SAT_BIN;
        return List.of(bin, formula);
    }

    public static SolverResult isSAT(String formula) throws IOException, InterruptedException {
        numOfCalls++;
        ProcessBuilder processBuilder = null;

        if (formula != null) {
            processBuilder = new ProcessBuilder(buildCommand(formula));
        }

        boolean timeout = false;
        Process p = null;

        try {
            if (processBuilder != null) {
                try {
                    p = processBuilder.start();
                } catch (IOException e) {
                    if (!usesBlack()) throw e;
                    System.out.println("ERR: " + e.getMessage());
                    numOfError++;
                    return SolverResult.ERROR;
                }
            }

            assert p != null;
            if (!p.waitFor(Settings.SAT_TIMEOUT, TimeUnit.SECONDS)) {
                timeout = true;
                p.destroy();
            }

            if (timeout) {
                numOfTimeout++;
                return SolverResult.TIMEOUT;
            }
            return usesBlack() ? parseBlackAnswer(p, formula) : parseAaltaAnswer(p, formula);
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }

    private static SolverResult parseAaltaAnswer(Process p, String formula) throws IOException, InterruptedException {
        SolverResult sat;
        String aux;
        try (InputStream in = p.getInputStream();
             InputStreamReader inread = new InputStreamReader(in);
             BufferedReader bufferedreader = new BufferedReader(inread)) {

            sat = SolverResult.UNSAT;

            while ((aux = bufferedreader.readLine()) != null) {
                if (aux.equals("sat") || aux.contains("Formula 1: satisfiable")) {
                    sat = SolverResult.SAT;
                    break;
                }
            }
        }

        try (InputStream err = p.getErrorStream();
             InputStreamReader errread = new InputStreamReader(err);
             BufferedReader errbufferedreader = new BufferedReader(errread)) {

            while ((aux = errbufferedreader.readLine()) != null) {
                System.out.println("ERR: " + aux);
                sat = SolverResult.ERROR;
            }
        }

        if (p.waitFor() != 0) {
            System.out.println("exit value = " + p.exitValue());
            System.out.println(formula);
            numOfError++;
        }
        return sat;
    }

    /** Only an exact verdict on a clean exit counts: anything else is inconclusive. */
    private static SolverResult parseBlackAnswer(Process p, String formula) throws IOException, InterruptedException {
        List<String> out = readLines(p.getInputStream());
        for (String line : readLines(p.getErrorStream())) {
            System.out.println("ERR: " + line);
        }
        String verdict = out.stream().map(String::trim).filter(l -> !l.isEmpty()).findFirst().orElse("");
        if (p.waitFor() == 0) {
            if (verdict.equals("SAT")) return SolverResult.SAT;
            if (verdict.equals("UNSAT")) return SolverResult.UNSAT;
            if (verdict.startsWith("UNKNOWN")) return SolverResult.UNKNOWN;
        }
        System.out.println("exit value = " + p.exitValue() + ", answer = '" + verdict + "'");
        System.out.println(formula);
        numOfError++;
        return SolverResult.ERROR;
    }

    private static List<String> readLines(InputStream stream) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }


    public enum SolverResult {
        SAT,
        UNSAT,
        TIMEOUT,
        ERROR,
        UNKNOWN;

        public boolean inconclusive() {
            return this == TIMEOUT || this == ERROR || this == UNKNOWN;
        }
    }
}
