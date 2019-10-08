package main;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.LtlParser;
import owl.ltl.tlsf.Tlsf;
import tlsf.Formula_Utils;
import tlsf.TLSF_Utils;
import main.PreciseModelCountingEvaluation;

public class PreciseModelCountingEvaluationTest {

	@Test
	public void Test1() throws IOException, InterruptedException {
		List<String> vars = List.of("a","b","c");
		
		BufferedReader reader;
		reader = new BufferedReader(new FileReader(
					"examples/modelcountExamples/P0.5N3L5.form"));
		//First formula as original one
		String line = reader.readLine();
		Formula original_formula = LtlParser.syntax(line, vars);
	
		List<Formula> refined_formulas = new ArrayList<Formula>();
		line = reader.readLine();
		while (line != null) {
			refined_formulas.add(LtlParser.syntax(line, vars));
			line = reader.readLine();
		}
		reader.close();
		
        int num_of_formulas = refined_formulas.size();
        List<BigInteger>[] solutions = new List [num_of_formulas];
        List<BigInteger>[] solutions2 = new List [num_of_formulas];
        int index = 0;
        int bound = 5;
        boolean prefixes = true;

        System.out.println("Counting...");
        for(Formula ref : refined_formulas) {
            System.out.println(index + " Formula: "+ LabelledFormula.of(ref,vars));
            List<BigInteger> result = null;
            List<BigInteger> result2 = null;
            
            if (!prefixes) {
                result = PreciseModelCountingEvaluation.countModels(original_formula, ref, vars.size(), bound, 1);
                result2 = PreciseModelCountingEvaluation.countModels(ref, original_formula, vars.size(), bound, 1);
            }
            else {
                result = PreciseModelCountingEvaluation.countPrefixes(original_formula, ref, vars,bound);
                result2 = PreciseModelCountingEvaluation.countPrefixes(ref, original_formula, vars,bound);
            }
                      
			solutions[index] = result;
			solutions2[index] = result2;
            index++;
        }
        
        
        System.out.println("Formula ranking for bounds 1..k");
        List<Integer>[] ranking = new List [bound];
        List<Integer>[] ranking2 = new List [bound];
        
        for(int k = 0; k < bound; k++){
            List<BigInteger> k_values = new LinkedList<>();
            List<BigInteger> k_values2 = new LinkedList<>();
            
            for(int i = 0; i < num_of_formulas; i++){
                k_values.add(solutions[i].get(k));
                k_values2.add(solutions2[i].get(k));
            }
            List<BigInteger> k_values_copy = List.copyOf(k_values);
            List<BigInteger> k_values_copy2 = List.copyOf(k_values2);
            
            Collections.sort(k_values);
            Collections.sort(k_values2);
            
            List<Integer> order = new LinkedList<>();
            List<Integer> order2 = new LinkedList<>();
            
            for(int i = 0; i < num_of_formulas; i++){
                order.add(k_values_copy.indexOf(k_values.get(i)));
                order2.add(k_values_copy2.indexOf(k_values2.get(i)));
            }
            
            ranking[k] = order;
            ranking2[k] = order2;
        
            for (int i = 0; i < num_of_formulas; i++) {
	            System.out.println("original " + LabelledFormula.of(original_formula, vars) + 
	            			"-> ref: " + LabelledFormula.of(refined_formulas.get(i), vars) + " k: " + (k+1)+
	            			" "+ ranking[k]);
	            	System.out.println("ref " +LabelledFormula.of(refined_formulas.get(i), vars) + 
	            			"-> original: " +  LabelledFormula.of(original_formula, vars)  + " k: " + (k+1)+
	            			" "+ ranking2[k]);            
            }
            //System.out.println("ref -> original: "+(k+1)+" "+order2);
        }
        
        System.out.println("Global ranking...");
        int[] global_ranking = new int [num_of_formulas];
        int[] global_ranking2 = new int [num_of_formulas];
        
        List<BigInteger> totalNumOfModels = new LinkedList<>();
        List<BigInteger> totalNumOfModels2 = new LinkedList<>();
        
        for(int i = 0; i < num_of_formulas; i++){
            BigInteger f_result = BigInteger.ZERO;
            BigInteger f_result2 = BigInteger.ZERO;
            if (!prefixes) {
            	for(BigInteger v : solutions[i])
            		f_result = f_result.add(v);
            	for(BigInteger v : solutions2[i])
            		f_result2 = f_result2.add(v);
            }
            else {
            	f_result = solutions[i].get(bound-1);
            	f_result2 = solutions2[i].get(bound-1);
            }
            totalNumOfModels.add(f_result);
            totalNumOfModels2.add(f_result2);
        }
        List<BigInteger> total_values_copy =  List.copyOf(totalNumOfModels);
        List<BigInteger> total_values_copy2 =  List.copyOf(totalNumOfModels2);
        Collections.sort(totalNumOfModels);
        Collections.sort(totalNumOfModels2);
        String global = "[";

        for(int i = 0; i < num_of_formulas; i++){
            global_ranking[i] = total_values_copy.indexOf(totalNumOfModels.get(i));
            global += ""+global_ranking[i];
            if (i < num_of_formulas-1)
            	global +=", ";
            else
            	global +="]";
        }
        System.out.println(global);
        global = "[";
        for(int i = 0; i < num_of_formulas; i++){
            global_ranking2[i] = total_values_copy2.indexOf(totalNumOfModels2.get(i));
            global += ""+global_ranking2[i];
            if (i < num_of_formulas-1)
            	global +=", ";
            else
            	global +="]";
        }
        System.out.println(global);
	}

}
