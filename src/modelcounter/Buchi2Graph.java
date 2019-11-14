package modelcounter;

import java.io.IOException;

import automata.State;
import automata.Transition;
import automata.fsa.FiniteStateAutomaton;
import de.uni_luebeck.isp.rltlconv.automata.Nba;
import owl.ltl.LabelledFormula;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.immutable.List;
import scala.collection.immutable.Map;
import scala.collection.immutable.Set;
import scala.collection.immutable.Vector;
import scala.collection.immutable.VectorIterator;
import gov.nasa.ltl.graph.*;
import tlsf.FormulaToAutomaton;
import tlsf.FormulaToRE;

public class Buchi2Graph {
	 
//	public static Graph<String> LTL2Graph(String formula) throws IOException, InterruptedException {
//		Rltlconv_LTLModelCounter translator = new Rltlconv_LTLModelCounter() ;
//		Nba nba = translator.ltl2nba(formula);
//		translator.generateLabels(nba);
//		automata.Automaton dfa = translator.nbaTodfa(nba);
//
////		System.out.println(dfa);
//		return dfaToGraph(dfa);
//	}

	public static Graph<String> LTL2Graph(LabelledFormula formula) throws IOException, InterruptedException {
		FormulaToAutomaton translator = new FormulaToAutomaton();
		translator.generateLabels(formula.variables());
		automata.Automaton dfa = translator.formulaToDfa(formula);
//		System.out.println(dfa);
		return dfaToGraph(dfa);
	}

	public static Graph<String> dfaToGraph (automata.Automaton dfa) {
		Graph<String> g = new Graph<>();
		//Setear estados iniciales.
		State is = dfa.getInitialState();
		if (is == null) //dfa is empty
			return g;
		Node<String> in= getNode(g, is.getName());
		g.setInit(in);
		Transition[] transitions = dfa.getTransitions();

		for(int i = 0; i < transitions.length; i++){
			Transition t = transitions[i];

			State from = t.getFromState();
			Node<String> s1= getNode(g,from.getName());

			//get Label
			String l = t.getDescription();
			Guard<String> label = new Guard<>();
			label.add(new Literal<String>(l, false));

			State to = t.getToState();
			Node<String> s2= getNode(g, to.getName());

			Edge<String> e = new Edge<>(s1, s2, label);
			s1.getOutgoingEdges().add(e);
			s2.getIncomingEdges().add(e);
		}
			
		//add final states
		State[] finalStates = dfa.getFinalStates();
		for(int i = 0; i < finalStates.length; i++){
			State acc = finalStates[i];
			Node<String> s = getNode(g, acc.getName());
			s.setBooleanAttribute("accepting", true);
		}
//		System.out.println(g);
		return g;
	  }
	
	 public static Node<String> getNode(Graph<String> g, String name){
		 for(Node<String> n : g.getNodes()){
			 if (n.getAttributes().getString("_name").equals(name))
				 return n;
		 }
		 Node<String> s = new Node<>(g);
		 s.setStringAttribute("_name", name);
		 s.setBooleanAttribute("accepting", false);
		 return s;
	 }
}
