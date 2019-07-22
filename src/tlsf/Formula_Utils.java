package tlsf;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;

import owl.grammar.LTLLexer;
import owl.grammar.LTLParser;
import owl.ltl.BooleanConstant;
import owl.ltl.Conjunction;
import owl.ltl.Formula;
import owl.ltl.Formula.LogicalOperator;
import owl.ltl.parser.FormulaParser;
import owl.ltl.parser.LtlParser;
import owl.ltl.parser.TokenErrorListener;
import owl.ltl.LabelledFormula;

public class Formula_Utils {
	
	public static List<LabelledFormula> subformulas (LabelledFormula f) {//, List<String> variables) {
		List<LabelledFormula> s = new LinkedList();
		
		for (Formula c : f.formula().children()) {
			LabelledFormula sf = LabelledFormula.of(c, f.variables());
			for(LabelledFormula e : subformulas(sf))
				s.add(e);		
		}
		s.add(LabelledFormula.of(f.formula(),f.variables()));
		return s;
	}
	
	public static List<LabelledFormula> splitConjunction (LabelledFormula f){
		List<LabelledFormula> conjuncts = new LinkedList<>();
		if (f.formula() instanceof Conjunction) {
		      Conjunction conjunction = (Conjunction) f.formula();
		      for (Formula c : conjunction.children)
		    	  conjuncts.add(LabelledFormula.of(c, f.variables()));
		      
	    }
		return conjuncts;
	}

	
	public static LabelledFormula replaceSubformula (LabelledFormula f0, LabelledFormula f1) {
		if (!f0.variables().containsAll(f1.variables()))
			throw new IllegalArgumentException("Formula_Utils.replaceSubformula: formulas should have the same set of variables.");
		
		Random rand = new Random();
		// select randomly the fub formula of f0 to be replaced
		List<LabelledFormula> subformulas_f0 = subformulas(f0);
		LabelledFormula src = subformulas_f0.get(rand.nextInt(subformulas_f0.size()));
//		System.out.println("Selected source formula "+ src);
		
		// get randomly the sub formula of f1 to be used to replace in f0.
		List<LabelledFormula> subformulas_f1 = subformulas(f1);
		LabelledFormula target = subformulas_f1.get(rand.nextInt(subformulas_f1.size()));
//		System.out.println("Selected target formula "+target);
		
		LabelledFormula f0_copy = LabelledFormula.of(f0.formula(), f0.variables());
		//replaceSubformula(f0_copy.formula(), src.formula(), target.formula());
		f0_copy = replaceSubformula(f0_copy, src, target);
		return f0_copy;
	}
	
	public static Formula replaceSubformula (Formula f0, Formula src, Formula target) {		
		if (f0 == src)
			return target;
		if(f0.children().contains(src)) {
			f0.children().remove(src);
			f0.children().add(target);
		}
		else {
			for (Formula child : f0.children()) {
				Formula res = replaceSubformula(child, src, target);
				if (!child.equals(res))
					return res;
			}
		}
		return f0;
	}

	public static LabelledFormula replaceSubformula(LabelledFormula f, LabelledFormula src, LabelledFormula target) {
		String form = f.formula().toString().replace(src.formula().toString(), target.formula().toString());
//		System.out.println(form);
		
		CharStream input = CharStreams.fromString(form);
		// Tokenize the stream
	    LTLLexer lexer = new LTLLexer(input);
	    // Don't print long error messages on the console
	    lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);
	    // Add a fail-fast behaviour for token errors
	    lexer.addErrorListener(new TokenErrorListener());
	    CommonTokenStream tokens = new CommonTokenStream(lexer);

	    // Parse the tokens
	    LTLParser parser = new LTLParser(tokens);
		 // Convert the AST into a proper object
	    FormulaParser formVisitor = new FormulaParser(f.variables());
	    return LabelledFormula.of(formVisitor.visit(parser.formula()), formVisitor.variables());
	}
}
