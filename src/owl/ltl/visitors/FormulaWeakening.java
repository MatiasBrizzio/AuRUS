package owl.ltl.visitors;

import main.Settings;
import owl.ltl.*;
import owl.ltl.rewriter.NormalForms;
import utils.FormulaUtils;

import java.util.*;

/**
 * The <b>weakening</b> mutation mode of AuRUS: rewrites an LTL formula (in
 * negation normal form) into a semantically weaker one, i.e. a formula
 * &phi;<sub>w</sub> such that &phi; &#8872; &phi;<sub>w</sub> — every model
 * of the original formula survives the rewrite. Applied to an assumption,
 * this relaxes a constraint on the environment; see
 * {@link owl.ltl.visitors.FormulaStrengthening} for the dual mode, and
 * {@code geneticalgorithm.SpecificationMutator} for how the two are selected
 * during the search.
 *
 * <p>This class implements the weakening rules of Table 4.1 in the paper and
 * thesis this project is the reference implementation of (one {@code visit}
 * method per LTL operator, one rewrite rule per numbered {@code option}); see
 * the individual {@code visit} methods below for the correspondence. The two
 * additional operators {@code M} (strong release) and {@code R} (release)
 * are handled via their standard dualities with {@code U}/{@code W}
 * (noted inline) rather than appearing as separate rows in the thesis table.</p>
 *
 * <p><b>How mutation is applied.</b> This is a recursive {@link Visitor}: it
 * walks the formula bottom-up, and at <i>every</i> node — not just the root —
 * it may independently roll the dice to weaken that node. Two knobs control
 * this, both threaded through the constructor:</p>
 * <ul>
 *   <li><b>{@code weakening_rate}</b> — the per-node mutation probability is
 *       {@code 1/weakening_rate} (a fresh {@code Random.nextInt(weakening_rate) == 0}
 *       roll at each node); see {@code SpecificationMutator.weakenFormula},
 *       which derives this from {@code Settings.GA_GENE_MUTATION_RATE}.</li>
 *   <li><b>{@code numOfAllowedWeakenings}</b> — a mutable budget, decremented
 *       every time a node is actually weakened; once it reaches {@code 0}, no
 *       further node is touched, bounding how many changes one mutation call
 *       can make to a single formula.</li>
 * </ul>
 *
 * <p><b>Bloat guard.</b> Several {@code visit} methods count the number of
 * temporal operators in the (already partially rewritten) current formula and
 * abandon further weakening of that node — returning the <i>original,
 * unweakened</i> sub-formula — once the count exceeds {@code 2}. This keeps
 * mutated formulas from growing without bound, the classic bloat problem in
 * genetic-programming-style search.</p>
 *
 * <p><b>Universal weakest bound.</b> At every node, one of the random options
 * is simply {@code true} — the weakest possible formula — giving the search a
 * way to discard a sub-formula entirely regardless of its shape.</p>
 *
 * <p><b>A note on soundness of the fallback rules.</b> Most rules below are
 * the same directed rewrites as the thesis table and are sound by
 * construction (entailment-preserving for every substitution of the
 * variables involved). A few fallback branches — e.g. the last option of
 * {@link #visit(FOperator)}, {@link #visit(UOperator)} and
 * {@link #visit(WOperator)}, which combine the current formula with a
 * <i>freshly drawn, but not fresh in the logical sense</i>, existing
 * proposition via {@link #new_literal(Formula)} — are <i>not</i> guaranteed
 * entailments for every possible choice of that proposition (e.g.
 * {@code F(a) &#8872; c W a} does not hold for every {@code c}). These
 * branches are best understood as heuristic exploration in the spirit of the
 * undirected general mutation, injected into the weakening operator for
 * extra diversity; any resulting semantic drift is caught downstream by the
 * fitness function's semantic-similarity component, not by this class.</p>
 *
 * <p>Part of the reference implementation of: <i>Brizzio, Cordy, Papadakis,
 * S&aacute;nchez, Aguirre, Degiovanni. "Automated Repair of Unrealisable LTL
 * Specifications Guided by Model Counting", GECCO 2023
 * (<a href="https://doi.org/10.1145/3583131.3590454">doi:10.1145/3583131.3590454</a>).</i></p>
 *
 * @author Mat&iacute;as Brizzio
 * @see FormulaStrengthening
 * @see GeneralFormulaMutator
 */
public class FormulaWeakening implements Visitor<Formula> {

    /**
     * Print step-by-step before/after traces of each applied weakening to
     * standard output — off by default; flip to {@code true} for local
     * debugging, mirroring {@link FormulaStrengthening#print_debug_info}.
     */
    private final boolean print_debug_info = false;

    /** Literal cache aligned index-for-index with {@link #variables}. */
    private final List<Literal> literalCache;

    /** The specification's variable names, in the fixed index order used to build {@link Literal}s. */
    private final List<String> variables;

    /** Always {@code true} in the current constructor — the variable set is fixed at construction time, not grown on demand. */
    private final boolean fixedVariables;

    /** Per-node mutation probability is {@code 1/weakening_rate}. */
    private final int weakening_rate;

    /** Remaining budget of sub-formulas this visitor is still allowed to weaken; decremented on each applied mutation. */
    private int numOfAllowedWeakenings;

    /**
     * Creates a weakening visitor over the given variable alphabet.
     *
     * @param literals                    the specification's variable names, in index order
     * @param weakening_rate              per-node mutation probability is {@code 1/weakening_rate}
     * @param num_of_weakening_to_apply   maximum number of sub-formulas this visitor may weaken in one pass
     */
    public FormulaWeakening(List<String> literals, int weakening_rate, int num_of_weakening_to_apply) {
        ListIterator<String> literalIterator = literals.listIterator();
        List<Literal> literalList = new ArrayList<>();
        List<String> variableList = new ArrayList<>();

        while (literalIterator.hasNext()) {
            int index = literalIterator.nextIndex();
            String name = literalIterator.next();
            literalList.add(Literal.of(index));
            variableList.add(name);
        }

        literalCache = List.copyOf(literalList);
        variables = List.copyOf(variableList);
        fixedVariables = true;
        this.weakening_rate = weakening_rate;
        this.numOfAllowedWeakenings = num_of_weakening_to_apply;

    }

    /** @return a defensive copy of the variable alphabet this visitor was built with */
    public List<String> variables() {
        return List.copyOf(variables);
    }

    /**
     * Entry point: applies this visitor to the root of the given formula.
     *
     * @param formula the formula to weaken (should be in negation normal form)
     * @return the (possibly) weakened formula
     */
    @Override
    public Formula apply(Formula formula) {
        return formula.accept(this);
    }

    /**
     * Weakens a boolean constant: {@code false ⤳ true} (the only possible
     * weakening of a constant; {@code true} is already the weakest formula
     * and is left untouched by the random draw).
     */
    @Override
    public Formula visit(BooleanConstant booleanConstant) {
        Formula current = booleanConstant;
        if (numOfAllowedWeakenings > 0) {
            boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(weakening_rate) == 0);
            if (mutate) {
                numOfAllowedWeakenings--;
                current = BooleanConstant.TRUE;
            }
        }

        return current;
    }

    /**
     * Weakens an atomic literal {@code p} into one of, chosen uniformly at
     * random: {@code true} (option 0); {@code p ∨ q} for a fresh literal
     * {@code q} (option 1 — Table 4.1's "add disjunct" rule); or
     * {@code F(p)} (option 2 — "eventually" is weaker than "now").
     */
    @Override
    public Formula visit(Literal literal) {
        Formula current = literal;
        if (numOfAllowedWeakenings > 0) {
            boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(weakening_rate) == 0);
            if (mutate) {
                numOfAllowedWeakenings--;
                // 0: TRUE 1: add disjunct 2:F
                int option = Settings.RANDOM_GENERATOR.nextInt(3);
                if (option == 0)
                    current = BooleanConstant.TRUE;
                else if (option == 1) {
                    // weak(a) = a | b
                    int new_variable = Settings.RANDOM_GENERATOR.nextInt(variables.size());
                    while (new_variable == literal.getAtom())
                        new_variable = Settings.RANDOM_GENERATOR.nextInt(variables.size());
                    Literal new_literal = createVariable(variables.get(new_variable));
                    if (Settings.RANDOM_GENERATOR.nextBoolean())
                        new_literal = new_literal.not();
                    current = Disjunction.of(current, new_literal);
                } else {
                    // weak(a) = F(a)
                    current = FOperator.of(current);
                }
            }
        }
        return current;
    }

    /**
     * Weakens {@code X(a)} (recursing into {@code a} first) into one of:
     * {@code true} (option 0, default); {@code F(a)} (option 1 — dropping the
     * "next" timing keeps only "eventually"); or, when {@code a} is itself
     * {@code F(b)}, simplifying {@code X F(b)} down to {@code F(b)}
     * (option 2 — "eventually" already absorbs one step of delay).
     */
    @Override
    public Formula visit(XOperator xOperator) {
        Formula operand = xOperator.operand.accept(this);
        Formula current = XOperator.of(operand);
        if (numOfAllowedWeakenings > 0) {
            boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(weakening_rate) == 0);
            if (mutate) {
                numOfAllowedWeakenings--;
                // 0:TRUE 1:F 2: remove X
                int option = Settings.RANDOM_GENERATOR.nextInt(3);
                current = BooleanConstant.TRUE; //(option == 0) and default
                if (option == 1) {
                    // weak (X(a)) = F(a)
                    current = FOperator.of(operand);
                } else if (option == 2 && operand instanceof FOperator) {
                    // weak (X F(a)) = F(a)
                    current = operand;
                }
            }
        }
        return current;
    }

    /**
     * Weakens {@code F(a)} (recursing into {@code a} first) into one of, drawn
     * uniformly out of 6 outcomes: {@code true} (option 0, default);
     * distributing over a conjunction, {@code F(a1 ∧ a2) ⤳ F(a1) ∧ F(a2)}
     * (option 1, only when {@code a} is a conjunction — each conjunct is
     * allowed to happen at its own time rather than simultaneously);
     * {@code F G(b) ⤳ G F(b)} (option 2, when {@code a = G(b)} — the classic
     * valid LTL implication "eventually always" &rArr; "infinitely often");
     * {@code F X(b) ⤳ F(b)} (option 3, when {@code a = X(b)} — dropping the
     * extra step of delay); {@code F G(b) ⤳ F(b)} (option 4, when
     * {@code a = G(b)} — a second, independent weakening applicable to the
     * same {@code F G(b)} shape as option 2, this one discarding the
     * "forever" and keeping only the first occurrence); or, as the fallback
     * for every other case, {@code F(a) ⤳ (c W a)} for a drawn literal
     * {@code c} (option 5 and any option whose structural guard failed) —
     * see the class-level caveat about this fallback's soundness.
     */
    @Override
    public Formula visit(FOperator fOperator) {
        Formula operand = fOperator.operand.accept(this);
        Formula current = FOperator.of(operand);
        int numOfTO = FormulaUtils.numOfTemporalOperators(current);
        if (numOfTO > 2)
            return fOperator;
        if (numOfAllowedWeakenings > 0) {
            boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(weakening_rate) == 0);
            if (mutate) {
                numOfAllowedWeakenings--;

                // 0:TRUE 1:distribute to conjunction 2:persistence to infinitely often 3:remove X 4:remove G
                int option = Settings.RANDOM_GENERATOR.nextInt(6);

                current = BooleanConstant.TRUE; // (option == 0) and default
                if (option == 1 && operand instanceof Conjunction && numOfTO < 2) {
                    // weak (F (a & b)) = F(a) & F(b)
                    for (Set<Formula> c : NormalForms.toCnf(operand)) {
                        Formula clause = Disjunction.of(c);
                        current = Conjunction.of(current, FOperator.of(clause));
                    }
                } else if (option == 2 && operand instanceof GOperator) {
                    // weak (F G (a)) = G F (a)
                    current = GOperator.of(FOperator.of(operand.children().iterator().next()));
                } else if (option == 3 && operand instanceof XOperator) {
                    // weak (F X (a)) = F (a)
                    current = FOperator.of(operand.children().iterator().next());
                } else if (option == 4 && operand instanceof GOperator) {
                    // weak (F G (a)) = F (a)
                    current = FOperator.of(operand.children().iterator().next());
                } else {
                    current = WOperator.of(new_literal(current), operand);
                }
            }
        }
        return current;
    }


    /**
     * Weakens {@code G(a)} (recursing into {@code a} first) into one of,
     * drawn uniformly out of 7 outcomes: {@code true} (option 0);
     * {@code a} itself (option 1 — dropping "always" entirely, Table 4.1's
     * "identity" row); {@code F(a)} (option 2 — "always" implies
     * "eventually"); {@code X(a)} (option 3 — "always" implies "next");
     * {@code G F(a)} (option 4, only when the formula so far has at most one
     * other temporal operator — "always" implies "infinitely often");
     * {@code F G(a)} (option 5, same guard — "always" implies "eventually
     * always", the classic valid direction); or {@code X G(a)} (option 6 —
     * delaying the "always" by one step is itself a weakening).
     */
    @Override
    public Formula visit(GOperator gOperator) {
        Formula operand = gOperator.operand.accept(this);
        Formula current = GOperator.of(operand);
        int numOfTO = FormulaUtils.numOfTemporalOperators(current);
        if (numOfTO > 2)
            return gOperator;
        if (numOfAllowedWeakenings > 0) {
            boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(weakening_rate) == 0);
            if (mutate) {
                numOfAllowedWeakenings--;
                // 0:TRUE 1:remove G 2:F 3:X 4:GF 5:FG 6:XG 7: U
                int option = Settings.RANDOM_GENERATOR.nextInt(7);
                if (option == 0)
                    current = BooleanConstant.TRUE;
                else if (option == 1) {
                    // weak (G(a)) = a
                    current = operand;
                } else if (option == 2) {
                    // weak (G(a)) = F(a)
                    current = FOperator.of(operand);
                } else if (option == 3) {
                    // weak (G(a)) = X(a)
                    current = XOperator.of(operand);
                } else if (option == 4 && numOfTO < 2) {
                    // weak (G(a)) = GF(a)
                    current = GOperator.of(FOperator.of(operand));
                } else if (option == 5 && numOfTO < 2) {
                    // weak (G(a)) = FG(a)
                    current = FOperator.of(GOperator.of(operand));
                } else if (option == 6) {
                    // weak (G(a)) = XG(a)
                    current = XOperator.of(GOperator.of(operand));
                }
            }
        }
        return current;
    }

    /**
     * Weakens a conjunction {@code a1 ∧ ... ∧ an} (recursing into every
     * conjunct first) into one of: {@code true} (option 0); dropping one
     * randomly chosen conjunct, {@code a1 ∧ a2 ⤳ a1} (option 1 — Table 4.1's
     * "drop conjunct" rule); turning the whole conjunction into a
     * disjunction, {@code a1 ∧ a2 ⤳ a1 ∨ a2} (option 2); or, when the formula
     * is not already temporally heavy, {@code F(a1 ∧ ... ∧ an)} (option 3 —
     * postponing the whole conjunction rather than requiring it now).
     */
    @Override
    public Formula visit(Conjunction conjunction) {
        Formula current = Conjunction.of(conjunction.children.stream().map(x -> x.accept(this)));
        int numOfTO = FormulaUtils.numOfTemporalOperators(current);
        if (numOfTO > 2)
            return conjunction;
        if (numOfAllowedWeakenings > 0) {
            boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(weakening_rate) == 0);
            if (mutate) {
                // 0: TRUE 1: remove conjunct 2:disjunction 3:F
                numOfAllowedWeakenings--;
                int option = Settings.RANDOM_GENERATOR.nextInt(4);
                if (option == 0)
                    current = BooleanConstant.TRUE;
                else if (option == 1) {// weak(a & b) = a
                    if (!current.children().isEmpty()) {
                        int to_be_removed = Settings.RANDOM_GENERATOR.nextInt(current.children().size());
                        List<Formula> new_set_children = new LinkedList<>();
                        Iterator<Formula> it = current.children().iterator();
                        int i = 0;
                        while (it.hasNext()) {
                            if (i != to_be_removed)
                                new_set_children.add(it.next());
                            i++;
                        }
                        current = Conjunction.of(new_set_children);
                    }
                } else if (option == 2) {
                    if (!current.children().isEmpty())
                        current = Disjunction.of(current.children()); // weak(a & b) = a | b
                } else if (numOfTO < 2) {
                    current = FOperator.of(current); // weak(a & b) = F(a & b)
                }
            }
        }
        return current;
    }

    /**
     * Weakens a disjunction {@code a1 ∨ ... ∨ an} (recursing into every
     * disjunct first) into one of: {@code true} (option 0); or adding one
     * more disjunct drawn from the alphabet, {@code a1 ∨ a2 ⤳ a1 ∨ a2 ∨ c}
     * (option 1 — a disjunction can only grow more permissive by adding
     * disjuncts).
     */
    @Override
    public Formula visit(Disjunction disjunction) {
        Formula current = Disjunction.of(disjunction.children.stream().map(x -> x.accept(this)));
        int numOfTO = FormulaUtils.numOfTemporalOperators(current);
        if (numOfTO > 2)
            return disjunction;
        if (numOfAllowedWeakenings > 0) {
            boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(weakening_rate) == 0);
            if (mutate) {
                // 0: TRUE 1: add disjunct 2:F
                numOfAllowedWeakenings--;
                int option = Settings.RANDOM_GENERATOR.nextInt(2);
                if (option == 0)
                    current = BooleanConstant.TRUE;
                else {
                    current = Disjunction.of(current, new_literal(current));
                }
            }
        }

        return current;
    }

    /**
     * Weakens {@code a U b} ("{@code a} holds until {@code b}", recursing
     * into both sides first) into one of: {@code true} (option 0);
     * {@code a W b} (option 1 — the weak-until Table 4.1 rule: {@code b} no
     * longer needs to eventually hold); {@code (a ∨ c) U b} for a drawn
     * literal {@code c} (option 2 — widening what may hold while waiting for
     * {@code b}); or {@code F(b)} (option 3 — keeping only the eventual
     * obligation, dropping the "until" precondition on {@code a}).
     */
    @Override
    public Formula visit(UOperator uOperator) {
        Formula left = uOperator.left.accept(this);
        Formula right = uOperator.right.accept(this);
        Formula current = UOperator.of(left, right);
        int numOfTO = FormulaUtils.numOfTemporalOperators(current);
        if (numOfTO > 2)
            return uOperator;
        if (numOfAllowedWeakenings > 0) {
            boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(weakening_rate) == 0);
            if (mutate) {
                numOfAllowedWeakenings--;
                // 0:TRUE 1:W 2:F
                int option = Settings.RANDOM_GENERATOR.nextInt(4);
                if (option == 0)
                    current = BooleanConstant.TRUE;
                else if (option == 1)
                    current = WOperator.of(left, right); // weak(a U b) = a W b
                else if (option == 2) {    // weak (a U b) = (a || c) U b
                    current = UOperator.of(Disjunction.of(new_literal(current), left), right);
                    if (print_debug_info) System.out.println("////" + current);
                } else
                    current = FOperator.of(right); // weak(a U b) = F(b)
            }
        }
        return current;
    }

    /**
     * Weakens {@code a W b} ("{@code a} holds unless/until {@code b}",
     * recursing into both sides first). Recalling that
     * {@code a W b ≡ G(a) ∨ (a U b)}, this weakens one of the two disjuncts
     * at a time: {@code true} (option 0); {@code F(a) ∨ (a U b)} (option 1 —
     * relaxing the "always" disjunct down to "eventually"); {@code G(a) ∨ F(b)}
     * (option 2 — relaxing the "until" disjunct down to a bare "eventually");
     * or {@code (a ∨ c) W b} for a drawn literal {@code c} (option 3 —
     * widening what may hold throughout).
     */
    @Override
    public Formula visit(WOperator wOperator) {
        Formula left = wOperator.left.accept(this);
        Formula right = wOperator.right.accept(this);
        Formula current = WOperator.of(left, right);
        int numOfTO = FormulaUtils.numOfTemporalOperators(current);
        if (numOfTO > 2)
            return wOperator;
        if (numOfAllowedWeakenings > 0) {
            boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(weakening_rate) == 0);
            if (mutate) {
                numOfAllowedWeakenings--;
                // a W b = G(a) || a U b.
                // we decided to weak each disjunct.
                // 0:TRUE 1:F 2:F
                int option = Settings.RANDOM_GENERATOR.nextInt(4);
                if (option == 0)
                    current = BooleanConstant.TRUE;
                else if (option == 1)
                    current = Disjunction.of(FOperator.of(left), UOperator.of(left, right)); // weak(a W b) = F(a) || a U b
                else if (option == 2)
                    current = Disjunction.of(GOperator.of(left), FOperator.of(right)); // weak(a W b) = G(a) || F(b)
                else {  // weak (a W b) = ((a || c) W b)
                    current = WOperator.of(Disjunction.of(left, new_literal(current)), right);
                    if (print_debug_info) System.out.println("------" + current);
                }
            }
        }
        return current;
    }

    /**
     * Weakens {@code a M b} ("strong release" — via the standard duality
     * {@code a M b ≡ b U (a ∧ b)} — recursing into both sides first) into
     * one of: {@code true} (option 0); {@code b W (a ∧ b)} (option 1 — the
     * weak-until relaxation of the duality above, dropping the requirement
     * that {@code a ∧ b} eventually hold); or {@code F(a ∧ b)} (option 2 —
     * keeping only the eventual joint obligation).
     */
    @Override
    public Formula visit(MOperator mOperator) {
        Formula left = mOperator.left.accept(this);
        Formula right = mOperator.right.accept(this);
        Formula current = MOperator.of(left, right);
        int numOfTO = FormulaUtils.numOfTemporalOperators(current);
        if (numOfTO > 2)
            return mOperator;
        if (numOfAllowedWeakenings > 0) {
            boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(weakening_rate) == 0);
            if (mutate) {
                numOfAllowedWeakenings--;

                // a M b = b U (a & b)
                // 0:TRUE 1:W 2:F
                int option = Settings.RANDOM_GENERATOR.nextInt(3);
                if (option == 0)
                    current = BooleanConstant.TRUE;
                else if (option == 1)
                    current = WOperator.of(right, Conjunction.of(left, right)); // weak(b U (a & b)) = b W (a & b)
                else
                    current = FOperator.of(Conjunction.of(left, right)); // weak(b U (a & b)) = F(a & b)
            }
        }
        return current;
    }

    /**
     * Weakens {@code a R b} ("release" — via the standard duality
     * {@code a R b ≡ b W (a ∧ b)} — recursing into both sides first) into
     * one of: {@code true} (option 0); {@code F(b) ∨ (b U (a ∧ b))}
     * (option 1 — relaxing the "always {@code b}" disjunct of the weak-until
     * expansion down to a bare "eventually {@code b}"); or
     * {@code G(b) ∨ F(a ∧ b)} (option 2 — relaxing the "until" disjunct down
     * to a bare "eventually" joint obligation).
     */
    @Override
    public Formula visit(ROperator rOperator) {
        Formula left = rOperator.left.accept(this);
        Formula right = rOperator.right.accept(this);
        Formula current = ROperator.of(left, right);
        int numOfTO = FormulaUtils.numOfTemporalOperators(current);
        if (numOfTO > 2)
            return rOperator;
        if (numOfAllowedWeakenings > 0) {
            boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(weakening_rate) == 0);
            if (mutate) {
                numOfAllowedWeakenings--;
                // a R b = b W (a & b)
                // 0:TRUE 1:F 2:F
                int option = Settings.RANDOM_GENERATOR.nextInt(3);
                if (option == 0)
                    current = BooleanConstant.TRUE;
                else if (option == 1)
                    // weak(b W (a & b)) = F(b) || b U (a & b)
                    current = Disjunction.of(FOperator.of(right), UOperator.of(right, Conjunction.of(left, right)));
                else
                    // weak(b W (a & b)) = G(b) || F (a & b)
                    current = Disjunction.of(GOperator.of(right), FOperator.of(Conjunction.of(left, right)));
            }
        }
        return current;
    }

    /** Rejected: the mutator only ever runs on formulas already converted to negation normal form. */
    @Override
    public Formula visit(Biconditional biconditional) {
        throw new UnsupportedOperationException("FormulaWeakening: formula in NNF was expected: " + biconditional);
    }

    /** Rejected: the mutator only ever runs on formulas already converted to negation normal form. */
    @Override
    public Formula visit(FrequencyG freq) {
        throw new UnsupportedOperationException("FormulaWeakening: formula in NNF was expected: " + freq);
    }

    /** Rejected: the mutator only ever runs on formulas already converted to negation normal form. */
    @Override
    public Formula visit(OOperator oOperator) {
        throw new UnsupportedOperationException("FormulaWeakening: formula in NNF was expected: " + oOperator);
    }

    /** Rejected: past-time operator, not expected in the LTL fragment this mutator handles. */
    @Override
    public Formula visit(HOperator hOperator) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("FormulaWeakening: formula in NNF was expected: " + hOperator);
    }

    /** Rejected: past-time operator, not expected in the LTL fragment this mutator handles. */
    @Override
    public Formula visit(TOperator tOperator) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("FormulaWeakening: formula in NNF was expected: " + tOperator);
    }

    /** Rejected: past-time operator, not expected in the LTL fragment this mutator handles. */
    @Override
    public Formula visit(SOperator sOperator) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("FormulaWeakening: formula in NNF was expected: " + sOperator);
    }

    /** Rejected: past-time operator, not expected in the LTL fragment this mutator handles. */
    @Override
    public Formula visit(YOperator yOperator) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("FormulaWeakening: formula in NNF was expected: " + yOperator);
    }

    /** Rejected: past-time operator, not expected in the LTL fragment this mutator handles. */
    @Override
    public Formula visit(ZOperator zOperator) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("FormulaWeakening: formula in NNF was expected: " + zOperator);
    }

    /**
     * Resolves a variable name to its cached {@link Literal}, growing the
     * cache on demand — unless {@link #fixedVariables} is set (always the
     * case with the current constructor), in which case an unknown name is a
     * programming error.
     *
     * @param name the variable name to resolve
     * @return the corresponding literal
     */
    private Literal createVariable(String name) {
        assert variables.size() == literalCache.size();
        int index = variables.indexOf(name);

        if (index == -1) {
            if (fixedVariables) {
                throw new IllegalStateException("Encountered unknown variable " + name
                        + " with fixed set " + variables);
            }

            int newIndex = variables.size();
            Literal literal = Literal.of(newIndex);
            variables.add(name);
            literalCache.add(literal);
            return literal;
        }

        return literalCache.get(index);
    }

    /**
     * Picks a literal, from the specification's variable alphabet, to
     * combine with a formula being weakened — used by the "add disjunct" /
     * "widen the until" style rules above. Tries (up to 5 attempts) to avoid
     * a variable already appearing (positively or negatively) in
     * {@code current}, then negates the result with 50% probability. Note
     * this is <i>not</i> a logically fresh Skolem variable: it is drawn from
     * the same fixed, finite alphabet as every other proposition in the
     * specification, which is what underlies the soundness caveat discussed
     * in the class documentation.
     *
     * @param current the formula being weakened, used only to bias the draw away from its own propositions
     * @return a literal (possibly negated) from the variable alphabet
     */
    public Literal new_literal(Formula current) {
        Set<Literal> props = current.accept(new PropositionVariablesExtractor());
        int max = variables.size();
        int new_variable = Settings.RANDOM_GENERATOR.nextInt(max);
        Literal new_literal = createVariable(variables.get(new_variable));

        int trying = 0;
        while ((props.contains(new_literal) || props.contains(new_literal.not())) && trying < 5) {
            trying++;
            new_variable = Settings.RANDOM_GENERATOR.nextInt(max);
            new_literal = createVariable(variables.get(new_variable));
        }

        if (Settings.RANDOM_GENERATOR.nextBoolean())
            new_literal = new_literal.not();
        return new_literal;
    }

}