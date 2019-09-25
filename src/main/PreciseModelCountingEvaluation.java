package main;

import owl.ltl.Conjunction;
import owl.ltl.Formula;
import owl.ltl.parser.LtlParser;
import solvers.PreciseLTLModelCounter;

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
        boolean precise = true;
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
            else if(args[i].startsWith("-no-precise")){
                precise = false;
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
            List<BigInteger> result = countModels(original_formula, ref, vars.size(), bound, precise);
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
    }

    static List<BigInteger> countModels(Formula original, Formula refined, int vars, int bound, boolean precise) throws IOException, InterruptedException {
        List<BigInteger> lostModels = new LinkedList<>();
        for(int k = 1; k <= bound; k++) {
            PreciseLTLModelCounter counter = new PreciseLTLModelCounter();
            counter.BOUND = k;
            if (!precise)
                counter.modelcounter = PreciseLTLModelCounter.MODEL_COUNTER.CACHET;
            Formula f = Conjunction.of(original, refined.not());
            BigInteger r = counter.count(f, vars);
            lostModels.add(r);
        }

        List<BigInteger> wonModels = new LinkedList<>();
        for(int k = 1; k <= bound; k++) {
            PreciseLTLModelCounter counter = new PreciseLTLModelCounter();
            counter.BOUND = k;
            if (!precise)
                counter.modelcounter = PreciseLTLModelCounter.MODEL_COUNTER.CACHET;
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

    private static void correctUssage(){
        System.out.println("Use ./modelcounter.sh [-ref=refined=formula | -k=bound | -vars=a,b,c | -no-precise] [-ltl=]LTL_original_formula");
    }
}
