package utils;

import main.Settings;
import owl.ltl.*;
import owl.ltl.visitors.SubformulaReplacer;

import java.util.*;

public class FormulaUtils {

    public static List<LabelledFormula> subformulas(LabelledFormula f) {//, List<String> variables) {
        List<LabelledFormula> s = new LinkedList<>();

        for (Formula c : f.formula().children()) {
            LabelledFormula sf = LabelledFormula.of(c, f.variables());
            s.addAll(subformulas(sf));
        }
        s.add(LabelledFormula.of(f.formula(), f.variables()));
        return s;
    }

    public static Set<Formula> subformulas(Formula f) {//, List<String> variables) {
        // A plain LinkedHashSet is not enough here: for Conjunction/Disjunction
        // nodes, f.children() itself iterates OWL's PropositionalFormula.children,
        // a java.util.Set with no order guarantee across JVM runs. A TreeSet with
        // a canonical (content-based) comparator gives deterministic iteration
        // order regardless of the order elements arrive in, so that later
        // .toArray()[Settings.RANDOM_GENERATOR.nextInt(n)] picks are reproducible
        // given the same -seed.
        Set<Formula> s = new TreeSet<>(Comparator.comparing(Object::toString));

        for (Formula c : f.children()) {
            s.addAll(subformulas(c));
        }
        s.add(f);
        return s;
    }

    public static int compare(Formula f0, Formula f1) {
        return Formulas.compare(subformulas(f0), subformulas(f1));
    }

    public static int formulaSize(Formula f) {//, List<String> variables) {
        int size = 1;
        for (Formula c : f.children())
            size += formulaSize(c);
        return size;
    }

    public static List<LabelledFormula> splitConjunction(LabelledFormula f) {
        List<LabelledFormula> conjuncts = new LinkedList<>();
        if (f.formula() instanceof Conjunction) {
            Conjunction conjunction = (Conjunction) f.formula();
            for (Formula c : conjunction.children)
                conjuncts.add(LabelledFormula.of(c, f.variables()));

        } else
            conjuncts.add(f);
        // OWL's PropositionalFormula.children is a java.util.Set, whose
        // iteration order is not guaranteed stable across JVM runs. Impose a
        // canonical order here so that any later indexing by a seeded random
        // draw (SpecificationMutator, SpecificationCrossover, ...) is
        // reproducible given the same -seed, instead of silently inheriting
        // OWL's unordered Set.
        conjuncts.sort(Comparator.comparing(Object::toString));
        return conjuncts;
    }

    public static List<Formula> splitConjunction(Formula f) {
        List<Formula> conjuncts = new LinkedList<>();
        if (f instanceof Conjunction) {
            Conjunction conjunction = (Conjunction) f;
            for (Formula c : conjunction.children)
                if (c != BooleanConstant.TRUE)
                    conjuncts.addAll(splitConjunction(c));
        } else if (f != BooleanConstant.TRUE)
            conjuncts.add(f);
        // See the LabelledFormula overload above: canonicalise the order,
        // since OWL's Conjunction.children (a Set) is not order-stable.
        conjuncts.sort(Comparator.comparing(Object::toString));
        return conjuncts;
    }

    public static List<Formula> splitConjunctions(List<Formula> formulas) {
        List<Formula> conjuncts = new LinkedList<>();
        for (Formula f : formulas) {
            if (f instanceof Conjunction) {
                Conjunction conjunction = (Conjunction) f;
                for (Formula c : conjunction.children)
                    if (c != BooleanConstant.TRUE)
                        conjuncts.addAll(splitConjunction(c));
            } else if (f != BooleanConstant.TRUE)
                conjuncts.add(f);
        }
        // See splitConjunction above: canonicalise the order, since OWL's
        // Conjunction.children (a Set) is not order-stable.
        conjuncts.sort(Comparator.comparing(Object::toString));
        return conjuncts;
    }

    /**
     * Returns the candidate graft sites of {@code f} for crossover: its
     * temporal sub-formulas, plus {@code f} itself when {@code f} has no
     * temporal sub-formula (a purely propositional conjunct) and
     * {@code Settings.allowNonTemporalCrossover} is set.
     *
     * <p>The set is empty for a propositional {@code f} while the flag is off;
     * {@link #replaceSubformula(Formula, Formula)} and
     * {@link #combineSubformula(Formula, Formula)} treat an empty set as "no
     * graft site" and return {@code null}, so with the flag off crossover
     * grafts over temporal structure only.</p>
     */
    private static Set<Formula> graftSubformulas(Formula f) {
        Set<Formula> sites = new LinkedHashSet<>(f.subformulas(Formula.TemporalOperator.class));
        if (sites.isEmpty() && Settings.allowNonTemporalCrossover)
            sites.add(f);
        return sites;
    }

    public static Formula replaceSubformula(Formula f0, Formula f1) {
        Random rand = Settings.RANDOM_GENERATOR;
        // graft site in f0: a temporal sub-formula, or f0 itself when
        // propositional and Settings.allowNonTemporalCrossover is set
        Set<Formula> subformulas_f0 = graftSubformulas(f0);
        if (subformulas_f0.isEmpty())
            return null;
        Formula src = (Formula) subformulas_f0.toArray()[rand.nextInt(subformulas_f0.size())];

        // donor drawn from f1 under the same rule
        Set<Formula> subformulas_f1 = graftSubformulas(f1);
        if (subformulas_f1.isEmpty())
            return null;
        Formula target = (Formula) subformulas_f1.toArray()[rand.nextInt(subformulas_f1.size())];

        // replace the chosen site of f0 with the donor
        SubformulaReplacer visitor = new SubformulaReplacer(src, target);
        return f0.accept(visitor);
    }

    public static Formula combineSubformula(Formula f0, Formula f1) {
        Random rand = Settings.RANDOM_GENERATOR;
        // graft site in f0: a temporal sub-formula, or f0 itself when
        // propositional and Settings.allowNonTemporalCrossover is set
        Set<Formula> subformulas_f0 = graftSubformulas(f0);
        if (subformulas_f0.isEmpty())
            return null;
        Formula left = (Formula) subformulas_f0.toArray()[rand.nextInt(subformulas_f0.size())];

        // donor drawn from f1 under the same rule
        Set<Formula> subformulas_f1 = graftSubformulas(f1);
        if (subformulas_f1.isEmpty())
            return null;
        Formula right = (Formula) subformulas_f1.toArray()[rand.nextInt(subformulas_f1.size())];

        // Join left and right under a random connective, then graft the result
        // over the chosen site. When both operands are propositional and
        // Settings.allowNonTemporalCrossover is set, use the propositional
        // connectives &, |, ->, <-> (implication has no OWL node, so
        // left -> right is (!left | right)); otherwise use &, |, U, W.
        int op = Settings.RANDOM_GENERATOR.nextInt(4);
        boolean propositional = Settings.allowNonTemporalCrossover
                && temporalNestingDepth(left) == 0 && temporalNestingDepth(right) == 0;
        if (propositional) {
            if (op == 0)
                right = Conjunction.of(left, right);
            else if (op == 1)
                right = Disjunction.of(left, right);
            else if (op == 2) {
                if (Settings.RANDOM_GENERATOR.nextBoolean())
                    right = Disjunction.of(left.not(), right);   // left -> right
                else
                    right = Disjunction.of(right.not(), left);   // right -> left
            } else
//                right = Biconditional.of(left, right);           // left <-> right
                // Biconditional
                right = Disjunction.of(Conjunction.of(left,right), Conjunction.of(right.not(),left.not()));
        } else {
            //0:& 1:| 2:U 3:W
            if (op == 0)
                right = Conjunction.of(left, right);
            else if (op == 1)
                right = Disjunction.of(left, right);
            else if (op == 2) {
                if (Settings.RANDOM_GENERATOR.nextBoolean())
                    right = UOperator.of(left, right);
                else
                    right = UOperator.of(right, left);
            } else {
                if (Settings.RANDOM_GENERATOR.nextBoolean())
                    right = WOperator.of(left, right);
                else
                    right = WOperator.of(right, left);
            }
        }

        SubformulaReplacer visitor = new SubformulaReplacer(left, right);
        return f0.accept(visitor);
    }


    /**
     * Maximum nesting depth of temporal operators in @p formula.
     * <p>
     * This is a depth, not a count: sibling operators do not add up, so
     * {@code G(F a | F b)} is 2, the same as {@code G F a}.
     * <p>
     *  - {@code X} does not add a level, since it shifts by one step rather than
     *    introducing a loop, but the walk descends past it: {@code X F a} is 1
     *    and {@code X(G(F(G a)))} is 2.
     *  - Owl normalizes at construction, so some nesting never reaches this
     *    function at all: {@code G F G a} is built as {@code F G a} and reads 2.
     * <p>
     * Consequently, a formula can be arbitrarily large and still read 2: this is
     * a bound on nesting, not on size.
     */
    public static int temporalNestingDepth(Formula formula) {
        if (formula == null || formula instanceof Literal)
            return 0;
        if (formula instanceof XOperator) {
            int max = 0;
            for (Formula c : formula.children()) {
                int aux = temporalNestingDepth(c);
                if (max < aux)
                    max = aux;
            }
            return max;                     // X no agrega nivel, pero no esconde
        }
        if (formula instanceof Formula.TemporalOperator) {
            int max = 0;
            for (Formula c : formula.children()) {
                int aux = temporalNestingDepth(c);
                if (max < aux)
                    max = aux;
            }
            return max + 1;
        }
        if (formula instanceof Formula.LogicalOperator) {
            int max = 0;
            for (Formula c : formula.children()) {
                int aux = temporalNestingDepth(c);
                if (max < aux)
                    max = aux;
            }
            return max;
        }
        return 0;
    }

}