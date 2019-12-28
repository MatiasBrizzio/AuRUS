package owl.ltl.visitors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import main.Settings;
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
import tlsf.Formula_Utils;

public class FormulaWeakening implements Visitor<Formula>{
	private final List<Literal> literalCache;
	  private final List<String> variables;
	  private final boolean fixedVariables;
	  private int weakening_rate;
	  private int numOfAllowedWeakenings = 0;
	  
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
		if (numOfAllowedWeakenings > 0) { 	
	    	boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(weakening_rate) == 0);
	    	if (mutate) {
	    	   	numOfAllowedWeakenings--;
	    	   	current = BooleanConstant.TRUE;
	    	}
	    }
	    
	    return current;	
	}

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
	    		}
	    		else {
	    			// weak(a) = F(a)
	    			current = FOperator.of(current);
	    		}
	    	}
		}
		return current;
	}

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
	    		}
	    		else if (option == 2 && operand instanceof FOperator) {
	    			// weak (X F(a)) = F(a)
	    			current = operand;
	    		}
	    	}
		}
		return current;
	}

	@Override
	public Formula visit(FOperator fOperator) {
		Formula operand = fOperator.operand.accept(this);
		Formula current = FOperator.of(operand);
		int numOfTO = Formula_Utils.numOfTemporalOperators(current);
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
	    		}
	    		else if (option == 2 && operand instanceof GOperator) {
	    			// weak (F G (a)) = G F (a)
	    			current = GOperator.of(FOperator.of(operand.children().iterator().next()));
	    		}
	    		else if (option == 3 && operand instanceof XOperator) {
	    			// weak (F X (a)) = F (a)
	    			current = FOperator.of(operand.children().iterator().next());
	    		}
	    		else if (option == 4 && operand instanceof GOperator) {
	    			// weak (F G (a)) = F (a)
	    			current = FOperator.of(operand.children().iterator().next());
	    		}
	    		else {
	    			// weak F a = a1 W a
	    			//Formula new_literal = createVariable(variables.get(Settings.RANDOM_GENERATOR.nextInt(variables.size())));
	    			//if (Settings.RANDOM_GENERATOR.nextBoolean())
	    				//new_literal = new_literal.not();
	    			//System.out.println("Weak");
	    			current = WOperator.of(new_literal(current), operand);
	    		}
	    	}
		}
		return current;
	}

	

	@Override
	public Formula visit(GOperator gOperator) {
		Formula operand = gOperator.operand.accept(this);
		Formula current = GOperator.of(operand);
		int numOfTO = Formula_Utils.numOfTemporalOperators(current);
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
	    		}
	    		else if (option == 2) {
	    			// weak (G(a)) = F(a)
	    			current = FOperator.of(operand);
	    		}
	    		else if (option == 3) { 
	    			// weak (G(a)) = X(a)
	    			current = XOperator.of(operand);
	    		}
	    		else if (option == 4 && numOfTO < 2) { 
	    			// weak (G(a)) = GF(a)
	    			current = GOperator.of(FOperator.of(operand));
	    		}
	    		else if (option == 5 && numOfTO < 2) { 
	    			// weak (G(a)) = FG(a)
	    			current = FOperator.of(GOperator.of(operand));
	    		}
	    		else if (option == 6) { 
	    			// weak (G(a)) = XG(a)
	    			current = XOperator.of(GOperator.of(operand));
	    		}
	    		else if (option == 7) {
	    			// weak G(a) = a U b
	    			//Formula new_literal = createVariable(variables.get(Settings.RANDOM_GENERATOR.nextInt(variables.size())));
	    			//if (Settings.RANDOM_GENERATOR.nextBoolean())
	    				//new_literal = new_literal.not();
	    			current = UOperator.of(operand, new_literal(current));
	    		}
	    	}
		}
		return current;
	}

	@Override
	public Formula visit(Conjunction conjunction) {
		Formula current = Conjunction.of(conjunction.children.stream().map(x -> x.accept(this)));
		int numOfTO = Formula_Utils.numOfTemporalOperators(current);
		if (numOfTO > 2)
			return  conjunction;
		if (numOfAllowedWeakenings > 0) { 	
	    	boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(weakening_rate) == 0);
	    	if (mutate) {
	    		// 0: TRUE 1: remove conjunct 2:disjunction 3:F
	    		numOfAllowedWeakenings--;
	    		int option = Settings.RANDOM_GENERATOR.nextInt(4);
	    		if (option == 0)
	    			current = BooleanConstant.TRUE;
	    		else if (option == 1){// weak(a & b) = a
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
		    			current = Conjunction.of(new_set_children);
	    			}
	    		}
	    		else if (option == 2) {
	    			if (current.children().size() > 0)
	    				current = Disjunction.of(current.children()); // weak(a & b) = a | b
	    		}
	    		else if (numOfTO < 2){
	    			current = FOperator.of(current); // weak(a & b) = F(a & b)
	    		}
	    	}
	    }
	    return current;
	}

	@Override
	public Formula visit(Disjunction disjunction) {
		Formula current = Disjunction.of(disjunction.children.stream().map(x -> x.accept(this)));
		int numOfTO = Formula_Utils.numOfTemporalOperators(current);
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
	    		else if (option ==1){
	    			//Formula new_literal = createVariable(variables.get(Settings.RANDOM_GENERATOR.nextInt(variables.size())));
	    			//if (Settings.RANDOM_GENERATOR.nextBoolean())
	    				//new_literal = new_literal.not();
	    			current = Disjunction.of(current, new_literal(current)); 
	    		}
	    		else if (numOfTO < 2){
	    			current = FOperator.of(current); // weak(a | b) = F(a | b)
	    		}
	    	}
	    }
	    
	    return current;	
	}

	@Override
	public Formula visit(UOperator uOperator) {
		Formula left = uOperator.left.accept(this);
		Formula right = uOperator.right.accept(this);
		Formula current = UOperator.of(left, right);
		int numOfTO = Formula_Utils.numOfTemporalOperators(current);
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
    	    	else if (option == 2) {	// weak (a U b) = (a || c) U b
	    			//Formula new_literal = createVariable(variables.get(Settings.RANDOM_GENERATOR.nextInt(variables.size())));
	    			//if (Settings.RANDOM_GENERATOR.nextBoolean())
	    				//new_literal = new_literal.not();
    	    		current = UOperator.of(Disjunction.of(new_literal(current), left), right);
    	    		System.out.println("////"+current);
    	    	}
    	    	else
    	    		current = FOperator.of(right); // weak(a U b) = F(b)
	    	}
		}
		return current;
	}

	@Override
	public Formula visit(WOperator wOperator) {
		Formula left = wOperator.left.accept(this);
		Formula right = wOperator.right.accept(this);
		Formula current = WOperator.of(left, right);
		int numOfTO = Formula_Utils.numOfTemporalOperators(current);
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
    	    		current = Disjunction.of(FOperator.of(left),UOperator.of(left, right)); // weak(a W b) = F(a) || a U b
    	    	else if (option == 2)
    	    		current = Disjunction.of(GOperator.of(left), FOperator.of(right)); // weak(a W b) = G(a) || F(b)
    	    	else {  // weak (a W b) = ((a || c) W b)
	    			//Formula new_literal = createVariable(variables.get(Settings.RANDOM_GENERATOR.nextInt(variables.size())));
	    			//if (Settings.RANDOM_GENERATOR.nextBoolean())
	    				//new_literal = new_literal.not();
    	    		current = WOperator.of(Disjunction.of(left, new_literal(current)), right);
//    	    		current =  current.accept(this);
    	    		System.out.println("------"+current);
    	    	}
	    	}
		}
		return current;
	}

	@Override
	public Formula visit(MOperator mOperator) {
		Formula left = mOperator.left.accept(this);
		Formula right = mOperator.right.accept(this);
		Formula current = MOperator.of(left, right);
		int numOfTO = Formula_Utils.numOfTemporalOperators(current);
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
		    		current = WOperator.of(right, Conjunction.of(left,right)); // weak(b U (a & b)) = b W (a & b)
		    	else
		    		current = FOperator.of(Conjunction.of(left,right)); // weak(b U (a & b)) = F(a & b)
	    	}
		}
		return current;
	}

	@Override
	public Formula visit(ROperator rOperator) {
		Formula left = rOperator.left.accept(this);
		Formula right = rOperator.right.accept(this);
		Formula current = ROperator.of(left, right);
		int numOfTO = Formula_Utils.numOfTemporalOperators(current);
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
		    		current = Disjunction.of(FOperator.of(right),UOperator.of(right, Conjunction.of(left,right))); 
		    	else
		    		// weak(b W (a & b)) = G(b) || F (a & b)
		    		current = Disjunction.of(GOperator.of(right), FOperator.of(Conjunction.of(left,right))); 
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
	
	public Literal new_literal (Formula current) {
		Set<Literal> props = current.accept(new PropositionVariablesExtractor());
		int max = variables.size();
//		if (numOfInputs > 0)
//			max = numOfInputs;
		int new_variable = Settings.RANDOM_GENERATOR.nextInt(max);
		Literal new_literal = createVariable(variables.get(new_variable));
		
		if (Settings.RANDOM_GENERATOR.nextBoolean()) {
			if (Settings.RANDOM_GENERATOR.nextBoolean())
				new_literal = new_literal.not();
			return new_literal;
		}
		int trying = 0;
		while ((props.contains(new_literal) || props.contains(new_literal.not()) && trying < 5)) {
			trying++;
			new_variable = Settings.RANDOM_GENERATOR.nextInt(max);
			new_literal = createVariable(variables.get(new_variable));
		}

		if (Settings.RANDOM_GENERATOR.nextBoolean())
			new_literal = new_literal.not();
		return new_literal;
	}

}
