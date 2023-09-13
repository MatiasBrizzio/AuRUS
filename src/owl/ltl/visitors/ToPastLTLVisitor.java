package owl.ltl.visitors;

import owl.ltl.*;

import java.util.List;

public class ToPastLTLVisitor implements Visitor<Formula> {
    private int max_x_deep;
    private List<String> vars;

    public ToPastLTLVisitor(int max_x_deep, List<String> vars) {
        this.max_x_deep = max_x_deep;
        this.vars = vars;
    }


    @Override
    public Formula apply(Formula formula) {
        return formula.accept(this);
    }

    @Override
    public Formula visit(Biconditional biconditional) {
        Formula left = biconditional.left.accept(this);
        Formula right = biconditional.right.accept(this);
//        return Conjunction.of(Disjunction.of(left.not(),right), Disjunction.of(left,right.not()));
        return Biconditional.of(left, right);
    }

    @Override
    public Formula visit(BooleanConstant booleanConstant) {
        return booleanConstant;
    }

    @Override
    public Formula visit(Conjunction conjunction) {
        return Conjunction.of(conjunction.children.stream().map(x -> x.accept(this)));
    }

    @Override
    public Formula visit(Disjunction disjunction) {
        return Disjunction.of(disjunction.children.stream().map(x -> x.accept(this)));
    }

    @Override
    public Formula visit(FOperator fOperator) {
        throw new UnsupportedOperationException("ToPastLTL: Diamond Operator is not supported. Just Safety: ");
    }

    @Override
    public Formula visit(FrequencyG freq) {
        throw new UnsupportedOperationException("ToPastLTL: FreqG Operator is not supported. Just Safety: ");
    }

    @Override
    public Formula visit(GOperator gOperator) {
        Formula operand = gOperator.operand.accept(this);
        return GOperator.of(operand);
    }

    @Override
    public Formula visit(HOperator hOperator) {
        Formula operand = hOperator.operand.accept(this);
        return HOperator.of(operand);
    }

    @Override
    public Formula visit(Literal literal) {
        if (this.max_x_deep == 0) return literal;
        if (LabelledFormula.of(literal, vars).toString().equals(new String("first"))) return literal;
        if (LabelledFormula.of(literal, vars).toString().equals(new String("!first"))) return literal;

        Formula result = ZOperator.of(literal);
        for (int i = 0; i < max_x_deep - 1; i++) {
            result = ZOperator.of(result);
        }
        return result;
    }

    @Override
    public Formula visit(MOperator mOperator) {
        // p M q" -> "q U (p & q)
        Formula left = mOperator.left.accept(this);
        Formula right = mOperator.right.accept(this);
        return UOperator.of(right, Conjunction.of(right, left));
    }

    @Override
    public Formula visit(OOperator oOperator) {
        Formula operand = oOperator.operand.accept(this);
        return OOperator.of(operand);
    }

    @Override
    public Formula visit(ROperator rOperator) {
        // p R q" -> "q W (p & q)
        Formula left = rOperator.left.accept(this);
        Formula right = rOperator.right.accept(this);
        Formula wformula = WOperator.of(right, Conjunction.of(right, left));
        return wformula.accept(this);
    }

    @Override
    public Formula visit(SOperator sOperator) {
        Formula left = sOperator.left.accept(this);
        Formula right = sOperator.right.accept(this);

        return SOperator.of(left, right);
    }

    @Override
    public Formula visit(TOperator tOperator) {
        Formula left = tOperator.left.accept(this);
        Formula right = tOperator.right.accept(this);

        return TOperator.of(left, right);
    }

    @Override
    public Formula visit(UOperator uOperator) {
        Formula left = uOperator.left.accept(this);
        Formula right = uOperator.right.accept(this);
        return UOperator.of(left, right);
    }

    @Override
    public Formula visit(WOperator wOperator) {
        Formula left = wOperator.left.accept(this);
        Formula right = wOperator.right.accept(this);
        return Disjunction.of(GOperator.of(left), UOperator.of(left, right));
    }

    @Override
    public Formula visit(XOperator xOperator) {
        System.out.println(xOperator);
        System.out.println(GOperator.of(xOperator).height());
        System.out.println(xOperator.height() - 1);
        int difference = this.max_x_deep - (xOperator.height() - 1);
        Formula child = xOperator.children().iterator().next();
        while (!(child instanceof Literal)) {
            child = child.children().iterator().next();
        }
        if (difference == 0)
            return child;
        else {
            Formula result = ZOperator.of(child);
            for (int i = 0; i < difference - 1; i++) {
                result = ZOperator.of(result);
            }
            return result;
        }
    }

    @Override
    public Formula visit(YOperator yOperator) {
        Formula operand = yOperator.operand.accept(this);
        return YOperator.of(operand);
    }

    @Override
    public Formula visit(ZOperator zOperator) {
        Formula operand = zOperator.operand.accept(this);
        return ZOperator.of(operand);
    }

}
