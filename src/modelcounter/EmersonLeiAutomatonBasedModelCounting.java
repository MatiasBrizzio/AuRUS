package modelcounter;


import it.unimi.dsi.fastutil.ints.IntArrayList;
import jhoafparser.ast.AtomAcceptance;
import jhoafparser.ast.BooleanExpression;
import main.Settings;
import org.apache.commons.math3.fraction.BigFraction;
import org.apache.commons.math3.linear.Array2DRowFieldMatrix;
import org.apache.commons.math3.linear.FieldMatrix;
import owl.automaton.Automaton;
import owl.automaton.acceptance.EmersonLeiAcceptance;
import owl.automaton.acceptance.OmegaAcceptance;
import owl.automaton.acceptance.ParityAcceptance;
import owl.automaton.edge.Edge;
import owl.automaton.output.HoaPrinter;
import owl.collections.ValuationSet;
import owl.factories.FactorySupplier;
import owl.factories.ValuationSetFactory;
import owl.ltl.LabelledFormula;
import owl.run.DefaultEnvironment;
import owl.run.Environment;
import owl.translations.LTL2NAFunction;
import owl.translations.delag.DelagBuilder;
import owl.translations.ltl2dpa.LTL2DPAFunction;
import solvers.SolverUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.IntConsumer;

import static owl.automaton.output.HoaPrinter.HoaOption.SIMPLE_TRANSITION_LABELS;
import static owl.translations.ltl2dpa.LTL2DPAFunction.Configuration.*;

public class EmersonLeiAutomatonBasedModelCounting<S> {

	private FieldMatrix<BigFraction> T = null;
	private Automaton<S,EmersonLeiAcceptance> automaton = null;
	private LabelledFormula formula = null;
    private Object[] states = null;
	//public static int TIMEOUT = 300;

	public EmersonLeiAutomatonBasedModelCounting(LabelledFormula formula) {
		this.formula = formula;
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		// Do the call in a separate thread, get a Future back
		Future<String> future = executorService.submit(this::parse);
		try {
			// Wait for at most TIMEOUT seconds until the result is returned
			String result = future.get(Settings.PARSING_TIMEOUT, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			System.out.println("EmersonLeiAutomatonBasedModelCounting: TIMEOUT parsing.");
		}
		catch (InterruptedException | ExecutionException e) {
			System.err.println("EmersonLeiAutomatonBasedModelCounting: ERROR while parsing. " + e.getMessage());
		}
	}

	private String parse() {
		// Convert the ltl formula to an automaton with OWL
		DelagBuilder translator = new DelagBuilder(DefaultEnvironment.standard());
		automaton = (Automaton<S, EmersonLeiAcceptance>) translator.apply(formula);
//		System.out.println(HoaPrinter.toString(automaton, EnumSet.of(SIMPLE_TRANSITION_LABELS)));
//		var environment = DefaultEnvironment.standard();
//		var translator = new LTL2DPAFunction(environment, EnumSet.of(
////				OPTIMISE_INITIAL_STATE,
//				COMPLEMENT_CONSTRUCTION,
//				GREEDY,
//				COMPRESS_COLOURS));
////				LTL2DPAFunction.RECOMMENDED_SYMMETRIC_CONFIG);
//
//		automaton = (Automaton<S, ParityAcceptance>) translator.apply(formula);

		states = automaton.states().toArray();

		return "OK";
	}


	public  BigInteger count(int bound) {
		//We compute uTkv, where u is the row vector such that ui = 1 if and only if i is the start state and 0 otherwise,
		// and v is the column vector where vi = 1 if and only if i is an accepting state and 0 otherwise.
		if (states == null)
			return null;
		BOUND = bound;
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		// Do the call in a separate thread, get a Future back
		Future<BigInteger> future = executorService.submit(this::countModels);
		try {
			// Wait for at most TIMEOUT seconds until the result is returned
			BigInteger result = future.get(Settings.MC_TIMEOUT, TimeUnit.SECONDS);
			return result;
		} catch (TimeoutException e) {
			System.out.println("EmersonLeiAutomatonBasedModelCounting::count TIMEOUT.");
		}
		catch (InterruptedException | ExecutionException e) {
			System.err.println("EmersonLeiAutomatonBasedModelCounting::count ERROR. " + e.getMessage());
		}
		return null;
	}

	int BOUND = 0;
	private BigInteger countModels() {
		T = buildTransferMatrix();
//		printMatrix(T);
		int n = T.getRowDimension();

		//set initial states
		FieldMatrix u = buildInitialStates() ;

		//set final states
		FieldMatrix v =  buildFinalStates();

		// count models
		FieldMatrix T_res = T.power(BOUND);
//		printMatrix(T_res);
		FieldMatrix reachable = u.multiply(T_res);
//		System.out.println("reachable: " + reachable.toString());
		FieldMatrix result = reachable.multiply(v);
//		System.out.println("result: " + result.toString());
		BigFraction value = (BigFraction)result.getEntry(0,0);
		BigInteger count = value.getNumerator();
		return count;
	}

	  /**
	   * Build the Transfer Matrix for the given DFA
	   * @param automaton is the DFA
	   * @return a n x n matrix M where M[i,j] is the number of transitions from state si to state sj
	   */
	  long transitions = 0;
	  public  FieldMatrix buildTransferMatrix() {

		  int n = automaton.size();
		  BigFraction[][] pData = new BigFraction[n][n];
		  for (int i = 0;i<n;i++) {
			  S si = (S) states[i];
			  for (int j = 0; j < n; j++) {
				  S sj = (S) states[j];
				  transitions = 0;
				  automaton.factory().universe().forEach(valuation -> {
						  Set<Edge<S>> edges = automaton.edges(si, valuation);
						  for (Edge<S> edge : edges) {
							  if (edge.successor().equals(sj))
								transitions++;
						  }
					  });
				  BigFraction v = new BigFraction(transitions);
				  pData[i][j] = v;
			  }
		  }
		  return new Array2DRowFieldMatrix<BigFraction>(pData, false);
	  }

	  public FieldMatrix buildInitialStates() {
		  int n = T.getRowDimension();
		  //set initial states
		  FieldMatrix u = createMatrix(1,n);
		  Set<S> initial_states = automaton.initialStates();
		  for(int j = 0; j < n; j++) {
			  if (initial_states.contains(states[j]))
				  u.addToEntry(0, j,new BigFraction(1));
//			else
//				u.addToEntry(0, j,new BigFraction(0));
		  }
		  return u;
	  }

	public FieldMatrix buildFinalStates() {
		int n = T.getRowDimension();
		//set final states
		Set<S> final_states = new HashSet<>();
		for (S s : automaton.states()) {
			Set<Edge<S>> edges = automaton.edges(s);
			for (Edge<S> edge : edges) {
				//check if it is an acceptance transition
				IntArrayList acceptanceSets = new IntArrayList();
				if (edge.acceptanceSetIterator().hasNext())
					edge.acceptanceSetIterator().forEachRemaining((IntConsumer) acceptanceSets::add);
				if (accConditionIsSatisfied(automaton.acceptance().booleanExpression(), acceptanceSets)) {
					final_states.add(edge.successor());
				}
			}
		}

		FieldMatrix v = createMatrix(n, 1);
		for (int i = 0; i < n; i++) {
			if (final_states.contains(states[i])) {
				v.addToEntry(i, 0, new BigFraction(1));
			}
		}
		return v;
	}

	public FieldMatrix createMatrix(int row, int column) {
		BigFraction[][] pData = new BigFraction[row][column];
		for (int i = 0; i<row; i++){
			for (int j = 0; j<column; j++){
				pData[i][j] = new BigFraction(0);
			}
		}
		return new Array2DRowFieldMatrix<BigFraction>(pData, false);
	}

	public void printMatrix(FieldMatrix<BigFraction> M) {
		int row = M.getRowDimension();
		int column = M.getColumnDimension();
		for (int i = 0; i<row; i++){
			for (int j = 0; j<column; j++){
				System.out.print(M.getEntry(i,j) + " ");
			}
			System.out.println();
		}
	}


    public boolean accConditionIsSatisfied(BooleanExpression<AtomAcceptance> acceptanceCondition, IntArrayList acceptanceSets) {
        boolean accConditionSatisfied = false;
        switch(acceptanceCondition.getType()) {
            case EXP_TRUE: { accConditionSatisfied = true; break; }
            case EXP_FALSE: break;
            case EXP_ATOM:
            {
                if (acceptanceCondition.getAtom().getType() == AtomAcceptance.Type.TEMPORAL_INF)
                    accConditionSatisfied = (acceptanceSets.contains(acceptanceCondition.getAtom().getAcceptanceSet()));
                else if (acceptanceCondition.getAtom().getType() == AtomAcceptance.Type.TEMPORAL_FIN) {
                    accConditionSatisfied = ! (acceptanceSets.contains(acceptanceCondition.getAtom().getAcceptanceSet()));
                }
                break;
            }
            case EXP_AND:
            {
                if (accConditionIsSatisfied(acceptanceCondition.getLeft(), acceptanceSets))
                    accConditionSatisfied = accConditionIsSatisfied(acceptanceCondition.getRight(), acceptanceSets);
                break;
            }
            case EXP_OR:
            {
                if (accConditionIsSatisfied(acceptanceCondition.getLeft(), acceptanceSets))
                    accConditionSatisfied = true;
                else
                    accConditionSatisfied = accConditionIsSatisfied(acceptanceCondition.getRight(), acceptanceSets);
                break;
            }
            case EXP_NOT: {
                accConditionSatisfied = !accConditionIsSatisfied(acceptanceCondition.getRight(), acceptanceSets);
                break;
            }
        }

        return accConditionSatisfied;
    }
	  
	 
}	

