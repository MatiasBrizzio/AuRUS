package main;

import geneticalgorithm.AutomataBasedModelCountingSpecificationFitness;
import owl.ltl.Formula;
import owl.ltl.tlsf.Tlsf;
import tlsf.TLSF_Utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SynSemDistanceAnalysis {

    public static void main(String[] args) {
        try {
            List<Tlsf> solutions = new LinkedList<>();
            List<Double> sol_fitness = new LinkedList<>();
            List<Double> sol_syntactic = new LinkedList<>();
            List<Double> sol_semantic = new LinkedList<>();
            String directoryName = "";
            String out_name = "";
            Tlsf original = null;
            int N = -1;
//            boolean computeSyn = false;
//            boolean computeSem = false;
            for (int i = 0; i < args.length; i++) {
                if (args[i].startsWith("-o=")) {
                    String orig_name = args[i].replace("-o=", "");
                    original = TLSF_Utils.toBasicTLSF(new File(orig_name));
                } else if (args[i].startsWith("-out=")) {
                    out_name = args[i].replace("-out=", "");
                }
//                } else if (args[i].startsWith("-all")) {
//                    computeSyn = true;
//                    computeSem = true;
//                } else if (args[i].startsWith("-syn")) {
//                    computeSyn = true;
//                } else if (args[i].startsWith("-sem")) {
//                    computeSem = true;
                else if (args[i].startsWith("-n=")) {
                    N = Integer.parseInt(args[i].replace("-n=", ""));
                } else {
                    directoryName = args[i];
                }
            }
            if (Objects.equals(directoryName, "")) {
                System.out.println("missing directory name.");
                System.exit(0);
            }


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
                double syntactic_distance = 0.0d;
                double semantic_distance = 0.0d;
                while ((aux = in.readLine()) != null) {
                    if ((aux.startsWith("//fitness"))) {
                        value = Double.valueOf(aux.substring(10));
                    } else if ((aux.startsWith("//syntactic"))) {
                        syntactic_distance = Double.valueOf(aux.substring(12));
                    } else if ((aux.startsWith("//semantic"))) {
                        semantic_distance = Double.valueOf(aux.substring(11));
                    }
                }
                sol_fitness.add(value);
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

            int[] sortedIndices = IntStream.range(0, sol_fitness.size())
                    .boxed().sorted((i, j) -> -sol_fitness.get(i).compareTo(sol_fitness.get(j)))
                    .mapToInt(ele -> ele).toArray();

            //compute syntactic/semantic distance
            Settings.check_REALIZABILITY = false;
            AutomataBasedModelCountingSpecificationFitness fitness = new AutomataBasedModelCountingSpecificationFitness(original);

            int MAX = solutions.size();
            if (N > 0 && MAX > 0)
                MAX = Integer.min(N, MAX);
            for (int i = 0; i < MAX; i++) {
                int index = sortedIndices[i];
                double value = sol_fitness.get(index);
                double syntactic_distance = sol_syntactic.get(index);
                double semantic_distance = sol_semantic.get(index);
                Tlsf tlsf = solutions.get(index);
                if (syntactic_distance == 0.0d) {
                    assert original != null;
                    syntactic_distance = fitness.compute_syntactic_distance(original, tlsf);
                }
                if (semantic_distance == 0.0d)
                    semantic_distance = fitness.compute_semantic_distance(original, tlsf);

                bw.write(i + "," + value + "," + syntactic_distance + "," + semantic_distance + "\n");
            }
            if (N > 0) {
                bw.write(solutions.size() - 1 + ",0,0,0\n");
            }
            bw.flush();
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }


    private static String toSolverSyntax(Formula f) {
        String LTLFormula = f.toString();
        LTLFormula = LTLFormula.replaceAll("\\!", "~");
        LTLFormula = LTLFormula.replaceAll("([A-Z])", " $1 ");
        return new String(LTLFormula);
    }

}
