package main;

import modelcounter.CountRltlConv;
import modelcounter.EmersonLeiAutomatonBasedModelCounting;
import owl.ltl.*;
import owl.ltl.parser.LtlParser;
import owl.ltl.rewriter.NormalForms;
import owl.ltl.rewriter.SyntacticSimplifier;
import owl.ltl.tlsf.Tlsf;
import solvers.PreciseLTLModelCounter;
import tlsf.CountREModels;
import tlsf.TLSF_Utils;

import java.io.*;
import java.math.BigInteger;
import java.util.*;

public class ModelCountingRanking {
    public static void main(String[] args) throws IOException, InterruptedException {
        String outname = null;
        boolean automaton_counting = false;
        boolean re_counting = false;
        int bound = 0;
        String filepath = null;
        List<String> vars = new LinkedList<>();
        for (String arg : args) {
            if (arg.startsWith("-k=")) {
                String val = arg.replace("-k=", "");
                bound = Integer.parseInt(val);
            } else if (arg.startsWith("-vars=")) {
                vars.addAll(Arrays.asList(arg.replace("-vars=", "").split(",")));
            } else if (arg.startsWith("-out=")) {
                outname = arg.replace("-out=", "");
            } else if (arg.startsWith("-auto")) {
                automaton_counting = true;
            } else if (arg.startsWith("-re")) {
                re_counting = true;
            } else {
                filepath = arg;
            }

        }

        List<Formula> formulas = new LinkedList<>();

        if (filepath == null) {
            System.out.println("Use ./modelcounter.sh [-b=pathToFile] [-k=bound | -vars=a,b,c | -no-precise]");
            return;
        } else if (filepath.endsWith(".tlsf")) {
            Tlsf tlsf = TLSF_Utils.toBasicTLSF(new File(filepath));
            formulas.add(tlsf.toFormula().formula());
            vars = tlsf.variables();
        } else if (filepath.endsWith(".list")) {
            BufferedReader reader;
            reader = new BufferedReader(new FileReader(filepath));

            String line = reader.readLine();
            int numOfVars = 0;
            while (line != null) {

                if (!line.startsWith("--") && line.endsWith(".tlsf")) {
                    Tlsf tlsf = TLSF_Utils.toBasicTLSF(new File(line));
                    formulas.add(tlsf.toFormula().formula());
                    numOfVars = Math.max(numOfVars, tlsf.variables().size());
                }
                line = reader.readLine();
            }
            reader.close();
            for (int i = 0; i < numOfVars; i++) {
                vars.add("v" + i);
            }
        } else {
            BufferedReader reader;
            reader = new BufferedReader(new FileReader(filepath));

            String line = reader.readLine();
            while (line != null) {
                if (!line.startsWith("--"))
                    formulas.add(LtlParser.syntax(line, vars));
                line = reader.readLine();
            }
            reader.close();
        }

        String directoryName = "result";
        String filename = "result/default.out";
        if (outname != null) {
            if (outname.contains(".")) {
                directoryName = outname.substring(0, outname.lastIndexOf('.'));

            }
            File outfolder = new File(directoryName);
            if (!outfolder.exists() && !outfolder.mkdirs()) {
                System.err.println("Failed to create directory: " + directoryName);
            }
            if (outname.contains("/")) {
                filename = directoryName + outname.substring(outname.lastIndexOf('/'));
            } else
                filename = outname;
        }

        if (automaton_counting || re_counting)
            runPrefixesMC(formulas, vars, bound, filename, re_counting);
        else
            runPreciseMC(formulas, vars, bound, filename);

        System.exit(0);
    }

    static void runPreciseMC(List<Formula> formulas, List<String> vars, int bound, String outname) throws IOException, InterruptedException {
        long initialTOTALTime = System.currentTimeMillis();
        int num_of_formulas = formulas.size();
        List<BigInteger>[] solutions = new List[num_of_formulas];
        List<Integer> timeout_formulas = new LinkedList<>();
        int index = 0;
        System.out.println("Counting...");

        for (Formula f : formulas) {
            long initialTime = System.currentTimeMillis();
            System.out.println(index + " Formula: " + LabelledFormula.of(f, vars));
            List<BigInteger> result = countModelsExact(f, vars.size(), bound);
            if (result != null) {
                System.out.println(result);
                long finalTime = System.currentTimeMillis();
                long totalTime = finalTime - initialTime;
                int min = (int) (totalTime) / 60000;
                int sec = (int) (totalTime - min * 60000) / 1000;
                String time = String.format("Time: %s m  %s s", min, sec);
                System.out.println(time);
                if (outname != null) {
                    String filename = outname.replace(".out", index + ".out");
                    writeFile(filename, result, time);
                }
                solutions[index] = result;
            } else {
                System.out.println("MC Timeout reached.");
                timeout_formulas.add(index);
            }
            index++;
        }
        System.out.println("Formula ranking for bounds 1..k");
        SortedMap<BigInteger, List<Integer>>[] ranking = new TreeMap[bound];
        for (int k = 0; k < bound; k++) {
            List<BigInteger> k_values = new LinkedList<>();
            for (int i = 0; i < num_of_formulas; i++) {
                if (solutions[i] != null)
                    k_values.add(solutions[i].get(k));
                else
                    k_values.add(null);
            }

            SortedMap<BigInteger, List<Integer>> order = getBigIntegerListSortedMap(num_of_formulas, timeout_formulas, k_values);
            ranking[k] = order;
            System.out.println((k + 1) + " " + order.values());
        }
        if (outname != null)
            writeRanking(outname, ranking);

        System.out.println("Global ranking...");
        List<BigInteger> totalNumOfModels = new LinkedList<>();
        StringBuilder sumTotalNumOfModels = new StringBuilder();
        for (int i = 0; i < num_of_formulas; i++) {
            BigInteger f_result = BigInteger.ZERO;
            if (solutions[i] == null)
                f_result = null;
            else {
                for (BigInteger v : solutions[i])
                    f_result = f_result.add(v);
            }
            sumTotalNumOfModels.append(i).append(" ").append(f_result).append("\n");
            totalNumOfModels.add(f_result);
        }

        if (outname != null)
            writeRanking(outname.replace(".out", "-summary.out"), sumTotalNumOfModels.toString(), "");

        SortedMap<BigInteger, List<Integer>> global_ranking = new TreeMap<>();
        for (int i = 0; i < num_of_formulas; i++) {
            BigInteger key = totalNumOfModels.get(i);
            if (key != null) {
                List<Integer> value;
                if (global_ranking.containsKey(key))
                    value = global_ranking.get(key);
                else
                    value = new LinkedList<>();
                value.add(i);
                global_ranking.put(key, value);
            }
        }

        StringBuilder global = new StringBuilder();
        StringBuilder flatten_ranking_str = new StringBuilder();
        int[] formula_ranking = new int[num_of_formulas];
        int pos = 0;
        for (BigInteger key : global_ranking.keySet()) {
            global.append(global_ranking.get(key)).append("\n");
            for (Integer f_pos : global_ranking.get(key)) {
                formula_ranking[f_pos] = pos;
                flatten_ranking_str.append(f_pos).append("\n");
            }
            pos++;
        }

        global.append("\nRanking Levels: ").append(pos).append("\n");
        if (!timeout_formulas.isEmpty()) {
            global.append("\nTimeout Formulas: ").append(timeout_formulas);
        }

        StringBuilder formula_ranking_str = new StringBuilder();
        for (int i = 0; i < num_of_formulas; i++) {
            formula_ranking_str.append(formula_ranking[i]).append("\n");
        }
        System.out.println(global);

        long finalTOTALTime = System.currentTimeMillis();
        long totalTime = finalTOTALTime - initialTOTALTime;
        int min = (int) (totalTime) / 60000;
        int sec = (int) (totalTime - min * 60000) / 1000;
        String time = String.format("Time: %s m  %s s", min, sec);
        System.out.println(time);

        if (outname != null) {
            writeRanking(outname.replace(".out", "-global.out"), global.toString(), time);
            writeRanking(outname.replace(".out", "-ranking-by-formula.out"), formula_ranking_str.toString(), "");
            writeRanking(outname.replace(".out", "-ranking.out"), flatten_ranking_str.toString(), "");
        }
    }

    private static SortedMap<BigInteger, List<Integer>> getBigIntegerListSortedMap(int num_of_formulas, List<Integer> timeout_formulas, List<BigInteger> k_values) {
        SortedMap<BigInteger, List<Integer>> order = new TreeMap<>();
        for (int i = 0; i < num_of_formulas; i++) {
            if (timeout_formulas.contains(i))
                continue;
            BigInteger key = k_values.get(i);
            List<Integer> value;
            if (order.containsKey(key))
                value = order.get(key);
            else
                value = new LinkedList<>();
            value.add(i);
            order.put(key, value);
        }
        return order;
    }

    static void runPrefixesMC(List<Formula> formulas, List<String> vars, int bound, String outname, boolean re_counting) throws IOException {
        long initialTOTALTime = System.currentTimeMillis();
        int num_of_formulas = formulas.size();
        BigInteger[] solutions = new BigInteger[num_of_formulas];
        int index = 0;
        System.out.println("Counting...");
        for (Formula f : formulas) {
            long initialTime = System.currentTimeMillis();
            System.out.println(index + " Formula: " + LabelledFormula.of(f, vars));
            BigInteger result = BigInteger.ZERO;
//            result = countExhaustiveAutomataBasedPrefixes(f, vars, bound);
            try {
                if (!re_counting)
                    result = countExhaustiveAutomataBasedPrefixes(f, vars, bound);
                else
                    result = countExhaustivePrefixesRltl(f, vars, bound);
                System.out.println(result);
            } catch (Exception e) {
                System.out.println(e.toString());
            }
            long finalTime = System.currentTimeMillis();
            long totalTime = finalTime - initialTime;
            int min = (int) (totalTime) / 60000;
            int sec = (int) (totalTime - min * 60000) / 1000;
            String time = String.format("Time: %s m  %s s", min, sec);
            System.out.println(time);
            if (outname != null) {
                String filename = outname.replace(".out", (re_counting ? "re-" : "auto-") + index + ".out");
                writeFile(filename, List.of(result), time);
            }
            solutions[index] = result;
            index++;
        }


        System.out.println("Global ranking...");
        SortedMap<BigInteger, List<Integer>> global_ranking = new TreeMap<>();
        for (int i = 0; i < num_of_formulas; i++) {
            BigInteger key = solutions[i];
            if (key != null) {
                List<Integer> value;
                if (global_ranking.containsKey(key))
                    value = global_ranking.get(key);
                else
                    value = new LinkedList<>();
                value.add(i);
                global_ranking.put(key, value);
            }
        }

        StringBuilder global = new StringBuilder();
        StringBuilder flatten_ranking_str = new StringBuilder();
        int[] formula_ranking = new int[num_of_formulas];
//        int i = 0;
        int pos = 0;
        for (BigInteger key : global_ranking.keySet()) {
            global.append(global_ranking.get(key)).append("\n");
//            i += global_ranking.get(key).size();
//            if (i < num_of_formulas-1)
//                global +=", ";
//            else
//                global +="]";

            for (Integer f_pos : global_ranking.get(key)) {
                //int f_pos = refined_formulas.indexOf(f);
                formula_ranking[f_pos] = pos;
                flatten_ranking_str.append(f_pos).append("\n");
            }
            pos++;
        }

        global.append("\nRanking Levels: ").append(pos).append("\n");
        StringBuilder formula_ranking_str = new StringBuilder();
        for (int i = 0; i < num_of_formulas; i++) {
            formula_ranking_str.append(formula_ranking[i]).append("\n");
        }

        System.out.println(global);

        long finalTOTALTime = System.currentTimeMillis();
        long totalTime = finalTOTALTime - initialTOTALTime;
        int min = (int) (totalTime) / 60000;
        int sec = (int) (totalTime - min * 60000) / 1000;
        String time = String.format("Time: %s m  %s s", min, sec);
        System.out.println(time);

        if (outname != null) {
            String filename = outname.replace(".out", (re_counting ? "re-" : "auto-") + "global.out");
            writeRanking(filename, global.toString(), time);
            String ranking_formula_filename = outname.replace(".out", (re_counting ? "re-" : "auto-") + "ranking-by-formula.out");
            writeRanking(ranking_formula_filename, formula_ranking_str.toString(), "");
            String ranking_filename = outname.replace(".out", (re_counting ? "re-" : "auto-") + "ranking.out");
            writeRanking(ranking_filename, flatten_ranking_str.toString(), "");
        }
    }


    static List<BigInteger> countModelsExact(Formula formula, int vars, int bound) throws IOException, InterruptedException {

        PreciseLTLModelCounter counter = new PreciseLTLModelCounter();
        counter.BOUND = bound;

        BigInteger models = counter.count(formula, vars);
        if (models == null)
            return null;


        List<BigInteger> result = new LinkedList<>();
        for (int i = 0; i < bound - 1; i++) {
            result.add(BigInteger.ZERO);
        }
        result.add(models);
        return result;
    }

    static List<BigInteger> countPrefixes(Formula original, Formula refined, List<String> vars, int bound) throws IOException, InterruptedException {
        List<BigInteger> lostModels = new LinkedList<>();
        for (int k = 1; k <= bound; k++) {
            LinkedList<LabelledFormula> formulas = new LinkedList<>();
//            formulas.add(LabelledFormula.of(original,vars));
//            formulas.add(LabelledFormula.of(refined.not(),vars));
            Formula f = Conjunction.of(original, refined.not());
            SyntacticSimplifier simp = new SyntacticSimplifier();
            Formula simplified = f.accept(simp);
//            System.out.println(simplified);
            if (simplified == BooleanConstant.FALSE) {
                lostModels.add(BigInteger.ZERO);
                continue;
            }
//            formulas.add(LabelledFormula.of(simplified,vars));
            for (Set<Formula> clause : NormalForms.toCnf(simplified.nnf())) {
                Formula c = Disjunction.of(clause);
                formulas.add(LabelledFormula.of(c, vars));
            }
            CountREModels counter = new CountREModels();
            BigInteger r = counter.count(formulas, k, false, true);
            if (k > 1) {
                BigInteger previous = lostModels.get(k - 2);
                r = r.subtract(previous);
            }
            lostModels.add(r);
        }

        List<BigInteger> wonModels = new LinkedList<>();
        for (int k = 1; k <= bound; k++) {
            LinkedList<LabelledFormula> formulas = new LinkedList<>();
//            formulas.add(LabelledFormula.of(original.not(),vars));
//            formulas.add(LabelledFormula.of(refined,vars));
            Formula f = Conjunction.of(original.not(), refined);
            SyntacticSimplifier simp = new SyntacticSimplifier();
            Formula simplified = f.accept(simp);
//            System.out.println(simplified);
            if (simplified == BooleanConstant.FALSE) {
                wonModels.add(BigInteger.ZERO);
                continue;
            }
//            formulas.add(LabelledFormula.of(simplified,vars));
            for (Set<Formula> clause : NormalForms.toCnf(simplified.nnf())) {
                Formula c = Disjunction.of(clause);
                formulas.add(LabelledFormula.of(c, vars));
            }
            CountREModels counter = new CountREModels();
            BigInteger r = counter.count(formulas, k, false, true);
            if (k > 1) {
                BigInteger previous = wonModels.get(k - 2);
                r = r.subtract(previous);
            }
            wonModels.add(r);
        }
        List<BigInteger> result = new LinkedList<>();
        for (int i = 0; i < bound; i++) {
            BigInteger pos = lostModels.get(i);
            BigInteger neg = wonModels.get(i);
            result.add(pos.add(neg));
        }

        return result;
    }

    static LabelledFormula getFormula(Formula formula1, Formula formula2, List<String> variables) {
        LabelledFormula form;
        if (formula2 == null)
            form = LabelledFormula.of(formula1, variables);
        else {
            Formula cnf = NormalForms.toCnfFormula(Conjunction.of(formula1, formula2));
            form = LabelledFormula.of(cnf, variables);
        }
        return form;
    }

    static BigInteger countExhaustivePrefixesRltl(Formula f, List<String> vars, int bound) throws IOException, InterruptedException {
        LabelledFormula form_lost = LabelledFormula.of(f, vars);
        CountRltlConv counter = new CountRltlConv();
        return counter.countPrefixes(form_lost, bound);
    }

    static BigInteger countExhaustiveAutomataBasedPrefixes(Formula f, List<String> vars, int bound){
        LabelledFormula form_lost = LabelledFormula.of(f, vars);
//        MatrixBigIntegerModelCounting counter = new MatrixBigIntegerModelCounting(form_lost,false);
        EmersonLeiAutomatonBasedModelCounting counter = new EmersonLeiAutomatonBasedModelCounting(form_lost);

        return counter.count(bound);
    }


    private static void writeFile(String filename, List<BigInteger> result, String time) throws IOException {
        File file = new File(filename);
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write("0 0\n");
        for (int i = 0; i < result.size(); i++) {
            BigInteger sol = result.get(i);
            bw.write("" + (i + 1));
            bw.write(" ");
            bw.write(sol.toString());
            bw.write("\n");
//            System.out.println((i+1) + " " + sol);
        }
        bw.write(time + "\n");
        bw.flush();
        bw.close();
    }

    private static void writeRanking(String filename, SortedMap<BigInteger, List<Integer>>[] ranking) throws IOException {
        File file = new File(filename);
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);

        for (SortedMap<BigInteger, List<Integer>> bigIntegerListSortedMap : ranking) {
            for (BigInteger key : bigIntegerListSortedMap.keySet())
                bw.write(bigIntegerListSortedMap.get(key).toString());
            bw.write("\n");
        }
        bw.close();
    }

    private static void writeRanking(String filename, String ranking, String time) throws IOException {
        File file = new File(filename);
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(ranking + "\n");
        bw.write(time + "\n");
        bw.flush();
        bw.close();
    }
}
