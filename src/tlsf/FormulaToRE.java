package tlsf;

import automata.fsa.*;
import com.google.common.base.Preconditions;
import gui.environment.Universe;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import jhoafparser.ast.AtomLabel;
import jhoafparser.ast.BooleanExpression;
import owl.automaton.Automaton;
import owl.automaton.AutomatonUtil;
import owl.automaton.acceptance.*;
import owl.automaton.algorithms.EmptinessCheck;
import owl.automaton.edge.Edge;
import owl.automaton.output.HoaPrinter;
import owl.automaton.transformations.RabinDegeneralization;
import owl.collections.ValuationSet;
import owl.factories.FactorySupplier;
import owl.factories.ValuationSetFactory;
import owl.ltl.EquivalenceClass;
import owl.ltl.LabelledFormula;
import owl.run.DefaultEnvironment;
import owl.run.Environment;
import owl.translations.LTL2DAFunction;
import owl.translations.ltl2nba.SymmetricNBAConstruction;
import owl.translations.nba2ldba.NBA2LDBA;

import javax.annotation.Nullable;
import javax.swing.tree.DefaultTreeModel;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.IntConsumer;

import static owl.automaton.output.HoaPrinter.HoaOption.SIMPLE_TRANSITION_LABELS;

public class FormulaToRE {

    //Map labels to ids
    public java.util.Map<String,String> labelIDs = new HashMap<>();
    int base = 48;//start with char 0
    public int encoded_alphabet = -1;
    public int[]state = {48,48};//start with char 0
    public int alphabetSize = 0;
    public FormulaToRE() {
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
            if (encoded_alphabet == -1)
                setLabel(l);
            else
                setLabelEncoded(l);
        });
        System.out.println(labelIDs);
    }

    public <S> String formulaToRegularExpression(LabelledFormula formula){
//        LTL2DAFunction translator = new LTL2DAFunction(DefaultEnvironment.standard(),
//                false, EnumSet.allOf(LTL2DAFunction.Constructions.class));
//        Automaton<?, ? extends OmegaAcceptance> automaton = translator.apply(formula);
        SymmetricNBAConstruction translator = (SymmetricNBAConstruction) SymmetricNBAConstruction.of(DefaultEnvironment.standard(), BuchiAcceptance.class);
        Automaton<S, BuchiAcceptance> automaton = translator.apply(formula);
//        NBA2LDBA nba2dba = new NBA2LDBA();
//        Automaton<S, BuchiAcceptance> automaton = (Automaton<S, BuchiAcceptance>) nba2dba.apply(nba);
        if (automaton.size() == 0)
            return null;
        BitSet acceptanceSets = automaton.acceptance().acceptingSet(); //new HashSet();
//        automaton.states().forEach(s ->
//        	automaton.edgeMap(s).forEach((edge, valuationSet) -> {
//        			edge.acceptanceSetIterator().forEachRemaining((IntConsumer) acceptanceSets::add);}));
        System.out.print(automaton.size()+"("+acceptanceSets+"/"+automaton.initialStates().size()+") ");
        System.out.println(formula+" ");
        System.out.println(HoaPrinter.toString(automaton, EnumSet.of(SIMPLE_TRANSITION_LABELS)));
//        alphabetSize = formula.variables().size();
        return automataToRegularExpression(automaton);
    }
    Object2IntMap stateNumbers = new Object2IntOpenHashMap();
    private <S> int getStateId(@Nullable S state) {
        Preconditions.checkState(state != null);
        int id = this.stateNumbers.computeIntIfAbsent(state, (k) -> {
            return this.stateNumbers.size();
        });
        return id;
    }

    public <S> String automataToRegularExpression(Automaton<S, ? extends OmegaAcceptance> automaton){
    	

        automata.Automaton fsa = new FiniteStateAutomaton();

        //Map nodes to states ids
//        java.util.Map<String,Integer> ids = new HashMap<>();
        stateNumbers = new Object2IntOpenHashMap();
        
        //create one unique initial state
        automata.State is = fsa.createStateWithId(new Point(),-1);
        fsa.setInitialState(is);
        
        //create one unique final state
//        automata.State fs = fsa.createStateWithId(new Point(),-2);
//        fsa.addFinalState(fs);
        
        //get initial nodes
        for(S in : automaton.initialStates()) {
            //create and set initial state
            automata.State ais = fsa.createStateWithId(new Point(),getStateId(in));
            //initial node ids
            FSATransition t = new FSATransition(is, ais, FSAToRegularExpressionConverter.LAMBDA);
            fsa.addTransition(t);
//            ids.put(in.toString(), ais.getID());
            System.out.println("initial: "+ getStateId(in));
        }

        for (S from : automaton.states()) {
            Map<Edge<S>, ValuationSet> edgeMap = automaton.edgeMap(from);
            edgeMap.forEach((edge, valuationSet) -> {
                S to = edge.successor();
                if (!valuationSet.isEmpty()) {

                	valuationSet.forEach(bitSet -> {
	                    //checks if ID exists
//	                    int ID = 0;
	                    automata.State fromState = fsa.getStateWithID(getStateId(from));
	                    if (fromState == null)
	                        fromState = fsa.createStateWithId(new Point(),getStateId(from));
//	                    if (ids.containsKey(from.toString())) {
//	                        int ID = ids.get(from.toString());
//	                        fromState = fsa.getStateWithID(ID);
//	                    } else {
//	                        //create new state
//	                        fromState = fsa.createState(new Point());
//	                        //update ids
//	                        ids.put(from.toString(), fromState.getID());
////	                        ID = fromState.getID();
//	                    }
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

//	                    if (encoded_alphabet == -1)
//	                        setLabel(l);
//	                    else
//	                        setLabelEncoded(l);
	
	                    String label = labelIDs.get(l);

                        System.out.println("from: "+ getStateId(from) + " label:"+ l + "("+label+") to:" + getStateId(to));
	                    //check if toState exists
	                    automata.State toState = fsa.getStateWithID(getStateId(to));
                        if (toState == null)
                            toState = fsa.createStateWithId(new Point(),getStateId(to));

//	                    if (ids.containsKey(to.toString())) {
//	                        int ID = ids.get(to.toString());
//	                        toState = fsa.getStateWithID(ID);
//	                    } else {
//	                        //create new state
//	                        toState = fsa.createState(new Point());
//	                        //update ids
//	                        ids.put(to.toString(), toState.getID());
////	                        ID = toState.getID();
//	                    }

	                    //add transition
	                    FSATransition t = new FSATransition(fromState, toState, label);
	                    fsa.addTransition(t);
                	});
                }
            

//              IntArrayList acceptanceSets = new IntArrayList();
//              edge.acceptanceSetIterator().forEachRemaining((IntConsumer) acceptanceSets::add);
	            //add final states
	            if (edge.acceptanceSetIterator().hasNext()) {
	                //get state
//	                int finalID = ids.get(to.toString());
	                automata.State as = fsa.getStateWithID(getStateId(to));
	              //add transition
//                    FSATransition t = new FSATransition(as, fs, FSAToRegularExpressionConverter.LAMBDA);
//                    fsa.addTransition(t);
	                fsa.addFinalState(as);
	            }
            });
        }
//        System.out.print("n");
//        System.out.println(fsa.toString());
        NFAToDFA determinizer = new NFAToDFA();
        automata.Automaton dfa = determinizer.convertToDFA((automata.Automaton)fsa.clone());
        System.out.println(dfa.toString());
        
        Minimizer min = new Minimizer();
//        automata.Automaton dfa_minimized = (automata.Automaton) dfa.clone();
//        boolean isminimized = false;
//        while (!isminimized) {
        	min.initializeMinimizer();
        	automata.Automaton to_minimize = min.getMinimizeableAutomaton((automata.Automaton) dfa.clone());
        	DefaultTreeModel tree = min.getDistinguishableGroupsTree(to_minimize);
        	automata.Automaton dfa_minimized = min.getMinimumDfa(to_minimize, tree);
//        	isminimized = min.isMinimized(dfa_minimized, tree);
//        }
        	System.out.println(dfa_minimized.toString());	
        FSAToRegularExpressionConverter.convertToSimpleAutomaton(dfa_minimized);
        System.out.println(dfa_minimized.toString());
//        System.out.print("f");
        String re = FSAToRegularExpressionConverter.convertToRegularExpression(dfa_minimized);
        System.out.println(re);
        return re;
    }

//    public static <S> Set<S> finalStates(Automaton<S, ?> automaton) {
//        OmegaAcceptance acceptance = automaton.acceptance();
//
//        if (acceptance instanceof AllAcceptance) {
//            return automaton.states();
//        }
//
//        if (acceptance instanceof BuchiAcceptance) {
//            Automaton<S, BuchiAcceptance> casted = AutomatonUtil.cast(automaton, BuchiAcceptance.class);
//            casted.acceptance().
//      /* assert Buchi.containsAcceptingLasso(casted, initialState)
//        == Buchi.containsAcceptingScc(casted, initialState); */
//            return !EmptinessCheck.Buchi.containsAcceptingLasso(casted, initialState);
//        }
//
//        if (acceptance instanceof GeneralizedBuchiAcceptance) {
//            Automaton<S, GeneralizedBuchiAcceptance> casted = AutomatonUtil.cast(automaton,
//                    GeneralizedBuchiAcceptance.class);
//
//            return !EmptinessCheck.Buchi.containsAcceptingScc(casted, initialState);
//        }
//
//        if (acceptance instanceof NoneAcceptance) {
//            return true;
//        }
//
//        if (acceptance instanceof ParityAcceptance) {
//            Automaton<S, ParityAcceptance> casted = AutomatonUtil.cast(automaton, ParityAcceptance.class);
//
//      /* assert Parity.containsAcceptingLasso(casted, initialState)
//        == Parity.containsAcceptingScc(casted, initialState); */
//            return !EmptinessCheck.Parity.containsAcceptingLasso(casted, initialState);
//        }
//
//        if (acceptance instanceof RabinAcceptance) {
//            Automaton<S, RabinAcceptance> casted = AutomatonUtil.cast(automaton, RabinAcceptance.class);
//
//      /* assert Rabin.containsAcceptingLasso(casted, initialState)
//        == Rabin.containsAcceptingScc(casted, initialState); */
//            return !EmptinessCheck.Rabin.containsAcceptingLasso(casted, initialState);
//        }
//
//        if (acceptance instanceof GeneralizedRabinAcceptance) {
//            Automaton<S, GeneralizedRabinAcceptance> casted = AutomatonUtil.cast(automaton,
//                    GeneralizedRabinAcceptance.class);
//
//            return isEmpty(RabinDegeneralization.degeneralize(casted));
//        }
//
//        throw new UnsupportedOperationException(
//                String.format("Emptiness check for %s not yet implemented.", acceptance.name()));
//    }



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
