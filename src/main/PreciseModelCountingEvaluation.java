package main;

import modelcounter.Rltlconv_LTLModelCounter;
import owl.ltl.Conjunction;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.LtlParser;
import solvers.PreciseLTLModelCounter;
import tlsf.CountREModels;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class PreciseModelCountingEvaluation {
    public static void main(String[] args) throws IOException, InterruptedException {
        String formula = null;
        List<String> refinemets = new LinkedList<>();
        String outname = null;
        int solver = 0;
        boolean prefixes = false;
        int bound = 0;
        List<String> vars = new LinkedList<>();
        for (int i = 0; i< args.length; i++ ){
            if(args[i].startsWith("-k=")){
                String val = args[i].replace("-k=","");
                bound = Integer.parseInt(val);
            }
            else if(args[i].startsWith("-vars=")){
                for(String v : args[i].replace("-vars=","").split(","))
                    vars.add(v);
            }
            else if(args[i].startsWith("-out=")){
                outname = args[i].replace("-out=","");
            }
            else if(args[i].startsWith("-cachet")){
                solver = 1;
            }
            else if(args[i].startsWith("-ganak")){
                solver = 2;
            }
            else if(args[i].startsWith("-prefix")){
                prefixes = true;
            }
            else if(args[i].startsWith("-ref=")){
                refinemets.add(args[i].replace("-ref=",""));
            }
            else if(args[i].startsWith("-ltl=")){
                formula = args[i].replace("-ltl=","");
            }
            else {
                //assume that the argument with no flag is the formula
                formula = args[i];
            }
        }

        if (formula == null || refinemets.isEmpty()) {
            correctUssage();
            return;
        }

        Formula original_formula = null;
        if (vars.isEmpty())
            original_formula = LtlParser.syntax(formula);
        else
            original_formula = LtlParser.syntax(formula,vars);

        List<Formula> refined_formulas = new LinkedList<>();
        for(String s : refinemets){
            if (vars.isEmpty())
                refined_formulas.add(LtlParser.syntax(s));
            else
                refined_formulas.add(LtlParser.syntax(s,vars));
        }
        int num_of_formulas = refined_formulas.size();
        List<BigInteger>[] solutions = new List [num_of_formulas];
        int index = 0;
        System.out.println("Counting...");
        for(Formula ref : refined_formulas) {
            System.out.println("Formula: "+ ref);
            List<BigInteger> result = null;
            if (!prefixes)
                result = countModels(original_formula, ref, vars.size(), bound, solver);
            else
                result = countPrefixes(original_formula, ref, vars,bound);
            System.out.println(result);
            if (outname != null) {
                String filename = outname.replace(".out", index + ".out");
                writeFile(filename, result);
            }
            solutions[index] = result;
            index++;
        }
        System.out.println("Formula ranking for bounds 1..k");
        List<Integer>[] ranking = new List [bound];
        for(int k = 0; k < bound; k++){
            List<BigInteger> k_values = new LinkedList<>();
            for(int i = 0; i < num_of_formulas; i++){
                k_values.add(solutions[i].get(k));
            }
            List<BigInteger> k_values_copy = List.copyOf(k_values);
            Collections.sort(k_values);
            List<Integer> order = new LinkedList<>();
            for(int i = 0; i < num_of_formulas; i++){
                order.add(k_values_copy.indexOf(k_values.get(i)));
            }
            ranking[k] = order;
            System.out.println((k+1)+" "+order);
        }
        if (outname != null)
            writeRanking(outname, ranking);

        System.out.println("Global ranking...");
        int[] global_ranking = new int [num_of_formulas];
        List<BigInteger> totalNumOfModels = new LinkedList<>();
        for(int i = 0; i < num_of_formulas; i++){
            BigInteger f_result = BigInteger.ZERO;
            for(BigInteger v : solutions[i])
                f_result = f_result.add(v);
            totalNumOfModels.add(f_result);
        }
        List<BigInteger> total_values_copy =  List.copyOf(totalNumOfModels);
        Collections.sort(totalNumOfModels);
        System.out.print("[");
        for(int i = 0; i < num_of_formulas; i++){
            global_ranking[i] = total_values_copy.indexOf(totalNumOfModels.get(i));
            System.out.print(global_ranking[i]);
            if (i < num_of_formulas-1)
                System.out.print(", ");
            else
                System.out.print("]\n");
        }

        if (outname != null)
            writeRanking(outname.replace(".out", "-global.out"), global_ranking);
    }

    static List<BigInteger> countModels(Formula original, Formula refined, int vars, int bound, int solver) throws IOException, InterruptedException {
        List<BigInteger> lostModels = new LinkedList<>();
        for(int k = 1; k <= bound; k++) {
            PreciseLTLModelCounter counter = new PreciseLTLModelCounter();
            counter.BOUND = k;
            if (solver==1)
                counter.modelcounter = PreciseLTLModelCounter.MODEL_COUNTER.CACHET;
            else if (solver == 2)
            	counter.modelcounter = PreciseLTLModelCounter.MODEL_COUNTER.GANAK;
            Formula f = Conjunction.of(original, refined.not());
            BigInteger r = counter.count(f, vars);
            lostModels.add(r);
        }

        List<BigInteger> wonModels = new LinkedList<>();
        for(int k = 1; k <= bound; k++) {
            PreciseLTLModelCounter counter = new PreciseLTLModelCounter();
            counter.BOUND = k;
            if (solver==1)
                counter.modelcounter = PreciseLTLModelCounter.MODEL_COUNTER.CACHET;
            else if (solver == 2)
            	counter.modelcounter = PreciseLTLModelCounter.MODEL_COUNTER.GANAK;
            Formula f = Conjunction.of(original.not(), refined);
            BigInteger r = counter.count(f, vars);
            wonModels.add(r);
        }
        List<BigInteger> result = new LinkedList<>();
        for(int i = 0; i < bound; i++) {
            BigInteger pos = lostModels.get(i);
            BigInteger neg = wonModels.get(i);
            result.add(pos.add(neg));
        }

        return result;
    }

    static List<BigInteger> countPrefixes(Formula original, Formula refined, List<String> vars, int bound) throws IOException, InterruptedException {
        List<BigInteger> lostModels = new LinkedList<>();
        for(int k = 1; k <= bound; k++) {
            Formula f = Conjunction.of(original, refined.not());
            LinkedList<LabelledFormula> formulas = new LinkedList<>();
            formulas.add(LabelledFormula.of(f,vars));
            CountREModels counter = new CountREModels();
		    BigInteger r = counter.count(formulas, k, false, true);
            lostModels.add(r);
        }

        List<BigInteger> wonModels = new LinkedList<>();
        for(int k = 1; k <= bound; k++) {
            Formula f = Conjunction.of(original.not(), refined);
            LinkedList<LabelledFormula> formulas = new LinkedList<>();
            formulas.add(LabelledFormula.of(f,vars));
            CountREModels counter = new CountREModels();
            BigInteger r = counter.count(formulas, k, false, true);
            wonModels.add(r);
        }
        List<BigInteger> result = new LinkedList<>();
        for(int i = 0; i < bound; i++) {
            BigInteger pos = lostModels.get(i);
            BigInteger neg = wonModels.get(i);
            result.add(pos.add(neg));
        }

        return result;
    }

     private static void writeFile(String filename, List<BigInteger> result) throws IOException {
        File file = new File(filename);
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write("0 0\n");
        for (int i = 0; i < result.size(); i++) {
            BigInteger sol = result.get(i);
            bw.write(""+(i+1));
            bw.write(" ");
            bw.write(sol.toString());
            bw.write("\n");
//            System.out.println((i+1) + " " + sol);
        }
        bw.close();
    }

    private static void writeRanking(String filename, List<Integer>[] ranking) throws IOException {
        File file = new File(filename);
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);

        for (int i = 0; i < ranking.length; i++) {
            bw.write(ranking[i].toString());
            bw.write("\n");
//            System.out.println((i+1) + " " + ranking[i].toString());
        }
        bw.close();
    }

    private static void writeRanking(String filename, int[] ranking) throws IOException {
        File file = new File(filename);
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);

        for (int i = 0; i < ranking.length; i++) {
            bw.write(ranking[i]);
            bw.write(", ");
//            System.out.println((i+1) + " " + ranking[i].toString());
        }
        bw.close();
    }
    private static void correctUssage(){
        System.out.println("Use ./modelcounter.sh [-ref=refined=formula | -k=bound | -vars=a,b,c | -no-precise] [-ltl=]LTL_original_formula");
    }
}
