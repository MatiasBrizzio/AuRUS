package modelcounter;

import owl.ltl.LabelledFormula;
import solvers.SolverUtils;

import java.io.*;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CountRltlConv {

    public int TIMEOUT = 180;

    public BigInteger countPrefixes(LabelledFormula formula, int bound) throws IOException, InterruptedException {
        String ltlStr = genRltlString(formula);
        return runCount(ltlStr, bound);
    }

    public String genRltlString(LabelledFormula formula) throws IOException, InterruptedException {
//		String ltl = SolverUtils.toLambConvSyntax(formula.formula().toString());
//		String alph = SolverUtils.createLambConvAlphabet(formula);
        List<String> alphabet = SolverUtils.genAlphabet(formula.variables().size());
        LabelledFormula label_formula = LabelledFormula.of(formula.formula(), alphabet);
        String ltl = SolverUtils.toLambConvSyntax(label_formula.toString());
        String alph = alphabet.toString();

        String form = "LTL=" + ltl;
        if (alph != null && !alph.isEmpty())
            form += ",ALPHABET=" + alph;
        return form;
    }

    private BigInteger runCount(String ltl, int bound) throws IOException, InterruptedException {
        String[] cmd = {"./modelcount-prefixes.sh", ltl, "" + bound};
        Process p = Runtime.getRuntime().exec(cmd);

        boolean timeout = false;
        if (!p.waitFor(TIMEOUT, TimeUnit.SECONDS)) {
            timeout = true; //kill the process.
            p.destroy(); // consider using destroyForcibly instead
        }

        if (timeout)
            throw new RuntimeException("TIMEOUT reached in CountRltlConv.");

        InputStream in = p.getInputStream();
        InputStreamReader inread = new InputStreamReader(in);
        BufferedReader bufferedreader = new BufferedReader(inread);
        String aux;
        String out = "";
        while ((aux = bufferedreader.readLine()) != null) {
            // System.out.println("AUX: " + aux);
            out = aux;
        }

        // Close the InputStream
        bufferedreader.close();
        inread.close();
        in.close();


        // Leer el error del programa.
        //InputStream err = p.getErrorStream();
        //InputStreamReader errread = new InputStreamReader(err);
        //BufferedReader errbufferedreader = new BufferedReader(errread);

        //while ((aux = errbufferedreader.readLine()) != null) {
        // System.out.println("FORMULA: " + ltl);
        //System.out.println("ERR: " + aux);
        //System.out.println("BOUND: " + bound);
        //System.out.println("OUT: " + out);
        //}
        // Close the ErrorStream
        //errbufferedreader.close();
        //errread.close();
        //err.close();
        // Check for failure
//		if (p.waitFor() != 0) {
//			System.out.println("exit value = " + p.exitValue());
//			throw new RuntimeException("ERROR in CountRltlConv.");
//		}

        //   			InputStream is = p.getInputStream();
        //  			InputStream es = p.getErrorStream();
        OutputStream os = p.getOutputStream();
//   				if (is!=null) is.close();
//   				if (es!=null) es.close();
        if (os != null) os.close();
        p.destroy();

        return new BigInteger(out);
    }
}
