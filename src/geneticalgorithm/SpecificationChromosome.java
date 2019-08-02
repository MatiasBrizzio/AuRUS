package geneticalgorithm;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.lagodiuk.ga.Chromosome;
import owl.ltl.parser.TlsfParser;
import owl.ltl.tlsf.Tlsf;
import tlsf.TLSF_Utils;

public class SpecificationChromosome implements Chromosome<SpecificationChromosome>, Cloneable {
	
	// 			 			/  ASSUMPTIONS  \
	// UNKNOWN --  BOTTOM --		  		 -- CONTRADICTORY --  UNREALIZABLE  --  REALIZABLE
	//			 			\  GUARANTEES   /
	
	// we distinguish the particular case when the specification is realizable
	// just because the assumptions are unsatisfiable.
	
	public static enum SPEC_STATUS {
		UNKNOWN, 		// UNKNOWN: the status of the specification has not been computed yet.
		BOTTOM,			// BOTTOM: both the assumptions and goals are unsatisfiable.
		ASSUMPTIONS, 	// ASSUMPTIONS: the assumptions are consistent, but not the goals.
		GUARANTEES, 	// GUARANTEES: the goals are consistent, but not the assumptions.
		CONTRADICTORY,	// CONTRADICTORY: the assumptions and goals become unsatisfiable when are putted together. 
		UNREALIZABLE, 	// UNREALIZABLE: the specification is satisfiable, but not realizable.
		REALIZABLE;		// REALIZABLE: the specification is satisfiable and realizable.
	
		public boolean compatible (SPEC_STATUS other) {
			if (this == UNKNOWN || other == UNKNOWN 
				|| this == BOTTOM || other == BOTTOM 
				|| (this == ASSUMPTIONS && other == ASSUMPTIONS) 
				|| (this == GUARANTEES && other == GUARANTEES)
				)
				return false;
			
			return true;
		}
		
		public boolean areAssumptionsSAT () {
			return (this == ASSUMPTIONS || this == CONTRADICTORY || this == UNREALIZABLE || this == REALIZABLE);
		}
		
		public boolean areGuaranteesSAT () {
			return (this == GUARANTEES || this == CONTRADICTORY || this == UNREALIZABLE || this == REALIZABLE);
		}
		
		public boolean isSpecificationConsistent () {
			return (this == UNREALIZABLE || this == REALIZABLE);
		}
	};
		
	public Tlsf spec = null;
	public SPEC_STATUS status = SPEC_STATUS.UNKNOWN;
	
	public SpecificationChromosome() {
		spec = null;
		this.status = SPEC_STATUS.UNKNOWN;
	}
	
	public SpecificationChromosome(Tlsf spec) {
		this.spec = TlsfParser.parse(TLSF_Utils.toTLSF(spec));
		//TODO: compute the state of spec
		this.status = SPEC_STATUS.UNKNOWN;
	}

	public SpecificationChromosome(SpecificationChromosome other) {
		this.spec = TlsfParser.parse(TLSF_Utils.toTLSF(other.spec));
		this.status = other.status;
	}
	
	@Override
	public List<SpecificationChromosome> crossover(SpecificationChromosome anotherChromosome) {
		List<SpecificationChromosome> result = new LinkedList<SpecificationChromosome>();
		// if the specifications will not lead us to a consistent specification, then do a random merge.
		List<Tlsf> mergedSpecs = SpecificationMerger.merge(this.spec, anotherChromosome.spec, this.status, anotherChromosome.status);
		for (Tlsf s : mergedSpecs) {
			result.add(new SpecificationChromosome(s));
		}
		result.add(new SpecificationChromosome());
//		if (!this.status.compatible(anotherChromosome.status)) {
//			int level = Settings.RANDOM_GENERATOR.nextInt(2);
//			List<Tlsf> mergedSpecs = SpecificationMerger.merge(this.spec, anotherChromosome.spec, this.status, anotherChromosome.status, level);
//			for (Tlsf s : mergedSpecs) {
//				result.add(new SpecificationChromosome(s));
//			}
//		} 
//		else {
//			int level = Settings.RANDOM_GENERATOR.nextInt(2) + 2;
//			List<Tlsf> mergedSpecs = SpecificationMerger.merge(this.spec, anotherChromosome.spec, this.status, anotherChromosome.status, level);
//			for (Tlsf s : mergedSpecs) {
//				result.add(new SpecificationChromosome(s));
//			}
//		}
		return result;
	}

	@Override
	public SpecificationChromosome mutate() {
		//clone the current specification
		Tlsf mutated_spec = SpecificationMutator.mutate(spec, status);
		SpecificationChromosome mutated_chromosome = new SpecificationChromosome(mutated_spec);
		return mutated_chromosome;
	}

	
	
}
