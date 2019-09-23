package main;

import owl.ltl.Formula;
import owl.ltl.parser.LtlParser;
import solvers.PreciseLTLModelCounter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

public class PreciseModelCountingEvaluation {
    public static void main(String[] args) throws IOException, InterruptedException {
        String formula = null;
        String outname = null;
        boolean precise = true;
        int k = 0;
        List<String> vars = new LinkedList<>();
        for (int i = 0; i< args.length; i++ ){
            if(args[i].startsWith("-k=")){
                String val = args[i].replace("-k=","");
                k = Integer.parseInt(val);
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
            else if(args[i].startsWith("-ltl=")){
                formula = args[i].replace("-ltl=","");
            }
            else {
                //assume that the argument with no flag is the formula
                formula = args[i];
            }
        }

        if (formula == null) {
            correctUssage();
            return;
        }

        Formula f = null;
        if (vars.isEmpty())
            f = LtlParser.syntax(formula);
        else
            f = LtlParser.syntax(formula,vars);

        List<BigInteger> result = countModels(f,vars.size(),k,precise);
        if (outname == null)
            outname = "numofmodels.out";

        File file = new File(outname);
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write("0 0\n");
        for (int i = 0; i < result.size(); i++) {
            BigInteger sol = result.get(i);
            bw.write(i+1);
            bw.write(" ");
            bw.write(sol.toString());
            bw.write("\n");
            System.out.println((i+1) + " " + sol);
        }
        bw.close();
    }

    static List<BigInteger> countModels(Formula f, int vars, int bound, boolean precise) throws IOException, InterruptedException {
        List<BigInteger> res = new LinkedList<>();
        for(int k = 1; k <= bound; k++) {
            PreciseLTLModelCounter counter = new PreciseLTLModelCounter();
            counter.BOUND = k;
            if (!precise)
                counter.modelcounter = PreciseLTLModelCounter.MODEL_COUNTER.CACHET;
            BigInteger r = counter.count(f, vars);
            res.add(r);
        }
        return res;
    }

    private static void correctUssage(){
        System.out.println("Use ./modelcounter.sh [-k=bound | -vars=a,b,c | -no-precise] [-ltl=]LTL_formula");
    }
}
