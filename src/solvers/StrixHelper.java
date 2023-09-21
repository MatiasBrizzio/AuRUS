package solvers;

import main.Settings;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.rewriter.SyntacticSimplifier;
import owl.ltl.spectra.Spectra;
import owl.ltl.tlsf.Tlsf;
import tlsf.TLSF_Utils;

import java.io.*;
import java.util.concurrent.TimeUnit;

public class StrixHelper {

    public static RealizabilitySolverResult checkRealizability(File tlsf) throws IOException, InterruptedException {
        TLSF_Utils.toBasicTLSF(tlsf);
        String tlsfBasic = tlsf.getPath().replace(".tlsf", "_basic.tlsf");
        return executeStrix(tlsfBasic);
    }

    public static RealizabilitySolverResult checkRealizability(String tlsf) throws IOException, InterruptedException {
        Tlsf tlsf2 = TLSF_Utils.toBasicTLSF(tlsf);
        return checkRealizability(tlsf2);
    }

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
                writer.write(TLSF_Utils.tlsf2spectra(tlsf));
//				else
//					writer.write(TLSF_Utils.adaptTLSFSpec(tlsf));
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return executeStrix(file.getPath());
        } else {
            // No docker, no strix
//			if (Settings.USE_DOCKER) {
//				file = new File((tlsf.title().replace("\"", "")+".tlsf").replaceAll("\\s",""));
//				try {
//					writer = new FileWriter(file.getPath());
//					writer.write(TLSF_Utils.adaptTLSFSpec(tlsf));
//					writer.flush();
//					writer.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//				return executeStrix(file.getPath());
//			}
//			// No docker, Strix
//			else {
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
//				System.out.println(formula);
            return executeStrix(formula, inputs.toString(), outputs.toString());
//			}
        }
    }

    public static RealizabilitySolverResult executeStrix(String path) throws IOException, InterruptedException {
        Process pr;
        System.out.println(path);
        if (Settings.USE_SPECTRA) {
            if (Settings.USE_DOCKER)
                pr = Runtime.getRuntime().exec(new String[]{"./run-docker-spectra.sh", path});
            else
                pr = Runtime.getRuntime().exec(new String[]{"java", "-Djava.library.path=/usr/local/lib/",
                        "-jar", "lib/Spectra/spectra-cli.jar", "-i", "./" + path});

        } else {
            if (Settings.USE_DOCKER)
                pr = Runtime.getRuntime().exec(new String[]{"./run-docker-strix.sh", path});
            else
                pr = Runtime.getRuntime().exec(new String[]{"lib/strix_tlsf.sh", "./" + path, "-r"});
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
            pr = Runtime.getRuntime().exec(new String[]{"./run-docker-stop.sh"});
        } else {

            InputStream in = pr.getInputStream();
            InputStreamReader inread = new InputStreamReader(in);
            BufferedReader bufferedreader = new BufferedReader(inread);

            while ((aux = bufferedreader.readLine()) != null) {
//		    	System.out.println(aux);
                if (!Settings.USE_SPECTRA && aux.equals("REALIZABLE")) {
                    realizable = RealizabilitySolverResult.REALIZABLE;
                    break;
                } else if (Settings.USE_SPECTRA && aux.contains("realizable") && !aux.contains("unrealizable")) {
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

    public static enum RealizabilitySolverResult {
        REALIZABLE,
        UNREALIZABLE,
        TIMEOUT,
        ERROR;

        public boolean inconclusive() {
            return this == TIMEOUT || this == ERROR;
        }
    }
}
