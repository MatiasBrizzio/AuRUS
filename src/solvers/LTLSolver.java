package solvers;

import main.Settings;

import java.io.*;
import java.util.concurrent.TimeUnit;

public class LTLSolver {

    public static int numOfTimeout = 0;
    public static int numOfError = 0;
    public static int numOfCalls = 0;

    private static String getCommand() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("mac") ? "./lib/aalta" : "./lib/aalta_linux";
    }

    public static SolverResult isSAT(String formula) throws IOException, InterruptedException {
        numOfCalls++;
        ProcessBuilder processBuilder = null;

        if (formula != null) {
            String cmd = getCommand();
            processBuilder = new ProcessBuilder(cmd, formula);
        }

        boolean timeout = false;
        Process p = null;

        try {
            if (processBuilder != null) {
                p = processBuilder.start();
            }

            assert p != null;
            if (!p.waitFor(Settings.SAT_TIMEOUT, TimeUnit.SECONDS)) {
                timeout = true;
                p.destroy();
            }

            SolverResult sat;
            String aux;

            if (timeout) {
                numOfTimeout++;
                sat = SolverResult.TIMEOUT;
            } else {
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
            }
            return sat;
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }


    public static enum SolverResult {
        SAT,
        UNSAT,
        TIMEOUT,
        ERROR;

        public boolean inconclusive() {
            return this == TIMEOUT || this == ERROR;
        }
    }
}
