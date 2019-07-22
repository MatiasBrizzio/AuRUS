package geneticalgorithm;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import geneticalgorithm.SpecificationChromosome.SPEC_STATUS;
import owl.ltl.Formula;
import owl.ltl.tlsf.Tlsf;
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
			
				
		}
		else if (level == 2 && status0.compatible(status1)) {
			
			
		}
		return merged_specifications;
	}
	

}
