package owl.ltl.visitors;

import owl.ltl.*;

/**
 * Replaces every occurrence of a given sub-formula with another, throughout
 * an LTL formula's syntax tree.
 *
 * <p>This is the "splice" step used across AuRUS's genetic operators: after
 * {@link geneticalgorithm.SpecificationMutator} rewrites a randomly chosen
 * sub-formula {@code to_mutate} (via {@link GeneralFormulaMutator},
 * {@link FormulaWeakening} or {@link FormulaStrengthening}) into
 * {@code mutated}, the change is spliced back into the full assumption or
 * guarantee with {@code new SubformulaReplacer(to_mutate, mutated).apply(original)}.
 * {@link geneticalgorithm.SpecificationCrossover} uses it the same way to
 * graft a fragment transplanted from one parent into the other.</p>
 *
 * <p><b>The pattern.</b> Every {@code visit} method below follows the same
 * template: if the current node structurally equals {@code source} (via
 * {@link Formula#equals}), it is replaced wholesale by {@code target};
 * otherwise the visitor recurses into the node's children and rebuilds the
 * same kind of node from the (possibly already-replaced) results. This is a
 * bottom-up tree rebuild, not an in-place mutation — {@link Formula}s are
 * immutable, so the entire path from the match up to the root is
 * reconstructed.</p>
 *
 * <p><b>Replace by value, not by position.</b> Matching is structural
 * equality, not object identity or tree position: if {@code source} occurs
 * more than once in the formula (e.g. the same sub-formula appearing under
 * two different conjuncts), <i>every</i> occurrence is replaced in the same
 * pass. This is consistent with how callers obtain {@code source} — e.g.
 * {@code FormulaUtils.subformulas(Formula)} returns a {@code Set<Formula>},
 * deduplicating structurally identical sub-formulas before one is chosen at
 * random — but is worth keeping in mind: this class has no notion of "the
 * occurrence at this specific tree position", only "occurrences equal to
 * this formula".</p>
 *
 * <p>Part of the reference implementation of: <i>Brizzio, Cordy, Papadakis,
 * S&aacute;nchez, Aguirre, Degiovanni. "Automated Repair of Unrealisable LTL
 * Specifications Guided by Model Counting", GECCO 2023
 * (<a href="https://doi.org/10.1145/3583131.3590454">doi:10.1145/3583131.3590454</a>).</i></p>
 *
 * @author Mat&iacute;as Brizzio
 * @see geneticalgorithm.SpecificationMutator
 * @see geneticalgorithm.SpecificationCrossover
 * @see utils.FormulaUtils
 */
public class SubformulaReplacer implements Visitor<Formula> {

    /** The sub-formula to look for (matched by structural equality). */
    private final Formula source;

    /** The formula every match is replaced with. */
    private final Formula target;

    /**
     * Creates a replacer that will substitute every occurrence of
     * {@code source} with {@code target}.
     *
     * @param source the sub-formula to find
     * @param target the replacement for every match
     */
    public SubformulaReplacer(Formula source, Formula target) {
        this.source = source;
        this.target = target;
    }

    /**
     * Entry point: applies the replacement throughout the given formula.
     *
     * @param formula the formula to rewrite
     * @return the formula with every occurrence of {@link #source} replaced by {@link #target}
     */
    @Override
    public Formula apply(Formula formula) {
        return formula.accept(this);
    }

    /** Replaces if this node matches {@link #source}; otherwise recurses into both operands. */
    @Override
    public Formula visit(Biconditional biconditional) {
        if (biconditional.equals(this.source))
            return this.target;

        Formula left = biconditional.left.accept(this);
        Formula right = biconditional.right.accept(this);

        return Biconditional.of(left, right);
    }

    /** Replaces if this node matches {@link #source}; a boolean constant has no children to recurse into. */
    @Override
    public Formula visit(BooleanConstant booleanConstant) {
        if (booleanConstant.equals(this.source))
            return this.target;
        return booleanConstant;
    }

    /** Replaces if this node matches {@link #source}; otherwise recurses into every conjunct. */
    @Override
    public Formula visit(Conjunction conjunction) {
        if (conjunction.equals(this.source))
            return this.target;

        return Conjunction.of(conjunction.children.stream().map(x -> x.accept(this)));
    }

    /** Replaces if this node matches {@link #source}; otherwise recurses into every disjunct. */
    @Override
    public Formula visit(Disjunction disjunction) {
        if (disjunction.equals(this.source))
            return this.target;

        return Disjunction.of(disjunction.children.stream().map(x -> x.accept(this)));
    }

    /** Replaces if this node matches {@link #source}; otherwise recurses into the operand. */
    @Override
    public Formula visit(FOperator fOperator) {
        if (fOperator.equals(this.source))
            return this.target;

        Formula operand = fOperator.operand.accept(this);
        return FOperator.of(operand);
    }

    /** Replaces if this node matches {@link #source}; otherwise recurses into the operand. */
    @Override
    public Formula visit(FrequencyG freq) {
        if (freq.equals(this.source))
            return this.target;

        Formula operand = freq.operand.accept(this);
        return FrequencyG.of(operand);
    }

    /** Replaces if this node matches {@link #source}; otherwise recurses into the operand. */
    @Override
    public Formula visit(GOperator gOperator) {
        if (gOperator.equals(this.source))
            return this.target;

        Formula operand = gOperator.operand.accept(this);
        return GOperator.of(operand);
    }

    /** Replaces if this node matches {@link #source}; otherwise recurses into the operand (past-time "historically"). */
    @Override
    public Formula visit(HOperator hOperator) {
        if (hOperator.equals(this.source))
            return this.target;

        Formula operand = hOperator.operand.accept(this);
        return HOperator.of(operand);
    }

    /** Replaces if this literal matches {@link #source}; a literal has no children to recurse into. */
    @Override
    public Formula visit(Literal literal) {
        if (literal.equals(this.source))
            return this.target;

        return literal;

    }

    /** Replaces if this node matches {@link #source}; otherwise recurses into both operands ("strong release"). */
    @Override
    public Formula visit(MOperator mOperator) {
        if (mOperator.equals(this.source))
            return this.target;

        Formula left = mOperator.left.accept(this);
        Formula right = mOperator.right.accept(this);
        return MOperator.of(left, right);
    }

    /** Replaces if this node matches {@link #source}; otherwise recurses into the operand (past-time "once"). */
    @Override
    public Formula visit(OOperator oOperator) {
        if (oOperator.equals(this.source))
            return this.target;

        Formula operand = oOperator.operand.accept(this);
        return OOperator.of(operand);
    }

    /** Replaces if this node matches {@link #source}; otherwise recurses into both operands ("release"). */
    @Override
    public Formula visit(ROperator rOperator) {
        if (rOperator.equals(this.source))
            return this.target;

        Formula left = rOperator.left.accept(this);
        Formula right = rOperator.right.accept(this);

        return ROperator.of(left, right);
    }

    /** Replaces if this node matches {@link #source}; otherwise recurses into both operands (past-time "since"). */
    @Override
    public Formula visit(SOperator sOperator) {
        if (sOperator.equals(this.source))
            return this.target;

        Formula left = sOperator.left.accept(this);
        Formula right = sOperator.right.accept(this);

        return SOperator.of(left, right);
    }

    /** Replaces if this node matches {@link #source}; otherwise recurses into both operands (past-time "trigger"). */
    @Override
    public Formula visit(TOperator tOperator) {
        if (tOperator.equals(this.source))
            return this.target;

        Formula left = tOperator.left.accept(this);
        Formula right = tOperator.right.accept(this);

        return TOperator.of(left, right);
    }

    /** Replaces if this node matches {@link #source}; otherwise recurses into both operands ("until"). */
    @Override
    public Formula visit(UOperator uOperator) {
        if (uOperator.equals(this.source))
            return this.target;
        Formula left = uOperator.left.accept(this);
        Formula right = uOperator.right.accept(this);
        return UOperator.of(left, right);
    }

    /** Replaces if this node matches {@link #source}; otherwise recurses into both operands ("weak until"). */
    @Override
    public Formula visit(WOperator wOperator) {
        if (wOperator.equals(this.source))
            return this.target;
        Formula left = wOperator.left.accept(this);
        Formula right = wOperator.right.accept(this);
        return WOperator.of(left, right);
    }

    /** Replaces if this node matches {@link #source}; otherwise recurses into the operand ("next"). */
    @Override
    public Formula visit(XOperator xOperator) {
        if (xOperator.equals(this.source))
            return this.target;

        Formula operand = xOperator.operand.accept(this);
        return XOperator.of(operand);
    }

    /** Replaces if this node matches {@link #source}; otherwise recurses into the operand (past-time "yesterday"). */
    @Override
    public Formula visit(YOperator yOperator) {
        if (yOperator.equals(this.source))
            return this.target;

        Formula operand = yOperator.operand.accept(this);
        return YOperator.of(operand);
    }

    /** Replaces if this node matches {@link #source}; otherwise recurses into the operand (past-time "weak yesterday"). */
    @Override
    public Formula visit(ZOperator zOperator) {
        if (zOperator.equals(this.source))
            return this.target;

        Formula operand = zOperator.operand.accept(this);
        return ZOperator.of(operand);
    }

}