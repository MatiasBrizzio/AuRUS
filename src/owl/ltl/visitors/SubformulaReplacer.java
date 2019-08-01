package owl.ltl.visitors;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import owl.ltl.Biconditional;
import owl.ltl.BooleanConstant;
import owl.ltl.Conjunction;
import owl.ltl.Disjunction;
import owl.ltl.FOperator;
import owl.ltl.Formula;
import owl.ltl.FrequencyG;
import owl.ltl.GOperator;
import owl.ltl.HOperator;
import owl.ltl.LabelledFormula;
import owl.ltl.Literal;
import owl.ltl.MOperator;
import owl.ltl.OOperator;
import owl.ltl.ROperator;
import owl.ltl.SOperator;
import owl.ltl.TOperator;
import owl.ltl.UOperator;
import owl.ltl.WOperator;
import owl.ltl.XOperator;
import owl.ltl.YOperator;
import owl.ltl.ZOperator;

public class SubformulaReplacer implements Visitor<Formula>{
	
	private final Formula source;
	private final Formula target;
	private List<String> variables = null;
	private List<Literal> literalCache;
	private boolean fixedVariables = false;
	
	public SubformulaReplacer (List<String> literals, Formula source, Formula target) {
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
		this.source = source;
		this.target = target;
	}

	@Override
	public Formula apply(Formula formula) {
		return formula.accept(this);
	}

	@Override
	public Formula visit(Biconditional biconditional) {
		if (biconditional.equals(this.source))
			return this.target;
		
		Formula left = biconditional.left.accept(this);
		Formula right = biconditional.right.accept(this);
		
		return Biconditional.of(left, right);
	}

	@Override
	public Formula visit(BooleanConstant booleanConstant) {
		if (booleanConstant.equals(this.source))
			return this.target;
		return booleanConstant;
	}

	@Override
	public Formula visit(Conjunction conjunction) {
		if (conjunction.equals(this.source))
			return this.target;
		
		return Conjunction.of(conjunction.children.stream().map(x -> x.accept(this)));
	}

	@Override
	public Formula visit(Disjunction disjunction) {
		if (disjunction.equals(this.source))
			return this.target;
		
		return Disjunction.of(disjunction.children.stream().map(x -> x.accept(this)));
	}

	@Override
	public Formula visit(FOperator fOperator) {
		if (fOperator.equals(this.source))
			return this.target;
		
		Formula operand = fOperator.operand.accept(this);
		return FOperator.of(operand);
	}

	@Override
	public Formula visit(FrequencyG freq) {
		if (freq.equals(this.source))
			return this.target;
		
		Formula operand = freq.operand.accept(this);
		return FrequencyG.of(operand);
	}

	@Override
	public Formula visit(GOperator gOperator) {
		if (gOperator.equals(this.source))
			return this.target;
		
		Formula operand = gOperator.operand.accept(this);
		return GOperator.of(operand);
	}

	@Override
	public Formula visit(HOperator hOperator) {
		if (hOperator.equals(this.source))
			return this.target;
		
		Formula operand = hOperator.operand.accept(this);
		return HOperator.of(operand);
	}

	@Override
	public Formula visit(Literal literal) {
		if (literal.equals(this.source))
			return this.target;
		
		return literal;
		
	}

	@Override
	public Formula visit(MOperator mOperator) {
		if (mOperator.equals(this.source))
			return this.target;
		
		Formula left = mOperator.left.accept(this);
		Formula right = mOperator.right.accept(this);
		return MOperator.of(left, right);
	}

	@Override
	public Formula visit(OOperator oOperator) {
		if (oOperator.equals(this.source))
			return this.target;
		
		Formula operand = oOperator.operand.accept(this);
		return OOperator.of(operand);
	}

	@Override
	public Formula visit(ROperator rOperator) {
		if (rOperator.equals(this.source))
			return this.target;
		
		Formula left = rOperator.left.accept(this);
		Formula right = rOperator.right.accept(this);
		
		return ROperator.of(left, right);
	}

	@Override
	public Formula visit(SOperator sOperator) {
		if (sOperator.equals(this.source))
			return this.target;
		
		Formula left = sOperator.left.accept(this);
		Formula right = sOperator.right.accept(this);
		
		return SOperator.of(left, right);
	}

	@Override
	public Formula visit(TOperator tOperator) {
		if (tOperator.equals(this.source))
			return this.target;
		
		Formula left = tOperator.left.accept(this);
		Formula right = tOperator.right.accept(this);
		
		return TOperator.of(left, right);
	}

	@Override
	public Formula visit(UOperator uOperator) {
		if (uOperator.equals(this.source))
			return this.target;
		Formula left = uOperator.left.accept(this);
		Formula right = uOperator.right.accept(this);
		return UOperator.of(left, right);
	}

	@Override
	public Formula visit(WOperator wOperator) {
		if (wOperator.equals(this.source))
			return this.target;
		Formula left = wOperator.left.accept(this);
		Formula right = wOperator.right.accept(this);
		return WOperator.of(left, right);
	}

	@Override
	public Formula visit(XOperator xOperator) {
		if (xOperator.equals(this.source))
			return this.target;
		
		Formula operand = xOperator.operand.accept(this);
		return XOperator.of(operand);
	}

	@Override
	public Formula visit(YOperator yOperator) {
		if (yOperator.equals(this.source))
			return this.target;
		
		Formula operand = yOperator.operand.accept(this);
		return YOperator.of(operand);
	}

	@Override
	public Formula visit(ZOperator zOperator) {
		if (zOperator.equals(this.source))
			return this.target;
		
		Formula operand = zOperator.operand.accept(this);
		return YOperator.of(operand);
	}
	
}
