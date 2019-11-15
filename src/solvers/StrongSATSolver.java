package solvers;

import automata.AutomatonChecker;
import automata.fsa.*;
import gov.nasa.ltl.graph.Graph;
import gov.nasa.ltl.graph.Node;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import jhoafparser.ast.AtomLabel;
import jhoafparser.ast.BooleanExpression;
import main.Settings;
import modelcounter.AutomataBasedModelCounting;
import modelcounter.Buchi2Graph;
import modelcounter.EmersonLeiAutomatonBasedModelCounting;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import owl.automaton.*;
import owl.automaton.acceptance.*;
import owl.automaton.algorithms.EmptinessCheck;
import owl.automaton.edge.Edge;
import owl.automaton.minimizations.MinimizationUtil;
import owl.automaton.output.HoaPrinter;
import owl.collections.ValuationSet;
import owl.factories.FactorySupplier;
import owl.factories.ValuationSetFactory;
import owl.ltl.LabelledFormula;
import owl.ltl.rewriter.SimplifierFactory;
import owl.run.DefaultEnvironment;
import owl.run.Environment;
import owl.translations.LTL2DAFunction;
import owl.translations.LTL2DAFunction.Constructions;
import owl.translations.LTL2NAFunction;
import owl.translations.delag.DelagBuilder;
import owl.translations.ltl2dpa.LTL2DPAFunction;
import owl.translations.ltl2ldba.AnnotatedLDBA;
import owl.translations.ltl2ldba.AsymmetricLDBAConstruction;
import owl.translations.ltl2ldba.SymmetricLDBAConstruction;
import tlsf.FormulaToAutomaton;
import tlsf.FormulaToRE;

import javax.annotation.Nullable;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.*;

import static com.google.common.base.Preconditions.checkState;
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
        Automaton<S, ?> input_automaton = buildInputAutomata(automaton,new ArrayList<>(formula.player1Variables()), formula.variables());
        System.out.println(HoaPrinter.toString(input_automaton, EnumSet.of(SIMPLE_TRANSITION_LABELS)));
        System.out.println("Checking...");
//        boolean isStronSAT = checkStrongSAT(input_automaton);
//        return isStronSAT;
        FormulaToAutomaton translatorLTLtoRE = new FormulaToAutomaton();
        translatorLTLtoRE.generateLabels(formula.variables());
        System.out.println("Determinizing...");
        automata.Automaton dfa = translatorLTLtoRE.telaToDfa(input_automaton);
        Graph<String> graph = Buchi2Graph.dfaToGraph(dfa);
        AutomataBasedModelCounting counter = new AutomataBasedModelCounting(graph,false);
//        EmersonLeiAutomatonBasedModelCounting counter = new EmersonLeiAutomatonBasedModelCounting(input_automaton);
        System.out.println("Counting...");
        BigInteger numOfModels = counter.count(Settings.MC_BOUND);
        BigInteger expectedNumOfModels =  (BigInteger.valueOf(2).pow(formula.player1Variables().size())).pow(Settings.MC_BOUND);
        return (numOfModels.equals(expectedNumOfModels));

    }

    public <S> MutableAutomaton<S, ?> buildInputAutomata(Automaton<S, EmersonLeiAcceptance> automaton, List<String> inputVars, List<String> vars){
        //create valuation factory
        Environment env = DefaultEnvironment.standard();
        FactorySupplier factorySupplier = env.factorySupplier();
        ValuationSetFactory inputFactory = factorySupplier.getValuationSetFactory(new ArrayList<>(inputVars));

        MutableAutomaton<S,?> input_automaton = MutableAutomatonFactory.create(automaton.acceptance(),inputFactory);

        //set initial state
        input_automaton.initialStates(automaton.initialStates());
        //set all states
        for(S s : automaton.states()) {
            input_automaton.addState(s);
        }
        //set transitions and acceptance
        ValuationSetFactory vsFactory = factorySupplier.getValuationSetFactory(new ArrayList<>(vars));
        for(S from : automaton.states()) {
            vsFactory.universe().forEach(valuation -> {
                //get edge
                Set<Edge<S>> edges = automaton.edges(from, valuation);
                if (edges != null && !edges.isEmpty()) {
                    var input_valuation = inputFactory.of(valuation);
                     for(Edge<S> edge : edges) {

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

    public <S> boolean checkStrongSAT(Automaton<S,?> automaton) {
//        System.out.println(automaton.is(Automaton.Property.COMPLETE));
//        MutableAutomaton<S,?> input_automaton = MutableAutomatonFactory.copy(automaton);
//        System.out.println(input_automaton.is(Automaton.Property.COMPLETE));
//        Automaton<?, ?> minimized_input_automata = MinimizationUtil.minimizeDefault(input_automaton, MinimizationUtil.MinimizationLevel.ALL);
//        System.out.println(HoaPrinter.toString(minimized_input_automata));
//        MutableAutomaton<Object, ?>  complete_input_automata = MutableAutomatonUtil.castMutable(input_automaton, Object.class, OmegaAcceptance.class);
//        complete_input_automata.trim();
//        Optional<Object> completed = MutableAutomatonUtil.complete(complete_input_automata);
//        System.out.println(HoaPrinter.toString(complete_input_automata));



        Automaton<S,?> complete_input_automata = Views.complete(automaton, null);
        Automaton<?, ?> complement_input_automata =  Views.complement(complete_input_automata, null);
        System.out.println(HoaPrinter.toString(complement_input_automata));

        boolean isStrongSAT = EmptinessCheck.isEmpty(complement_input_automata);
//        input_automaton.trim();
//        System.out.println(input_automaton.acceptance());
//        boolean isEmpty = EmptinessCheck.isEmpty(input_automaton);
//        boolean isComplete = input_automaton.is(Automaton.Property.COMPLETE);
//        boolean isStrongSAT = !isEmpty && isComplete;
        return isStrongSAT;
    }



}
