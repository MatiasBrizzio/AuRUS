package solvers;

import owl.ltl.Formula;

import java.io.*;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PreciseLTLModelCounter {
    public final String BASENAME = "lib/ltl-model-counter/result/numofmodels";
    public final String INFILE = BASENAME + ".ltl";
    public int BOUND = 5;
    public int numOfTimeout = 0;
    public int numOfError = 0;
    public int numOfCalls = 0;
    public int TIMEOUT = 180;
    public MODEL_COUNTER modelcounter = MODEL_COUNTER.RELSAT;

    ;

    public String getCommandLTL2PL() {
        String cmd;
        String currentOS = System.getProperty("os.name");
        if (currentOS.startsWith("Mac"))
            cmd = "./lib/ltl-model-counter/ltl2pl_macos.sh " + INFILE + " " + BASENAME + " " + BOUND + " " + modelcounter.toString();
        else
            cmd = "./lib/ltl-model-counter/ltl2pl.sh" + INFILE + " " + BASENAME + " " + BOUND + " " + modelcounter.toString();
        return cmd;
    }

    public String getCommand() {
        String cmd;
        String currentOS = System.getProperty("os.name");
        if (currentOS.startsWith("Mac"))
            cmd = "./lib/ltl-model-counter/ltl-modelcountermac.sh " + INFILE + " " + BASENAME + " " + BOUND + " " + modelcounter.toString();
        else
            cmd = "./lib/ltl-model-counter/ltl-modelcounterlinux.sh " + INFILE + " " + BASENAME + " " + BOUND + " " + modelcounter.toString();
        return cmd;
    }

    public BigInteger count(Formula f, int numOfVars) throws IOException, InterruptedException {
        String formula = f.toString().replaceAll("([A-Z])", " $1 ");
        List<String> vars = new LinkedList<String>();
        for (int i = 0; i < numOfVars; i++)
            vars.add("p" + i);
        return count(formula, vars);
    }

    public BigInteger count(String formula, List<String> variables) throws IOException, InterruptedException {
        numOfCalls++;
        Process p = null;

        // make formula file
        if (formula != null && variables != null) {
            File f = new File(INFILE);
            FileWriter fw = new FileWriter(f);
            for (String v : variables)
                fw.append(v).append("\n");
            fw.append("###\n");
            fw.append(formula).append("\n");
            fw.close();
            // run counting command
            String cmd = getCommand();
            p = Runtime.getRuntime().exec(cmd);
        }

        boolean timeout = false;
        assert p != null;
        if (!p.waitFor(TIMEOUT, TimeUnit.SECONDS)) {
            timeout = true; //kill the process.
            p.destroy(); // consider using destroyForcibly instead
        }

        String aux;
        BigInteger numOfModels = null;
        if (timeout) {
            numOfTimeout++;
            p.destroy();
        } else {
            InputStream in = p.getInputStream();
            InputStreamReader inread = new InputStreamReader(in);
            BufferedReader bufferedreader = new BufferedReader(inread);
            while ((aux = bufferedreader.readLine()) != null) {
                if (aux.startsWith(modelcounter.solution())) {
//		    		System.out.println(aux);
                    if (modelcounter == MODEL_COUNTER.GANAK)
                        aux = bufferedreader.readLine(); //in ganak the solution appears in the next line
                    String val = modelcounter.getNumber(aux);
                    if (val.startsWith("inf"))
                        numOfModels = BigInteger.valueOf(Long.MAX_VALUE);
                    else
                        numOfModels = new BigInteger(val);
                    break;
                } else if (aux.startsWith("UNSAT")) {
                    numOfModels = BigInteger.ZERO;
                    break;
                }
            }

            // Leer el error del programa.
            InputStream err = p.getErrorStream();
            InputStreamReader errread = new InputStreamReader(err);
            BufferedReader errbufferedreader = new BufferedReader(errread);
            while ((aux = errbufferedreader.readLine()) != null) {
                System.out.println("ERR: " + aux + " Formula: " + formula);
            }

            // Check for failure
            if (p.waitFor() != 0) {
                System.out.println("exit value = " + p.exitValue());
                System.out.println(formula);
                numOfError++;
            }

            // Close the InputStream
            bufferedreader.close();
            inread.close();
            in.close();

            // Close the ErrorStream
            errbufferedreader.close();
            errread.close();
            err.close();
        }

        OutputStream os = p.getOutputStream();
        if (os != null) os.close();

        return numOfModels;
    }

    public enum MODEL_COUNTER {
        RELSAT, CACHET, MINIC2D, GANAK;

        public String toString() {
            if (this == GANAK) return "ganak";
            if (this == CACHET) return "cachet";
            if (this == MINIC2D) return "miniC2D";
            return "relsat";
        }

        public String solution() {
            if (this == GANAK) return "# solutions";
            if (this == CACHET) return "s ";
            if (this == MINIC2D) return "Counting... ";
            return "Number of solutions: ";
        }

        public String getNumber(String str) {
            if (this == GANAK) return str;
            if (this == CACHET) return str.replace("s ", "");
            if (this == MINIC2D) return str.substring(str.indexOf(" models")).replace("Counting... ", "");
            return str.replace("Number of solutions: ", "");
        }

    }
}
