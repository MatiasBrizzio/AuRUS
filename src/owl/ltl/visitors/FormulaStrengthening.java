package owl.ltl.visitors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import geneticalgorithm.Settings;
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
import owl.ltl.rewriter.NormalForms;

public class FormulaStrengthening implements Visitor<Formula>{
	private final List<Literal> literalCache;
	  private final List<String> variables;
	  private final boolean fixedVariables;
	  private int strengthening_rate;
	  private int numOfAllowedStrengthenings = 0;
	  private boolean print_debug_info = true;
	  
	public FormulaStrengthening(List<String> literals, int strengthening_rate, int num_of_strengthening_to_appply) {
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
	    this.numOfAllowedStrengthenings = num_of_strengthening_to_appply;
		
	}
	
	public List<String> variables() {
	    return List.copyOf(variables);
	  }
	
	@Override
	public Formula apply(Formula formula) {
		return formula.accept(this);
	}



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
	    			Formula new_literal = createVariable(variables.get(Settings.RANDOM_GENERATOR.nextInt(variables.size())));
	    			if (Settings.RANDOM_GENERATOR.nextBoolean())
	    				new_literal = new_literal.not();
	    			current = Conjunction.of(current, new_literal); 
	    		}
	    		else {
	    			// strength(a) = G(a)
	    			current = GOperator.of(current);
	    		}
	    		if (print_debug_info) System.out.println(" after: " + current);
	    	}
		}
		return current;
	}

	@Override
	public Formula visit(XOperator xOperator) {
		Formula operand = xOperator.operand.accept(this);
		Formula current = XOperator.of(operand);
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
	    		}
	    		else if (option == 2 && operand instanceof GOperator) {
	    			// strength(X G a) = G(a)
	    			current = operand;
	    		}
	    		if (print_debug_info) System.out.println(" after: " + current);
	    	}
		}
		return current;
	}
	
	
	@Override
	public Formula visit(FOperator fOperator) {
		Formula operand = fOperator.operand.accept(this);
		Formula current = FOperator.of(operand);
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
	    		}
	    		else if (option == 3) {
	    			// strength (F(a)) = G (a)
	    			current = GOperator.of(operand);
	    		}
	    		else if (option == 4) {
	    			// strength (F(a)) = F X(a)
	    			current = FOperator.of(XOperator.of(operand));
	    		}
	    		else if (option == 5) {
	    			// strength (F(a)) = F G(a)
	    			current = FOperator.of(GOperator.of(operand));
	    		}
	    		else if (option == 6) {
	    			// strength (F(a)) = G F(a)
	    			current = GOperator.of(FOperator.of(operand));
	    		}
	    		else if (option == 7) {
	    			// strength (F(a)) = b U a
	    			Formula new_literal = createVariable(variables.get(Settings.RANDOM_GENERATOR.nextInt(variables.size())));
	    			if (Settings.RANDOM_GENERATOR.nextBoolean())
	    				new_literal = new_literal.not();
	    			current = UOperator.of(new_literal, operand);
	    		}
    			if (print_debug_info) System.out.println(" after: " + current);
	    	}
		}
		return current;
	}

	

	@Override
	public Formula visit(GOperator gOperator) {
		Formula operand = gOperator.operand.accept(this);
		Formula current = GOperator.of(operand);
		if (numOfAllowedStrengthenings > 0) {
	    	boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(strengthening_rate) == 0);
	    	if (mutate) {
	    		numOfAllowedStrengthenings--;		
	    		// 0:FLASE 1:distribute to disjunction 2:infinitely often to persistence 3:remove X 4:remove F
    			int option = Settings.RANDOM_GENERATOR.nextInt(5);
    			if (print_debug_info) System.out.print("before: " + gOperator + " random: " + option);
    			current = BooleanConstant.FALSE; // (option == 0) and default
	    		if (option == 1 && operand instanceof Disjunction) {
	    			// strengthen (G (a | b)) = G(a) | G(b)
	    			for (Set<Formula> c : NormalForms.toDnf(operand)) {
	    				Formula clause = Conjunction.of(c);
	    				current = Disjunction.of(current, GOperator.of(clause));
	    			}
	    		}
	    		else if (option == 2 && operand instanceof FOperator) {
	    			// strengthen (G F (a)) = F G (a)
	    			current = FOperator.of(GOperator.of(operand.children().iterator().next()));
	    		}
	    		else if (option == 3 && operand instanceof XOperator) {
	    			// strengthen (G X (a)) = G (a)
	    			current = GOperator.of(operand.children().iterator().next());
	    		}
	    		else if (option == 4 && operand instanceof GOperator) {
	    			// weak (G F (a)) = G (a)
	    			current = GOperator.of(operand.children().iterator().next());
	    		}
	    		if (print_debug_info) System.out.println(" after: " + current);
	    	}
		}
		return current;
	}

	

	@Override
	public Formula visit(Conjunction conjunction) {
		Formula current = Conjunction.of(conjunction.children.stream().map(x -> x.accept(this)));
		if (numOfAllowedStrengthenings > 0) { 	
	    	boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(strengthening_rate) == 0);
	    	if (mutate) {
	    		// 0: FALSE 1:add conjunct 2:G
	    		numOfAllowedStrengthenings--;
	    		int option = Settings.RANDOM_GENERATOR.nextInt(3);
	    		if (print_debug_info) System.out.print("before: " + conjunction + " random: " + option);
	    		if (option == 0)
	    			current = BooleanConstant.FALSE;
	    		else if (option == 1){
	    			Formula new_literal = createVariable(variables.get(Settings.RANDOM_GENERATOR.nextInt(variables.size())));
	    			if (Settings.RANDOM_GENERATOR.nextBoolean())
	    				new_literal = new_literal.not();
	    			current = Conjunction.of(current, new_literal); // strengthen(a & b) = a & b & c
	    		}
	    		else {
	    			current = GOperator.of(current); // strengthen(a & b) = G(a & b)
	    		}
	    		if (print_debug_info) System.out.println(" after: " + current);
	    	}
	    }
	    return current;
	}

	@Override
	public Formula visit(Disjunction disjunction) {
		Formula current = Disjunction.of(disjunction.children.stream().map(x -> x.accept(this)));
		if (numOfAllowedStrengthenings > 0) {
	    	boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(strengthening_rate) == 0);
	    	if (mutate) {
	    		// 0: FALSE 1:conjunct 2: remove disjunct 3:G
	    		numOfAllowedStrengthenings--;
	    		int option = Settings.RANDOM_GENERATOR.nextInt(4);
	    		if (print_debug_info) System.out.print("before: " + disjunction + " random: " + option);
	    		if (option == 0) 
	    			current = BooleanConstant.FALSE;
	    		else if (option == 1){
	    			current = Conjunction.of(current.children()); // strengthen(a | b) = a & b
	    		}
	    		else if (option == 2) {
	    			if (current.children().size() > 0) {
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
	    		}
	    		else {
	    			current = GOperator.of(current); // strengthen(a | b) = G(a | b)
	    		}
	    		if (print_debug_info) System.out.println(" after: " + current);
	    	}
	    }
	    return current;	
	}
	
	
	@Override
	public Formula visit(UOperator uOperator) {
		Formula left = uOperator.left.accept(this);
		Formula right = uOperator.right.accept(this);
		Formula current = UOperator.of(left, right);
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
    	    	else
    	    		current = Conjunction.of(left, right.not(), XOperator.of(current)); // strengthen(a U b) = a & !b & X(a U b)
    	    	if (print_debug_info) System.out.println(" after: " + current);
	    	}
		}
		return current;
	}

	@Override
	public Formula visit(WOperator wOperator) {
		Formula left = wOperator.left.accept(this);
		Formula right = wOperator.right.accept(this);
		Formula current = WOperator.of(left, right);
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

	
	
	@Override
	public Formula visit(MOperator mOperator) {
		Formula left = mOperator.left.accept(this);
		Formula right = mOperator.right.accept(this);
		Formula current = MOperator.of(left, right);
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
    	    		current = Conjunction.of(left,right); // strengthen(a M b) = (a & b)
    	    	else
    	    		current = Conjunction.of(right, Conjunction.of(left,right).not(), XOperator.of(current)); // strengthen(a M b) = b & !(a & b) & X(a M b)
    	    	if (print_debug_info) System.out.println(" after: " + current);
	    	}
		}
		return current;
	}

	@Override
	public Formula visit(ROperator rOperator) {
		Formula left = rOperator.left.accept(this);
		Formula right = rOperator.right.accept(this);
		Formula current = ROperator.of(left, right);
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
	
	@Override
	public Formula visit(Biconditional biconditional) {
		throw new UnsupportedOperationException("FormulaWeakening: formula in NNF was expected: " + biconditional);
	}

	@Override
	public Formula visit(FrequencyG freq) {
		throw new UnsupportedOperationException("FormulaWeakening: formula in NNF was expected: " + freq);
	}
	
	@Override
	public Formula visit(OOperator oOperator) {
		throw new UnsupportedOperationException("FormulaWeakening: formula in NNF was expected: " + oOperator);
	}

	@Override
	public Formula visit(HOperator hOperator) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("FormulaWeakening: formula in NNF was expected: " + hOperator);
	}

	@Override
	public Formula visit(TOperator tOperator) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("FormulaWeakening: formula in NNF was expected: " + tOperator);
	}

	@Override
	public Formula visit(SOperator sOperator) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("FormulaWeakening: formula in NNF was expected: " + sOperator);
	}

	@Override
	public Formula visit(YOperator yOperator) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("FormulaWeakening: formula in NNF was expected: " + yOperator);
	}

	@Override
	public Formula visit(ZOperator zOperator) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("FormulaWeakening: formula in NNF was expected: " + zOperator);
	}
	
	private Formula createVariable(String name) {
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

}
