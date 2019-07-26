package owl.ltl.parser;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.TerminalNode;

import geneticalgorithm.FormulaMutator;
import owl.grammar.LTLParserBaseVisitor;
import owl.grammar.LTLParser.AndExpressionContext;
import owl.grammar.LTLParser.BinaryOpContext;
import owl.grammar.LTLParser.BinaryOperationContext;
import owl.grammar.LTLParser.BoolContext;
import owl.grammar.LTLParser.BooleanContext;
import owl.grammar.LTLParser.DoubleQuotedVariableContext;
import owl.grammar.LTLParser.ExpressionContext;
import owl.grammar.LTLParser.FormulaContext;
import owl.grammar.LTLParser.FractionContext;
import owl.grammar.LTLParser.FrequencyOpContext;
import owl.grammar.LTLParser.NestedContext;
import owl.grammar.LTLParser.OrExpressionContext;
import owl.grammar.LTLParser.ProbabilityContext;
import owl.grammar.LTLParser.SingleQuotedVariableContext;
import owl.grammar.LTLParser.UnaryOpContext;
import owl.grammar.LTLParser.UnaryOperationContext;
import owl.grammar.LTLParser.VariableContext;
import owl.ltl.Biconditional;
import owl.ltl.BooleanConstant;
import owl.ltl.Conjunction;
import owl.ltl.Disjunction;
import owl.ltl.FOperator;
import owl.ltl.Formula;
import owl.ltl.FrequencyG;
import owl.ltl.GOperator;
import owl.ltl.Literal;
import owl.ltl.MOperator;
import owl.ltl.ROperator;
import owl.ltl.UOperator;
import owl.ltl.WOperator;
import owl.ltl.XOperator;
import tlsf.Formula_Utils;
import owl.ltl.FrequencyG.Limes;

public class FormulaWeakening extends LTLParserBaseVisitor<Formula> {
	  private final List<Literal> literalCache;
	  private final List<String> variables;
	  private final boolean fixedVariables;
	  private int weakening_rate;
	  private int numOfRemainingWeakenings = 0;
	 

	  public FormulaWeakening(List<String> literals, int weakening_rate, int num_of_weakening_to_appply) {
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
		    this.numOfRemainingWeakenings = num_of_weakening_to_appply;
	  }

	  public List<String> variables() {
	    return List.copyOf(variables);
	  }

	  @Override
	  public Formula visitAndExpression(AndExpressionContext ctx) {
	    assert ctx.getChildCount() > 0;
	    
	    Formula current = Conjunction.of(ctx.children.stream()
	  	      .filter(child -> !(child instanceof TerminalNode))
		      .map(this::visit));
	    
	    if (numOfRemainingWeakenings > 0) { 	
	    	Random rand = new Random(System.currentTimeMillis());
	    	boolean mutate = (rand.nextInt(weakening_rate) == 0);
	    	if (mutate) {
	    		// 0: TRUE 1: disjunction
	    		numOfRemainingWeakenings--;
	    		int option = rand.nextInt(2);
	    		if (option == 0) 
	    			current = BooleanConstant.TRUE;
	    		else {
	    			current = Disjunction.of(current.children()); // weak(a & b) = a | b
//	    			List<Formula> new_conjuncts = new LinkedList<>();
//	    			for (Formula c : current.children())
//	    				new_conjuncts.add(c);
//	    			int to_be_deleted = rand.nextInt(new_conjuncts.size());
//	    			new_conjuncts.remove(to_be_deleted);
//	    			current = Conjunction.of(new_conjuncts);
	    		}
	    	}
	    }
	    return current;
	  }

	  @Override
	  public Formula visitBinaryOperation(BinaryOperationContext ctx) {
	    assert ctx.getChildCount() == 3;
	    assert ctx.left != null && ctx.right != null;

	    BinaryOpContext binaryOp = ctx.binaryOp();
	    
	    if (binaryOp.BIIMP() != null || binaryOp.IMP() != null || binaryOp.XOR() != null) 
	    	throw new ParseCancellationException("FormulaWeakening: formula in NNF was expected: " + ctx.left + binaryOp + ctx.right);

	    Formula left = visit(ctx.left);
	    Formula right = visit(ctx.right);
	    Formula current = null;
	    
	    if (numOfRemainingWeakenings <= 0) {
		    if (binaryOp.UNTIL() != null) {
		    	current = UOperator.of(left, right);
		    }
	
		    if (binaryOp.WUNTIL() != null) {
		    	current = WOperator.of(left, right);
		    }
	
		    if (binaryOp.RELEASE() != null) {
		    	current = ROperator.of(left, right);
		    }
	
		    if (binaryOp.SRELEASE() != null) {
		    	current = MOperator.of(left, right);
		    }
	    
		    if (current == null)
		    	throw new ParseCancellationException("Unknown operator");
	    }
	    else {
	    	Random rand = new Random(System.currentTimeMillis());
	    	boolean mutate = (rand.nextInt(weakening_rate) == 0);
	    	if (mutate) {
	    		numOfRemainingWeakenings--;
	    		
	    	    if (binaryOp.UNTIL() != null) {
	    	    	// 0:TRUE 1:W 2:F
	    	    	int option = rand.nextInt(3);
	    	    	if (option == 0)
	    	    		current = BooleanConstant.TRUE;
	    	    	else if (option == 1)
	    	    		current = WOperator.of(left, right); // weak(a U b) = a W b
	    	    	else
	    	    		current = FOperator.of(right); // weak(a U b) = F(b)
	    	    }

	    	    if (binaryOp.WUNTIL() != null) {
	    	    	// a W b = G(a) || a U b.
	    	    	// we decided to weak the each disjunct.
	    	    	// 0:TRUE 1:F 2:F
	    	    	int option = rand.nextInt(3);
	    	    	if (option == 0)
	    	    		current = BooleanConstant.TRUE;
	    	    	else if (option == 1)
	    	    		current = Disjunction.of(FOperator.of(left),UOperator.of(left, right)); // weak(a W b) = F(a) || a U b
	    	    	else
	    	    		current = Disjunction.of(GOperator.of(left), FOperator.of(right)); // weak(a W b) = G(a) || F(b)
	    	    }

	    	    if (binaryOp.RELEASE() != null) {
	    	    	// a R b = b W (a & b)
	    	    	// 0:TRUE 1:F 2:F
	    	    	int option = rand.nextInt(3);
	    	    	if (option == 0)
	    	    		current = BooleanConstant.TRUE;
	    	    	else if (option == 1)
	    	    		// weak(b W (a & b)) = F(b) || b U (a & b)
	    	    		current = Disjunction.of(FOperator.of(right),UOperator.of(right, Conjunction.of(left,right))); 
	    	    	else
	    	    		// weak(b W (a & b)) = G(b) || F (a & b)
	    	    		current = Disjunction.of(GOperator.of(right), FOperator.of(Conjunction.of(left,right))); 
	    	    }

	    	    if (binaryOp.SRELEASE() != null) {
	    	    	// a R b = b U (a & b)
	    	    	// 0:TRUE 1:W 2:F
	    	    	int option = rand.nextInt(3);
	    	    	if (option == 0)
	    	    		current = BooleanConstant.TRUE;
	    	    	else if (option == 1)
	    	    		current = WOperator.of(right, Conjunction.of(left,right)); // weak(b U (a & b)) = b W (a & b)
	    	    	else
	    	    		current = FOperator.of(Conjunction.of(left,right)); // weak(b U (a & b)) = F(a & b)
	    	    }
	    	}
	    }
	    
	    return current;	    
	  }

	  @Override
	  public Formula visitBoolean(BooleanContext ctx) {
	    assert ctx.getChildCount() == 1;
	    BoolContext constant = ctx.bool();

	    Formula current = null;
	    
	    if (numOfRemainingWeakenings <= 0) {
		    if (constant.FALSE() != null) {
		      current = BooleanConstant.FALSE;
		    }
		    else if (constant.TRUE() != null) {
		      current = BooleanConstant.TRUE;
		    }
	    }
	    else {
	    	numOfRemainingWeakenings--;
	    	current = BooleanConstant.TRUE;
	    }
	    
	    return current;	
	  }

	  @Override
	  public Formula visitExpression(ExpressionContext ctx) {
	    assert ctx.getChildCount() == 1;
	    return visit(ctx.getChild(0));
	  }

	  @Override
	  public Formula visitFormula(FormulaContext ctx) {
	    // Contained formula + EOF
	    assert ctx.getChildCount() == 2 : ctx.getChildCount();
	    return visit(ctx.getChild(0));
	  }

	  @Override
	  public Formula visitNested(NestedContext ctx) {
	    assert ctx.getChildCount() == 3;
	    return visit(ctx.nested);
	  }

	  @Override
	  public Formula visitOrExpression(OrExpressionContext ctx) {
	    assert ctx.getChildCount() > 0;

	    Formula current = Disjunction.of(ctx.children.stream()
	      .filter(child -> !(child instanceof TerminalNode))
	      .map(this::visit));
	    
	    if (numOfRemainingWeakenings > 0) {
	    	Random rand = new Random(System.currentTimeMillis());
	    	boolean mutate = (rand.nextInt(weakening_rate) == 0);
	    	if (mutate) {
	    		// 0: TRUE 1: add disjunct
	    		numOfRemainingWeakenings--;
	    		int option = rand.nextInt(2);
	    		if (option == 0) 
	    			current = BooleanConstant.TRUE;
	    		else {
	    			Formula new_literal = createVariable(variables.get(rand.nextInt(variables.size())));
	    			if (rand.nextBoolean())
	    				new_literal = new_literal.not();
	    			current = Disjunction.of(current, new_literal); 
	    		}
	    	}
	    }
	    
	    return current;	
	  }

	  @Override
	  @SuppressWarnings("PMD.ConfusingTernary")
	  public Formula visitUnaryOperation(UnaryOperationContext ctx) {
	    assert ctx.getChildCount() == 2;
	    UnaryOpContext unaryOp = ctx.unaryOp();
	    Formula operand = visit(ctx.inner);

	    Formula current = null;
	    
	    if (unaryOp.NOT() != null) {
	    	current = operand.not();
	    }

	    if (unaryOp.FINALLY() != null) {
	    	current = FOperator.of(operand);
	    }

	    if (unaryOp.GLOBALLY() != null) {
	    	current = GOperator.of(operand);
	    }

	    if (unaryOp.NEXT() != null) {
	    	current = XOperator.of(operand);
	    }

	    if (current == null)
	    	throw new AssertionError("Unreachable Code");
	    
	    if (numOfRemainingWeakenings > 0) { 	
	    	Random rand = new Random(System.currentTimeMillis());
	    	boolean mutate = (rand.nextInt(weakening_rate) == 0);
	    	if (mutate) {
	    		numOfRemainingWeakenings --;
	    		current = FormulaMutator.mutate(current, variables);
	    	}
	    }
	    
	    return current;
	  }

	  @Override
	  public Formula visitVariable(VariableContext ctx) {
	    assert ctx.getChildCount() == 1;
	    Formula current =  createVariable(ctx.getText());
	    
	    if (numOfRemainingWeakenings > 0) { 	
	    	Random rand = new Random(System.currentTimeMillis());
	    	boolean mutate = (rand.nextInt(weakening_rate) == 0);
	    	if (mutate) {
	    		numOfRemainingWeakenings --;
	    		current = FormulaMutator.mutate(current, variables);
	    	}
	    }
	    
	    return current;
	  }

	  @Override
	  public Formula visitSingleQuotedVariable(SingleQuotedVariableContext ctx) {
	    assert ctx.getChildCount() == 3;
	    Formula current = createVariable(ctx.variable.getText());
	    
	    if (numOfRemainingWeakenings > 0) { 	
	    	Random rand = new Random(System.currentTimeMillis());
	    	boolean mutate = (rand.nextInt(weakening_rate) == 0);
	    	if (mutate) {
	    		numOfRemainingWeakenings --;
	    		current = FormulaMutator.mutate(current, variables);
	    	}
	    }
	    
	    return current;
	  }

	  @Override
	  public Formula visitDoubleQuotedVariable(DoubleQuotedVariableContext ctx) {
	    assert ctx.getChildCount() == 3;
	    Formula current = createVariable(ctx.variable.getText());
	    
	    if (numOfRemainingWeakenings > 0) { 	
	    	Random rand = new Random(System.currentTimeMillis());
	    	boolean mutate = (rand.nextInt(weakening_rate) == 0);
	    	if (mutate) {
	    		numOfRemainingWeakenings --;
	    		current = FormulaMutator.mutate(current, variables);
	    	}
	    }
	    
	    return current;
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

