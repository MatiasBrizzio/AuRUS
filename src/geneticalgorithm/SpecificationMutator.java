package geneticalgorithm;

import java.util.List;
import java.util.Random;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.tree.ParseTree;

import geneticalgorithm.SpecificationChromosome.SPEC_STATUS;
import owl.grammar.LTLLexer;
import owl.grammar.LTLParser;
import owl.ltl.BooleanConstant;
import owl.ltl.Conjunction;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.Literal;
import owl.ltl.parser.SubformulaMutator;
import owl.ltl.parser.SubformulaReplacer;
import owl.ltl.parser.TokenErrorListener;
import owl.ltl.tlsf.Tlsf;
import tlsf.Formula_Utils;
import tlsf.TLSF_Utils;

public class SpecificationMutator {
	
	public static Tlsf mutate(Tlsf spec, SPEC_STATUS status) {
		Random rand = new Random(System.currentTimeMillis());

		//create empty specification
		Tlsf new_spec = TLSF_Utils.empty_spec(spec);
		
		// set initially
		if (!status.areAssumptionsSAT()) {
			int modification =  rand.nextInt(3);
			if (spec.initially().compareTo(BooleanConstant.TRUE) == 0)
				modification = rand.nextInt(2);
			
			Formula new_init = null;
			switch (modification) {
				// arbitrary mutation
				case 0 : new_init = applyMutation(LabelledFormula.of(spec.initially(), spec.variables()));
							
				// strengthen mutation
				case 1 : 
					
				// weaken mutation
				case 2 : 
			}
			new_spec = TLSF_Utils.change_initially(new_spec, new_init);
		}
			
		
		// set preset
//		turn = rand.nextInt(2);
//		if (turn == 0)
//			new_spec = TLSF_Utils.change_preset(new_spec, spec0.preset());
//		else 
//			new_spec = TLSF_Utils.change_preset(new_spec, spec1.preset());
//		
//		// set require
//		turn = rand.nextInt(2);
//		if (turn == 0)
//			new_spec = TLSF_Utils.change_require(new_spec, spec0.require());
//		else 
//			new_spec = TLSF_Utils.change_require(new_spec, spec1.require());
//		
//		// set assert
//		turn = rand.nextInt(2);
//		if (turn == 0)
//			new_spec = TLSF_Utils.change_assert(new_spec, spec0.assert_());
//		else 
//			new_spec = TLSF_Utils.change_assert(new_spec, spec1.assert_());
//		
//		// set assume
//		turn = rand.nextInt(2);
//		if (turn == 0)
//			new_spec = TLSF_Utils.change_assume(new_spec, spec0.assume());
//		else 
//			new_spec = TLSF_Utils.change_assume(new_spec, spec1.assume());
//		
//		// set guarantees
//		turn = rand.nextInt(2);
//		if (turn == 0)
//			new_spec = TLSF_Utils.change_guarantees(new_spec, spec0.guarantee());
//		else 
//			new_spec = TLSF_Utils.change_guarantees(new_spec, spec1.guarantee());
		
		return new_spec;
	}

		
		public static Formula applyMutation (LabelledFormula f) {
			CharStream input = CharStreams.fromString(f.toString());
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
		    SubformulaMutator formVisitor = new SubformulaMutator(f.variables(), Formula_Utils.formulaSize(f.formula()), 1);
		    return formVisitor.visit(parser.formula());
		}
		
		public static Formula weakenFormula (LabelledFormula f) {
			Random rand = new Random(System.currentTimeMillis());
			List<LabelledFormula> conjuncts = Formula_Utils.splitConjunction(f);
			LabelledFormula to_be_weaken = conjuncts.remove(rand.nextInt(conjuncts.size()));
			
			
			return null;
		}
		
		public static Formula strengthenFormula (LabelledFormula f) {
			
			return null;
		}
}
