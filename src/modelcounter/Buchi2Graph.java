package modelcounter;

import automata.State;
import automata.Transition;
import gov.nasa.ltl.graph.*;
import owl.ltl.LabelledFormula;
import tlsf.FormulaToAutomaton;

import java.io.IOException;

public class Buchi2Graph {

    public static Graph<String> LTL2Graph(LabelledFormula formula) throws IOException, InterruptedException {
        FormulaToAutomaton translator = new FormulaToAutomaton();
        translator.generateLabels(formula.variables());
        automata.Automaton dfa = translator.formulaToDfa(formula);
//		System.out.println(dfa);
        return dfaToGraph(dfa);
    }

    public static Graph<String> dfaToGraph(automata.Automaton dfa) {
        Graph<String> g = new Graph<>();
        //Setear estados iniciales.
        State is = dfa.getInitialState();
        if (is == null) //dfa is empty
            return g;
        Node<String> in = getNode(g, is.getName());
        g.setInit(in);
        Transition[] transitions = dfa.getTransitions();

        for (Transition t : transitions) {
            State from = t.getFromState();
            Node<String> s1 = getNode(g, from.getName());

            //get Label
            String l = t.getDescription();
            Guard<String> label = new Guard<>();
            label.add(new Literal<String>(l, false));

            State to = t.getToState();
            Node<String> s2 = getNode(g, to.getName());

            Edge<String> e = new Edge<>(s1, s2, label);
            s1.getOutgoingEdges().add(e);
            s2.getIncomingEdges().add(e);
        }
        //add final states
        State[] finalStates = dfa.getFinalStates();
        for (State acc : finalStates) {
            Node<String> s = getNode(g, acc.getName());
            s.setBooleanAttribute("accepting", true);
        }
        return g;
    }

    public static Node<String> getNode(Graph<String> g, String name) {
        for (Node<String> n : g.getNodes()) {
            if (n.getAttributes().getString("_name").equals(name))
                return n;
        }
        Node<String> s = new Node<>(g);
        s.setStringAttribute("_name", name);
        s.setBooleanAttribute("accepting", false);
        return s;
    }
}
