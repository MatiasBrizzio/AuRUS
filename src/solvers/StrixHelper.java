package solvers;

import main.Settings;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.rewriter.SyntacticSimplifier;
import owl.ltl.spectra.Spectra;
import owl.ltl.tlsf.Tlsf;
import utils.SolverUtils;
import utils.TlsfUtils;

import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * Bridge to the external <b>Strix</b> reactive-synthesis tool, used by AuRUS
 * to check the realisability of a candidate specification — the
 * {@code status(S')} component of the fitness function.
 *
 * <p>Every {@code checkRealizability}/{@code executeStrix} overload does the
 * same three things: (1) turn the specification into the plain-text
 * arguments Strix expects, (2) launch Strix as an external process — either
 * the native binary or, when {@code Settings.USE_DOCKER} is set, the
 * {@code run-docker-strix.sh}/{@code run-docker-spectra.sh} wrapper — and
 * (3) parse its stdout for the {@code REALIZABLE} verdict, enforcing
 * {@code Settings.STRIX_TIMEOUT}. The two input formats supported —
 * TLSF/LTL and Spectra — are dispatched on {@code Settings.USE_SPECTRA}.</p>
 *
 * <p><b>Which path actually runs by default (no Docker, no Spectra).</b> The
 * TLSF specification is <i>not</i> shelled out to {@code syfco} for
 * translation. Instead, {@link #checkRealizability(Tlsf)} simplifies the
 * formula in Java ({@code SyntacticSimplifier}), renders it to Strix's LTL
 * syntax ({@code SolverUtils.toSolverSyntax}), extracts the input/output
 * signal lists, and calls the 3-argument
 * {@link #executeStrix(String, String, String)}, which invokes
 * {@code lib/new_strix/strix} directly. <b>Caveat:</b> the input/output
 * extraction assumes the TLSF variable ordering convention — inputs occupy a
 * contiguous run starting at index 0, immediately followed by outputs — and
 * stops at the first bit that breaks the run; this is the ordering AuRUS's
 * own TLSF writer produces, but is not re-validated here.</p>
 *
 * @author Mat&iacute;as Brizzio
 * @see Settings
 * @see geneticalgorithm.AutomataBasedModelCountingSpecificationFitness#compute_status
 */
public class StrixHelper {

    /**
     * Parses the given TLSF text and checks its realisability.
     *
     * @param tlsf the specification, in TLSF syntax
     * @return the realisability verdict
     * @throws IOException          if launching Strix fails
     * @throws InterruptedException if the Strix process is interrupted
     */
    public static RealizabilitySolverResult checkRealizability(String tlsf) throws IOException, InterruptedException {
        Tlsf tlsf2 = TlsfUtils.toBasicTLSF(tlsf);
        return checkRealizability(tlsf2);
    }

    /**
     * Checks the realisability of a parsed TLSF specification.
     *
     * <p>When {@code Settings.USE_SPECTRA} is set, the specification is first
     * converted to Spectra syntax ({@code TlsfUtils.tlsf2spectra}) and written
     * to a {@code .spectra} file under {@code Settings.SPECTRA_PATH}, then
     * checked via {@link #executeStrix(String)}. Otherwise the formula is
     * simplified and rendered to Strix's LTL syntax directly in Java, and
     * checked via {@link #executeStrix(String, String, String)} — see the
     * class documentation for exactly which binary this invokes.</p>
     *
     * @param tlsf the specification to check
     * @return the realisability verdict
     * @throws IOException          if launching Strix fails
     * @throws InterruptedException if the Strix process is interrupted
     */
    public static RealizabilitySolverResult checkRealizability(Tlsf tlsf) throws IOException, InterruptedException {
        File file;
        if (Settings.USE_SPECTRA) {
            String directoryName = Settings.SPECTRA_PATH;
            File outfolder = new File(directoryName);
            if (!outfolder.exists() && !outfolder.mkdirs()) {
                System.err.println("Failed to create directory: " + directoryName);
            }
            file = new File((tlsf.title().replace("\"", "") + ".spectra").replaceAll("\\s", ""));
            try {
                //	private static int TIMEOUT = 180;
                FileWriter writer = new FileWriter(file.getPath());
                writer.write(TlsfUtils.tlsf2spectra(tlsf));
//				else
//					writer.write(TlsfUtils.adaptTLSFSpec(tlsf));
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return executeStrix(file.getPath());
        } else {
            SyntacticSimplifier simp = new SyntacticSimplifier();
            Formula form = tlsf.toFormula().formula().accept(simp);
            String formula = SolverUtils.toSolverSyntax(LabelledFormula.of(form, tlsf.variables()));
            StringBuilder inputs = new StringBuilder();
            StringBuilder outputs = new StringBuilder();
            int i = 0;
            while (tlsf.inputs().get(i)) {
                inputs.append(tlsf.variables().get(i)).append(",");
                i++;
            }
            while (tlsf.outputs().get(i)) {
                outputs.append(tlsf.variables().get(i)).append(",");
                i++;
            }
            for (String v : tlsf.variables()) {
                inputs = new StringBuilder(inputs.toString().replaceAll(v, v.toLowerCase()));
                outputs = new StringBuilder(outputs.toString().replaceAll(v, v.toLowerCase()));
            }
            if (outputs.length() != 0)
                outputs = new StringBuilder(outputs.substring(0, outputs.length() - 1));
            else
                outputs = new StringBuilder();
            if (inputs.length() != 0)
                inputs = new StringBuilder(inputs.substring(0, inputs.length() - 1));
            else inputs = new StringBuilder();
            return executeStrix(formula, inputs.toString(), outputs.toString());
//			}
        }
    }

    /**
     * Runs the Spectra CLI on an already-written {@code .spectra} file,
     * dispatching on {@code Settings.USE_DOCKER}. This method is only ever
     * called from the Spectra branch of {@link #checkRealizability(Tlsf)};
     * TLSF/native realisability checks go through
     * {@link #executeStrix(String, String, String)} instead.
     *
     * <p>On timeout the process is destroyed and a cleanup script
     * ({@code run-docker-stop.sh}) is launched to stop any leftover Docker
     * container. Otherwise, stdout is scanned line by line for the verdict —
     * a line containing {@code "realizable"} but not {@code "unrealizable"} —
     * and stderr output, if any, downgrades the result to
     * {@link RealizabilitySolverResult#ERROR}.</p>
     *
     * @param path path to the {@code .spectra} file to check
     * @return the realisability verdict
     * @throws IOException          if launching the process fails
     * @throws InterruptedException if the process is interrupted while waiting
     */
    public static RealizabilitySolverResult executeStrix(String path) throws IOException, InterruptedException {
        Process pr;
        System.out.println(path);
        if (Settings.USE_DOCKER)
            pr = Runtime.getRuntime().exec(new String[]{"./run-docker-spectra.sh", path});
        else
            pr = Runtime.getRuntime().exec(new String[]{"java", "-Djava.library.path=/usr/local/lib/",
                    "-jar", "lib/Spectra/spectra-cli.jar", "-i", "./" + path});
        boolean timeout = false;
        if (!pr.waitFor(Settings.STRIX_TIMEOUT, TimeUnit.SECONDS)) {
            timeout = true; //kill the process.
            pr.destroy(); // consider using destroyForcibly instead
        }

        RealizabilitySolverResult realizable = RealizabilitySolverResult.UNREALIZABLE;
        String aux;
        if (timeout) {
            realizable = RealizabilitySolverResult.TIMEOUT;
            pr.destroy();
            pr = Runtime.getRuntime().exec(new String[]{"./run-docker-stop.sh"});
        } else {

            InputStream in = pr.getInputStream();
            InputStreamReader inread = new InputStreamReader(in);
            BufferedReader bufferedreader = new BufferedReader(inread);

            while ((aux = bufferedreader.readLine()) != null) {
                if (aux.contains("realizable") && !aux.contains("unrealizable")) {
                    realizable = RealizabilitySolverResult.REALIZABLE;
                    break;
                }
                if (aux.contains("Error")) {
                    System.out.println("ERR: " + aux);
                    realizable = RealizabilitySolverResult.ERROR;
                    break;
                }
            }

            //read program's error
            InputStream err = pr.getErrorStream();
            InputStreamReader errread = new InputStreamReader(err);
            BufferedReader errbufferedreader = new BufferedReader(errread);
            while ((aux = errbufferedreader.readLine()) != null) {
                System.out.println("ERR: " + aux);
                realizable = RealizabilitySolverResult.ERROR;
            }

            // Check for failure
            if (pr.waitFor() != 0) {
                System.out.println("exit value = " + pr.exitValue());
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

        if (pr != null) {
            OutputStream os = pr.getOutputStream();
            if (os != null) os.close();
        }

        return realizable;
    }

    /**
     * Checks the realisability of a Spectra specification object directly
     * (as opposed to {@link #checkRealizability(Tlsf)}'s TLSF-to-Spectra
     * conversion path): renders its formula to Strix syntax, extracts the
     * input/output signal lists under the same contiguous-run convention
     * described in the class documentation, and dispatches to
     * {@link #executeStrix(String, String, String)}.
     *
     * @param spectra the Spectra specification to check
     * @return the realisability verdict
     * @throws IOException          if launching Strix fails
     * @throws InterruptedException if the Strix process is interrupted
     */
    public static RealizabilitySolverResult checkRealizability(Spectra spectra) throws IOException, InterruptedException {
        String formula = SolverUtils.toSolverSyntax(spectra.toFormula());
        StringBuilder inputs = new StringBuilder();
        StringBuilder outputs = new StringBuilder();
        int i = 0;
        while (spectra.inputs().get(i)) {
            inputs.append(spectra.variables().get(i)).append(",");
            i++;
        }
        while (spectra.outputs().get(i)) {

            outputs.append(spectra.variables().get(i)).append(",");
            i++;
        }
        for (String v : spectra.variables()) {
            inputs = new StringBuilder(inputs.toString().replaceAll(v, v.toLowerCase()));
            outputs = new StringBuilder(outputs.toString().replaceAll(v, v.toLowerCase()));
        }
        outputs = new StringBuilder(outputs.substring(0, outputs.length() - 1));
        inputs = new StringBuilder(inputs.substring(0, inputs.length() - 1));
        return executeStrix(formula, inputs.toString(), outputs.toString());

    }

    /**
     * Runs Strix directly on an LTL formula plus explicit input/output signal
     * lists — the path actually taken by default (no Docker, no Spectra):
     * dispatches to the native {@code lib/new_strix/strix} binary, or to
     * {@code run-docker-strix.sh} when {@code Settings.USE_DOCKER} is set.
     * Empty signal lists are passed as the literal {@code ""} Strix expects.
     * Timeout and output-parsing behaviour mirror
     * {@link #executeStrix(String)} (native/TLSF branch): the verdict is
     * {@code REALIZABLE} iff a line equal to {@code "REALIZABLE"} is seen on
     * stdout before any {@code "Error"} line or process failure.
     *
     * @param formula the specification's formula, already in Strix's LTL syntax
     * @param ins     comma-separated list of input signal names (lowercased upstream)
     * @param outs    comma-separated list of output signal names (lowercased upstream)
     * @return the realisability verdict
     * @throws IOException          if launching Strix fails
     * @throws InterruptedException if the Strix process is interrupted
     */
    public static RealizabilitySolverResult executeStrix(String formula, String ins, String outs) throws IOException, InterruptedException {
        Process pr;
        if (outs.isEmpty()) outs = "\"\"";
        if (ins.isEmpty()) ins = "\"\"";
        if (Settings.USE_DOCKER)
            pr = Runtime.getRuntime().exec(new String[]{"./run-docker-strix.sh", formula, ins, outs});
        else {
//			System.out.println(outs + " "+ ins);
            pr = Runtime.getRuntime().exec(new String[]{"lib/new_strix/strix", "-f " + formula, "--ins=" + ins, "--outs=" + outs});
        }
        boolean timeout = false;
        if (!pr.waitFor(Settings.STRIX_TIMEOUT, TimeUnit.SECONDS)) {
            timeout = true; //kill the process.
            pr.destroy(); // consider using destroyForcibly instead
        }

        RealizabilitySolverResult realizable = RealizabilitySolverResult.UNREALIZABLE;
        String aux;
        if (timeout) {
            realizable = RealizabilitySolverResult.TIMEOUT;
            pr.destroy();
        } else {

            InputStream in = pr.getInputStream();
            InputStreamReader inread = new InputStreamReader(in);
            BufferedReader bufferedreader = new BufferedReader(inread);

            while ((aux = bufferedreader.readLine()) != null) {
                //System.out.println(aux);
                if (aux.equals("REALIZABLE")) {
                    realizable = RealizabilitySolverResult.REALIZABLE;
                    break;
                }
                if (aux.contains("Error")) {
                    System.out.println("ERR: " + aux);
                    realizable = RealizabilitySolverResult.ERROR;
                    break;
                }
            }

            //read program's error
            InputStream err = pr.getErrorStream();
            InputStreamReader errread = new InputStreamReader(err);
            BufferedReader errbufferedreader = new BufferedReader(errread);
            while ((aux = errbufferedreader.readLine()) != null) {
                System.out.println("ERR: " + aux);
                realizable = RealizabilitySolverResult.ERROR;
            }

            // Check for failure
            if (pr.waitFor() != 0) {
                System.out.println("exit value = " + pr.exitValue());
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

        OutputStream os = pr.getOutputStream();
        if (os != null) os.close();

        return realizable;

    }

    /**
     * Outcome of a Strix realisability query.
     */
    public enum RealizabilitySolverResult {
        /** A controller exists: the specification is realisable. */
        REALIZABLE,
        /** No controller exists: the specification is unrealisable. */
        UNREALIZABLE,
        /** Strix did not answer within {@code Settings.STRIX_TIMEOUT} seconds. */
        TIMEOUT,
        /** Strix reported an error, or wrote to stderr, or exited abnormally. */
        ERROR;

        /**
         * @return {@code true} iff the result is {@link #TIMEOUT} or
         *         {@link #ERROR} — an inconclusive answer that callers should
         *         treat as "unknown", not as a negative verdict
         */
        public boolean inconclusive() {
            return this == TIMEOUT || this == ERROR;
        }
    }
}