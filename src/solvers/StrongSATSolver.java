package solvers;

import gov.nasa.ltl.graph.Graph;
import main.Settings;
import modelcounter.AutomataBasedModelCounting;
import modelcounter.Buchi2Graph;
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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static owl.automaton.output.HoaPrinter.HoaOption.SIMPLE_TRANSITION_LABELS;

public class StrongSATSolver<S> {


    public <S> boolean isStrongSatisfiable(LabelledFormula formula) {
//        System.out.println("Simplifying...");
//        LabelledFormula formula = SimplifierFactory.apply(input, SimplifierFactory.Mode.SYNTACTIC_FIXPOINT);

        System.out.println("Parsing...");
//        LTL2NAFunction translator = new LTL2NAFunction(DefaultEnvironment.standard(), EnumSet.of(LTL2NAFunction.Constructions.BUCHI));
//        LTL2DPAFunction translator = new LTL2DPAFunction(DefaultEnvironment.standard(), LTL2DPAFunction.RECOMMENDED_ASYMMETRIC_CONFIG);
//        Automaton<S, ParityAcceptance> automaton = (Automaton<S, ParityAcceptance>)translator.apply(formula);
        //LTL2DAFunction translator = new LTL2DAFunction(DefaultEnvironment.standard(), false, EnumSet.of(Constructions.EMERSON_LEI));//allOf(Constructions.class));
        DelagBuilder translator = new DelagBuilder(DefaultEnvironment.standard());
        Automaton<S, EmersonLeiAcceptance> automaton = (Automaton<S, EmersonLeiAcceptance>) translator.apply(formula);
//        SymmetricLDBAConstruction translator = SymmetricLDBAConstruction.of(DefaultEnvironment.standard(), BuchiAcceptance.class);
//        AnnotatedLDBA automaton = translator.apply(formula);
        System.out.println(HoaPrinter.toString(automaton, EnumSet.of(SIMPLE_TRANSITION_LABELS)));

//        System.out.println(HoaPrinter.toString(automaton));
        System.out.println("Building Input...");
        Automaton<S, ?> input_automaton = buildInputAutomata(automaton, new ArrayList<>(formula.player1Variables()), formula.variables());
        System.out.println(HoaPrinter.toString(input_automaton, EnumSet.of(SIMPLE_TRANSITION_LABELS)));
        System.out.println("Checking...");
//        boolean isStronSAT = checkStrongSAT(input_automaton);
//        return isStronSAT;
        FormulaToAutomaton translatorLTLtoRE = new FormulaToAutomaton();
        translatorLTLtoRE.generateLabels(formula.variables());
        System.out.println("Determinizing...");
        automata.Automaton dfa = translatorLTLtoRE.telaToDfa(input_automaton);
        Graph<String> graph = Buchi2Graph.dfaToGraph(dfa);
        AutomataBasedModelCounting counter = new AutomataBasedModelCounting(graph, false);
//        EmersonLeiAutomatonBasedModelCounting counter = new EmersonLeiAutomatonBasedModelCounting(input_automaton);
        System.out.println("Counting...");
        BigInteger numOfModels = counter.count(Settings.MC_BOUND);
        BigInteger expectedNumOfModels = (BigInteger.valueOf(2).pow(formula.player1Variables().size())).pow(Settings.MC_BOUND);
        return (numOfModels.equals(expectedNumOfModels));

    }

    public <S> MutableAutomaton<S, ?> buildInputAutomata(Automaton<S, EmersonLeiAcceptance> automaton, List<String> inputVars, List<String> vars) {
        //create valuation factory
        Environment env = DefaultEnvironment.standard();
        FactorySupplier factorySupplier = env.factorySupplier();
        ValuationSetFactory inputFactory = factorySupplier.getValuationSetFactory(new ArrayList<>(inputVars));

        MutableAutomaton<S, ?> input_automaton = MutableAutomatonFactory.create(automaton.acceptance(), inputFactory);

        //set initial state
        input_automaton.initialStates(automaton.initialStates());
        //set all states
        for (S s : automaton.states()) {
            input_automaton.addState(s);
        }
        //set transitions and acceptance
        ValuationSetFactory vsFactory = factorySupplier.getValuationSetFactory(new ArrayList<>(vars));
        for (S from : automaton.states()) {
            vsFactory.universe().forEach(valuation -> {
                //get edge
                Set<Edge<S>> edges = automaton.edges(from, valuation);
                if (!edges.isEmpty()) {
                    var input_valuation = inputFactory.of(valuation);
                    for (Edge<S> edge : edges) {

                        input_automaton.addEdge(from, input_valuation, edge);
//                         input_valuation.forEach(bitset -> input_automaton.addEdge(from, bitset, edge));
                    }
                }
            });
        }
        input_automaton.trim();
//        System.out.println(HoaPrinter.toString(input_automaton));
        return input_automaton;

    }

    public <S> boolean checkStrongSAT(Automaton<S, ?> automaton) {
        Automaton<S, ?> complete_input_automata = Views.complete(automaton, null);
        Automaton<?, ?> complement_input_automata = Views.complement(complete_input_automata, null);
        System.out.println(HoaPrinter.toString(complement_input_automata));
        return EmptinessCheck.isEmpty(complement_input_automata);
    }


}
