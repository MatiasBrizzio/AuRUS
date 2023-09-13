package owl.ltl.visitors;

import owl.ltl.*;
import owl.ltl.FrequencyG.Limes;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GeneratePyAigerInput implements Visitor<String> {

    private final boolean parenthesize;

    @Nullable
    private final List<String> variableMapping;

    public GeneratePyAigerInput(boolean parenthesize, @Nullable List<String> variableMapping) {
        this.variableMapping = variableMapping;
        this.parenthesize = parenthesize;
    }

    public static String toString(Formula formula, @Nullable List<String> variableMapping) {
        return toString(formula, variableMapping, false);
    }

    public static String toString(Formula formula, @Nullable List<String> variableMapping, boolean parenthesize) {
        GeneratePyAigerInput visitor = new GeneratePyAigerInput(parenthesize, variableMapping);
        return (String) formula.accept(visitor);
    }

    public static String toString(LabelledFormula formula, boolean parenthesize) {
        GeneratePyAigerInput visitor = new GeneratePyAigerInput(parenthesize, formula.variables());
        return (String) formula.formula().accept(visitor);
    }

    public String visit(Biconditional biconditional) {
        String var10000 = this.visitParenthesized(biconditional.left);
        return "(" + var10000 + " <-> " + this.visitParenthesized(biconditional.right) + ")";
    }

    public String visit(BooleanConstant booleanConstant) {
        return booleanConstant.toString();
    }

    public String visit(Conjunction conjunction) {
        Stream var10000 = conjunction.children.stream().sorted(Comparator.naturalOrder()).map(this::visitParenthesized);
        int length = conjunction.children.size();
        if (length == 2) {
            return "(" + (String) var10000.collect(Collectors.joining(" & ")) + ")";
        } else {
            List<String> lst = (List<String>) var10000.collect(Collectors.toList());
            StringBuilder res = new StringBuilder("(" + lst.get(0) + " & " + lst.get(1) + ")");
            for (int i = 2; i < lst.size(); i++) {
                res = new StringBuilder("(" + res + " & " + lst.get(i) + ")");
            }
//            System.out.println(res);
            return res.toString();
        }
    }

    public String visit(Disjunction disjunction) {
        Stream var10000 = disjunction.children.stream().sorted(Comparator.naturalOrder()).map(this::visitParenthesized);
        int length = disjunction.children.size();
        if (length == 2) {
            return "(" + (String) var10000.collect(Collectors.joining(" | ")) + ")";
        } else {
            List<String> lst = (List<String>) var10000.collect(Collectors.toList());
            StringBuilder res = new StringBuilder("(" + lst.get(0) + " | " + lst.get(1) + ")");
            for (int i = 2; i < lst.size(); i++) {
                res = new StringBuilder("(" + res + " | " + lst.get(i) + ")");
            }
            return res.toString();
        }
    }

    public String visit(FOperator fOperator) {
        return this.visit((UnaryModalOperator) fOperator);
    }

    public String visit(OOperator oOperator) {
        return this.visit((UnaryModalOperator) oOperator);
    }

    public String visit(FrequencyG freq) {
        Limes var10000 = freq.limes;
        return "G {" + var10000 + freq.cmp + freq.bound + "} " + (String) freq.operand.accept(this);
    }

    public String visit(GOperator gOperator) {
        return this.visit((UnaryModalOperator) gOperator);
    }

    public String visit(Literal literal) {
        String name = this.variableMapping == null ? "p" + literal.getAtom() : (String) this.variableMapping.get(literal.getAtom());
        return literal.isNegated() ? "~" + name : name;
    }

    public String visit(MOperator mOperator) {
        return this.visit((BinaryModalOperator) mOperator);
    }

    public String visit(ROperator rOperator) {
        return this.visit((BinaryModalOperator) rOperator);
    }

    public String visit(UOperator uOperator) {
        return this.visit((BinaryModalOperator) uOperator);
    }

    public String visit(WOperator wOperator) {
        return this.visit((BinaryModalOperator) wOperator);
    }

    public String visit(XOperator xOperator) {
        return this.visit((UnaryModalOperator) xOperator);
    }

    public String visit(YOperator yOperator) {
        return this.visit((UnaryModalOperator) yOperator);
    }

    public String visit(ZOperator zOperator) {
        return this.visit((UnaryModalOperator) zOperator);
    }

    public String visit(HOperator hOperator) {
        return this.visit((UnaryModalOperator) hOperator);
    }

    private String visit(UnaryModalOperator operator) {
        String var10000 = operator.operatorSymbol();
        return var10000 + " " + this.visitParenthesized(operator.operand);
    }

    private String visit(BinaryModalOperator operator) {
        String var10000 = (String) operator.left.accept(this);
        if (var10000.equals("S")) {
            return "[" + var10000 + " " + operator.operatorSymbol() + " " + (String) operator.right.accept(this) + "]";
        } else {
//            throw new IllegalArgumentException("Beside S. No other Binary operator is supported");
            return "(" + var10000 + " " + operator.operatorSymbol() + " " + (String) operator.right.accept(this) + ")";
        }
    }

    private String visitParenthesized(Formula formula) {
        return this.parenthesize ? "(" + (String) formula.accept(this) + ")" : (String) formula.accept(this);
    }
}
