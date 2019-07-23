package geneticalgorithm;

import java.util.Random;

import geneticalgorithm.SpecificationChromosome.SPEC_STATUS;
import owl.ltl.Literal;
import owl.ltl.tlsf.Tlsf;
import tlsf.TLSF_Utils;

public class SpecificationMutator {
	
	public static Tlsf mutate(Tlsf spec, SPEC_STATUS status) {
		Random rand = new Random(System.currentTimeMillis());

		//create empty specification
		Tlsf new_spec = TLSF_Utils.empty_spec(spec);
		
		if (spec.assume() instanceof Literal)
		{ }
		
		// set initially
//		int turn = rand.nextInt(2);
//		if (turn == 0)
//			new_spec = TLSF_Utils.change_initially(new_spec, spec0.initially());
//		else 
//			new_spec = TLSF_Utils.change_initially(new_spec, spec1.initially());
//		
//		// set preset
//		turn = rand.nextInt(2);
//		if (turn == 0)
//			new_spec = TLSF_Utils.change_preset(new_spec, spec0.preset());
//		else 
//			new_spec = TLSF_Utils.change_preset(new_spec, spec1.preset());
//		
//		// set require
//		turn = rand.nextInt(2);
//		if (turn == 0)
//			new_spec = TLSF_Utils.change_require(new_spec, spec0.require());
//		else 
//			new_spec = TLSF_Utils.change_require(new_spec, spec1.require());
//		
//		// set assert
//		turn = rand.nextInt(2);
//		if (turn == 0)
//			new_spec = TLSF_Utils.change_assert(new_spec, spec0.assert_());
//		else 
//			new_spec = TLSF_Utils.change_assert(new_spec, spec1.assert_());
//		
//		// set assume
//		turn = rand.nextInt(2);
//		if (turn == 0)
//			new_spec = TLSF_Utils.change_assume(new_spec, spec0.assume());
//		else 
//			new_spec = TLSF_Utils.change_assume(new_spec, spec1.assume());
//		
//		// set guarantees
//		turn = rand.nextInt(2);
//		if (turn == 0)
//			new_spec = TLSF_Utils.change_guarantees(new_spec, spec0.guarantee());
//		else 
//			new_spec = TLSF_Utils.change_guarantees(new_spec, spec1.guarantee());
		
		return new_spec;
	}

}
