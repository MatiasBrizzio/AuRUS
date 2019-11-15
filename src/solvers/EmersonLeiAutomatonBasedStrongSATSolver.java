package solvers;

import gov.nasa.ltl.graph.Graph;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import jhoafparser.ast.AtomAcceptance;
import jhoafparser.ast.BooleanExpression;
import main.Settings;
import modelcounter.AutomataBasedModelCounting;
import modelcounter.Buchi2Graph;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import owl.automaton.Automaton;
import owl.automaton.MutableAutomaton;
import owl.automaton.MutableAutomatonFactory;
import owl.automaton.Views;
import owl.automaton.acceptance.EmersonLeiAcceptance;
import owl.automaton.algorithms.EmptinessCheck;
import owl.automaton.edge.Edge;
import owl.automaton.output.HoaPrinter;
import owl.factories.FactorySupplier;
import owl.factories.ValuationSetFactory;
import owl.ltl.LabelledFormula;
import owl.run.DefaultEnvironment;
import owl.run.Environment;
import owl.translations.delag.DelagBuilder;
import tlsf.FormulaToAutomaton;

import java.math.BigInteger;
import java.util.*;
import java.util.function.IntConsumer;

import static owl.automaton.output.HoaPrinter.HoaOption.SIMPLE_TRANSITION_LABELS;

public class EmersonLeiAutomatonBasedStrongSATSolver<S> {

    private DMatrixRMaj T = null;
    private DMatrixRMaj I = null;
    private Automaton<S,EmersonLeiAcceptance> automaton = null;
    private List<String> input_vars = null;
    private Object[] states = null;
    public static int TIMEOUT = 300;

    public EmersonLeiAutomatonBasedStrongSATSolver(LabelledFormula formula) {
        // Convert the ltl formula to an automaton with OWL
        DelagBuilder translator = new DelagBuilder(DefaultEnvironment.standard());
        automaton = (Automaton<S, EmersonLeiAcceptance>) translator.apply(formula);
        System.out.println(HoaPrinter.toString(automaton, EnumSet.of(SIMPLE_TRANSITION_LABELS)));
        input_vars = new ArrayList<>(formula.player1Variables());
        states = automaton.states().toArray();
        //From A0 we construct the (n + 1) × (n + 1) transfer matrix T. A0 has n + 1
        //states s1, s2, . . . sn+1. The matrix entry Ti,j is the number of transitions from
        //state si to state sj
        T = buildInputAutomatonTransferMatrix();

		System.out.println("T: " + T.toString());
        int n = automaton.size();
        I = CommonOps_DDRM.identity(n);
    }

    public <S> boolean isStrongSatisfiable() {
        System.out.println("Parsing...");
        BigInteger numOfModels = count(Settings.MC_BOUND);
        BigInteger expectedNumOfModels =  (BigInteger.valueOf(2).pow(input_vars.size())).pow(Settings.MC_BOUND);
        return (numOfModels.equals(expectedNumOfModels));

    }



    public  BigInteger count(int bound){
        //We compute uTkv, where u is the row vector such that ui = 1 if and only if i is the start state and 0 otherwise,
        // and v is the column vector where vi = 1 if and only if i is an accepting state and 0 otherwise.
        if (automaton == null || automaton.size() == 0)
            return BigInteger.ZERO;
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

        for(int i=1; i<bound; i++) {
            long initialTime = System.currentTimeMillis();
            DMatrixRMaj T_i = T.copy();
            DMatrixRMaj T_aux = T_res.copy();
            CommonOps_DDRM.mult(T_aux, T_i, T_res);
//			System.out.println(i + ": " + T_res.toString());
            //check for timeout
            long currentTime = System.currentTimeMillis();
            long totalTime = currentTime-initialTime;
            int min = (int) (totalTime)/60000;
            int sec = (int) (totalTime - min*60000)/1000;
            if (sec > TIMEOUT) {
                System.out.print("TO ");
                return BigInteger.ZERO;
            }
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

    long transitions = 0;
    public DMatrixRMaj buildInputAutomatonTransferMatrix() {
        int n = automaton.size();
        //create valuation factory
        Environment env = DefaultEnvironment.standard();
        FactorySupplier factorySupplier = env.factorySupplier();
        ValuationSetFactory inputFactory = factorySupplier.getValuationSetFactory(new ArrayList<>(input_vars));
        DMatrixRMaj M = new DMatrixRMaj(n,n);
        for (int i = 0;i<n;i++) {
            S si = (S) states[i];
            for (int j = 0; j < n; j++) {
                S sj = (S) states[j];
                transitions = 0;
                inputFactory.universe().forEach(valuation -> {
                    Set<Edge<S>> edges = automaton.edges(si, valuation);
                    for (Edge<S> edge : edges) {
                        if (edge.successor().equals(sj))
                            transitions++;
                    }
                });
                System.out.printf("M[%d,%d] = %d \n", i,j,transitions);
                M.add(i, j, transitions);
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
