package geneticalgorithm;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.lagodiuk.ga.Chromosome;

import main.Settings;
import owl.ltl.Formula;
import owl.ltl.parser.TlsfParser;
import owl.ltl.rewriter.SyntacticSimplifier;
import owl.ltl.tlsf.Tlsf;
import tlsf.TLSF_Utils;

public class SpecificationChromosome implements Chromosome<SpecificationChromosome>, Cloneable {
	
	// 			 			/  ASSUMPTIONS  \
	// UNKNOWN --  BOTTOM --		  		 -- CONTRADICTORY --  UNREALIZABLE  --  REALIZABLE
	//			 			\  GUARANTEES   /
	
	// we distinguish the particular case when the specification is realizable
	// just because the assumptions are unsatisfiable.
	
	public enum SPEC_STATUS {
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

		@Override
		public String toString(){
			switch (this) {
				case UNKNOWN : return "unknown";
				case BOTTOM : return "BOTTOM: both the assumptions and goals are unsatisfiable.";
				case ASSUMPTIONS: return "ASSUMPTIONS: the assumptions are consistent, but not the goals.";
				case GUARANTEES: return "GUARANTEES: the goals are consistent, but not the assumptions.";
				case CONTRADICTORY : return "CONTRADICTORY: the assumptions and goals become unsatisfiable when are putted together. ";
				case UNREALIZABLE:  return "UNREALIZABLE: the specification is satisfiable, but not realizable.";
				case REALIZABLE: return "REALIZABLE: the specification is satisfiable and realizable.";
			};
			return null;
		}
	};
		
	public Tlsf spec = null;
	public SPEC_STATUS status = SPEC_STATUS.UNKNOWN;
	public double fitness = 0d;
	
	public SpecificationChromosome() {
		spec = null;
		this.status = SPEC_STATUS.UNKNOWN;
	}
	
	public SpecificationChromosome(Tlsf spec) {
		this.spec = TlsfParser.parse(TLSF_Utils.toTLSF(spec));
		//TODO: compute the state of spec
		this.status = SPEC_STATUS.UNKNOWN;
	}

//	public SpecificationChromosome(SpecificationChromosome other) {
//		this.spec = TlsfParser.parse(TLSF_Utils.toTLSF(other.spec));
//		this.status = other.status;
//	}
	
	@Override
	public List<SpecificationChromosome> crossover(SpecificationChromosome anotherChromosome) {
		List<SpecificationChromosome> result = new LinkedList<SpecificationChromosome>();
		// if the specifications will not lead us to a consistent specification, then do a random merge.
//		List<Tlsf> mergedSpecs = SpecificationMerger.merge(this.spec, anotherChromosome.spec, this.status, anotherChromosome.status);
		List<Tlsf> mergedSpecs = SpecificationCrossover.apply(this.spec, anotherChromosome.spec, this.status, anotherChromosome.status, Settings.RANDOM_GENERATOR.nextInt(4));
//		List<Tlsf> mergedSpecs = SpecificationCrossover.apply(this.spec, anotherChromosome.spec);
		for (Tlsf s : mergedSpecs) {
			result.add(new SpecificationChromosome(s));
		}


		//result.add(new SpecificationChromosome());
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
		if (mutated_spec == null)
			return null;
		SpecificationChromosome mutated_chromosome = new SpecificationChromosome(mutated_spec);
		return mutated_chromosome;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(fitness);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((spec == null) ? 0 : spec.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SpecificationChromosome other = (SpecificationChromosome) obj;
		if (Double.doubleToLongBits(fitness) != Double.doubleToLongBits(other.fitness))
			return false;
		if (spec == null) {
			if (other.spec != null)
				return false;
		} else {
			SyntacticSimplifier simp = new SyntacticSimplifier();
			Formula thiz = spec.toFormula().formula().accept(simp);
			Formula that = other.spec.toFormula().formula().accept(simp);
			if (!thiz.equals(that))
				return false;
		}
		if (status != other.status)
			return false;
		return true;
	}

	
	
}
