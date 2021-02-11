package main;

import geneticalgorithm.AutomataBasedModelCountingSpecificationFitness;
import geneticalgorithm.SpecificationChromosome;
import owl.ltl.Conjunction;
import owl.ltl.Formula;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.visitors.SolverSyntaxOperatorReplacer;
import solvers.LTLSolver;
import tlsf.TLSF_Utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SynSemDistanceAnalysis {

    public static void main(String[] args)  {
        try {
            List<Tlsf> solutions = new LinkedList<>();
            List<Double> sol_fitness = new LinkedList<>();
            List<Double> sol_syntactic = new LinkedList<>();
            List<Double> sol_semantic = new LinkedList<>();
            String directoryName = "";
            String out_name = "";
            Tlsf original = null;
            for (int i = 0; i < args.length; i++) {
                if (args[i].startsWith("-o=")) {
                    String orig_name = args[i].replace("-o=", "");
                    original = TLSF_Utils.toBasicTLSF(new File(orig_name));
                } else if (args[i].startsWith("-out=")) {
                    out_name = args[i].replace("-out=", "");
                } else {
                    directoryName = args[i];
                }
            }
            if (directoryName == "") {
                System.out.println("missing directory name.");
                System.exit(0);
            }

            //compute syntactic/semantic distance
            Settings.check_REALIZABILITY = false;
            AutomataBasedModelCountingSpecificationFitness fitness = new AutomataBasedModelCountingSpecificationFitness(original);

            Stream<Path> walk = Files.walk(Paths.get(directoryName));
            List<String> specifications = walk.map(x -> x.toString())
                    .filter(f -> f.endsWith(".tlsf") && !f.endsWith("_basic.tlsf") && !f.endsWith("Spec.tlsf")).collect(Collectors.toList());
            for (String filename : specifications) {
                System.out.println(filename);
                Tlsf tlsf = TLSF_Utils.toBasicTLSF(new File(filename));
                solutions.add(tlsf);
                //read the fitness from file
                FileReader f = new FileReader(filename);
                BufferedReader in = new BufferedReader(f);
                String aux = "";
                double value = 0.0d;
                while ((aux = in.readLine()) != null) {
                    if ((aux.startsWith("//fitness"))) {
                        value = Double.valueOf(aux.substring(10));
                    }
                }
                sol_fitness.add(value);
                double syntactic_distance = fitness.compute_syntactic_distance(original, tlsf);
                double semantic_distance = fitness.compute_semantic_distance(original, tlsf);
                sol_syntactic.add(syntactic_distance);
                sol_semantic.add(semantic_distance);
            }

            if (out_name == "")
                out_name = directoryName + "/distances.csv";

            //saving the time execution and configuration details
            File file = new File(out_name);
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("id,fit,syn,sem\n");

            for (int i = 0; i < solutions.size(); i++) {
                bw.write(i + "," + sol_fitness.get(i) + "," + sol_syntactic.get(i) + "," + sol_semantic.get(i) + "\n");
            }
            bw.flush();
            bw.close();
        }
        catch (Exception e) {e.printStackTrace();}
        finally{       System.exit(0); }
    }



    private static String toSolverSyntax(Formula f) {
        String LTLFormula = f.toString();
        LTLFormula = LTLFormula.replaceAll("\\!", "~");
        LTLFormula = LTLFormula.replaceAll("([A-Z])", " $1 ");
        return new String(LTLFormula);
    }

}
