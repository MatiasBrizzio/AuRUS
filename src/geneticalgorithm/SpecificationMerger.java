package geneticalgorithm;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import geneticalgorithm.SpecificationChromosome.SPEC_STATUS;
import owl.ltl.tlsf.Tlsf;

public class SpecificationMerger {
	
	
	public static List<Tlsf> merge(Tlsf spec0, Tlsf spec1) {
		return merge(spec0, spec1, SPEC_STATUS.UNKNOWN, SPEC_STATUS.UNKNOWN, 0);
	}

	// level == 0 implements random merge; 
	// level == 1 merges the specifications according their status; and
	// level == 2 merges the assumptions and guarantees, whether it can be applied.
	public static List<Tlsf> merge(Tlsf spec0, Tlsf spec1, SPEC_STATUS status0, SPEC_STATUS status1, int level) {
		if (!status0.compatible(status1))
			return new LinkedList<>();
		Random rand = new Random(System.currentTimeMillis());
		if (level == 0) {
			Tlsf new_spec = TLSF_Utils.empty_spec(spec0);
			
		}
		return null;
	}
}
