package owl.ltl.visitors;

import owl.ltl.Biconditional;
import owl.ltl.BooleanConstant;
import owl.ltl.Conjunction;
import owl.ltl.Disjunction;
import owl.ltl.FOperator;
import owl.ltl.Formula;
import owl.ltl.FrequencyG;
import owl.ltl.GOperator;
import owl.ltl.HOperator;
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
	
	public SubformulaReplacer (Formula source, Formula target) {
		this.source = source;
		this.target = target;
	}

	@Override
	public Formula apply(Formula formula) {
		return formula.accept(this);
	}

	@Override
	public Formula visit(Biconditional biconditional) {
		// TODO Auto-generated method stub
		return Visitor.super.visit(biconditional);
	}

	@Override
	public Formula visit(BooleanConstant booleanConstant) {
		// TODO Auto-generated method stub
		return Visitor.super.visit(booleanConstant);
	}

	@Override
	public Formula visit(Conjunction conjunction) {
		// TODO Auto-generated method stub
		return Visitor.super.visit(conjunction);
	}

	@Override
	public Formula visit(Disjunction disjunction) {
		// TODO Auto-generated method stub
		return Visitor.super.visit(disjunction);
	}

	@Override
	public Formula visit(FOperator fOperator) {
		// TODO Auto-generated method stub
		return Visitor.super.visit(fOperator);
	}

	@Override
	public Formula visit(FrequencyG freq) {
		// TODO Auto-generated method stub
		return Visitor.super.visit(freq);
	}

	@Override
	public Formula visit(GOperator gOperator) {
		// TODO Auto-generated method stub
		return Visitor.super.visit(gOperator);
	}

	@Override
	public Formula visit(HOperator hOperator) {
		// TODO Auto-generated method stub
		return Visitor.super.visit(hOperator);
	}

	@Override
	public Formula visit(Literal literal) {
		// TODO Auto-generated method stub
		return Visitor.super.visit(literal);
	}

	@Override
	public Formula visit(MOperator mOperator) {
		// TODO Auto-generated method stub
		return Visitor.super.visit(mOperator);
	}

	@Override
	public Formula visit(OOperator oOperator) {
		// TODO Auto-generated method stub
		return Visitor.super.visit(oOperator);
	}

	@Override
	public Formula visit(ROperator rOperator) {
		// TODO Auto-generated method stub
		return Visitor.super.visit(rOperator);
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
		// TODO Auto-generated method stub
		return Visitor.super.visit(uOperator);
	}

	@Override
	public Formula visit(WOperator wOperator) {
		// TODO Auto-generated method stub
		return Visitor.super.visit(wOperator);
	}

	@Override
	public Formula visit(XOperator xOperator) {
		// TODO Auto-generated method stub
		return Visitor.super.visit(xOperator);
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

}
