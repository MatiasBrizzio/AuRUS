package geneticalgorithm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import geneticalgorithm.SpecificationChromosome.SPEC_STATUS;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.tlsf.Tlsf;
import tlsf.Formula_Utils;
import tlsf.TLSF_Utils;

public class SpecificationMerger {
	
	
	public static List<Tlsf> merge(Tlsf spec0, Tlsf spec1) {
		return merge(spec0, spec1, SPEC_STATUS.UNKNOWN, SPEC_STATUS.UNKNOWN, 0);
	}

	// level == 0 implements random swap (no guarantee consistency); 
	// level == 1 implements random merge of the assumptions and guarantees (no guarantee consistency);
	// level == 2 swaps assumptions and guarantees preserving consistency; and
	// level == 3 merges the assumptions and guarantees preserving consistency.
	public static List<Tlsf> merge(Tlsf spec0, Tlsf spec1, SPEC_STATUS status0, SPEC_STATUS status1, int level) {
		List<Tlsf> merged_specifications = new LinkedList<>();
		Random rand = new Random(System.currentTimeMillis());
		if (level == 0) {
			//create empty specification
			Tlsf new_spec = TLSF_Utils.empty_spec(spec0);
			
			// set initially
			int turn = rand.nextInt(2);
			if (turn == 0)
				new_spec = TLSF_Utils.change_initially(new_spec, spec0.initially());
			else 
				new_spec = TLSF_Utils.change_initially(new_spec, spec1.initially());
			
			// set preset
			turn = rand.nextInt(2);
			if (turn == 0)
				new_spec = TLSF_Utils.change_preset(new_spec, spec0.preset());
			else 
				new_spec = TLSF_Utils.change_preset(new_spec, spec1.preset());
			
			// set require
			turn = rand.nextInt(2);
			if (turn == 0)
				new_spec = TLSF_Utils.change_require(new_spec, spec0.require());
			else 
				new_spec = TLSF_Utils.change_require(new_spec, spec1.require());
			
			// set assert
			turn = rand.nextInt(2);
			if (turn == 0)
				new_spec = TLSF_Utils.change_assert(new_spec, spec0.assert_());
			else 
				new_spec = TLSF_Utils.change_assert(new_spec, spec1.assert_());
			
			// set assume
			turn = rand.nextInt(2);
			if (turn == 0)
				new_spec = TLSF_Utils.change_assume(new_spec, spec0.assume());
			else 
				new_spec = TLSF_Utils.change_assume(new_spec, spec1.assume());
			
			// set guarantees
			turn = rand.nextInt(2);
			if (turn == 0)
				new_spec = TLSF_Utils.change_guarantees(new_spec, spec0.guarantee());
			else 
				new_spec = TLSF_Utils.change_guarantees(new_spec, spec1.guarantee());
			
			merged_specifications.add(new_spec);
		}
		else if (level == 1) {
			//Create a new spec to fill all tlsf file 
			List<Tlsf> newSpec = merge(spec0,spec1,status0,status1,0);
			Tlsf newspec = newSpec.get(0);
			
			//Get assume form spec0
			LabelledFormula assumspec0 =  LabelledFormula.of(spec0.assume(), spec0.variables());
			List<LabelledFormula> assumesspec0 = Formula_Utils.splitConjunction(assumspec0);
			
			//Get assume from spec1
			LabelledFormula assumspec1 =  LabelledFormula.of(spec1.assume(), spec1.variables());
			List<LabelledFormula> assumesspec1 = Formula_Utils.splitConjunction(assumspec1);
			
			List<Formula> f = getRandomFormulas(assumesspec0);
			f.addAll(getRandomFormulas(assumesspec1));
			newspec = TLSF_Utils.change_assume(newspec,f);
			
			//merge guarantees randomly
			newspec = TLSF_Utils.change_guarantees(newspec, mergeGuarantess(spec0.guarantee(),spec1.guarantee()));		
		}
		else if (level == 2 && status0.compatible(status1)) {
			
			
		}
		return merged_specifications;
	}

	private static List<Formula> mergeGuarantess(List<Formula> guarantee, List<Formula> guarantee2) {
		Random rand1 = new Random(System.currentTimeMillis());
		int amountOfFormulas2 = rand1.nextInt(guarantee2.size());
		int amountOfFormulas1 = rand1.nextInt(guarantee.size());
		
		List<Formula> newg = new ArrayList<Formula>();
		Formula selectedFormula;
		while (newg.size() != amountOfFormulas1) {
			selectedFormula = guarantee.get(rand1.nextInt(guarantee.size()));
			if (newg.contains(selectedFormula)) continue;
			else newg.add(selectedFormula);
		}
		
		List<Formula> newg2 = new ArrayList<Formula>();
		while (newg2.size() != amountOfFormulas2) {
			selectedFormula = guarantee2.get(rand1.nextInt(guarantee2.size()));
			if (newg2.contains(selectedFormula)) continue;
			else newg2.add(selectedFormula);
		}
		
		newg.addAll(newg2);
		return newg;
	}

	private static List<Formula> getRandomFormulas(List<LabelledFormula> assumesspec0) {
		Random rand1 = new Random(System.currentTimeMillis());
		int amountOfFormulas = rand1.nextInt(assumesspec0.size());
		
		List<LabelledFormula> newAssumes = new ArrayList<LabelledFormula>();

		LabelledFormula selectedFormula;
		while (newAssumes.size() != amountOfFormulas) {
			selectedFormula = assumesspec0.get(rand1.nextInt(assumesspec0.size()));
			if (newAssumes.contains(selectedFormula)) continue;
			else newAssumes.add(selectedFormula);
		}
		List<Formula> newAssm = new ArrayList<Formula>();
		for (LabelledFormula lf : newAssumes) {
			newAssm.add(lf.formula());
		}
		return newAssm;
	}
	}
