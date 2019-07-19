package geneticalgorithm;

import java.util.List;
import com.lagodiuk.ga.Chromosome;
import owl.ltl.parser.TlsfParser;
import owl.ltl.tlsf.Tlsf;
import tlsf.TLSF_Utils;

public class SpecificationChromosome implements Chromosome<SpecificationChromosome>, Cloneable {
	
	// 			 /  ASSUMPTIONS  \
	// UNKNOWN --				  --  UNREALIZABLE  --  REALIZABLE
	//			 \  GUARANTEES   /
	
	// we distinguish the particular case when the specification is realizable
	// just because the assumptions are unsatisfiable.
	
	public static enum SPEC_STATUS {
		UNKNOWN, 		// UNKNOWN: the status of the specification has not been computed yet.
		ASSUMPTIONS, 	// ASSUMPTIONS: the environment assumptions are consistent, but not the goals.
		GUARANTEES, 	// GUARANTEES: the goals are consistent, but not the environment assumptions.
		UNREALIZABLE, 	// UNREALIZABLE: the specification is consistent, but not realizable.
		REALIZABLE;		// REALIZABLE: the specification is consistent and realizable.
	
		public boolean compatible (SPEC_STATUS other) {
			if (this == UNKNOWN || other == UNKNOWN 
				|| (this == ASSUMPTIONS && other == ASSUMPTIONS) 
				|| (this == GUARANTEES && other == GUARANTEES)
				)
				return false;
			
			return true;
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
		this.status = SPEC_STATUS.UNKNOWN;
	}

	public SpecificationChromosome(SpecificationChromosome other) {
		this.spec = TlsfParser.parse(TLSF_Utils.toTLSF(other.spec));
		this.status = other.status;
	}
	
	@Override
	public List<SpecificationChromosome> crossover(SpecificationChromosome anotherChromosome) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SpecificationChromosome mutate() {
		//clone the current specification
		SpecificationChromosome mutated_chromosome = new SpecificationChromosome(this);
		
		return mutated_chromosome;
	}

	
	
}
