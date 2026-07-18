package geneticalgorithm;

import com.lagodiuk.ga.Chromosome;
import main.Settings;
import owl.ltl.Formula;
import owl.ltl.parser.TlsfParser;
import owl.ltl.rewriter.SyntacticSimplifier;
import owl.ltl.tlsf.Tlsf;
import utils.TlsfUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * The <b>individual</b> of AuRUS's genetic search: a complete assume-guarantee
 * specification, together with its cached evaluation results.
 *
 * <p>A chromosome is not a single formula but a full {@link Tlsf}
 * specification {@code (A', G')}; the genes are the sub-formulas of its
 * assumptions and guarantees, and the genetic operators —
 * {@link #crossover(SpecificationChromosome)} and {@link #mutate()} — act
 * directly on their syntax trees. Alongside the specification, the chromosome
 * caches the results of its (expensive) evaluation: the realisability
 * {@link #status}, the {@link #fitness} value, and the syntactic and semantic
 * distances to the original specification, all filled in by
 * {@link AutomataBasedModelCountingSpecificationFitness} and read back by the
 * search loop so that no candidate is ever evaluated twice.</p>
 *
 * <p>Part of the reference implementation of: <i>Brizzio, Cordy, Papadakis,
 * S&aacute;nchez, Aguirre, Degiovanni. "Automated Repair of Unrealisable LTL
 * Specifications Guided by Model Counting", GECCO 2023
 * (<a href="https://doi.org/10.1145/3583131.3590454">doi:10.1145/3583131.3590454</a>).</i></p>
 *
 * @author Mat&iacute;as Brizzio
 * @see SpecificationGeneticAlgorithm
 * @see SpecificationMutator
 * @see SpecificationCrossover
 * @see AutomataBasedModelCountingSpecificationFitness
 */
public class SpecificationChromosome implements Chromosome<SpecificationChromosome>, Cloneable {

    // 			 			/  ASSUMPTIONS  \
    // UNKNOWN --  BOTTOM --		  		 -- CONTRADICTORY --  UNREALIZABLE  --  REALIZABLE
    //			 			\  GUARANTEES   /

    // we distinguish the particular case when the specification is realizable
    // just because the assumptions are unsatisfiable.

    /** The assume-guarantee specification this individual represents. */
    public Tlsf spec;

    /**
     * Cached realisability status, computed lazily by the fitness function.
     * {@link SPEC_STATUS#UNKNOWN} until the chromosome is evaluated.
     */
    public SPEC_STATUS status = SPEC_STATUS.UNKNOWN;

    /** Cached fitness value, filled in when the chromosome is evaluated. */
    public double fitness = 0d;

    /** Cached syntactic similarity to the original specification, in {@code [0, 1]}. */
    public double syntactic_distance = 0d;

    /** Cached semantic similarity to the original specification, in {@code [0, 1]}. */
    public double semantic_distance = 0d;

    /** Creates an empty chromosome with no specification (used internally). */
    public SpecificationChromosome() {
        spec = null;
    }

    /**
     * Creates a chromosome for the given specification.
     *
     * <p>The specification is round-tripped through its TLSF textual form
     * ({@code TlsfParser.parse(TlsfUtils.toTLSF(spec))}): this produces a
     * normalised deep copy, so chromosomes never share formula structure with
     * one another — a mutation applied to one individual can never leak into
     * a sibling. The status starts as {@link SPEC_STATUS#UNKNOWN}, marking
     * the chromosome as not yet evaluated.</p>
     *
     * @param spec the specification this individual should represent
     */
    public SpecificationChromosome(Tlsf spec) {
        this.spec = TlsfParser.parse(TlsfUtils.toTLSF(spec));
        this.status = SPEC_STATUS.UNKNOWN;
    }

    /**
     * Recombines this individual with another parent, producing offspring
     * that mix assumptions and guarantees from both.
     *
     * <p>For each side (assumptions, guarantees) a recombination <i>level</i>
     * is drawn uniformly from {@code {0, 1, 2}} and the pair is handed to
     * {@link SpecificationCrossover#apply(Tlsf, Tlsf, int, int)}:</p>
     * <ul>
     *   <li><b>level 0</b> — the whole side is inherited from one randomly
     *       chosen parent (a wholesale swap, no mixing);</li>
     *   <li><b>level 1</b> — the side is a random merge of conjuncts drawn
     *       from both parents;</li>
     *   <li><b>level 2</b> — sub-formulas of the two parents are combined at
     *       the syntax-tree level (transplanting or merging sub-trees), the
     *       deepest form of mixing.</li>
     * </ul>
     *
     * <p>The guarantee-preference factor ({@code Settings.
     * GA_GUARANTEES_PREFERENCE_FACTOR}, flag {@code -GPR}) biases where the
     * interesting recombination happens: with probability {@code GPR}% the
     * assumption side is forced to level 0 (inherited wholesale) so the
     * guarantees carry the mixing, and with the complementary probability the
     * guarantee side is forced to level 0 instead.</p>
     *
     * @param anotherChromosome the second parent
     * @return the offspring produced by the crossover (possibly several)
     */
    @Override
    public List<SpecificationChromosome> crossover(SpecificationChromosome anotherChromosome) {
        List<SpecificationChromosome> result = new LinkedList<>();
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


    /**
     * Produces a mutated copy of this individual by delegating to
     * {@link SpecificationMutator}, which selects an assumption or a
     * guarantee (biased by {@code -GPR}), picks a random sub-formula inside
     * it, and rewrites it with one of the three mutation modes (general,
     * weakening, strengthening).
     *
     * @return a fresh chromosome carrying the mutated specification, or
     *         {@code null} if the mutator could not produce a valid mutant
     */
    @Override
    public SpecificationChromosome mutate() {
        //clone the current specification
        Tlsf mutated_spec = SpecificationMutator.mutate(spec);
        if (mutated_spec == null)
            return null;
        return new SpecificationChromosome(mutated_spec);
    }

    /**
     * Hash consistent with {@link #equals(Object)}, combining the
     * specification, the status and the fitness.
     *
     * <p><b>Caveat:</b> {@link #fitness} and {@link #status} are mutable and
     * change when the chromosome is evaluated, so the hash of an individual
     * is not stable across evaluation. Chromosomes should not be stored in
     * hash-based collections before their evaluation has settled; the search
     * itself only relies on list membership ({@code contains}), which uses
     * {@code equals} directly.</p>
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Double.hashCode(fitness);
        result = prime * result + ((spec == null) ? 0 : spec.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        return result;
    }

    /**
     * Semantic-leaning equality used to deduplicate candidates (e.g. in the
     * solutions list): two chromosomes are equal when their specifications
     * are <i>syntactically equivalent after simplification</i> — both
     * formulas are normalised with {@code SyntacticSimplifier} and compared —
     * and, if both individuals have already been evaluated
     * ({@code fitness > 0}), their fitness values and statuses also agree.
     * Comparing the simplified forms means trivially different spellings of
     * the same specification (reordered conjuncts, double negations, ...)
     * count as the same individual.
     */
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

    /** Shallow clone (the specification reference is shared, not copied). */
    @Override
    public SpecificationChromosome clone() {
        try {
            return (SpecificationChromosome) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    /**
     * The evaluation lattice of a candidate specification, ordered from
     * completely broken to repaired:
     *
     * <pre>
     *                    /  ASSUMPTIONS  \
     * UNKNOWN -- BOTTOM                    -- CONTRADICTORY -- UNREALIZABLE -- REALIZABLE
     *                    \  GUARANTEES   /
     * </pre>
     *
     * <p>The fitness function maps this ladder to graded scores (0.0, 0.05,
     * 0.1, 0.2, 0.5, 1.0) so that partially fixed candidates still receive
     * gradient — see
     * {@link AutomataBasedModelCountingSpecificationFitness}. The
     * {@code GUARANTEES} rung exists to distinguish the degenerate case of a
     * specification that is "realisable" only because its assumptions are
     * unsatisfiable (vacuous realisability, which conveys no intent).</p>
     */
    public enum SPEC_STATUS {
        /** The status of the specification has not been computed yet. */
        UNKNOWN,        // UNKNOWN: the status of the specification has not been computed yet.
        /** Both the assumptions and the guarantees are unsatisfiable. */
        BOTTOM,            // BOTTOM: both the assumptions and goals are unsatisfiable.
        /** The assumptions are satisfiable, but the guarantees are not. */
        ASSUMPTIONS,    // ASSUMPTIONS: the assumptions are consistent, but not the goals.
        /** The guarantees are satisfiable, but the assumptions are not (vacuous realisability). */
        GUARANTEES,    // GUARANTEES: the goals are consistent, but not the assumptions.
        /** Each side is satisfiable in isolation, but their conjunction is not. */
        CONTRADICTORY,    // CONTRADICTORY: the assumptions and goals become unsatisfiable when are putted together.
        /** The specification is consistent, but no controller can realise it. */
        UNREALIZABLE,    // UNREALIZABLE: the specification is satisfiable, but not realizable.
        /** The specification is consistent and realisable — a repair. */
        REALIZABLE;        // REALIZABLE: the specification is satisfiable and realizable.

        /**
         * Whether two statuses can meaningfully be combined during the
         * search: both must be computed and not completely broken, and the
         * pair must not consist of two individuals broken on the same side.
         *
         * @param other the status to combine with
         * @return {@code true} iff the combination is admissible
         */
        public boolean compatible(SPEC_STATUS other) {
            return this != UNKNOWN && other != UNKNOWN
                    && this != BOTTOM && other != BOTTOM
                    && (this != ASSUMPTIONS || other != ASSUMPTIONS)
                    && (this != GUARANTEES || other != GUARANTEES);
        }

        /** @return {@code true} iff this status implies the assumptions are satisfiable */
        public boolean areAssumptionsSAT() {
            return (this == ASSUMPTIONS || this == CONTRADICTORY || this == UNREALIZABLE || this == REALIZABLE);
        }

        /** @return {@code true} iff this status implies the guarantees are satisfiable */
        public boolean areGuaranteesSAT() {
            return (this == GUARANTEES || this == CONTRADICTORY || this == UNREALIZABLE || this == REALIZABLE);
        }

        /**
         * @return {@code true} iff the specification is consistent as a whole
         *         (assumptions and guarantees jointly satisfiable) — the
         *         precondition for computing the semantic-similarity ratios
         */
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
            return null;
        }
    }


}