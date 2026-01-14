package solvers;

import main.Settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class LTLSolver {

    public static int numOfTimeout = 0;
    public static int numOfError = 0;
    public static int numOfCalls = 0;

    private static File createFormulaFile(String formula) throws IOException {
        File tempFile = File.createTempFile("ltl_formula_", ".ltl");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(formula);
        }
        return tempFile;
    }

    private static ProcessBuilder buildProcessBuilder(File formulaFile) {
        String shellCmd = "ltl2tgba -F '" + formulaFile.getAbsolutePath() + "' | autfilt --is-empty";
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", shellCmd);
        return pb;
    }

    private static SolverResult executeAndCheckResult(Process p, int timeoutSeconds) throws InterruptedException {
        if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            numOfTimeout++;
            p.destroy();
            return SolverResult.TIMEOUT;
        }
        // Consume stdout
        try (InputStream in = p.getInputStream();
             InputStreamReader inread = new InputStreamReader(in);
             BufferedReader bufferedreader = new BufferedReader(inread)) {
            while (bufferedreader.readLine() != null) {
            }
        } catch (IOException e) {
            return SolverResult.ERROR;
        }
        // Check stderr
        try (InputStream err = p.getErrorStream();
             InputStreamReader errread = new InputStreamReader(err);
             BufferedReader errbufferedreader = new BufferedReader(errread)) {
            String line;
            while ((line = errbufferedreader.readLine()) != null) {
                System.out.println("ERR: " + line);
                return SolverResult.ERROR;
            }
        } catch (IOException e) {
            return SolverResult.ERROR;
        }
        // Check exit code: autfilt --is-empty returns 0 if automaton is empty (UNSAT)
        int exitCode = p.exitValue();
        return (exitCode == 0) ? SolverResult.UNSAT : SolverResult.SAT;
    }

    public static SolverResult isSAT(String formula) throws IOException, InterruptedException {
        numOfCalls++;
        if (formula == null) {
            return SolverResult.ERROR;
        }
        File tempFile = null;
        Process p = null;
        try {
            tempFile = createFormulaFile(formula);
            ProcessBuilder processBuilder = buildProcessBuilder(tempFile);
            p = processBuilder.start();
            return executeAndCheckResult(p, Settings.SAT_TIMEOUT);
        } finally {
            if (p != null) {
                p.destroy();
            }
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    public enum SolverResult {
        SAT,
        UNSAT,
        TIMEOUT,
        ERROR;

        public boolean inconclusive() {
            return this == TIMEOUT || this == ERROR;
        }
    }
}
