package owl.ltl.visitors;

import main.Settings;
import owl.ltl.*;
import owl.ltl.rewriter.NormalForms;
import utils.FormulaUtils;

import java.util.*;

/**
 * The <b>strengthening</b> mutation mode of AuRUS: rewrites an LTL formula
 * (in negation normal form) into a semantically stronger one, i.e. a formula
 * &phi;<sub>s</sub> such that &phi;<sub>s</sub> &#8872; &phi; — no new model
 * is introduced by the rewrite. Applied to a guarantee, this tightens an
 * obligation on the system — the default direction for guarantee mutations,
 * since unrealisability most often originates there (see
 * {@code geneticalgorithm.SpecificationMutator}). See
 * {@link FormulaWeakening} for the dual mode; the two classes are structural
 * mirrors of each other, operator for operator.
 *
 * <p>This class implements the strengthening rules of Table 4.1 in the paper
 * and thesis this project is the reference implementation of (one
 * {@code visit} method per LTL operator, one rewrite rule per numbered
 * {@code option}); see the individual {@code visit} methods below for the
 * correspondence. The two additional operators {@code M} (strong release)
 * and {@code R} (release) are handled via their standard dualities with
 * {@code U}/{@code W} (noted inline) rather than appearing as separate rows
 * in the thesis table.</p>
 *
 * <p><b>How mutation is applied.</b> This is a recursive {@link Visitor}: it
 * walks the formula bottom-up, and at <i>every</i> node — not just the root —
 * it may independently roll the dice to strengthen that node. Two knobs
 * control this, both threaded through the constructor, mirroring
 * {@link FormulaWeakening}: <b>{@code strengthening_rate}</b> (per-node
 * mutation probability {@code 1/strengthening_rate}) and
 * <b>{@code numOfAllowedStrengthenings}</b> (a mutable budget of remaining
 * mutations, decremented on each applied change).</p>
 *
 * <p><b>Bloat guard.</b> As in {@link FormulaWeakening}, several
 * {@code visit} methods count the number of temporal operators in the
 * (already partially rewritten) current formula and abandon further
 * strengthening of that node — returning the <i>original</i> sub-formula —
 * once the count exceeds {@code 2}.</p>
 *
 * <p><b>Universal strongest bound.</b> At every node, one of the random
 * options is simply {@code false} — the strongest possible formula — giving
 * the search a way to discard a sub-formula's satisfiability entirely.</p>
 *
 * <p><b>A note on soundness of the fallback rules.</b> As with
 * {@link FormulaWeakening}, most rules below are sound by construction. The
 * unrolling fallback branches of {@link #visit(UOperator)},
 * {@link #visit(FOperator)} (option 7) and their siblings, which introduce a
 * fresh draw via {@link #new_literal(Formula)}, share the same caveat
 * documented there: the drawn proposition is taken from the same fixed
 * alphabet as the rest of the specification, not a logically fresh variable,
 * so these particular branches are heuristic exploration rather than
 * guaranteed entailments for every possible draw.</p>
 *
 * <p>Part of the reference implementation of: <i>Brizzio, Cordy, Papadakis,
 * S&aacute;nchez, Aguirre, Degiovanni. "Automated Repair of Unrealisable LTL
 * Specifications Guided by Model Counting", GECCO 2023
 * (<a href="https://doi.org/10.1145/3583131.3590454">doi:10.1145/3583131.3590454</a>).</i></p>
 *
 * @author Mat&iacute;as Brizzio
 * @see FormulaWeakening
 * @see GeneralFormulaMutator
 */
public class FormulaStrengthening implements Visitor<Formula> {

    /** Literal cache aligned index-for-index with {@link #variables}. */
    private final List<Literal> literalCache;

    /** The specification's variable names, in the fixed index order used to build {@link Literal}s. */
    private final List<String> variables;

    /** Always {@code true} in the current constructor — the variable set is fixed at construction time, not grown on demand. */
    private final boolean fixedVariables;

    /** Per-node mutation probability is {@code 1/strengthening_rate}. */
    private final int strengthening_rate;

    /** Print step-by-step before/after traces of each applied strengthening to standard output — off by default. */
    private final boolean print_debug_info = false;

    /** Remaining budget of sub-formulas this visitor is still allowed to strengthen; decremented on each applied mutation. */
    private int numOfAllowedStrengthenings;

    /**
     * Creates a strengthening visitor over the given variable alphabet.
     *
     * @param literals                        the specification's variable names, in index order
     * @param strengthening_rate              per-node mutation probability is {@code 1/strengthening_rate}
     * @param num_of_strengthening_to_apply    maximum number of sub-formulas this visitor may strengthen in one pass
     */
    public FormulaStrengthening(List<String> literals, int strengthening_rate, int num_of_strengthening_to_apply) {
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
        this.strengthening_rate = strengthening_rate;
        this.numOfAllowedStrengthenings = num_of_strengthening_to_apply;

    }

    /** @return a defensive copy of the variable alphabet this visitor was built with */
    public List<String> variables() {
        return List.copyOf(variables);
    }

    /**
     * Entry point: applies this visitor to the root of the given formula.
     *
     * @param formula the formula to strengthen (should be in negation normal form)
     * @return the (possibly) strengthened formula
     */
    @Override
    public Formula apply(Formula formula) {
        return formula.accept(this);
    }


    /**
     * Strengthens a boolean constant: {@code true ⤳ false} (the only
     * possible strengthening of a constant; {@code false} is already the
     * strongest formula and is left untouched by the random draw).
     */
    @Override
    public Formula visit(BooleanConstant booleanConstant) {
        Formula current = booleanConstant;
        if (numOfAllowedStrengthenings > 0) {
            boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(strengthening_rate) == 0);
            if (mutate) {
                numOfAllowedStrengthenings--;
                current = BooleanConstant.FALSE;
            }
        }
        return current;
    }

    /**
     * Strengthens an atomic literal {@code p} into one of, chosen uniformly
     * at random: {@code false} (option 0); {@code p ∧ q} for a fresh literal
     * {@code q} (option 1 — Table 4.1's "add conjunct" rule); or
     * {@code G(p)} (option 2 — "always" is stronger than "now").
     */
    @Override
    public Formula visit(Literal literal) {
        Formula current = literal;
        if (numOfAllowedStrengthenings > 0) {
            boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(strengthening_rate) == 0);
            if (mutate) {
                numOfAllowedStrengthenings--;
                // 0: FALSE 1: add conjunct 2:G
                int option = Settings.RANDOM_GENERATOR.nextInt(3);
                if (print_debug_info) System.out.print("before: " + literal + " random: " + option);
                if (option == 0)
                    current = BooleanConstant.FALSE;
                else if (option == 1) {
                    // strength(a) = a & b
                    int new_variable = Settings.RANDOM_GENERATOR.nextInt(variables.size());
                    while (new_variable == literal.getAtom())
                        new_variable = Settings.RANDOM_GENERATOR.nextInt(variables.size());
                    Literal new_literal = createVariable(variables.get(new_variable));
                    if (Settings.RANDOM_GENERATOR.nextBoolean())
                        new_literal = new_literal.not();
                    current = Conjunction.of(current, new_literal);
                } else {
                    // strength(a) = G(a)
                    current = GOperator.of(current);
                }
                if (print_debug_info) System.out.println(" after: " + current);
            }
        }
        return current;
    }

    /**
     * Strengthens {@code X(a)} (recursing into {@code a} first) into one of:
     * {@code false} (option 0, default); {@code G(a)} (option 1 — "always"
     * from the next step on is stronger than a single "next"); or, when
     * {@code a} is itself {@code G(b)}, simplifying {@code X G(b)} down to
     * {@code G(b)} (option 2 — equivalent, since "always from the next step"
     * already implies "always").
     */
    @Override
    public Formula visit(XOperator xOperator) {
        Formula operand = xOperator.operand.accept(this);
        Formula current = XOperator.of(operand);
        int numOfTO = FormulaUtils.numOfTemporalOperators(current);
        if (numOfTO > 2)
            return xOperator;
        if (numOfAllowedStrengthenings > 0) {
            boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(strengthening_rate) == 0);
            if (mutate) {
                numOfAllowedStrengthenings--;
                // 0:FALSE 1:G 2: remove X
                int option = Settings.RANDOM_GENERATOR.nextInt(3);
                if (print_debug_info) System.out.print("before: " + xOperator + " random: " + option);
                current = BooleanConstant.FALSE; //(option == 0) and default
                if (option == 1) {
                    // strength(X a) = G(a)
                    current = GOperator.of(operand);
                } else if (option == 2 && operand instanceof GOperator) {
                    // strength(X G a) = G(a)
                    current = operand;
                }
                if (print_debug_info) System.out.println(" after: " + current);
            }
        }
        return current;
    }


    /**
     * Strengthens {@code F(a)} (recursing into {@code a} first) into one of,
     * drawn uniformly out of 8 outcomes: {@code false} (option 0);
     * {@code a} itself (option 1 — requiring it <i>now</i> rather than
     * eventually); {@code X(a)} (option 2 — requiring it at the very next
     * step); {@code G(a)} (option 3 — requiring it forever, Table 4.1's
     * strongest "eventually" tightening); {@code F X(a)} (option 4, guarded
     * against runaway size — delaying by exactly one step); {@code F G(a)}
     * (option 5, same guard — "eventually always", strictly implying plain
     * "eventually"); {@code G F(a)} (option 6, same guard — "infinitely
     * often" is the valid strengthening direction of "eventually always",
     * the mirror of {@link FormulaWeakening#visit(GOperator)}'s weakening
     * of {@code G F} down to {@code F G}); or {@code a U b} for a drawn
     * literal {@code b} (option 7 — requiring {@code a} to hold at least
     * until some point, rather than merely eventually).
     */
    @Override
    public Formula visit(FOperator fOperator) {
        Formula operand = fOperator.operand.accept(this);
        Formula current = FOperator.of(operand);
        int numOfTO = FormulaUtils.numOfTemporalOperators(current);
        if (numOfTO > 2)
            return fOperator;
        if (numOfAllowedStrengthenings > 0) {
            boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(strengthening_rate) == 0);
            if (mutate) {
                numOfAllowedStrengthenings--;
                // 0:FALSE 1:removeOp 2:X 3:G 4:FX 5:FG 6:GF 7:U
                int option = Settings.RANDOM_GENERATOR.nextInt(8);
                if (print_debug_info) System.out.print("before: " + fOperator + " random: " + option);
                if (option == 0)
                    current = BooleanConstant.FALSE;
                else if (option == 1)
                    // strength (F (a)) = a
                    current = operand;
                else if (option == 2) {
                    // strength (F (a)) = X (a)
                    current = XOperator.of(operand);
                } else if (option == 3) {
                    // strength (F(a)) = G (a)
                    current = GOperator.of(operand);
                } else if (option == 4 && numOfTO < 2) {
                    // strength (F(a)) = F X(a)
                    current = FOperator.of(XOperator.of(operand));
                } else if (option == 5 && numOfTO < 2) {
                    // strength (F(a)) = F G(a)
                    current = FOperator.of(GOperator.of(operand));
                } else if (option == 6 && numOfTO < 2) {
                    // strength (F(a)) = G F(a)
                    current = GOperator.of(FOperator.of(operand));
                } else if (option == 7) {
                    // strength (F(a)) = a U b
                    //Formula new_literal = createVariable(variables.get(Settings.RANDOM_GENERATOR.nextInt(variables.size())));
                    //if (Settings.RANDOM_GENERATOR.nextBoolean())
                    //new_literal = new_literal.not();
                    current = UOperator.of(operand, new_literal(current));
                }
                if (print_debug_info) System.out.println(" after: " + current);
            }
        }
        return current;
    }


    /**
     * Strengthens {@code G(a)} (recursing into {@code a} first) into one of,
     * drawn uniformly out of 5 outcomes: {@code false} (option 0, default);
     * distributing over a disjunction, {@code G(a1 ∨ a2) ⤳ G(a1) ∨ G(a2)}
     * (option 1, only when {@code a} is a disjunction, via the disjunctive
     * normal form of the operand — each disjunct is required to hold
     * forever on its own, rather than the weaker requirement that some
     * disjunct hold at each step); {@code G F(b) ⤳ F G(b)} (option 2, when
     * {@code a = F(b)} — the valid strengthening direction noted above);
     * simplifying {@code G X(b)} down to {@code G(b)} (option 3, when
     * {@code a = X(b)} — equivalent, since "always" already covers the next
     * step); or simplifying the (already-idempotent) {@code G G(b)} down to
     * {@code G(b)} (option 4, when {@code a} is itself a {@code G}-formula —
     * a non-strict, equivalence-preserving simplification rather than a
     * proper strengthening).
     */
    @Override
    public Formula visit(GOperator gOperator) {
        Formula operand = gOperator.operand.accept(this);
        Formula current = GOperator.of(operand);
        int numOfTO = FormulaUtils.numOfTemporalOperators(current);
        if (numOfTO > 2)
            return gOperator;
        if (numOfAllowedStrengthenings > 0) {
            boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(strengthening_rate) == 0);
            if (mutate) {
                numOfAllowedStrengthenings--;
                // 0:FLASE 1:distribute to disjunction 2:infinitely often to persistence 3:remove X 4:remove F
                int option = Settings.RANDOM_GENERATOR.nextInt(5);
                if (print_debug_info) System.out.print("before: " + gOperator + " random: " + option);
                current = BooleanConstant.FALSE; // (option == 0) and default
                if (option == 1 && operand instanceof Disjunction && numOfTO < 2) {
                    // strengthen (G (a | b)) = G(a) | G(b)
                    for (Set<Formula> c : NormalForms.toDnf(operand)) {
                        Formula clause = Conjunction.of(c);
                        current = Disjunction.of(current, GOperator.of(clause));
                    }
                } else if (option == 2 && operand instanceof FOperator) {
                    // strengthen (G F (a)) = F G (a)
                    current = FOperator.of(GOperator.of(operand.children().iterator().next()));
                } else if (option == 3 && operand instanceof XOperator) {
                    // strengthen (G X (a)) = G (a)
                    current = GOperator.of(operand.children().iterator().next());
                } else if (option == 4 && operand instanceof GOperator) {
                    // strengthen (G G (a)) = G (a) -- idempotence simplification, not a strict strengthening
                    current = GOperator.of(operand.children().iterator().next());
                }
                if (print_debug_info) System.out.println(" after: " + current);
            }
        }
        return current;
    }


    /**
     * Strengthens a conjunction {@code a1 ∧ ... ∧ an} (recursing into every
     * conjunct first) into one of: {@code false} (option 0); adding one more
     * conjunct drawn from the alphabet, {@code a1 ∧ a2 ⤳ a1 ∧ a2 ∧ c}
     * (option 1 — a conjunction can only become more demanding by adding
     * conjuncts); or, when not already temporally heavy, {@code G(a1 ∧ ... ∧ an)}
     * (option 2 — requiring the whole conjunction to hold forever rather
     * than just now).
     */
    @Override
    public Formula visit(Conjunction conjunction) {
        Formula current = Conjunction.of(conjunction.children.stream().map(x -> x.accept(this)));
        int numOfTO = FormulaUtils.numOfTemporalOperators(current);
        if (numOfTO > 2)
            return conjunction;
        if (numOfAllowedStrengthenings > 0) {
            boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(strengthening_rate) == 0);
            if (mutate) {
                // 0: FALSE 1:add conjunct 2:G
                numOfAllowedStrengthenings--;
                int option = Settings.RANDOM_GENERATOR.nextInt(3);
                if (print_debug_info) System.out.print("before: " + conjunction + " random: " + option);
                if (option == 0)
                    current = BooleanConstant.FALSE;
                else if (option == 1) {
                    //Formula new_literal = createVariable(variables.get(Settings.RANDOM_GENERATOR.nextInt(variables.size())));
                    //if (Settings.RANDOM_GENERATOR.nextBoolean())
                    //new_literal = new_literal.not();
                    current = Conjunction.of(current, new_literal(current)); // strengthen(a & b) = a & b & c
                } else if (numOfTO < 2) {
                    current = GOperator.of(current); // strengthen(a & b) = G(a & b)
                }
                if (print_debug_info) System.out.println(" after: " + current);
            }
        }
        return current;
    }

    /**
     * Strengthens a disjunction {@code a1 ∨ ... ∨ an} (recursing into every
     * disjunct first) into one of: {@code false} (option 0); turning the
     * whole disjunction into a conjunction, {@code a1 ∨ a2 ⤳ a1 ∧ a2}
     * (option 1 — Table 4.1's "conjunct" rule); dropping one randomly chosen
     * disjunct, {@code a1 ∨ a2 ⤳ a1} (option 2 — narrowing which alternative
     * is allowed); or, when not already temporally heavy,
     * {@code G(a1 ∨ ... ∨ an)} (option 3 — requiring the disjunction to hold
     * forever rather than just now).
     */
    @Override
    public Formula visit(Disjunction disjunction) {
        Formula current = Disjunction.of(disjunction.children.stream().map(x -> x.accept(this)));
        int numOfTO = FormulaUtils.numOfTemporalOperators(current);
        if (numOfTO > 2)
            return disjunction;
        if (numOfAllowedStrengthenings > 0) {
            boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(strengthening_rate) == 0);
            if (mutate) {
                // 0: FALSE 1:conjunct 2: remove disjunct 3:G
                numOfAllowedStrengthenings--;
                int option = Settings.RANDOM_GENERATOR.nextInt(4);
                if (print_debug_info) System.out.print("before: " + disjunction + " random: " + option);
                if (option == 0)
                    current = BooleanConstant.FALSE;
                else if (option == 1) {
                    if (!current.children().isEmpty())
                        current = Conjunction.of(current.children()); // strengthen(a | b) = a & b
                } else if (option == 2) {
                    if (!current.children().isEmpty()) {
                        int to_be_removed = Settings.RANDOM_GENERATOR.nextInt(current.children().size());
                        List<Formula> new_set_children = new LinkedList<Formula>();
                        Iterator<Formula> it = current.children().iterator();
                        int i = 0;
                        while (it.hasNext()) {
                            if (i != to_be_removed)
                                new_set_children.add(it.next());
                            i++;
                        }
                        current = Disjunction.of(new_set_children);
                    }
                } else if (numOfTO < 2) {
                    current = GOperator.of(current); // strengthen(a | b) = G(a | b)
                }
                if (print_debug_info) System.out.println(" after: " + current);
            }
        }
        return current;
    }


    /**
     * Strengthens {@code a U b} (recursing into both sides first) via its
     * unrolling {@code a U b ≡ b ∨ (a ∧ ¬b ∧ X(a U b))}, into one of:
     * {@code false} (option 0); {@code b} (option 1 — requiring the
     * eventual condition to hold <i>now</i>, dropping the "until" build-up
     * entirely); or, when not already temporally heavy,
     * {@code a ∧ ¬b ∧ X(a U b)} (option 2 — unrolling one step: {@code a}
     * must hold now, {@code b} must not yet, and the same obligation
     * continues from the next step).
     */
    @Override
    public Formula visit(UOperator uOperator) {
        Formula left = uOperator.left.accept(this);
        Formula right = uOperator.right.accept(this);
        Formula current = UOperator.of(left, right);
        int numOfTO = FormulaUtils.numOfTemporalOperators(current);
        if (numOfTO > 2)
            return uOperator;
        if (numOfAllowedStrengthenings > 0) {
            boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(strengthening_rate) == 0);
            if (mutate) {
                numOfAllowedStrengthenings--;
                // a U b = b || a & !b & X(a U b).
                // we decided to strengthen each disjunct.
                // 0:FALSE 1:b 2:a & X(a U b)
                int option = Settings.RANDOM_GENERATOR.nextInt(3);
                if (print_debug_info) System.out.print("before: " + uOperator + " random: " + option);
                if (option == 0)
                    current = BooleanConstant.FALSE;
                else if (option == 1)
                    current = right; // strengthen(a U b) = b
                else // numOfTO < 2
                    current = Conjunction.of(left, right.not(), XOperator.of(current)); // strengthen(a U b) = a & !b & X(a U b)
                if (print_debug_info) System.out.println(" after: " + current);
            }
        }
        return current;
    }

    /**
     * Strengthens {@code a W b} (recursing into both sides first). Recalling
     * {@code a W b ≡ G(a) ∨ (a U b)}, this collapses the disjunction to one
     * side rather than weakening it: {@code false} (option 0);
     * {@code G(a)} (option 1 — keeping only the "always" disjunct); or
     * {@code a U b} (option 2 — keeping only the "until" disjunct, which
     * additionally requires {@code b} to eventually hold — strictly stronger
     * than the original weak-until).
     */
    @Override
    public Formula visit(WOperator wOperator) {
        Formula left = wOperator.left.accept(this);
        Formula right = wOperator.right.accept(this);
        Formula current = WOperator.of(left, right);
        int numOfTO = FormulaUtils.numOfTemporalOperators(current);
        if (numOfTO > 2)
            return wOperator;
        if (numOfAllowedStrengthenings > 0) {
            boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(strengthening_rate) == 0);
            if (mutate) {
                numOfAllowedStrengthenings--;
                // a W b = G(a) || a U b.
                // we decided to weak the each disjunct.
                // 0:FALSE 1:G(a) 2:a U b
                int option = Settings.RANDOM_GENERATOR.nextInt(3);
                if (print_debug_info) System.out.print("before: " + wOperator + " random: " + option);
                if (option == 0)
                    current = BooleanConstant.FALSE;
                else if (option == 1)
                    current = GOperator.of(left); // strengthen(a W b) = G(a)
                else
                    current = UOperator.of(left, right); // strengthen(a W b) = a U b
                if (print_debug_info) System.out.println(" after: " + current);
            }
        }
        return current;
    }


    /**
     * Strengthens {@code a M b} ("strong release" — via the standard
     * duality {@code a M b ≡ b U (a ∧ b)}, and its own unrolling
     * {@code ≡ (a ∧ b) ∨ (b ∧ ¬(a ∧ b) ∧ X(b U (a ∧ b)))} — recursing into
     * both sides first) into one of: {@code false} (option 0);
     * {@code a ∧ b} (option 1 — requiring the joint condition to hold
     * <i>now</i>, the strongest immediate reading); or
     * {@code b ∧ ¬(a ∧ b) ∧ X(a M b)} (option 2 — unrolling one step of the
     * "strong release" obligation).
     */
    @Override
    public Formula visit(MOperator mOperator) {
        Formula left = mOperator.left.accept(this);
        Formula right = mOperator.right.accept(this);
        Formula current = MOperator.of(left, right);
        int numOfTO = FormulaUtils.numOfTemporalOperators(current);
        if (numOfTO > 2)
            return mOperator;
        if (numOfAllowedStrengthenings > 0) {
            boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(strengthening_rate) == 0);
            if (mutate) {
                numOfAllowedStrengthenings--;

                // a M b = b U (a & b) = (a & b) || b & !(a & b) & X(b U (a & b))
                // we decided to strengthen each disjunct.
                // 0:FALSE 1:(a & b) 2:b & !(a & b) & X(b U (a & b))
                int option = Settings.RANDOM_GENERATOR.nextInt(3);
                if (print_debug_info) System.out.print("before: " + mOperator + " random: " + option);
                if (option == 0)
                    current = BooleanConstant.FALSE;
                else if (option == 1)
                    current = Conjunction.of(left, right); // strengthen(a M b) = (a & b)
                else
                    current = Conjunction.of(right, Conjunction.of(left, right).not(), XOperator.of(current)); // strengthen(a M b) = b & !(a & b) & X(a M b)
                if (print_debug_info) System.out.println(" after: " + current);
            }
        }
        return current;
    }

    /**
     * Strengthens {@code a R b} ("release" — via the standard duality
     * {@code a R b ≡ b W (a ∧ b) ≡ G(b) ∨ (a M b)} — recursing into both
     * sides first) into one of: {@code false} (option 0); {@code G(b)}
     * (option 1 — keeping only the "always {@code b}" disjunct); or
     * {@code a M b} (option 2 — keeping only the "strong release" disjunct,
     * strictly stronger than the original release).
     */
    @Override
    public Formula visit(ROperator rOperator) {
        Formula left = rOperator.left.accept(this);
        Formula right = rOperator.right.accept(this);
        Formula current = ROperator.of(left, right);
        int numOfTO = FormulaUtils.numOfTemporalOperators(current);
        if (numOfTO > 2)
            return rOperator;
        if (numOfAllowedStrengthenings > 0) {
            boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(strengthening_rate) == 0);
            if (mutate) {
                numOfAllowedStrengthenings--;
                // a R b = b W (a & b) = G(b) || a M b
                // we decided to weak the each disjunct.
                // 0:FALSE 1:G(a) 2:a U b
                int option = Settings.RANDOM_GENERATOR.nextInt(3);
                if (print_debug_info) System.out.print("before: " + rOperator + " random: " + option);
                if (option == 0)
                    current = BooleanConstant.FALSE;
                else if (option == 1)
                    current = GOperator.of(right); // strengthen(a W b) = G(b)
                else
                    current = MOperator.of(left, right); // strengthen(a R b) = a M b
                if (print_debug_info) System.out.println(" after: " + current);
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
     * combine with a formula being strengthened. Mirrors
     * {@link FormulaWeakening#new_literal(Formula)}: tries (up to 5
     * attempts) to avoid a variable already appearing in {@code current},
     * then negates the result with 50% probability. Not a logically fresh
     * Skolem variable — see the class-level soundness caveat.
     *
     * @param current the formula being strengthened, used only to bias the draw away from its own propositions
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