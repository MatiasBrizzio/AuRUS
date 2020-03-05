package main;

import modelcounter.*;
import owl.ltl.BooleanConstant;
import owl.ltl.Conjunction;
import owl.ltl.Disjunction;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.LtlParser;
import owl.ltl.rewriter.NormalForms;
import owl.ltl.rewriter.SyntacticSimplifier;
import solvers.PreciseLTLModelCounter;
import tlsf.CountREModels;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

public class PreciseModelCountingEvaluation {
    public static void main(String[] args) throws IOException, InterruptedException {
        String formula = null;
        List<String> refinemets = new LinkedList<>();
        String outname = null;
        int solver = 0;
        boolean re_counting = false;
        boolean automaton_counting = false;
        int bound = 0;
        String filepath = null;
        boolean benchmarkusage = false;
        List<String> vars = new LinkedList<>();
        for (int i = 0; i< args.length; i++ ){
        	if(args[i].startsWith("-b=")) {
        		String val = args[i].replace("-b=","");
        		filepath = val;
        		benchmarkusage = true;
        	}
            else if(args[i].startsWith("-k=")){
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
            else if(args[i].startsWith("-re")){
                re_counting = true;
            }
            else if(args[i].startsWith("-auto")){
                automaton_counting = true;
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
        
        Formula original_formula = null;
        List<Formula> refined_formulas = new LinkedList<>();
        
        if (benchmarkusage && filepath == null) {
        	 System.out.println("Use ./modelcounter.sh [-b=pathToFile] [-k=bound | -vars=a,b,c | -no-precise]");
        	 return;
        }
        else if (benchmarkusage && filepath != null) {
     		
    		BufferedReader reader;
    		reader = new BufferedReader(new FileReader(filepath));
    		
    		//First formula as original one
    		if (formula == null)
    		    formula = reader.readLine();

    		original_formula = LtlParser.syntax(formula, vars);
    	
    		refined_formulas = new ArrayList<Formula>();
    		String line = reader.readLine();
    		while (line != null ) {
    		    if (!line.startsWith("--"))
        			refined_formulas.add(LtlParser.syntax(line, vars));
    			line = reader.readLine();
    		}
    		reader.close();
        }
        else if (formula == null || refinemets.isEmpty()) {
            correctUssage();
            return;
        }
        
        if (!benchmarkusage) {
            if (vars.isEmpty())
                original_formula = LtlParser.syntax(formula);
            else
                original_formula = LtlParser.syntax(formula,vars);
            
            for(String s : refinemets){
                if (vars.isEmpty())
                    refined_formulas.add(LtlParser.syntax(s));
                else
                    refined_formulas.add(LtlParser.syntax(s,vars));
            }       
        }
        String directoryName = "result" ;
        String filename = "result/default.out";
        if (outname != null) {
            if (outname.contains(".")) {
                directoryName = outname.substring(0, outname.lastIndexOf('.'));

            }
            File outfolder = new File(directoryName);
            if (!outfolder.exists())
                outfolder.mkdir();
            if(outname.contains("/")) {
                filename = directoryName + outname.substring(outname.lastIndexOf('/'));
            }
            else
                filename = outname;
        }

        if (automaton_counting || re_counting)
            runPrefixesMC(automaton_counting,original_formula,refined_formulas,vars,bound,filename);
        else
            runPreciseMC(original_formula,refined_formulas,vars,bound,solver,filename);

    }

    static void runPreciseMC(Formula original_formula, List<Formula> refined_formulas, List<String> vars, int bound, int solver, String outname) throws IOException, InterruptedException {
        long initialTOTALTime = System.currentTimeMillis();
        int num_of_formulas = refined_formulas.size();
        List<BigInteger>[] solutions = new List [num_of_formulas];
        List<Integer> timeout_formulas = new LinkedList<>();
        int index = 0;
        System.out.println("Base: " +LabelledFormula.of(original_formula,vars));
        System.out.println("Counting...");

        for(Formula ref : refined_formulas) {
            long initialTime = System.currentTimeMillis();
            System.out.println(index+" Formula: "+ LabelledFormula.of(ref,vars));
            List<BigInteger> result = countModelsExact(original_formula, ref, vars.size(), bound, solver);
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
            }
            else {
                System.out.println("MC Timeout reached.");
                timeout_formulas.add(index);
            }
            index++;
        }
        System.out.println("Formula ranking for bounds 1..k");
        SortedMap<BigInteger,List<Integer>>[] ranking = new TreeMap [bound];
        for(int k = 0; k < bound; k++){
            List<BigInteger> k_values = new LinkedList<>();
            for(int i = 0; i < num_of_formulas; i++){
                if (solutions[i] != null)
                    k_values.add(solutions[i].get(k));
                else
                    k_values.add(null);
            }

            SortedMap<BigInteger,List<Integer>> order = new TreeMap<>();
            for(int i = 0; i < num_of_formulas; i++){
                if (timeout_formulas.contains(i))
                    continue;
                BigInteger key = k_values.get(i);
                List<Integer> value ;
                if (order.containsKey(key))
                    value = order.get(key);
                else
                    value = new LinkedList<>();
                value.add(i);
                order.put(key,value);
            }
            ranking[k] = order;
            System.out.println((k+1)+" "+order.values());
        }
        if (outname != null)
            writeRanking(outname, ranking);

        System.out.println("Global ranking...");
        List<BigInteger> totalNumOfModels = new LinkedList<>();
        String sumTotalNumOfModels = "";
        for(int i = 0; i < num_of_formulas; i++){
            BigInteger f_result = BigInteger.ZERO;
            if (solutions[i] == null)
                f_result = null;
            else {
                for(BigInteger v : solutions[i])
                    f_result = f_result.add(v);
            }
            sumTotalNumOfModels += i + " " + f_result + "\n";
            totalNumOfModels.add(f_result);
        }

        if (outname != null)
            writeRanking(outname.replace(".out", "-summary.out"), sumTotalNumOfModels, "");

        SortedMap<BigInteger,List<Integer>> global_ranking = new TreeMap<>();
        for(int i = 0; i < num_of_formulas; i++){
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

        String global = "";
        String flatten_ranking_str = "";
        int[] formula_ranking = new int[num_of_formulas];
//        int i = 0;
        int pos = 0;
        for(BigInteger key : global_ranking.keySet()){
            global += global_ranking.get(key)+"\n";
//            i += global_ranking.get(key).size();
//            if (i < num_of_formulas-1)
//                global +=", ";
//            else
//                global +="]";
            for(Integer f_pos : global_ranking.get(key)) {
                //int f_pos = refined_formulas.indexOf(f);
                formula_ranking[f_pos] = pos;
                flatten_ranking_str += f_pos+ "\n";
            }
            pos++;
        }

        global += "\nRanking Levels: " + pos +"\n";
        if (!timeout_formulas.isEmpty()) {
            global += "\nTimeout Formulas: " + timeout_formulas.toString();
        }

        String formula_ranking_str = "";
        for(int i = 0; i < num_of_formulas; i++) {
            formula_ranking_str += formula_ranking[i]+"\n";
        }
        System.out.println(global);

        long finalTOTALTime = System.currentTimeMillis();
        long totalTime = finalTOTALTime-initialTOTALTime;
        int min = (int) (totalTime)/60000;
        int sec = (int) (totalTime - min*60000)/1000;
        String time = String.format("Time: %s m  %s s",min, sec);
        System.out.println(time);

        if (outname != null) {
            writeRanking(outname.replace(".out", "-global.out"), global, time);
            writeRanking(outname.replace(".out", "-ranking-by-formula.out"), formula_ranking_str, "");
            writeRanking(outname.replace(".out", "-ranking.out"), flatten_ranking_str, "");
        }
    }

    static void runPrefixesMC(boolean automaton, Formula original_formula, List<Formula> refined_formulas, List<String> vars, int bound, String outname) throws IOException, InterruptedException {
        long initialTOTALTime = System.currentTimeMillis();
        int num_of_formulas = refined_formulas.size();
        BigInteger[] solutions = new BigInteger [num_of_formulas];
        int index = 0;
        System.out.println("Counting...");
        for(Formula ref : refined_formulas) {
            long initialTime = System.currentTimeMillis();
            System.out.println(index+" Formula: "+ LabelledFormula.of(ref,vars));
            BigInteger result ;
            if (automaton)
                result = countExhaustiveAutomataBasedPrefixes(original_formula, ref, vars, bound);
            else
                result = countExhaustivePrefixesRltl(original_formula, ref, vars, bound);
            System.out.println(result);
            long finalTime = System.currentTimeMillis();
            long totalTime = finalTime-initialTime;
            int min = (int) (totalTime)/60000;
            int sec = (int) (totalTime - min*60000)/1000;
            String time = String.format("Time: %s m  %s s",min, sec);
            System.out.println(time);
            if (outname != null) {
                String filename = automaton?outname.replace(".out", "auto-"+index + ".out"):outname.replace(".out", "re-"+index + ".out");
                writeFile(filename, List.of(result), time);
            }
            solutions[index] = result;
            index++;
        }


        System.out.println("Global ranking...");
        SortedMap<BigInteger,List<Integer>> global_ranking = new TreeMap<>();
        for(int i = 0; i < num_of_formulas; i++){
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

        String global = "";
        String flatten_ranking_str = "";
        int[] formula_ranking = new int[num_of_formulas];
//        int i = 0;
        int pos = 0;
        for(BigInteger key : global_ranking.keySet()){
            global += global_ranking.get(key)+"\n";
//            i += global_ranking.get(key).size();
//            if (i < num_of_formulas-1)
//                global +=", ";
//            else
//                global +="]";

            for(Integer f_pos : global_ranking.get(key)) {
                //int f_pos = refined_formulas.indexOf(f);
                formula_ranking[f_pos] = pos;
                flatten_ranking_str += f_pos+ "\n";
            }
            pos++;
        }

        global += "\nRanking Levels: " + pos +"\n";
        String formula_ranking_str = "";
        for(int i = 0; i < num_of_formulas; i++) {
            formula_ranking_str += formula_ranking[i]+"\n";
        }

        System.out.println(global);

        long finalTOTALTime = System.currentTimeMillis();
        long totalTime = finalTOTALTime-initialTOTALTime;
        int min = (int) (totalTime)/60000;
        int sec = (int) (totalTime - min*60000)/1000;
        String time = String.format("Time: %s m  %s s",min, sec);
        System.out.println(time);

        if (outname != null) {
            String filename = automaton ? outname.replace(".out", "auto-global.out") : outname.replace(".out", "re-global.out");
            writeRanking(filename, global, time);
            String ranking_formula_filename = automaton ? outname.replace(".out", "auto-ranking-by-formula.out") : outname.replace(".out", "re-ranking-by-formula.out");
            writeRanking(ranking_formula_filename, formula_ranking_str, "");
            String ranking_filename = automaton ? outname.replace(".out", "auto-ranking.out") : outname.replace(".out", "re-ranking.out");
            writeRanking(ranking_filename, flatten_ranking_str, "");
        }
    }

    static List<BigInteger> countModels(Formula original, Formula refined, int vars, int bound, int solver) throws IOException, InterruptedException {
//        List<BigInteger> lostModels = new LinkedList<>();
//        for(int k = 1; k <= bound; k++) {
//            PreciseLTLModelCounter counter = new PreciseLTLModelCounter();
//            counter.BOUND = k;
//            if (solver==1)
//                counter.modelcounter = PreciseLTLModelCounter.MODEL_COUNTER.CACHET;
//            else if (solver == 2)
//            	counter.modelcounter = PreciseLTLModelCounter.MODEL_COUNTER.GANAK;
//            Formula f = Conjunction.of(original, refined.not());
//            BigInteger r = counter.count(f, vars);
//            if (r == null)
//                return null;
//            lostModels.add(r);
//        }
//
//        List<BigInteger> wonModels = new LinkedList<>();
//        for(int k = 1; k <= bound; k++) {
//            PreciseLTLModelCounter counter = new PreciseLTLModelCounter();
//            counter.BOUND = k;
//            if (solver==1)
//                counter.modelcounter = PreciseLTLModelCounter.MODEL_COUNTER.CACHET;
//            else if (solver == 2)
//            	counter.modelcounter = PreciseLTLModelCounter.MODEL_COUNTER.GANAK;
//            Formula f = Conjunction.of(original.not(), refined);
//            BigInteger r = counter.count(f, vars);
//            if (r == null)
//                return null;
//            wonModels.add(r);
//        }
        List<BigInteger> result = new LinkedList<>();
//        for(int i = 0; i < bound; i++) {
//            BigInteger neg = lostModels.get(i);
//            BigInteger pos = wonModels.get(i);
//            result.add(neg.add(pos));
//        }
//
        return result;
    }

    static List<BigInteger> countModelsExact(Formula original, Formula refined, int vars, int bound, int solver) throws IOException, InterruptedException {

        PreciseLTLModelCounter counter = new PreciseLTLModelCounter();
        counter.BOUND = bound;
        if (solver==1)
            counter.modelcounter = PreciseLTLModelCounter.MODEL_COUNTER.CACHET;
        else if (solver == 2)
            counter.modelcounter = PreciseLTLModelCounter.MODEL_COUNTER.GANAK;
        Formula f = Conjunction.of(original, refined.not());
        BigInteger lostModels = counter.count(f, vars);
        if (lostModels == null)
            return null;


        PreciseLTLModelCounter counter2 = new PreciseLTLModelCounter();
        counter2.BOUND = bound;
        if (solver==1)
            counter2.modelcounter = PreciseLTLModelCounter.MODEL_COUNTER.CACHET;
        else if (solver == 2)
            counter2.modelcounter = PreciseLTLModelCounter.MODEL_COUNTER.GANAK;
        Formula f2 = Conjunction.of(original.not(), refined);
        BigInteger wonModels = counter2.count(f2, vars);
        if (wonModels == null)
            return null;

        List<BigInteger> result = new LinkedList<>();
        for(int i = 0; i < bound-1; i++) {
            result.add(BigInteger.ZERO);
        }
        result.add(lostModels.add(wonModels));
        return result;
    }

    static List<BigInteger> countPrefixes(Formula original, Formula refined, List<String> vars, int bound) throws IOException, InterruptedException {
        List<BigInteger> lostModels = new LinkedList<>();
        for(int k = 1; k <= bound; k++) {
            LinkedList<LabelledFormula> formulas = new LinkedList<>();
//            formulas.add(LabelledFormula.of(original,vars));
//            formulas.add(LabelledFormula.of(refined.not(),vars));
            Formula f = Conjunction.of(original, refined.not());
            SyntacticSimplifier simp = new SyntacticSimplifier();
            Formula simplified = f.accept(simp);
//            System.out.println(simplified);
            if(simplified == BooleanConstant.FALSE) {
            	lostModels.add(BigInteger.ZERO);
            	continue;
            }
//            formulas.add(LabelledFormula.of(simplified,vars));
            for(Set<Formula> clause : NormalForms.toCnf(simplified.nnf())) {
    			Formula c = Disjunction.of(clause);
    			formulas.add(LabelledFormula.of(c, vars));
    		}
            CountREModels counter = new CountREModels();
            BigInteger r = counter.count(formulas, k, false, true);
            if (k > 1) {
            	BigInteger previous = lostModels.get(k-2);
            	r = r.subtract(previous);
            }
            lostModels.add(r);
        }

        List<BigInteger> wonModels = new LinkedList<>();
        for(int k = 1; k <= bound; k++) {
            LinkedList<LabelledFormula> formulas = new LinkedList<>();
//            formulas.add(LabelledFormula.of(original.not(),vars));
//            formulas.add(LabelledFormula.of(refined,vars));
            Formula f = Conjunction.of(original.not(), refined);
            SyntacticSimplifier simp = new SyntacticSimplifier();
            Formula simplified = f.accept(simp);
//            System.out.println(simplified);
            if(simplified == BooleanConstant.FALSE) {
            	wonModels.add(BigInteger.ZERO);
            	continue;
            }
//            formulas.add(LabelledFormula.of(simplified,vars));
            for(Set<Formula> clause : NormalForms.toCnf(simplified.nnf())) {
    			Formula c = Disjunction.of(clause);
    			formulas.add(LabelledFormula.of(c, vars));
    		}
            CountREModels counter = new CountREModels();
            BigInteger r = counter.count(formulas, k, false, true);
            if (k > 1) {
            	BigInteger previous = wonModels.get(k-2);
            	r = r.subtract(previous);
            }
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
    
    static LabelledFormula getFormula(Formula formula1, Formula formula2, List<String> variables) {
    	LabelledFormula form = null;
		if (formula2 == null)
			form = LabelledFormula.of(formula1, variables);
		else {
			Formula cnf = NormalForms.toCnfFormula(Conjunction.of(formula1,formula2));
			form = LabelledFormula.of(cnf, variables);
		}
		return form;
    }
    static List<BigInteger> countPrefixesRltl(Formula original, Formula refined, List<String> vars, int bound) throws IOException, InterruptedException {
        List<BigInteger> lostModels = new LinkedList<>();
//        List<String> alphabet = genAlphabet(vars.size());
        for(int k = 1; k <= bound; k++) {
//            LinkedList<String> formulas = new LinkedList<>();
//            formulas.add(toLambConvSyntax(original,alphabet));
//            formulas.add(toLambConvSyntax(refined.not(),alphabet));
//        	LinkedList<LabelledFormula> formulas = new LinkedList<>();
//        	formulas.add(LabelledFormula.of(original, alphabet));
//        	formulas.add(LabelledFormula.of(refined.not(), alphabet));
//            String alph = alphabet.toString();
        	LabelledFormula form = getFormula(original, refined.not(), vars);
        	CountRltlConv counter = new CountRltlConv();
		    BigInteger r = counter.countPrefixes(form, k);
            lostModels.add(r);
        }

        List<BigInteger> wonModels = new LinkedList<>();
        for(int k = 1; k <= bound; k++) {
//        	LinkedList<String> formulas = new LinkedList<>();
//            formulas.add(toLambConvSyntax(original.not(),alphabet));
//            formulas.add(toLambConvSyntax(refined,alphabet));
//        	LinkedList<LabelledFormula> formulas = new LinkedList<>();
//        	formulas.add(LabelledFormula.of(original.not(), alphabet));
//        	formulas.add(LabelledFormula.of(refined, alphabet));
//            String alph = alphabet.toString();
        	LabelledFormula form = getFormula(original.not(), refined, vars);
            CountRltlConv counter = new CountRltlConv();
		    BigInteger r = counter.countPrefixes(form, k);
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

    static BigInteger countExhaustivePrefixesRltl(Formula original, Formula refined, List<String> vars, int bound) throws IOException, InterruptedException {
        Formula conj_lost = Conjunction.of(original, refined.not());
        LabelledFormula form_lost = LabelledFormula.of(conj_lost, vars);
        CountRltlConv counter = new CountRltlConv();
        BigInteger lostModels = counter.countPrefixes(form_lost,bound);

        Formula conj_won = Conjunction.of(original.not(), refined);
        LabelledFormula form_won = LabelledFormula.of(conj_won, vars);
        CountRltlConv counter2 = new CountRltlConv();
        BigInteger wonModels = counter2.countPrefixes(form_won,bound);
        BigInteger result = lostModels.add(wonModels);
        return result;
    }
    static BigInteger countExhaustiveAutomataBasedPrefixes(Formula original, Formula refined, List<String> vars, int bound) throws IOException, InterruptedException {

        Formula conj_lost = Conjunction.of(original, refined.not());
        LabelledFormula form_lost = LabelledFormula.of(conj_lost, vars);
        MatrixBigIntegerModelCounting counter = new MatrixBigIntegerModelCounting(form_lost,false);
        BigInteger lostModels = counter.count(bound);

        Formula conj_won = Conjunction.of(original.not(), refined);
        LabelledFormula form_won = LabelledFormula.of(conj_won, vars);
        MatrixBigIntegerModelCounting counter2 = new MatrixBigIntegerModelCounting(form_won,false);
        BigInteger wonModels = counter2.count(bound);
        BigInteger result = lostModels.add(wonModels);
        return result;
    }

    static List<BigInteger> countAutomataBasedPrefixes(Formula original, Formula refined, List<String> vars, int bound) throws IOException, InterruptedException {
        List<BigInteger> lostModels = new LinkedList<>();
        for(int k = 1; k <= bound; k++) {
            Formula conj = Conjunction.of(original, refined.not());
            LabelledFormula form = LabelledFormula.of(conj, vars);
            AutomataBasedModelCounting counter = new AutomataBasedModelCounting(form,false);
            BigInteger r = counter.count(k);
            lostModels.add(r);
        }

        List<BigInteger> wonModels = new LinkedList<>();
        for(int k = 1; k <= bound; k++) {
            Formula conj = Conjunction.of(original.not(), refined);
            LabelledFormula form = LabelledFormula.of(conj, vars);
            AutomataBasedModelCounting counter = new AutomataBasedModelCounting(form,false);
            BigInteger r = counter.count(k);
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
    
    static List<String> genAlphabet(int n){
    	List<String> alphabet = new LinkedList();
		for (int i = 0; i < n; i++) {
			String v = ""+Character.toChars(97+i)[0];
			alphabet.add(v);
		}
		return alphabet;
    }
    static String toLambConvSyntax(Formula f, List<String> alphabet) {
		String LTLFormula = LabelledFormula.of(f, alphabet).toString();
		LTLFormula = LTLFormula.replaceAll("&", "&&");
		LTLFormula = LTLFormula.replaceAll("\\|", "||");
		return new String(LTLFormula); 
	}

     private static void writeFile(String filename, List<BigInteger> result, String time) throws IOException {
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
         bw.write(time +"\n");
        bw.flush();
        bw.close();
    }

    private static void writeRanking(String filename, SortedMap<BigInteger,List<Integer>>[] ranking) throws IOException {
        File file = new File(filename);
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);

        for (int i = 0; i < ranking.length; i++) {
            for(BigInteger key : ranking[i].keySet())
                bw.write(ranking[i].get(key).toString());
            bw.write("\n");
//            System.out.println((i+1) + " " + ranking[i].toString());
        }
        bw.close();
    }

    private static void writeRanking(String filename, String ranking, String time) throws IOException {
        File file = new File(filename);
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(ranking+"\n");
        bw.write(time+"\n");
        bw.flush();
        bw.close();
    }
    private static void correctUssage(){
        System.out.println("Use ./modelcounter.sh [-ref=refined=formula | -k=bound | -vars=a,b,c | -no-precise] [-ltl=]LTL_original_formula");
    }
}
