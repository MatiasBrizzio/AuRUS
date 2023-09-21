package solvers;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import jhoafparser.ast.AtomAcceptance;
import jhoafparser.ast.BooleanExpression;
import main.Settings;
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

import java.util.*;
import java.util.concurrent.*;
import java.util.function.IntConsumer;

public class StrongSatisfiabilityChecker<S> {

    boolean isAcceptance = false;
    boolean noAcceptance = false;
    //    private DMatrixRMaj T = null;
//    private DMatrixRMaj I = null;
    private Automaton<S, EmersonLeiAcceptance> automaton = null;
    //    private List<String> variables = null;
//    private Map<S,Integer> states = null;
    //public static int TIMEOUT = 120;
    private final LabelledFormula formula;
    private List<String> input_vars = null;

    public StrongSatisfiabilityChecker(LabelledFormula formula) {
        this.formula = formula;
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        // Do the call in a separate thread, get a Future back
        Future<String> future = executorService.submit(this::parse);
        try {
            // Wait for at most TIMEOUT seconds until the result is returned
            String result = future.get(Settings.PARSING_TIMEOUT, TimeUnit.SECONDS);
            input_vars = new ArrayList<>(formula.player1Variables());
        } catch (TimeoutException e) {
            System.out.println("StrongSatisfiabilityChecker: TIMEOUT parsing.");
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("StrongSatisfiabilityChecker: ERROR while parsing. " + e.getMessage());
        }
    }

    private String parse() {
        // Convert the ltl formula to an automaton with OWL
        DelagBuilder translator = new DelagBuilder(DefaultEnvironment.standard());
        automaton = (Automaton<S, EmersonLeiAcceptance>) translator.apply(formula);
        return "OK";
    }

    public Boolean checkStrongSatisfiable() {
        if (automaton == null)
            return null;
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        // Do the call in a separate thread, get a Future back
        Future<Boolean> future = executorService.submit(this::isStrongSAT);
        try {
            // Wait for at most TIMEOUT seconds until the result is returned
            Boolean result = future.get(Settings.STRONG_SAT_TIMEOUT, TimeUnit.SECONDS);
            return result;
        } catch (TimeoutException e) {
            System.out.println("StrongSatisfiabilityChecker::isStrongSAT TIMEOUT.");
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("StrongSatisfiabilityChecker::isStrongSAT ERROR. " + e.getMessage());
        }
        return null;
    }

    private boolean isStrongSAT() {
//        BigInteger numOfInputEvents = BigInteger.valueOf(2).pow(input_vars.size());
        List<S> visitedStates = new LinkedList<>();
        List<S> undesiredStates = new LinkedList<>();
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
                for (S succ : automaton.successors(current)) {
                    if (!visitedStates.contains(succ) && !statesToAnalyse.contains(succ))
                        statesToAnalyse.add(succ);
                }
            } else {
                // current is a sink state with no successors
                undesiredStates.add(current); //check if it can be avoided
            }
        }
        System.out.printf("initial states = %d \n", automaton.initialStates().size());
        System.out.printf("states = %d \n", automaton.size());
        System.out.printf("undesiredStates = %d \n", undesiredStates.size());

        if (undesiredStates.contains(automaton.onlyInitialState())) {
            return false;
        }

        //check if there is a (controlable) way to avoid reaching the bad state

//        for (S bad_state : undesiredStates) {
        while (!undesiredStates.isEmpty()) {
            S bad_state = undesiredStates.remove(0);
            if (automaton.initialStates().contains(bad_state)) {
                System.out.print("there");
                return false;
            }
            visitedUndesiredStates.add(bad_state);
            for (S predecessor : automaton.predecessors(bad_state)) {
                if (visitedUndesiredStates.contains(predecessor) || undesiredStates.contains(predecessor))
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
                    undesiredStates.add(predecessor);
                }
            }
        }
        return true;
    }

    public boolean accConditionIsSatisfied(BooleanExpression<AtomAcceptance> acceptanceCondition, IntArrayList acceptanceSets) {
        boolean accConditionSatisfied = false;
        switch (acceptanceCondition.getType()) {
            case EXP_TRUE: {
                accConditionSatisfied = true;
                break;
            }
            case EXP_FALSE:
                break;
            case EXP_ATOM: {
                if (acceptanceCondition.getAtom().getType() == AtomAcceptance.Type.TEMPORAL_INF)
                    accConditionSatisfied = (acceptanceSets.contains(acceptanceCondition.getAtom().getAcceptanceSet()));
                else if (acceptanceCondition.getAtom().getType() == AtomAcceptance.Type.TEMPORAL_FIN) {
                    accConditionSatisfied = !(acceptanceSets.contains(acceptanceCondition.getAtom().getAcceptanceSet()));
                }
                break;
            }
            case EXP_AND: {
                if (accConditionIsSatisfied(acceptanceCondition.getLeft(), acceptanceSets))
                    accConditionSatisfied = accConditionIsSatisfied(acceptanceCondition.getRight(), acceptanceSets);
                break;
            }
            case EXP_OR: {
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
