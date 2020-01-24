package solvers;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import jhoafparser.ast.AtomAcceptance;
import jhoafparser.ast.BooleanExpression;
import main.Settings;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import owl.automaton.Automaton;
import owl.automaton.acceptance.EmersonLeiAcceptance;
import owl.automaton.edge.Edge;
import owl.automaton.output.HoaPrinter;
import owl.collections.ValuationSet;
import owl.factories.FactorySupplier;
import owl.factories.ValuationSetFactory;
import owl.ltl.LabelledFormula;
import owl.run.DefaultEnvironment;
import owl.run.Environment;
import owl.translations.delag.DelagBuilder;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.IntConsumer;

import static owl.automaton.output.HoaPrinter.HoaOption.SIMPLE_TRANSITION_LABELS;

public class PotentiallyRealizabilityChecker<S> {

//    private DMatrixRMaj T = null;
//    private DMatrixRMaj I = null;
    private Automaton<S,EmersonLeiAcceptance> automaton = null;
    private LabelledFormula formula;
    private List<String> input_vars = null;
//    private List<String> variables = null;
//    private Map<S,Integer> states = null;
    //public static int TIMEOUT = 120;

    public PotentiallyRealizabilityChecker(LabelledFormula formula) {
        this.formula = formula;
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        // Do the call in a separate thread, get a Future back
        Future<String> future = executorService.submit(this::parse);
        try {
            // Wait for at most TIMEOUT seconds until the result is returned
            String result = future.get(Settings.PARSING_TIMEOUT, TimeUnit.SECONDS);
            input_vars = new ArrayList<>(formula.player1Variables());
        } catch (TimeoutException e) {
            System.out.println("PotentiallyRealizabilityChecker: TIMEOUT parsing.");
            System.err.println(formula);
        }
        catch (InterruptedException | ExecutionException e) {
            System.err.println("PotentiallyRealizabilityChecker: ERROR while parsing. " + e.getMessage());
            System.err.println(formula);
        }
//        System.out.println("Parsed...");
//        System.out.println(HoaPrinter.toString(automaton, EnumSet.of(SIMPLE_TRANSITION_LABELS)));
//        input_vars = new ArrayList<>(formula.player1Variables());
//        variables = formula.variables();
//        states = new HashMap<>();
//        int index = 0;
//        for(S s : automaton.states()) {
//            states.put(s,index);
//            index++;
//        }
        //From A0 we construct the (n + 1) × (n + 1) transfer matrix T. A0 has n + 1
        //states s1, s2, . . . sn+1. The matrix entry Ti,j is the number of transitions from
        //state si to state sj
//        T = buildInputAutomatonTransferMatrix();
//        setAcceptanceTransitions();
//
//		System.out.println("T: " + T.toString());
//        int n = automaton.size();
//        I = CommonOps_DDRM.identity(n);
    }

    private String parse() {
        // Convert the ltl formula to an automaton with OWL
        DelagBuilder translator = new DelagBuilder(DefaultEnvironment.standard());
        automaton = (Automaton<S, EmersonLeiAcceptance>) translator.apply(formula);
        return "OK";
    }

//    public boolean isStrongSatisfiable() {
//        BigInteger numOfModels = count(Settings.MC_BOUND);
//        BigInteger expectedNumOfModels =  (BigInteger.valueOf(2).pow(input_vars.size())).pow(Settings.MC_BOUND);
//        System.out.printf("numOfModels: %d   expected: %d\n", numOfModels,expectedNumOfModels);
//        return (numOfModels.compareTo(expectedNumOfModels) >= 0);
//
//    }

    boolean isAcceptance = false;
    boolean noAcceptance = false;
//    long numOfAcceptanceTransition = 0;
    public Boolean checkPotentiallyRealizability() {
        if (automaton == null)
            return null;
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        // Do the call in a separate thread, get a Future back
        Future<Boolean> future = executorService.submit(this::isPotentiallyRealizable);
        try {
            // Wait for at most TIMEOUT seconds until the result is returned
            Boolean result = future.get(Settings.STRONG_SAT_TIMEOUT, TimeUnit.SECONDS);
            return result;
        } catch (TimeoutException e) {
            System.out.println("PotentiallyRealizabilityChecker::isPotentiallyRealizable TIMEOUT.");
            System.err.println(formula);
        }
        catch (InterruptedException | ExecutionException e) {
            System.err.println("PotentiallyRealizabilityChecker::isPotentiallyRealizable ERROR. " + e.getMessage());
            System.err.println(formula);
        }
        return null;
    }

    private boolean isPotentiallyRealizable() {
//        BigInteger numOfInputEvents = BigInteger.valueOf(2).pow(input_vars.size());
        List<S> visitedStates = new LinkedList<>();
        Set<S> undesiredStates = new HashSet<>();
        Set<S> visitedUndesiredStates = new HashSet<>();
        List<S> statesToAnalyse = new LinkedList<>(automaton.initialStates());
//        System.out.println(automaton.size());
        while (!statesToAnalyse.isEmpty()) {
//            System.out.print(".");
            S current = statesToAnalyse.remove(0);
            if (visitedStates.contains(current))
                continue;
            visitedStates.add(current);
//        for(S current : automaton.states()) {
//            numOfAcceptanceTransition = 0;
            Map<Edge<S>, ValuationSet> edges = automaton.edgeMap(current);
            if (!edges.isEmpty()) {
                //create valuation factory
                Environment env = DefaultEnvironment.standard();
                FactorySupplier factorySupplier = env.factorySupplier();
                ValuationSetFactory inputFactory = factorySupplier.getValuationSetFactory(new ArrayList<>(input_vars));
                noAcceptance = false;
                inputFactory.universe().forEach(inputBitSet -> {
                    if (noAcceptance)
                        return;

                    isAcceptance = false;
                    edges.forEach((edge, valuation) -> {
                        if (isAcceptance)
                            return;
                        valuation.forEach(bitSet -> {
                            BitSet bs1 = bitSet.get(0, input_vars.size());
                            //                            System.out.println(inputBitSet + ": "+ bitSet + "  ->  " + bs1);
                            if (bs1.equals(inputBitSet)) {
                                //check if it is an acceptance transition
                                IntArrayList acceptanceSets = new IntArrayList();
                                if (edge.acceptanceSetIterator().hasNext())
                                    edge.acceptanceSetIterator().forEachRemaining((IntConsumer) acceptanceSets::add);
                                if (accConditionIsSatisfied(automaton.acceptance().booleanExpression(), acceptanceSets)) {
                                    isAcceptance = true;
                                    //                                if (!statesToAnalyse.contains(edge.successor()))
                                    //                                    statesToAnalyse.add(edge.successor());
                                }
                            }
                        });
                    });
                    if (!isAcceptance) {
                        noAcceptance = true;
                        if (!visitedUndesiredStates.contains(current))
                            undesiredStates.add(current);
                    }
                });
                if (!noAcceptance) {
                    for (S succ : automaton.successors(current)) {
                        if (!visitedStates.contains(succ) && !statesToAnalyse.contains(succ))
                            statesToAnalyse.add(succ);
                    }
                }
            }
            else {
                // current is a sink state with no successors
                undesiredStates.add(current); //check if it can be avoided
            }
            if (!undesiredStates.isEmpty()) {
//                System.out.print("+");
                //check if there is a (controllable) way to avoid reaching the bad state
//                List<S> list_undesired_states = new LinkedList<>(undesiredStates);
//                while (!list_undesired_states.isEmpty()) {
                for(S bad_state : undesiredStates) {
                    for (S predecessor : automaton.predecessors(bad_state)) {
                        if (predecessor.equals(bad_state) || undesiredStates.contains(predecessor) || visitedUndesiredStates.contains(predecessor) || !visitedStates.contains(predecessor))
                            continue;

                        Map<Edge<S>, ValuationSet> predecessor_edges = automaton.edgeMap(predecessor);
                        //create valuation factory
                        Environment env = DefaultEnvironment.standard();
                        FactorySupplier factorySupplier = env.factorySupplier();
                        ValuationSetFactory inputFactory = factorySupplier.getValuationSetFactory(new ArrayList<>(input_vars));

                        noAcceptance = false;
                        inputFactory.universe().forEach(inputBitSet -> {
                            if (noAcceptance)
                                return;

                            isAcceptance = false;
                            predecessor_edges.forEach((predecessor_edge, valuation) -> {
                                if (isAcceptance)
                                    return;
                                if (predecessor_edge.successor().equals(bad_state))
                                    return;
                                valuation.forEach(bitSet -> {
                                    BitSet bs1 = bitSet.get(0, input_vars.size());
                                    //                            System.out.println(inputBitSet + ": "+ bitSet + "  ->  " + bs1);
                                    if (bs1.equals(inputBitSet)) {
                                        //check if it is an acceptance transition
                                        IntArrayList acceptanceSets = new IntArrayList();
                                        if (predecessor_edge.acceptanceSetIterator().hasNext())
                                            predecessor_edge.acceptanceSetIterator().forEachRemaining((IntConsumer) acceptanceSets::add);
                                        if (accConditionIsSatisfied(automaton.acceptance().booleanExpression(), acceptanceSets)) {
                                            isAcceptance = true;
                                        }
                                    }
                                });
                            });
                            if (!isAcceptance) {
                                noAcceptance = true;
                            }
                        });
                        if (noAcceptance) {
//                            if(automaton.initialStates().contains(predecessor)) { //no way to avoid this path
//                                System.out.printf("ERROR: %s\n", bad_state);
                                return false;
//                            }
                            //add predecessors of the path of undesired states to see if it is possible to avoid it
//                            list_undesired_states.add(predecessor);
                        }
                    }
                    visitedUndesiredStates.add(bad_state);
                }
                undesiredStates.clear();
            }
        }

//        System.out.printf("states = %d \n", automaton.size());
//        System.out.printf("undesiredStates = %d \n", undesiredStates.size());
////            if(! numOfInputEvents.equals(BigInteger.valueOf(numOfAcceptanceTransition))) {
//
//        if (undesiredStates.contains(automaton.onlyInitialState()))
//            return false;
//
//        //check if there is a (controlable) way to avoid reaching the bad state
//        for (S bad_state : undesiredStates) {
//            for(S predecessor : automaton.predecessors(bad_state)) {
//                if (undesiredStates.contains(predecessor))
//                    continue;
//
//                Map<Edge<S>, ValuationSet> predecessor_edges = automaton.edgeMap(predecessor);
//                if(!predecessor_edges.isEmpty()) {
//                    //create valuation factory
//                    Environment env = DefaultEnvironment.standard();
//                    FactorySupplier factorySupplier = env.factorySupplier();
//                    ValuationSetFactory inputFactory = factorySupplier.getValuationSetFactory(new ArrayList<>(input_vars));
//
//                    noAcceptance = false;
//                    inputFactory.universe().forEach(inputBitSet -> {
//                        if (noAcceptance)
//                            return;
//
//                        isAcceptance = false;
//                        predecessor_edges.forEach((predecessor_edge, valuation) -> {
//                            if (isAcceptance)
//                                return;
//                            if (predecessor_edge.successor().equals(bad_state))
//                                return;
//                            valuation.forEach(bitSet -> {
//                                BitSet bs1 = bitSet.get(0, input_vars.size());
//                                //                            System.out.println(inputBitSet + ": "+ bitSet + "  ->  " + bs1);
//                                if (bs1.equals(inputBitSet)) {
//                                    //check if it is an acceptance transition
//                                    IntArrayList acceptanceSets = new IntArrayList();
//                                    if (predecessor_edge.acceptanceSetIterator().hasNext())
//                                        predecessor_edge.acceptanceSetIterator().forEachRemaining((IntConsumer) acceptanceSets::add);
//                                    if (accConditionIsSatisfied(automaton.acceptance().booleanExpression(), acceptanceSets)) {
//                                        isAcceptance = true;
//                                    }
//                                }
//                            });
//                        });
//                        if (!isAcceptance) {
//                            noAcceptance = true;
//                        }
//                    });
//                    if (noAcceptance) {
////                        System.out.printf("ERROR: state[%d] = %s\n", states.get(bad_state), bad_state);
//                        return false;
//                    }
//                }
//            }
//        }
        return true;
    }


//    public  BigInteger count(int bound){
//        //We compute uTkv, where u is the row vector such that ui = 1 if and only if i is the start state and 0 otherwise,
//        // and v is the column vector where vi = 1 if and only if i is an accepting state and 0 otherwise.
//        if (automaton == null || automaton.size() == 0)
//            return BigInteger.ZERO;
//        int N = automaton.size();
//        //set initial states
//        DMatrixRMaj u = new DMatrixRMaj(1,N+1);
//        for(S is : automaton.initialStates()) {
//            u.set(0, states.get(is),1);
//        }
//        System.out.println("Initial: " + u.toString());
//        //set final states
//        DMatrixRMaj v = new DMatrixRMaj(N+1,1);
//        v.set(N, 0, 1);
//
////        Set<S> final_states = new HashSet<>();
////        for (S s : automaton.states()) {
////            Set<Edge<S>> edges = automaton.edges(s);
////            for(Edge<S> edge : edges) {
////                //check if it is an acceptance transition
////                IntArrayList acceptanceSets = new IntArrayList();
////                if (edge.acceptanceSetIterator().hasNext())
////                    edge.acceptanceSetIterator().forEachRemaining((IntConsumer) acceptanceSets::add);
////                if (accConditionIsSatisfied(automaton.acceptance().booleanExpression(), acceptanceSets)) {
////                    final_states.add(edge.successor());
////                }
////            }
////        }
////        for(S fs : final_states) {
////            v.set(states.get(fs), 0, 1);
////        }
//        System.out.println("Final: " + v.toString());
//        // count models
//
//        DMatrixRMaj T_res = T.copy();
//
//        for(int i=1; i<bound; i++) {
//            long initialTime = System.currentTimeMillis();
//            DMatrixRMaj T_i = T.copy();
//            DMatrixRMaj T_aux = T_res.copy();
//            CommonOps_DDRM.mult(T_aux, T_i, T_res);
////			System.out.println(i + ": " + T_res.toString());
//            //check for timeout
//            long currentTime = System.currentTimeMillis();
//            long totalTime = currentTime-initialTime;
//            int min = (int) (totalTime)/60000;
//            int sec = (int) (totalTime - min*60000)/1000;
//            if (sec > TIMEOUT) {
//                System.out.print("TO ");
//                return BigInteger.ZERO;
//            }
//        }
//        DMatrixRMaj reachable = new DMatrixRMaj(1,N+1);
//        CommonOps_DDRM.mult(u, T_res, reachable);
////		System.out.println("reachable: " + reachable.toString());
//        DMatrixRMaj result = new DMatrixRMaj(1,1);
//        CommonOps_DDRM.mult(reachable,v,result);
////		System.out.println("result: " + result.toString());
//        long value = (long)result.get(0,0);
//        BigInteger count = BigInteger.valueOf(value);
//        return count;
//    }
//
//    long transitions = 0;
//    boolean existsInputTransition = false;
//    public DMatrixRMaj buildInputAutomatonTransferMatrix() {
//        int n = automaton.size();
//        //add one sink state thar represents the acceptance state
//        DMatrixRMaj M = new DMatrixRMaj(n+1,n+1);
//        for (S si : automaton.states()){
//            Map<Edge<S>, ValuationSet> edges = automaton.edgeMap(si);
//            for(S sj : automaton.successors(si)) {
//                transitions = 0;
//                //create valuation factory
//                Environment env = DefaultEnvironment.standard();
//                FactorySupplier factorySupplier = env.factorySupplier();
//                ValuationSetFactory inputFactory = factorySupplier.getValuationSetFactory(new ArrayList<>(input_vars));
//                inputFactory.universe().forEach(inputBitSet -> {
//                    existsInputTransition = false;
//                    edges.forEach((edge,valuation) -> {
//                        if (!existsInputTransition && edge.successor().equals(sj)) {
//                            //                    //create valuation factory
//                            //                    Environment env2 = DefaultEnvironment.standard();
//                            //                    FactorySupplier factorySupplier2 = env2.factorySupplier();
//                            //                    ValuationSetFactory allFactory = factorySupplier2.getValuationSetFactory(variables);
////                            System.out.printf("M[%d,%d]\n", states.get(si),states.get(sj));
//                            valuation.forEach(bitSet -> {
//                                if (existsInputTransition)
//                                    return;
//                                BitSet bs1 = bitSet.get(0,input_vars.size());
////                                System.out.println(inputBitSet + ": "+ bitSet + "  ->  " + bs1);
//                                if (bs1.equals(inputBitSet)) {
////                                    System.out.println("found....");
//                                    existsInputTransition = true;
//                                }
//                            });
//                        }
//                    });
//                    if (existsInputTransition)
//                        transitions++;
//                });
//                System.out.printf("M[%d,%d] = %d \n", states.get(si),states.get(sj),transitions);
//                M.set(states.get(si), states.get(sj), transitions);
//            }
//        }
//
//
//        return M;
//    }
//
//    boolean existsAcceptanceTransition = false;
//    long acceptanceTransitions = 0;
//    public void setAcceptanceTransitions() {
//        int n = automaton.size();
//        for (S si : automaton.states()){
//            acceptanceTransitions = 0;
//            Map<Edge<S>, ValuationSet> edges = automaton.edgeMap(si);
//            //create valuation factory
//            Environment env = DefaultEnvironment.standard();
//            FactorySupplier factorySupplier = env.factorySupplier();
//            ValuationSetFactory inputFactory = factorySupplier.getValuationSetFactory(new ArrayList<>(input_vars));
//            inputFactory.universe().forEach(inputBitSet -> {
//                existsAcceptanceTransition = false;
//                edges.forEach((edge,valuation) -> {
//                    if (!existsAcceptanceTransition) {
//                        valuation.forEach(bitSet -> {
//                            if (existsAcceptanceTransition)
//                                return;
//                            BitSet bs1 = bitSet.get(0,input_vars.size());
////                            System.out.println(inputBitSet + ": "+ bitSet + "  ->  " + bs1);
//                            if (bs1.equals(inputBitSet)) {
//                                //check if it is an acceptance transition
//                                IntArrayList acceptanceSets = new IntArrayList();
//                                if (edge.acceptanceSetIterator().hasNext())
//                                    edge.acceptanceSetIterator().forEachRemaining((IntConsumer) acceptanceSets::add);
//                                if (accConditionIsSatisfied(automaton.acceptance().booleanExpression(), acceptanceSets)) {
//                                    existsAcceptanceTransition = true;
//                                }
//                            }
//                        });
//                    }
//
//                });
//                if (existsAcceptanceTransition)
//                    acceptanceTransitions++;
//            });
//            System.out.printf("M[%d,%d] = %d \n", states.get(si),n,acceptanceTransitions);
//            T.set(states.get(si), n, acceptanceTransitions);
//        }
////        T.set(n, n, 1);
//    }


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
