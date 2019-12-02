package owl.ltl.visitors;

import main.Settings;
import owl.ltl.*;
import tlsf.Formula_Utils;

import java.util.*;

public class GeneralFormulaMutator implements Visitor<Formula>{
	private final List<Literal> literalCache;
	  private final List<String> variables;
//	  private final int numOfInputs;
	  private final boolean fixedVariables;
	  private int mutation_rate;
	  public int numOfAllowedMutations = 0;

	  private boolean print_debug_info = false;

	public GeneralFormulaMutator(List<String> literals, int mutation_rate, int max_num_of_mutations_to_apply) {
		ListIterator<String> literalIterator = literals.listIterator();
		List<Literal> literalList = new ArrayList<>();
		List<String> variableList = new ArrayList<>();
//		numOfInputs = 0;
		while (literalIterator.hasNext()) {
			int index = literalIterator.nextIndex();
			String name = literalIterator.next();
			literalList.add(Literal.of(index));
			variableList.add(name);
		}

		literalCache = List.copyOf(literalList);
		variables = List.copyOf(variableList);
		fixedVariables = true;
		this.mutation_rate = mutation_rate;
		this.numOfAllowedMutations = max_num_of_mutations_to_apply;
	}

//	  public GeneralFormulaMutator(List<String> literals, int num_of_inputs, int mutation_rate, int max_num_of_mutations_to_apply) {
//			ListIterator<String> literalIterator = literals.listIterator();
//		    List<Literal> literalList = new ArrayList<>();
//		    List<String> variableList = new ArrayList<>();
//			numOfInputs = num_of_inputs;
//		    while (literalIterator.hasNext()) {
//		      int index = literalIterator.nextIndex();
//		      String name = literalIterator.next();
//		      literalList.add(Literal.of(index));
//		      variableList.add(name);
//		    }
//
//		    literalCache = List.copyOf(literalList);
//		    variables = List.copyOf(variableList);
//		    fixedVariables = true;
//		    this.mutation_rate = mutation_rate;
//		    this.numOfAllowedMutations = max_num_of_mutations_to_apply;
//	}
	
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
		if (numOfAllowedMutations > 0) { 	
	    	boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(mutation_rate) == 0);
	    	if (mutate) {
	    		this.numOfAllowedMutations --;
	    		int random = Settings.RANDOM_GENERATOR.nextInt(2); 
	    		//0: negateFormula 1:new literal
	    		if (print_debug_info) System.out.print("before: " + booleanConstant + " random: " + random);
	    		if (random == 0)
	    				current = current.not();
	    		else {
	    			current = new_literal(current);
	    		}
	    		if (print_debug_info) System.out.println(" after: " + current);
	    	}
	    }
	    
	    return current;	
	}
	
	@Override
	public Formula visit(Literal literal) {
		Formula current = literal;		    
		if (numOfAllowedMutations > 0) {
	    	boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(mutation_rate) == 0);
	    	if (mutate) {
	    		this.numOfAllowedMutations --;
	    		int random = Settings.RANDOM_GENERATOR.nextInt(4);
	    		//0: TRUE 1:FALSE 2:new literal 3: new unary op
	    		if (print_debug_info) System.out.print("before: " + literal + " random: " + random);
	    		if (random == 0)
	    				current =BooleanConstant.TRUE;
	    		else if (random == 1)
    				current =BooleanConstant.FALSE;
	    		else if (random == 2) {
					current = new_literal(current);
	    		}
	    		else {
	    			//0:not 1:X 2:F 3:G
					int op = Settings.RANDOM_GENERATOR.nextInt(4);
					if (op == 0)
						current = literal.not();
					else if (op == 1)
						current = XOperator.of(literal);
					else if (op == 2)
						current = FOperator.of(literal);
					else if (op == 3)
						current = GOperator.of(literal);
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
		int numOfTO = Formula_Utils.numOfTemporalOperators(current);
		if (numOfTO > 2)
			return xOperator;
		if (numOfAllowedMutations > 0) {
	    	boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(mutation_rate) == 0);
	    	if (mutate) {
	    		this.numOfAllowedMutations --;
	    		int random = Settings.RANDOM_GENERATOR.nextInt(3);
	    		//0: removeOP 1:changeOp 2:addOp
	    		if (print_debug_info) System.out.print("before: " + xOperator + " random: " + random);
	    		if (random == 0)
	    			current = operand;
	    		else if (random == 1) {
	    			//0:not 1:F 2:G
    				int op = Settings.RANDOM_GENERATOR.nextInt(3);
    				if (op == 0)
    					current = operand.not();
    				else if (op == 1 && numOfTO < 2)
    					current = FOperator.of(operand);
    				else if (numOfTO < 2)
    					current = GOperator.of(operand);
	    		}
				else if (random == 2) {
					//0:not 1:F 2:G
					int op = Settings.RANDOM_GENERATOR.nextInt(3);
					if (op == 0)
						current = current.not();
					else if (op == 1 && numOfTO < 2)
						current = FOperator.of(current);
					else if (numOfTO < 2)
						current = GOperator.of(current);
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
		int numOfTO = Formula_Utils.numOfTemporalOperators(current);
		if (numOfTO > 2)
			return fOperator;
		if (numOfAllowedMutations > 0) {
	    	boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(mutation_rate) == 0);
	    	if (mutate) {
	    		this.numOfAllowedMutations --;
	    		int random = Settings.RANDOM_GENERATOR.nextInt(3); 
	    		//0: removeOP 1:changeOp 2:addOp
	    		if (print_debug_info) System.out.print("before: " + fOperator + " random: " + random);
	    		if (random == 0)
	    			current = operand;
				else if (random == 1) {
					//0:not 1:X 2:G
					int op = Settings.RANDOM_GENERATOR.nextInt(3);
					if (op == 0)
						current = operand.not();
					else if (op == 1)
						current = XOperator.of(operand);
					else
						current = GOperator.of(operand);
				}
				else if (random == 2) {
					//0:not 1:X 2:G
					int op = Settings.RANDOM_GENERATOR.nextInt(3);
					if (op == 0)
						current = current.not();
					else if (op == 1)
						current = XOperator.of(current);
					else if (numOfTO < 2)
						current = GOperator.of(current);
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
		int numOfTO = Formula_Utils.numOfTemporalOperators(current);
		if (numOfTO > 2)
			return gOperator;
		if (numOfAllowedMutations > 0) {
	    	boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(mutation_rate) == 0);
	    	if (mutate) {
	    		this.numOfAllowedMutations --;
	    		int random = Settings.RANDOM_GENERATOR.nextInt(3); 
	    		//0: removeOP 1:changeOp 2:addOp
	    		if (print_debug_info) System.out.print("before: " + gOperator + " random: " + random);
	    		if (random == 0)
	    			current = operand;
				else if (random == 1) {
					//0:not 1:F 2:X
					int op = Settings.RANDOM_GENERATOR.nextInt(3);
					if (op == 0)
						current = operand.not();
					else if (op == 1)
						current = FOperator.of(operand);
					else
						current = XOperator.of(operand);
				}
				else if (random == 2) {
					//0:not 1:F 2:X
					int op = Settings.RANDOM_GENERATOR.nextInt(3);
					if (op == 0)
						current = current.not();
					else if (op == 1 && numOfTO < 2)
						current = FOperator.of(current);
					else //if (numOfTO < 2)
						current = XOperator.of(current);
				}
	    		if (print_debug_info) System.out.println(" after: " + current);
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
		if (numOfAllowedMutations > 0) { 	
	    	boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(mutation_rate) == 0);
	    	if (mutate) {
	    		this.numOfAllowedMutations --;
	    		int random = Settings.RANDOM_GENERATOR.nextInt(4);
	    		//0 -> addOp; 1 -> remove conjunct; 2 -> add disjunct; 3 -> changeOp;
	    		if (print_debug_info) System.out.print("before: " + conjunction + " random: " + random);
	    		if (random == 0) {
					//0:not 1:X 2:F 3:G
					int op = Settings.RANDOM_GENERATOR.nextInt(4);
					if (op == 0)
						current = current.not();
					else if (op == 1)
						current = XOperator.of(current);
					else if (op == 2 && numOfTO < 2)
						current = FOperator.of(current);
					else if (numOfTO < 2)
						current = GOperator.of(current);
	    		}
	    		else if (random == 1) { 
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
//	    		else if (random == 2) {
//					Literal new_literal =new_literal(current);
//	    			current = Conjunction.of(current, new_literal);
//	    		}
	    		else if (random == 2 && current.children().size() > 0){ // random == 3
					//0:| 1:U 2:W 3:R 4:M
    				int op = Settings.RANDOM_GENERATOR.nextInt(5);
    				if (op == 0)
    					current = Disjunction.of(current.children());
    				else if (numOfTO < 2){
    					Iterator<Formula> it = current.children().iterator();
    	    			Formula left = it.next();
    	    			List<Formula> rightChild = new LinkedList<Formula>();

    	    			while (it.hasNext()) {
    	    				Formula c = it.next();
    	    				if (Settings.RANDOM_GENERATOR.nextBoolean()) {
    	    					rightChild.add(left);
    	    					left = c;
    	    				}
    	    				else
    	    					rightChild.add(c);
    	    			}
    	    			Formula right = Conjunction.of(rightChild);
    	    			
    					if (op == 1)
							current = UOperator.of(left, right);
	    				else if (op == 2)
	    					current = WOperator.of(left, right);
	    				else if (op == 3)
	    					current = ROperator.of(left, right);
	    				else //op == 4
	    					current = MOperator.of(left, right);
    				}
					else {
						Literal new_literal =new_literal(current);
						current = Conjunction.of(current, new_literal);
					}
	    		}
	    		if (print_debug_info) System.out.println(" after: " + current);
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
		if (numOfAllowedMutations > 0) { 	
	    	boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(mutation_rate) == 0);
	    	if (mutate) {
	    		this.numOfAllowedMutations --;
	    		int random = Settings.RANDOM_GENERATOR.nextInt(4);
	    		//0 -> addOp; 1 -> remove disjunct; 2 -> add disjunct; 3 -> changeOp
				if (print_debug_info) System.out.print("before: " + disjunction + " random: " + random);
	    		if (random == 0) {
					//0:not 1:X 2:F 3:G
					int op = Settings.RANDOM_GENERATOR.nextInt(4);
					if (op == 0)
						current = current.not();
					else if (op == 1)
						current = XOperator.of(current);
					else if (op == 2 && numOfTO < 2)
						current = FOperator.of(current);
					else if (numOfTO < 2)
						current = GOperator.of(current);
	    		}
	    		else if (random == 1) { 
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
//	    		else if(random == 2) {
//					Literal new_literal = new_literal(current);
//	    			current = Disjunction.of(current, new_literal);
//	    		}
	    		else if (random == 2 && current.children().size() > 0) { // random == 3
					//0:& 1:U 2:W 3:R 4:M
    				int op = Settings.RANDOM_GENERATOR.nextInt(5);
    				if (op == 0) {		
    						current = Conjunction.of(current.children());
    				}
    				else if (numOfTO < 2) {
    					Iterator<Formula> it = current.children().iterator();
    	    			Formula left = it.next();
    	    			List<Formula> rightChild = new LinkedList<Formula>();

    	    			while (it.hasNext()) {
    	    				Formula c = it.next();
    	    				if (Settings.RANDOM_GENERATOR.nextBoolean()) {
    	    					rightChild.add(left);
    	    					left = c;
    	    				}
    	    				else
    	    					rightChild.add(c);
    	    			}
    	    			Formula right = Disjunction.of(rightChild);
    	    			
    					if (op == 1)
							current = UOperator.of(left, right);
	    				else if (op == 2)
	    					current = WOperator.of(left, right);
	    				else if (op == 3)
	    					current = ROperator.of(left, right);
	    				else //op == 4
	    					current = MOperator.of(left, right);
    				}
	    		}
				else { //random == 3
					Literal new_literal = new_literal(current);
					current = Disjunction.of(current, new_literal);
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
		int numOfTO = Formula_Utils.numOfTemporalOperators(current);
		if (numOfTO > 2)
			return uOperator;
	    if (numOfAllowedMutations > 0) { 	
	    	boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(mutation_rate) == 0);
	    	if (mutate) {
	    		this.numOfAllowedMutations --;
	    		int random = Settings.RANDOM_GENERATOR.nextInt(3); 
	    		//0 -> removeOP; 1 -> addOp; 2 -> changeOp
	    		if (print_debug_info) System.out.print("before: " + uOperator + " random: " + random);
	    		if (random == 0) {
	    			if (Settings.RANDOM_GENERATOR.nextBoolean())
	    				current = left;
	    			else
	    				current = right;
	    		}
	    		else if (random == 1) {
					//0:not 1:X 2:F 3:G
					int op = Settings.RANDOM_GENERATOR.nextInt(4);
					if (op == 0)
						current = current.not();
					else if (op == 1 && numOfTO < 2)
						current = XOperator.of(current);
					else if (op == 2 && numOfTO < 2)
						current = FOperator.of(current);
					else if (numOfTO < 2)
						current = GOperator.of(current);
	    		}
	    		else { // random == 2
	    			//0:& 1:| 2:W 3:R 4:M
    				int op = Settings.RANDOM_GENERATOR.nextInt(5);
    				if (op == 0)
    					current = Conjunction.of(left, right);
    				else if (op == 1)
						current = Disjunction.of(left, right);
    				else if (op == 2)
    					current = WOperator.of(left, right);
    				else if (op == 3)
    					current = ROperator.of(left, right);
    				else //op == 4
    					current = MOperator.of(left, right);
	    		}
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
		int numOfTO = Formula_Utils.numOfTemporalOperators(current);
		if (numOfTO > 2)
			return wOperator;
	    if (numOfAllowedMutations > 0) {
	    	boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(mutation_rate) == 0);
	    	if (mutate) {
	    		this.numOfAllowedMutations --;
	    		int random = Settings.RANDOM_GENERATOR.nextInt(3); 
	    		//0 -> removeOP; 1 -> addOp; 2 -> changeOp
	    		if (print_debug_info) System.out.print("before: " + wOperator + " random: " + random);
	    		if (random == 0) {
	    			if (Settings.RANDOM_GENERATOR.nextInt(2) == 0)
	    				current = left;
	    			else
	    				current = right;
	    		}
	    		else if (random == 1) {
					//0:not 1:X 2:F 3:G
					int op = Settings.RANDOM_GENERATOR.nextInt(4);
					if (op == 0)
						current = current.not();
					else if (op == 1)
						current = XOperator.of(current);
					else if (op == 2 && numOfTO < 2)
						current = FOperator.of(current);
					else if (numOfTO < 2)
						current = GOperator.of(current);
	    		}
	    		else { // random == 2
	    			//0:& 1:| 2:U 3:R 4:M
    				int op = Settings.RANDOM_GENERATOR.nextInt(5);
    				if (op == 0)
    					current = Conjunction.of(left, right);
    				else if (op == 1)
						current = Disjunction.of(left, right);
    				else if (op == 2)
    					current = UOperator.of(left, right);
    				else if (op == 3)
    					current = ROperator.of(left, right);
    				else //op == 4
    					current = MOperator.of(left, right);
	    		}
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
		int numOfTO = Formula_Utils.numOfTemporalOperators(current);
		if (numOfTO > 2)
			return mOperator;
	    if (numOfAllowedMutations > 0) {
	    	boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(mutation_rate) == 0);
	    	if (mutate) {
	    		this.numOfAllowedMutations --;
	    		int random = Settings.RANDOM_GENERATOR.nextInt(3); 
	    		//0 -> removeOP; 1 -> addOp; 2 -> changeOp
	    		if (print_debug_info) System.out.print("before: " + mOperator + " random: " + random);
	    		if (random == 0) {
	    			if (Settings.RANDOM_GENERATOR.nextInt(2) == 0)
	    				current = left;
	    			else
	    				current = right;
	    		}
	    		else if (random == 1) {
					//0:not 1:X 2:F 3:G
					int op = Settings.RANDOM_GENERATOR.nextInt(4);
					if (op == 0)
						current = current.not();
					else if (op == 1)
						current = XOperator.of(current);
					else if (op == 2 && numOfTO < 2)
						current = FOperator.of(current);
					else if (numOfTO < 2)
						current = GOperator.of(current);
	    		}
	    		else { // random == 2
	    			//0:& 1:| 2:U 3:R 4:W
    				int op = Settings.RANDOM_GENERATOR.nextInt(5);
    				if (op == 0)
    					current = Conjunction.of(left, right);
    				else if (op == 1)
						current = Disjunction.of(left, right);
    				else if (op == 2)
    					current = UOperator.of(left, right);
    				else if (op == 3)
    					current = ROperator.of(left, right);
    				else //op == 4
    					current = WOperator.of(left, right);
	    		}
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
		int numOfTO = Formula_Utils.numOfTemporalOperators(current);
		if (numOfTO > 2)
			return rOperator;
	    if (numOfAllowedMutations > 0) {
	    	boolean mutate = (Settings.RANDOM_GENERATOR.nextInt(mutation_rate) == 0);
	    	if (mutate) {
	    		this.numOfAllowedMutations --;
	    		int random = Settings.RANDOM_GENERATOR.nextInt(3); 
	    		//0 -> removeOP; 1 -> addOp; 2 -> changeOp
	    		if (print_debug_info) System.out.print("before: " + rOperator + " random: " + random);
	    		if (random == 0) {
	    			if (Settings.RANDOM_GENERATOR.nextInt(2) == 0)
	    				current = left;
	    			else
	    				current = right;
	    		}
	    		else if (random == 1) {
					//0:not 1:X 2:F 3:G
					int op = Settings.RANDOM_GENERATOR.nextInt(4);
					if (op == 0)
						current = current.not();
					else if (op == 1)
						current = XOperator.of(current);
					else if (op == 2 && numOfTO < 2)
						current = FOperator.of(current);
					else if (numOfTO < 2)
						current = GOperator.of(current);
	    		}
	    		else { // random == 2
	    			//0:& 1:| 2:U 3:W 4:M
    				int op = Settings.RANDOM_GENERATOR.nextInt(5);
    				if (op == 0)
    					current = Conjunction.of(left, right);
    				else if (op == 1)
						current = Disjunction.of(left, right);
    				else if (op == 2)
    					current = UOperator.of(left, right);
    				else if (op == 3)
    					current = WOperator.of(left, right);
    				else //op == 4
    					current = MOperator.of(left, right);
	    		}
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
		int trying = 0;
		while (props.contains(new_literal) && trying < 5) {
			trying++;
			new_variable = Settings.RANDOM_GENERATOR.nextInt(max);
			new_literal = createVariable(variables.get(new_variable));
		}

		if (Settings.RANDOM_GENERATOR.nextBoolean())
			new_literal = new_literal.not();
		return new_literal;
	}
}
