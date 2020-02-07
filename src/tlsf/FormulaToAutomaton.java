package tlsf;

import automata.fsa.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import jhoafparser.ast.AtomAcceptance;
import jhoafparser.ast.AtomLabel;
import jhoafparser.ast.BooleanExpression;
import owl.automaton.Automaton;
import owl.automaton.MutableAutomaton;
import owl.automaton.acceptance.*;
import owl.automaton.edge.Edge;
import owl.automaton.output.HoaPrinter;
import owl.collections.ValuationSet;
import owl.factories.FactorySupplier;
import owl.factories.ValuationSetFactory;
import owl.ltl.LabelledFormula;
import owl.run.DefaultEnvironment;
import owl.run.Environment;
import owl.translations.LTL2DAFunction;
import owl.translations.LTL2DAFunction.Constructions;
import owl.translations.LTL2NAFunction;
import owl.translations.delag.DelagBuilder;
import owl.translations.delag.State;
import owl.translations.ltl2dpa.LTL2DPAFunction;
import owl.translations.ltl2nba.SymmetricNBAConstruction;

import javax.annotation.Nullable;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.IntConsumer;

import static com.google.common.base.Preconditions.checkState;
import static owl.automaton.output.HoaPrinter.HoaOption.SIMPLE_TRANSITION_LABELS;

public class FormulaToAutomaton<S> {

    //Map labels to ids
    public Map<String,String> labelIDs = new HashMap<>();
    int base = 48;//start with char 0
    public int encoded_alphabet = -1;
    public int[]state = {48,48};//start with char 0
    public int alphabetSize = 0;
    public FormulaToAutomaton() {
        base = 48;
        labelIDs.clear();
        encoded_alphabet = -1;
        alphabetSize = 0;
    }

    public void generateLabels(List<String> variables) {
        if (!labelIDs.isEmpty())
            return;
        Environment env = DefaultEnvironment.standard();
        FactorySupplier factorySupplier = env.factorySupplier();
        ValuationSetFactory vsFactory = factorySupplier.getValuationSetFactory(variables);
        alphabetSize = variables.size();
        vsFactory.universe().forEach(bitSet -> {
            //get Label
            List<BooleanExpression<AtomLabel>> conjuncts = new ArrayList<>(alphabetSize);
            for (int i = 0; i < alphabetSize; i++) {
                BooleanExpression<AtomLabel> atom = new BooleanExpression<>(AtomLabel.createAPIndex(i));

                if (bitSet.get(i)) {
                    conjuncts.add(atom);
                } else {
                    conjuncts.add(atom.not());
                }
            }
            String l = BooleanExpressions.createConjunction(conjuncts).toString();

            if(variables.size()>5 && variables.size()<12)
                encoded_alphabet = 0;
            else if(variables.size() >=12)
                encoded_alphabet = 1;

            if (encoded_alphabet == -1)
                setLabel(l);
            else
                setLabelEncoded(l);
        });
//        System.out.println(labelIDs);
    }



    public <S> automata.Automaton formulaToDfa(LabelledFormula formula){
//        SymmetricNBAConstruction translator = (SymmetricNBAConstruction) SymmetricNBAConstruction.of(DefaultEnvironment.standard(), BuchiAcceptance.class);
//        System.out.println(formula);
//        var automaton = translator.apply(formula);
//        LTL2NAFunction translator = new LTL2NAFunction(DefaultEnvironment.standard(), EnumSet.of(LTL2NAFunction.Constructions.GENERALIZED_BUCHI));//class));
//        Automaton<?, ? extends OmegaAcceptance> automaton = translator.apply(formula);
//        LTL2DAFunction translator = new LTL2DAFunction(DefaultEnvironment.standard(),false, EnumSet.allOf(LTL2DAFunction.Constructions.class));
//        Automaton<?, ? extends OmegaAcceptance> automaton = translator.apply(formula);
//        if (automaton.size() == 0)
//            return null;
//        System.out.println(automaton.acceptance().acceptingSet());
//        System.out.println(automaton.acceptance().booleanExpression());
//        System.out.println(HoaPrinter.toString(automaton, EnumSet.of(SIMPLE_TRANSITION_LABELS)));
//        System.out.println(HoaPrinter.toString(automaton));
//        return nbaToDfa(automaton);
//        System.out.println("Parsing...");
//        DelagBuilder translator = new DelagBuilder(DefaultEnvironment.standard());
//        Automaton<State<Object>, EmersonLeiAcceptance> automaton = translator.apply(formula);
//        return telaToDfa(automaton);

        var environment = DefaultEnvironment.standard();
		var translator = new LTL2DPAFunction(environment, LTL2DPAFunction.RECOMMENDED_SYMMETRIC_CONFIG);

		var automaton = (Automaton<S, ParityAcceptance>) translator.apply(formula);
		return PAtoDfa(automaton);
    }

    private Object2IntMap stateNumbers;
    private <S> int getStateId(@Nullable S state) {
        checkState(state != null);
        return stateNumbers.computeIntIfAbsent(state, k -> stateNumbers.size());
    }

    public <S> automata.Automaton nbaToDfa(Automaton<S, ? extends OmegaAcceptance> automaton){

        automata.Automaton fsa = new FiniteStateAutomaton();
        stateNumbers = new Object2IntOpenHashMap();
         //Map nodes to states ids
        Map<S,automata.State> ids = new HashMap<>();
        for (S s : automaton.states()) {
            int id = getStateId(s);
            automata.State state = fsa.createStateWithId(new Point(),id);
            ids.put(s, state);
        }

        int N = automaton.size();
        //create one unique initial state
        automata.State is = fsa.createStateWithId(new Point(),N+1);
        fsa.setInitialState(is);

        //create one unique final state
        automata.State fs = fsa.createStateWithId(new Point(),N+2);
        fsa.addFinalState(fs);

        //get initial nodes
        for(S in : automaton.initialStates()) {
            //create and set initial state
//            automata.State ais = fsa.createStateWithId(new Point(),getStateId(in));
            automata.State ais = ids.get(in);
            //initial node ids
            FSATransition t = new FSATransition(is, ais, FSAToRegularExpressionConverter.LAMBDA);
            fsa.addTransition(t);
//            ids.put(in.toString(), ais.getID());
//            System.out.println("initial: "+ in.toString());
        }

        for (S from : automaton.states()) {
            Map<Edge<S>, ValuationSet> edgeMap = automaton.edgeMap(from);
            edgeMap.forEach((edge, valuationSet) -> {
                S to = edge.successor();
                if (!valuationSet.isEmpty()) {
                    valuationSet.forEach(bitSet -> {
                        //checks if ID exists
                        automata.State fromState = ids.get(from);

                        //get Label
                        List<BooleanExpression<AtomLabel>> conjuncts = new ArrayList<>(alphabetSize);
                        for (int i = 0; i < alphabetSize; i++) {
                            BooleanExpression<AtomLabel> atom = new BooleanExpression<>(AtomLabel.createAPIndex(i));

                            if (bitSet.get(i)) {
                                conjuncts.add(atom);
                            } else {
                                conjuncts.add(atom.not());
                            }
                        }
                        String l = BooleanExpressions.createConjunction(conjuncts).toString();
                        String label = labelIDs.get(l);

                        //check if toState exists
                        automata.State toState = ids.get(to);

                        FSATransition t = new FSATransition(fromState, toState, label);
                        fsa.addTransition(t);


                        if (edge.acceptanceSetIterator().hasNext()) {
//                            IntArrayList acceptanceSets = new IntArrayList();
//                            edge.acceptanceSetIterator().forEachRemaining((IntConsumer) acceptanceSets::add);
//                            int toID = getStateId(to);
//                            if (automaton.acceptance().acceptingSet().get(toID)) {
                                //get state
                                //automata.State as = ids.get(to);
                                //add transition
                                FSATransition final_t = new FSATransition(fromState, fs, label);
                                fsa.addTransition(final_t);
//                            }
                        }
                    });

//              IntArrayList acceptanceSets = new IntArrayList();
//              edge.acceptanceSetIterator().forEachRemaining((IntConsumer) acceptanceSets::add);
                //add final states
//                if (edge.acceptanceSetIterator().hasNext()) {
//                    //get state
//                    automata.State as = ids.get(to);
//                    //add transition
//                    FSATransition t = new FSATransition(as, fs, FSAToRegularExpressionConverter.LAMBDA);
//                    fsa.addTransition(t);
//                    fsa.addFinalState(as);
                }
            });
        }
//        System.out.print("n");
//        System.out.println(fsa.toString());
        NFAToDFA determinizer = new NFAToDFA();
        automata.Automaton dfa = determinizer.convertToDFA((automata.Automaton)fsa.clone());
//        System.out.println(dfa.toString());

        Minimizer min = new Minimizer();
        min.initializeMinimizer();
        automata.Automaton to_minimize = min.getMinimizeableAutomaton((automata.Automaton) dfa.clone());
        DefaultTreeModel tree = min.getDistinguishableGroupsTree(to_minimize);
        automata.Automaton dfa_minimized = min.getMinimumDfa(to_minimize, tree);
//        System.out.println(dfa_minimized.toString());
        return dfa_minimized;
    }

    public <S> automata.Automaton telaToDfa(Automaton<S, EmersonLeiAcceptance> automaton){
        System.out.println("Building DFA...");
        automata.Automaton fsa = new FiniteStateAutomaton();
        stateNumbers = new Object2IntOpenHashMap();
        //Map nodes to states ids
        Map<S,automata.State> ids = new HashMap<>();
        for (S s : automaton.states()) {
            int id = getStateId(s);
            automata.State state = fsa.createStateWithId(new Point(),id);
            ids.put(s, state);
        }

        int N = automaton.size();
        //create one unique initial state
        automata.State is = fsa.createStateWithId(new Point(),N+1);
        fsa.setInitialState(is);

        //create one unique final state
        automata.State fs = fsa.createStateWithId(new Point(),N+2);
        fsa.addFinalState(fs);

        //get initial nodes
        for(S in : automaton.initialStates()) {
            //create and set initial state
//            automata.State ais = fsa.createStateWithId(new Point(),getStateId(in));
            automata.State ais = ids.get(in);
            //initial node ids
            FSATransition t = new FSATransition(is, ais, FSAToRegularExpressionConverter.LAMBDA);
            fsa.addTransition(t);
//            ids.put(in.toString(), ais.getID());
//            System.out.println("initial: "+ in.toString());
        }

        for (S from : automaton.states()) {
            Map<Edge<S>, ValuationSet> edgeMap = automaton.edgeMap(from);
            edgeMap.forEach((edge, valuationSet) -> {
                S to = edge.successor();
                if (!valuationSet.isEmpty()) {
                    valuationSet.forEach(bitSet -> {
                        //checks if ID exists
                        automata.State fromState = ids.get(from);

                        //get Label
                        List<BooleanExpression<AtomLabel>> conjuncts = new ArrayList<>(alphabetSize);
                        for (int i = 0; i < alphabetSize; i++) {
                            BooleanExpression<AtomLabel> atom = new BooleanExpression<>(AtomLabel.createAPIndex(i));

                            if (bitSet.get(i)) {
                                conjuncts.add(atom);
                            } else {
                                conjuncts.add(atom.not());
                            }
                        }
                        String l = BooleanExpressions.createConjunction(conjuncts).toString();
                        String label = labelIDs.get(l);

                        //check if toState exists
                        automata.State toState = ids.get(to);

                        FSATransition t = new FSATransition(fromState, toState, label);
                        fsa.addTransition(t);

                        //check if it is an acceptance transition
                        IntArrayList acceptanceSets = new IntArrayList();
                        if (edge.acceptanceSetIterator().hasNext())
                            edge.acceptanceSetIterator().forEachRemaining((IntConsumer) acceptanceSets::add);
                        if (accConditionIsSatisfied(automaton.acceptance().booleanExpression(), acceptanceSets)) {
                            FSATransition final_t = new FSATransition(fromState, fs, label);
                            fsa.addTransition(final_t);
                        }
                    });

//              IntArrayList acceptanceSets = new IntArrayList();
//              edge.acceptanceSetIterator().forEachRemaining((IntConsumer) acceptanceSets::add);
                    //add final states
//                if (edge.acceptanceSetIterator().hasNext()) {
//                    //get state
//                    automata.State as = ids.get(to);
//                    //add transition
//                    FSATransition t = new FSATransition(as, fs, FSAToRegularExpressionConverter.LAMBDA);
//                    fsa.addTransition(t);
//                    fsa.addFinalState(as);
                }
            });
        }
//        System.out.print("n");
//        System.out.println(fsa.toString());
        System.out.println("Determinizing ...");
        NFAToDFA determinizer = new NFAToDFA();
        automata.Automaton dfa = determinizer.convertToDFA((automata.Automaton)fsa.clone());
//        System.out.println(dfa.toString());

        Minimizer min = new Minimizer();
        min.initializeMinimizer();
        automata.Automaton to_minimize = min.getMinimizeableAutomaton((automata.Automaton) dfa.clone());
        DefaultTreeModel tree = min.getDistinguishableGroupsTree(to_minimize);
        automata.Automaton dfa_minimized = min.getMinimumDfa(to_minimize, tree);
//        System.out.println(dfa_minimized.toString());
        return dfa_minimized;
    }

    public <S> automata.Automaton PAtoDfa(Automaton<S, ParityAcceptance> automaton){
//        System.out.println("Building DFA...");
        automata.Automaton fsa = new FiniteStateAutomaton();
        stateNumbers = new Object2IntOpenHashMap();
        //Map nodes to states ids
        Map<S,automata.State> ids = new HashMap<>();
        for (S s : automaton.states()) {
            int id = getStateId(s);
            automata.State state = fsa.createStateWithId(new Point(),id);
            ids.put(s, state);
        }

        int N = automaton.size();
        //create one unique initial state
        automata.State is = fsa.createStateWithId(new Point(),N+1);
        fsa.setInitialState(is);

        //create one unique final state
        automata.State fs = fsa.createStateWithId(new Point(),N+2);
        fsa.addFinalState(fs);

        //get initial nodes
        for(S in : automaton.initialStates()) {
            //create and set initial state
//            automata.State ais = fsa.createStateWithId(new Point(),getStateId(in));
            automata.State ais = ids.get(in);
            //initial node ids
            FSATransition t = new FSATransition(is, ais, FSAToRegularExpressionConverter.LAMBDA);
            fsa.addTransition(t);
//            ids.put(in.toString(), ais.getID());
//            System.out.println("initial: "+ in.toString());
        }

        for (S from : automaton.states()) {
            Map<Edge<S>, ValuationSet> edgeMap = automaton.edgeMap(from);
            edgeMap.forEach((edge, valuationSet) -> {
                S to = edge.successor();
                if (!valuationSet.isEmpty()) {
                    valuationSet.forEach(bitSet -> {
                        //checks if ID exists
                        automata.State fromState = ids.get(from);

                        //get Label
                        List<BooleanExpression<AtomLabel>> conjuncts = new ArrayList<>(alphabetSize);
                        for (int i = 0; i < alphabetSize; i++) {
                            BooleanExpression<AtomLabel> atom = new BooleanExpression<>(AtomLabel.createAPIndex(i));

                            if (bitSet.get(i)) {
                                conjuncts.add(atom);
                            } else {
                                conjuncts.add(atom.not());
                            }
                        }
                        String l = BooleanExpressions.createConjunction(conjuncts).toString();
                        String label = labelIDs.get(l);

                        //check if toState exists
                        automata.State toState = ids.get(to);

                        FSATransition t = new FSATransition(fromState, toState, label);
                        fsa.addTransition(t);

                        //check if it is an acceptance transition
                        IntArrayList acceptanceSets = new IntArrayList();
                        if (edge.acceptanceSetIterator().hasNext())
                            edge.acceptanceSetIterator().forEachRemaining((IntConsumer) acceptanceSets::add);
                        if (accConditionIsSatisfied(automaton.acceptance().booleanExpression(), acceptanceSets)) {
                            FSATransition final_t = new FSATransition(fromState, fs, label);
                            fsa.addTransition(final_t);
                        }
                    });

//              IntArrayList acceptanceSets = new IntArrayList();
//              edge.acceptanceSetIterator().forEachRemaining((IntConsumer) acceptanceSets::add);
                    //add final states
//                if (edge.acceptanceSetIterator().hasNext()) {
//                    //get state
//                    automata.State as = ids.get(to);
//                    //add transition
//                    FSATransition t = new FSATransition(as, fs, FSAToRegularExpressionConverter.LAMBDA);
//                    fsa.addTransition(t);
//                    fsa.addFinalState(as);
                }
            });
        }
//        System.out.print("n");
//        System.out.println(fsa.toString());
//        System.out.println("Determinizing ...");
//        NFAToDFA determinizer = new NFAToDFA();
//        automata.Automaton dfa = determinizer.convertToDFA((automata.Automaton)fsa.clone());
//        System.out.println(dfa.toString());

//        Minimizer min = new Minimizer();
//        min.initializeMinimizer();
//        automata.Automaton to_minimize = min.getMinimizeableAutomaton((automata.Automaton) dfa.clone());
//        DefaultTreeModel tree = min.getDistinguishableGroupsTree(to_minimize);
//        automata.Automaton dfa_minimized = min.getMinimumDfa(to_minimize, tree);
//        System.out.println(dfa_minimized.toString());
        return fsa;
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

    public void setLabel(String l) throws RuntimeException{
        if(labelIDs.containsKey(l)){
            return;
        }

        labelIDs.put(l, ""+Character.toChars(base)[0]);

        //update base
        if(base==57)
            base = 65; //jump to A
        else if (base == 90)
            base = 97; //jump to a
        else
            base++;

        if(base > 122)
            throw new RuntimeException("Maximum number of characters reached.");

    }


    public void setLabelEncoded(String l) throws RuntimeException{
        if(labelIDs.containsKey(l)){
            return;
        }
        String label = "";
        if(encoded_alphabet==1)
            label += Character.toChars(state[1])[0];
        label += Character.toChars(state[0])[0];
        label += Character.toChars(base)[0];
        labelIDs.put(l, label);

        //update base
        if(base==57)
            base = 65; //jump to A
        else if (base == 90)
            base = 97; //jump to a
        else
            base++;

        if(base > 122){
            base = 48;

            //update state[0]
            if(state[0]==57)
                state[0] = 65; //jump to A
            else if (state[0] == 90)
                state[0] = 97; //jump to a
            else
                state[0]++;

            if(state[0] > 122){
                state[0] = 48;

                //update state[1]
                if(state[1]==57)
                    state[1] = 65; //jump to A
                else if (state[1] == 90)
                    state[1] = 97; //jump to a
                else
                    state[1]++;

                if(state[1] > 122)
                    throw new RuntimeException("Maximum number of characters reached.");
            }
        }
    }
}
