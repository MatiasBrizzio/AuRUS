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
		Formula left = biconditional.left.accept(this);
		Formula right = biconditional.right.accept(this);
		
		if (biconditional.equals(this.source))
			return this.target;
		
		return Biconditional.of(left, right);
	}

	@Override
	public Formula visit(BooleanConstant booleanConstant) {
		// TODO Auto-generated method stub
		return Visitor.super.visit(booleanConstant);
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
		Formula operand = fOperator.operand.accept(this);
		
		if (fOperator.equals(this.source))
			return this.target;
		
		return FOperator.of(operand);
	}

	@Override
	public Formula visit(FrequencyG freq) {
		Formula operand = freq.operand.accept(this);
		
		if (freq.equals(this.source))
			return this.target;
		
		return FrequencyG.of(operand);
	}

	@Override
	public Formula visit(GOperator gOperator) {
		Formula operand = gOperator.operand.accept(this);
		
		if (gOperator.equals(this.source))
			return this.target;
		
		return GOperator.of(operand);
	}

	@Override
	public Formula visit(HOperator hOperator) {
		// TODO Auto-generated method stub
		return Visitor.super.visit(hOperator);
	}

	@Override
	public Formula visit(Literal literal) {
		Formula current = literal;
		
		if (current.equals(this.source))
			return this.target;
		
		return createVariable(LabelledFormula.of(current, variables).toString());
		
	}

	@Override
	public Formula visit(MOperator mOperator) {
		Formula left = mOperator.left.accept(this);
		Formula right = mOperator.right.accept(this);
		
		if (mOperator.equals(this.source))
			return this.target;
		
		return MOperator.of(left, right);
	}

	@Override
	public Formula visit(OOperator oOperator) {
		// TODO Auto-generated method stub
		return Visitor.super.visit(oOperator);
	}

	@Override
	public Formula visit(ROperator rOperator) {
		Formula left = rOperator.left.accept(this);
		Formula right = rOperator.right.accept(this);
		
		if (rOperator.equals(this.source))
			return this.target;
		
		return ROperator.of(left, right);
	}

	@Override
	public Formula visit(SOperator sOperator) {
		// TODO Auto-generated method stub
		return Visitor.super.visit(sOperator);
	}

	@Override
	public Formula visit(TOperator tOperator) {
		// TODO Auto-generated method stub
		return Visitor.super.visit(tOperator);
	}

	@Override
	public Formula visit(UOperator uOperator) {
		Formula left = uOperator.left.accept(this);
		Formula right = uOperator.right.accept(this);
		
		if (uOperator.equals(this.source))
			return this.target;
		
		return UOperator.of(left, right);
	}

	@Override
	public Formula visit(WOperator wOperator) {
		Formula left = wOperator.left.accept(this);
		Formula right = wOperator.right.accept(this);
		
		if (wOperator.equals(this.source))
			return this.target;
		
		return WOperator.of(left, right);
	}

	@Override
	public Formula visit(XOperator xOperator) {
		Formula operand = xOperator.operand.accept(this);
		
		if (xOperator.equals(this.source))
			return this.target;
		
		return XOperator.of(operand);
	}

	@Override
	public Formula visit(YOperator yOperator) {
		// TODO Auto-generated method stub
		return Visitor.super.visit(yOperator);
	}

	@Override
	public Formula visit(ZOperator zOperator) {
		// TODO Auto-generated method stub
		return Visitor.super.visit(zOperator);
	}

	private Formula createVariable(String name) {
		assert variables.size() == literalCache.size();
		int index = variables.indexOf(name);
	
		if (index == -1) {
			
			if (fixedVariables) {
				throw new IllegalStateException("Encountered unknown variable " + name    + " with fixed set " + variables);
			}
			
	      int newIndex = variables.size();
	      Literal literal = Literal.of(newIndex);
	      variables.add(name);
	      literalCache.add(literal);
	      return literal;
	    }
	
	    return literalCache.get(index);
	  }
	
	
}
