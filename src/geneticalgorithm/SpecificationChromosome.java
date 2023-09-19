package geneticalgorithm;

import com.lagodiuk.ga.Chromosome;
import main.Settings;
import owl.ltl.Formula;
import owl.ltl.parser.TlsfParser;
import owl.ltl.rewriter.SyntacticSimplifier;
import owl.ltl.tlsf.Tlsf;
import tlsf.TLSF_Utils;

import java.util.LinkedList;
import java.util.List;

public class SpecificationChromosome implements Chromosome<SpecificationChromosome>, Cloneable {

    // 			 			/  ASSUMPTIONS  \
    // UNKNOWN --  BOTTOM --		  		 -- CONTRADICTORY --  UNREALIZABLE  --  REALIZABLE
    //			 			\  GUARANTEES   /

    // we distinguish the particular case when the specification is realizable
    // just because the assumptions are unsatisfiable.

    public Tlsf spec;

    ;
    public SPEC_STATUS status = SPEC_STATUS.UNKNOWN;
    public double fitness = 0d;
    public double syntactic_distance = 0d;
    public double semantic_distance = 0d;

    public SpecificationChromosome() {
        spec = null;
    }

    public SpecificationChromosome(Tlsf spec) {
        this.spec = TlsfParser.parse(TLSF_Utils.toTLSF(spec));
        this.status = SPEC_STATUS.UNKNOWN;
    }

    @Override
    public List<SpecificationChromosome> crossover(SpecificationChromosome anotherChromosome) {
        List<SpecificationChromosome> result = new LinkedList<SpecificationChromosome>();
        int assumption_level = Settings.RANDOM_GENERATOR.nextInt(3);
        int guarantee_level = Settings.RANDOM_GENERATOR.nextInt(3);
        int random = Settings.RANDOM_GENERATOR.nextInt(100);
        if (random >= Settings.GA_GUARANTEES_PREFERENCE_FACTOR)
            guarantee_level = 0;
        else
            assumption_level = 0;

        List<Tlsf> mergedSpecs = SpecificationCrossover.apply(this.spec, anotherChromosome.spec, assumption_level, guarantee_level);
        for (Tlsf s : mergedSpecs) {
            result.add(new SpecificationChromosome(s));
        }

        return result;
    }


    @Override
    public SpecificationChromosome mutate() {
        //clone the current specification
        Tlsf mutated_spec = SpecificationMutator.mutate(spec, status);
        if (mutated_spec == null)
            return null;
        return new SpecificationChromosome(mutated_spec);
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
        if (fitness > 0.0d && other.fitness > 0.0d && Double.doubleToLongBits(fitness) != Double.doubleToLongBits(other.fitness))
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
        return !(fitness > 0.0d) || !(other.fitness > 0.0d) || status == other.status;
    }

    @Override
    public SpecificationChromosome clone() {
        try {
            return (SpecificationChromosome) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public enum SPEC_STATUS {
        UNKNOWN,        // UNKNOWN: the status of the specification has not been computed yet.
        BOTTOM,            // BOTTOM: both the assumptions and goals are unsatisfiable.
        ASSUMPTIONS,    // ASSUMPTIONS: the assumptions are consistent, but not the goals.
        GUARANTEES,    // GUARANTEES: the goals are consistent, but not the assumptions.
        CONTRADICTORY,    // CONTRADICTORY: the assumptions and goals become unsatisfiable when are putted together.
        UNREALIZABLE,    // UNREALIZABLE: the specification is satisfiable, but not realizable.
        REALIZABLE;        // REALIZABLE: the specification is satisfiable and realizable.

        public boolean compatible(SPEC_STATUS other) {
            if (this == UNKNOWN || other == UNKNOWN
                    || this == BOTTOM || other == BOTTOM
                    || (this == ASSUMPTIONS && other == ASSUMPTIONS)
                    || (this == GUARANTEES && other == GUARANTEES)
            )
                return false;

            return true;
        }

        public boolean areAssumptionsSAT() {
            return (this == ASSUMPTIONS || this == CONTRADICTORY || this == UNREALIZABLE || this == REALIZABLE);
        }

        public boolean areGuaranteesSAT() {
            return (this == GUARANTEES || this == CONTRADICTORY || this == UNREALIZABLE || this == REALIZABLE);
        }

        public boolean isSpecificationConsistent() {
            return (this == UNREALIZABLE || this == REALIZABLE);
        }

        @Override
        public String toString() {
            switch (this) {
                case UNKNOWN:
                    return "unknown";
                case BOTTOM:
                    return "BOTTOM: both the assumptions and goals are unsatisfiable.";
                case ASSUMPTIONS:
                    return "ASSUMPTIONS: the assumptions are consistent, but not the goals.";
                case GUARANTEES:
                    return "GUARANTEES: the goals are consistent, but not the assumptions.";
                case CONTRADICTORY:
                    return "CONTRADICTORY: the assumptions and goals become unsatisfiable when are putted together. ";
                case UNREALIZABLE:
                    return "UNREALIZABLE: the specification is satisfiable, but not realizable.";
                case REALIZABLE:
                    return "REALIZABLE: the specification is satisfiable and realizable.";
            }
            ;
            return null;
        }
    }


}
