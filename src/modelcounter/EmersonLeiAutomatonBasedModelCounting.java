package modelcounter;


import it.unimi.dsi.fastutil.ints.IntArrayList;
import jhoafparser.ast.AtomAcceptance;
import jhoafparser.ast.BooleanExpression;
import main.Settings;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import owl.automaton.Automaton;
import owl.automaton.acceptance.EmersonLeiAcceptance;
import owl.automaton.edge.Edge;
import owl.collections.ValuationSet;
import owl.factories.FactorySupplier;
import owl.factories.ValuationSetFactory;
import owl.ltl.LabelledFormula;
import owl.run.DefaultEnvironment;
import owl.run.Environment;
import owl.translations.delag.DelagBuilder;
import solvers.SolverUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.IntConsumer;

public class EmersonLeiAutomatonBasedModelCounting<S> {

	private DMatrixRMaj T = null;
//	private DMatrixRMaj I = null;
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
//		// Convert the ltl formula to an automaton with OWL
//		DelagBuilder translator = new DelagBuilder(DefaultEnvironment.standard());
//		automaton = (Automaton<S, EmersonLeiAcceptance>) translator.apply(formula);
//        states = automaton.states().toArray();
		//From A0 we construct the (n + 1) × (n + 1) transfer matrix T. A0 has n + 1
		//states s1, s2, . . . sn+1. The matrix entry Ti,j is the number of transitions from
		//state si to state sj
//		T = buildTransferMatrix();

//		System.out.println("T: " + T.toString());
//		int n = automaton.size();
//		I = CommonOps_DDRM.identity(n);
	}

	private String parse() {
		// Convert the ltl formula to an automaton with OWL
		DelagBuilder translator = new DelagBuilder(DefaultEnvironment.standard());
		automaton = (Automaton<S, EmersonLeiAcceptance>) translator.apply(formula);
		states = automaton.states().toArray();
		T = buildTransferMatrix();
		return "OK";
	}



	public  BigInteger count(int bound) {
		//We compute uTkv, where u is the row vector such that ui = 1 if and only if i is the start state and 0 otherwise,
		// and v is the column vector where vi = 1 if and only if i is an accepting state and 0 otherwise.
		if (automaton == null || automaton.size() == 0)
			return BigInteger.ZERO;
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
		return BigInteger.ZERO;
	}

	int BOUND = 0;
	private BigInteger countModels() {
		int N = T.numRows;
        //set initial states
		DMatrixRMaj u = new DMatrixRMaj(1,N);
		Set<S> initial_states = automaton.initialStates();
        for(int j = 0; j < N; j++) {
            if (initial_states.contains(states[j])) {
                u.set(0, j,1);
            }
        }

        //set final states
		DMatrixRMaj v = new DMatrixRMaj(N,1);
        Set<S> final_states = new HashSet<>();
        for (S s : automaton.states()) {
            Set<Edge<S>> edges = automaton.edges(s);
            for(Edge<S> edge : edges) {
                //check if it is an acceptance transition
                IntArrayList acceptanceSets = new IntArrayList();
                if (edge.acceptanceSetIterator().hasNext())
                    edge.acceptanceSetIterator().forEachRemaining((IntConsumer) acceptanceSets::add);
                if (accConditionIsSatisfied(automaton.acceptance().booleanExpression(), acceptanceSets)) {
                    final_states.add(edge.successor());
                }
            }
		}
        for(int i = 0; i < N; i++) {
            if (final_states.contains(states[i])) {
                v.set(i, 0, 1);
            }
        }

		// count models

		DMatrixRMaj T_res = T.copy();

		for(int i=1; i<BOUND; i++) {
			long initialTime = System.currentTimeMillis();
			DMatrixRMaj T_i = T.copy();
			DMatrixRMaj T_aux = T_res.copy();
			CommonOps_DDRM.mult(T_aux, T_i, T_res);
//			System.out.println(i + ": " + T_res.toString());
		}
		DMatrixRMaj reachable = new DMatrixRMaj(1,N);
		CommonOps_DDRM.mult(u, T_res, reachable);
//		System.out.println("reachable: " + reachable.toString());
		DMatrixRMaj result = new DMatrixRMaj(1,1);
		CommonOps_DDRM.mult(reachable,v,result);
//		System.out.println("result: " + result.toString());
		long value = (long)result.get(0,0);
		BigInteger count = BigInteger.valueOf(value);
		return count;
	}

	  /**
	   * Build the Transfer Matrix for the given DFA
	   * @param automaton is the DFA
	   * @return a n x n matrix M where M[i,j] is the number of transitions from state si to state sj 
	   */
	  long transitions = 0;
	  public  DMatrixRMaj buildTransferMatrix() {
		  int n = automaton.size();
		  DMatrixRMaj M = new DMatrixRMaj(n,n);
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
				  M.set(i, j, transitions);
			  }
		  }
		  return M;
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

