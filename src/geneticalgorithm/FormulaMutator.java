package geneticalgorithm;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

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

public class FormulaMutator {


	public static Formula mutate (Formula formula, List<String> variables) {
		if (formula instanceof BooleanConstant || formula instanceof Literal)
			return mutateLiteral(formula, variables);
		else if (formula instanceof FOperator || formula instanceof GOperator || formula instanceof XOperator)
			return mutateUnaryFormula(formula);
		else if (formula instanceof Biconditional || formula instanceof UOperator || formula instanceof WOperator)
			return mutateBinaryFormula(formula);
		else if (formula instanceof Conjunction || formula instanceof Disjunction)
			return mutatePropositionalFormula(formula);
		else //	    FrequencyG.class, YOperator.class, ZOperator.class, HOperator.class, MOperator.class, OOperator.class, ROperator.class, SOperator.class, TOperator.class,
			throw new IllegalArgumentException("FormulaMutator.mutate cannot handle formula: " + formula);
	}
	
	public static Formula mutateLiteral (Formula formula, List<String> variables) {
		if (!(formula instanceof BooleanConstant || formula instanceof Literal))
			throw new IllegalArgumentException("FormulaMutator.mutateLiteral can only mutate literals: " + formula);
		Random rand = new Random(System.currentTimeMillis());
		//0 -> FALSE, 1 -> TRUE, 2 -> negate, 3 -> replaceByOtherProp
		int random = rand.nextInt(3);
		if (formula instanceof BooleanConstant || variables.size() > 1)
			random =rand.nextInt(4); 
		switch (random) {
			case 0: return BooleanConstant.FALSE;
			case 1: return BooleanConstant.TRUE;
			case 2: return formula.not();
			case 3: { 
					int index = rand.nextInt(variables.size());
					while (index == ((Literal)formula).getAtom())
						index = rand.nextInt(variables.size());
					return Literal.of(index);
				}
		}
		return null;
	}
	
	
	public static Formula mutateUnaryFormula (Formula formula) {
		if (!(formula instanceof FOperator || formula instanceof GOperator || formula instanceof XOperator))
			throw new IllegalArgumentException("FormulaMutator.mutateUnaryFormula can only mutate unary formulas: " + formula);
		Random rand = new Random(System.currentTimeMillis());
		int random = rand.nextInt(3); 
		//0 -> removeOP; 1 -> negateFormula; 2 -> changeOp
		switch (random) {
			case 0: return formula.children().iterator().next();
			case 1: return formula.not();
			case 2: {
					int op = rand.nextInt(3);
					while ((formula instanceof FOperator && op == 0)  || (formula instanceof GOperator && op == 1) || (formula instanceof XOperator && op == 2))
						op = rand.nextInt(3);
					Formula child = formula.children().iterator().next();
					if (op == 0)
						return FOperator.of(child);
					else if (op == 1)
						return GOperator.of(child);
					else
						return XOperator.of(child);
			}
		}
		return null;
	}
	
	public static Formula mutateBinaryFormula (Formula formula) {
		if (!(formula instanceof Biconditional || formula instanceof UOperator || formula instanceof WOperator))
			throw new IllegalArgumentException("FormulaMutator.mutateBinaryFormula can only mutate binary formulas: " + formula);
		Random rand = new Random(System.currentTimeMillis());
		int random = rand.nextInt(3); 
		//0 -> removeOP; 1 -> negateFormula; 2 -> changeOp
		switch (random) {
		case 0: {
			Iterator<Formula> it = formula.children().iterator();
			Formula left = it.next();
			Formula right = it.next();
			if (rand.nextInt(2) == 0)
				return left;
			else
				return right;
			}
		case 1: return formula.not();
		case 2: {
				int op = rand.nextInt(3);
				while ((formula instanceof Biconditional && op == 0) || (formula instanceof UOperator && op == 1)  || (formula instanceof WOperator && op == 2))
					op = rand.nextInt(3);
				Iterator<Formula> it = formula.children().iterator();
				Formula left = it.next();
				Formula right = it.next();
				if (op == 0)
					return Biconditional.of(left, right);
				else if (op == 1)
					return UOperator.of(left, right);
				else
					return WOperator.of(left, right);
			}
		}
		return null;
	}
	
	public static Formula mutatePropositionalFormula (Formula formula) {
		if (!(formula instanceof Conjunction || formula instanceof Disjunction))
			throw new IllegalArgumentException("FormulaMutator.mutateBinaryFormula can only mutate binary formulas: " + formula);
		Random rand = new Random(System.currentTimeMillis());
		int random = rand.nextInt(3); 
		//0 -> removeOP; 1 -> negateFormula; 2 -> changeOp
		switch (random) {
		case 0: { //return one of the conjuncts/disjuncts
			Iterator<Formula> it = formula.children().iterator();
			Formula child = it.next();
			while (rand.nextBoolean() && it.hasNext()) {
				child = it.next();
			}
			return child;
			}
			
		case 1: return formula.not();
		case 2: {
				if (formula instanceof Conjunction)
					return Disjunction.of(formula.children());
				else 
					return Conjunction.of(formula.children());
			}
		}
		return null;
	}
}
