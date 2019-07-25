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

public class SubformulaMutator extends LTLParserBaseVisitor<Formula> {
	  private final List<Literal> literalCache;
	  private final List<String> variables;
	  private final boolean fixedVariables;
	  private int mutation_rate;
	 

	  public SubformulaMutator(List<String> literals, int mutation_rate) {
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
	    this.mutation_rate = mutation_rate;
	  }

	  public List<String> variables() {
	    return List.copyOf(variables);
	  }

	  @Override
	  public Formula visitAndExpression(AndExpressionContext ctx) {
	    assert ctx.getChildCount() > 0;
	    
	    Random rand = new Random(System.currentTimeMillis());
	    boolean mutate = (rand.nextInt(mutation_rate) == 0);
	    Formula current = Conjunction.of(ctx.children.stream()
	  	      .filter(child -> !(child instanceof TerminalNode))
		      .map(this::visit));
	    if (mutate) {
	    	current = FormulaMutator.mutate(current, variables);
	    }
	    return current;
	  }

	  @Override
	  public Formula visitBinaryOperation(BinaryOperationContext ctx) {
	    assert ctx.getChildCount() == 3;
	    assert ctx.left != null && ctx.right != null;

	    BinaryOpContext binaryOp = ctx.binaryOp();
	    Formula left = visit(ctx.left);
	    Formula right = visit(ctx.right);
	    
	    Formula current = null;
	    
	    if (binaryOp.BIIMP() != null) {
	    	current = Biconditional.of(left, right);
	    }

	    if (binaryOp.IMP() != null) {
	    	current = Disjunction.of(left.not(), right);
	    }

	    if (binaryOp.XOR() != null) {
	    	current = Biconditional.of(left.not(), right);
	    }

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
	    
	    Random rand = new Random(System.currentTimeMillis());
	    boolean mutate = (rand.nextInt(mutation_rate) == 0);
	    
	    if (mutate)
	    	current = FormulaMutator.mutate(current, variables);
	    
	    return current;	    
	  }

	  @Override
	  public Formula visitBoolean(BooleanContext ctx) {
	    assert ctx.getChildCount() == 1;
	    BoolContext constant = ctx.bool();

	    Formula current = null;

	    if (constant.FALSE() != null) {
	      current = BooleanConstant.FALSE;
	    }

	    if (constant.TRUE() != null) {
	      current = BooleanConstant.TRUE;
	    }
	    
	    if (current == null)
	    	throw new ParseCancellationException("Unknown constant");
	    
	    Random rand = new Random(System.currentTimeMillis());
	    boolean mutate = (rand.nextInt(mutation_rate) == 0);
	    
	    if (mutate)
	    	current = FormulaMutator.mutate(current, variables);
	    
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
	    
	    Random rand = new Random(System.currentTimeMillis());
	    boolean mutate = (rand.nextInt(mutation_rate) == 0);
	    
	    if (mutate)
	    	current = FormulaMutator.mutate(current, variables);
	    
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
	    
	    Random rand = new Random(System.currentTimeMillis());
	    boolean mutate = (rand.nextInt(mutation_rate) == 0);
	    
	    if (mutate)
	    	current = FormulaMutator.mutate(current, variables);
	    
	    return current;
	  }

	  @Override
	  public Formula visitVariable(VariableContext ctx) {
	    assert ctx.getChildCount() == 1;
	    Formula current =  createVariable(ctx.getText());
	    
	    Random rand = new Random(System.currentTimeMillis());
	    boolean mutate = (rand.nextInt(mutation_rate) == 0);
	    
	    if (mutate)
	    	current = FormulaMutator.mutate(current, variables);
	    
	    return current;
	  }

	  @Override
	  public Formula visitSingleQuotedVariable(SingleQuotedVariableContext ctx) {
	    assert ctx.getChildCount() == 3;
	    Formula current = createVariable(ctx.variable.getText());
	    
	    Random rand = new Random(System.currentTimeMillis());
	    boolean mutate = (rand.nextInt(mutation_rate) == 0);
	    
	    if (mutate)
	    	current = FormulaMutator.mutate(current, variables);
	    
	    return current;
	  }

	  @Override
	  public Formula visitDoubleQuotedVariable(DoubleQuotedVariableContext ctx) {
	    assert ctx.getChildCount() == 3;
	    Formula current = createVariable(ctx.variable.getText());
	    
	    Random rand = new Random(System.currentTimeMillis());
	    boolean mutate = (rand.nextInt(mutation_rate) == 0);
	    
	    if (mutate)
	    	current = FormulaMutator.mutate(current, variables);
	    
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

