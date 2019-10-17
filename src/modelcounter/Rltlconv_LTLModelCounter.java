package modelcounter;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import javax.management.RuntimeErrorException;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import automata.AutomatonChecker;
import automata.fsa.FSAToRegularExpressionConverter;
import automata.fsa.FSATransition;
import automata.fsa.FiniteStateAutomaton;
import automata.fsa.MinimizeTreeNode;
import automata.fsa.Minimizer;
import automata.fsa.NFAToDFA;
import de.uni_luebeck.isp.buchi.BuchiAutomaton;
import de.uni_luebeck.isp.buchi.Transition;
import de.uni_luebeck.isp.rltlconv.automata.Apa;
import de.uni_luebeck.isp.rltlconv.automata.DirectedState;
import de.uni_luebeck.isp.rltlconv.automata.Nba;
import de.uni_luebeck.isp.rltlconv.automata.Nfa;
import de.uni_luebeck.isp.rltlconv.automata.Sign;
import de.uni_luebeck.isp.rltlconv.automata.State;
import de.uni_luebeck.isp.rltlconv.cli.Conversion;
import de.uni_luebeck.isp.rltlconv.cli.Main;
import de.uni_luebeck.isp.rltlconv.cli.RltlConv;
import de.uni_luebeck.isp.rltlconv.formula.Formula;
import regular.Discretizer;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.immutable.List;
import scala.collection.immutable.Map;
import scala.collection.immutable.Set;
import scala.collection.immutable.Vector;
import scala.collection.immutable.VectorIterator;
import scala.math.Ordering;
import de.uni_luebeck.isp.rltlconv.cli.Conversion.ConversionVal;
import scala.collection.immutable.VectorIterator;


public class Rltlconv_LTLModelCounter {

	public int TIMEOUT = 180;
	//Map labels to ids
	public java.util.Map<String,String> labelIDs ;
	
	public Rltlconv_LTLModelCounter() {
		base = 48;
		labelIDs = new HashMap<>();
		encoded_alphabet = -1;
	}
	
	public void generateLabels(Nba nba) {
        if (!labelIDs.isEmpty())
            throw new RuntimeException("labelsID not empty.");
        Iterator<String> it = nba.alphabet().iterator();
        while(it.hasNext()){
        	String l = it.next();
        	if (encoded_alphabet == -1)
                setLabel(l);
            else
                setLabelEncoded(l);
        }
//        System.out.println(labelIDs);
	}
	
	public String ltl2RE(String formula) throws IOException, InterruptedException {
//		System.out.println(formula);
//		Nfa nfa = ltl2dfa(formula);
//		String re = automata2RE(nfa);
		Nba nba = ltl2nba(formula);
		generateLabels(nba);
		String re = automata2RE(nba);
		return re;
	}
	
	public Nfa ltl2dfa(String formula) throws IOException, InterruptedException{
		ConversionVal[] conv = {Conversion.FORMULA(),Conversion.PROPS(),Conversion.NBA(), Conversion.MIN(), Conversion.NFA(), Conversion.DFA()};
		Nfa dfa = (Nfa) RltlConv.convert(formula, conv);
//		System.out.println(dfa);
		return dfa.toNamedNfa();
	}
	
//	public Nba ltl2nba(String formula) throws IOException, InterruptedException{
//		ConversionVal[] conv = {Conversion.FORMULA(),Conversion.PROPS(),Conversion.APA(),Conversion.NBA()};
////		String [] conv = {"--formula","--props", "--apa", "--nba", "--min"};
//		Nba nba = (Nba) RltlConv.convert(formula, conv);
////		System.out.println(dfa);
//		return nba.toMinimizedNba().toNamedNba(true);
//	}
	
	private void writeFile(String fname,String text) throws IOException{
//		BufferedWriter output = null;
//        try {
        	FileOutputStream output = new FileOutputStream(fname);
//        	writer.close();
//        	File file = new File(fname);
//            output = new BufferedWriter(new FileWriter(file));
            output.write(text.getBytes());
            output.flush();
            output.close();
//        } catch ( IOException e ) {
//            e.printStackTrace();
//        } finally {
//          if ( output != null ) {
//            output.close();
//          }
//        }
	}
	
	private void runCommand(String cmd) throws IOException, InterruptedException{
		Process p = Runtime.getRuntime().exec(cmd);
		
    	boolean timeout = false;
		if(!p.waitFor(TIMEOUT, TimeUnit.SECONDS)) {
		    timeout = true; //kill the process. 
			p.destroy(); // consider using destroyForcibly instead
		}
		
		if (timeout)
			throw new RuntimeException("TIMEOUT reached in RltlConv translation");
		
		InputStream in = p.getInputStream();
    	InputStreamReader inread = new InputStreamReader(in);
    	BufferedReader bufferedreader = new BufferedReader(inread);
    	String aux;
    	String out = "";
	    while ((aux = bufferedreader.readLine()) != null) {
	    	out += aux+"\n";
	    }
	 // Close the InputStream
    	bufferedreader.close();
    	inread.close();
    	in.close();
    	
	    if(out!="")
	    	writeFile("rltlconv-out.txt",out);
	    
	 // Leer el error del programa.
    	InputStream err = p.getErrorStream();
    	InputStreamReader errread = new InputStreamReader(err);
    	BufferedReader errbufferedreader = new BufferedReader(errread);
    	
	    while ((aux = errbufferedreader.readLine()) != null) {
	    	System.out.println("ERR: " + aux);
	    }
	    // Close the ErrorStream
   		errbufferedreader.close();
   		errread.close();
   		err.close();
	    // Check for failure
		if (p.waitFor() != 0) {
			System.out.println("exit value = " + p.exitValue());
			throw new RuntimeException("ERROR in RltlConv translation.");
		}
  		
   		if (p!=null) {
//   			InputStream is = p.getInputStream();
 //  			InputStream es = p.getErrorStream();
  			OutputStream os = p.getOutputStream();
//   				if (is!=null) is.close();
//   				if (es!=null) es.close();
			if (os!=null) os.close();
			p.destroy();
   		}
	}
	public Nba ltl2nba(String formula) throws IOException, InterruptedException{
		//write results to file
			String fname = "rltlconv.txt";
			
			writeFile(fname,formula);
			
			runCommand("./rltlconv.sh @rltlconv.txt --props --formula --apa --nba --min");
			
			Object res = Main.load("@rltlconv-out.txt");
			
			Nba nba = (Nba) RltlConv.convert(res, Conversion.NBA());
			return nba;
	}
	
	public String automata2RE(Nfa ltl_fsa){
		
		FiniteStateAutomaton fsa = new FiniteStateAutomaton();
	
		//Map nodes to states ids
		java.util.Map<String,Integer> ids = new HashMap<>();
		
		//get initial node
		//create one unique initial state
//        automata.State is = fsa.createStateWithId(new Point(),-1);
//        fsa.setInitialState(is);
//        Iterator<State> init_it = ltl_fsa.start().iterator();
//        while(init_it.hasNext()) {
//        	State in = init_it.next(); 
//        	 //create and set initial state
//            automata.State ais = fsa.createState(new Point());
//            //initial node ids
//    		ids.put(in.name(), ais.getID());
//            //initial node ids
//            FSATransition t = new FSATransition(is, ais, FSAToRegularExpressionConverter.LAMBDA);
//            fsa.addTransition(t);
//            System.out.println("initial: "+ in.name());
//        }
		
		//get initial node
		if(ltl_fsa.start().size() > 1) throw new RuntimeException("automata2RE: more than 1 initial state found");
		State in = ltl_fsa.start().head(); //CUIDADO:que pasa si tenemos varios estados iniciales.

		//create and set initial state
		automata.State is = fsa.createState(new Point());
		fsa.setInitialState(is);

		//initial node ids
		ids.put(in.name(), is.getID());
        
		//Map labels to ids
//		java.util.Map<String,Integer> labelIDs = new HashMap<>();
		
//		Iterator<String> lit = ltl_ba.alphabet().iterator();
//		while(lit.hasNext()){
//			String l = lit.next();
////			System.out.println(l);
//			if(!labelIDs.containsKey(l)){
//				labelIDs.put("\""+l+"\"", ""+labelIDs.keySet().size());
//			}
//		}
		
			
		Map<Tuple2<State,Sign>, List<DirectedState>> trans = (Map<Tuple2<State, Sign>, List<DirectedState>>) ltl_fsa.transitions();
		Vector<Tuple2<Tuple2<State,Sign>,List<DirectedState>>> vector =  trans.toVector();
		VectorIterator<Tuple2<Tuple2<State,Sign>,List<DirectedState>>> ltl_ba_it = vector.iterator();
		while(ltl_ba_it.hasNext()){
			Tuple2<Tuple2<State,Sign>,List<DirectedState>> o = ltl_ba_it.next();
			State from = o._1()._1();
			//checks if ID exists
			int ID = 0;
			automata.State fromState = null;
			if (ids.containsKey(from.name())){
				ID = ids.get(from.name());
				fromState = fsa.getStateWithID(ID);
			}
			else{
				//create new state
				fromState = fsa.createState(new Point());
				//update ids
				ids.put(from.name(), fromState.getID());
				ID = fromState.getID();
			}
			
			//get Label
			String l = o._1()._2().toString();
			
//			if(encoded_alphabet==-1)
//				setLabel(l);
//			else
//				setLabelEncoded(l);

			String label = labelIDs.get(l);
			
			Iterator<DirectedState> listIt = o._2().iterator();
			while(listIt.hasNext()){
				State to = listIt.next().state();
				//check if toState exists
				automata.State toState = null;
				
				if (ids.containsKey(to.name())){
					ID = ids.get(to.name());
					toState = fsa.getStateWithID(ID);
				}
				else{
					//create new state
					toState = fsa.createState(new Point());
					//update ids
					ids.put(to.name(), toState.getID());
					ID = toState.getID();
				}
				
				//add transition
				FSATransition t = new FSATransition(fromState,toState,label);
				fsa.addTransition(t);
			}
		}
		
		//add final states
		Iterator<State> ac_it = ltl_fsa.accepting().iterator();
		while(ac_it.hasNext()){
			State a = ac_it.next();
			int ID = ids.get(a.name());
			automata.State as = fsa.getStateWithID(ID);
			fsa.addFinalState(as);
		}
		AutomatonChecker ac = new AutomatonChecker();
		if (ac.isNFA(fsa))throw new RuntimeException("automata2RE: fsa must be deterministic.");
		
//		NFAToDFA determinizer = new NFAToDFA();
//        automata.Automaton dfa = determinizer.convertToDFA((automata.Automaton)fsa.clone());
//        System.out.println(dfa.toString());
        
        Minimizer min = new Minimizer();
      	min.initializeMinimizer();
    	automata.Automaton to_minimize = min.getMinimizeableAutomaton((automata.Automaton) fsa.clone());
    	DefaultTreeModel tree = min.getDistinguishableGroupsTree(to_minimize);
    	automata.Automaton dfa_minimized = min.getMinimumDfa(to_minimize, tree);
		
    	System.out.println(dfa_minimized.toString());	
		
			
			
        FSAToRegularExpressionConverter.convertToSimpleAutomaton(dfa_minimized);
//        System.out.println(dfa_minimized.toString());
        String re = FSAToRegularExpressionConverter.convertToRegularExpression(dfa_minimized);
        System.out.println(re);
        return re;
	}
	
	
	
	public  String automata2RE(Nba ltl_ba){
//		System.out.println(ltl_ba);
		FiniteStateAutomaton fsa = new FiniteStateAutomaton();
	
		//Map nodes to states ids
		java.util.Map<State,automata.State> ids = new HashMap<>();
		Iterator<State> states_it = ltl_ba.states().iterator();
		while(states_it.hasNext()) {
			State s = states_it.next();
//			System.out.println(s.toString());
//			int stateID = Integer.valueOf(s.name().substring(1));
//			System.out.println(stateID);
//			automata.State state = fsa.createStateWithId(new Point(),stateID);
			automata.State state = fsa.createState(new Point());
			//initial node ids
			ids.put(s, state);
		}
		
		
		//get initial node
		if(ltl_ba.start().size() > 1) throw new RuntimeException("automata2RE: more than 1 initial state found");
		State in = ltl_ba.start().head(); //CUIDADO:que pasa si tenemos varios estados iniciales.
		//create and set initial state
//		automata.State is = fsa.createState(new Point());
		automata.State is = ids.get(in);
		fsa.setInitialState(is);

		//initial node ids
//		ids.put(in, is.getID());
	
		Map<Tuple2<State,Sign>, List<DirectedState>> trans = (Map<Tuple2<State, Sign>, List<DirectedState>>) ltl_ba.transitions();
		Vector<Tuple2<Tuple2<State,Sign>,List<DirectedState>>> vector =  trans.toVector();
		VectorIterator<Tuple2<Tuple2<State,Sign>,List<DirectedState>>> ltl_ba_it = vector.iterator();
		while(ltl_ba_it.hasNext()){
			Tuple2<Tuple2<State,Sign>,List<DirectedState>> o = ltl_ba_it.next();
			State from = o._1()._1();
			automata.State fromState = ids.get(from);
			//checks if ID exists
//			int ID = 0;
//			automata.State fromState = null;
//			if (ids.containsKey(from)){
//				ID = ids.get(from);
//				fromState = fsa.getStateWithID(ID);
//			}
//			else{
//				//create new state
//				fromState = fsa.createState(new Point());
//				//update ids
//				ids.put(from, fromState.getID());
////				ID = fromState.getID();
//			}
			
			//get Label
			String l = o._1()._2().toString().replace("\"", "");
			
//			if(encoded_alphabet==-1)
//				setLabel(l);
//			else
//				setLabelEncoded(l);
//			System.out.println(l);
			String label = labelIDs.get(l);
			
			Iterator<DirectedState> listIt = o._2().iterator();
			while(listIt.hasNext()){
				State to = listIt.next().state();
				automata.State toState = ids.get(to);
				//check if toState exists
//				automata.State toState = null;
//				
//				if (ids.containsKey(to)){
//					ID = ids.get(to);
//					toState = fsa.getStateWithID(ID);
//				}
//				else{
//					//create new state
//					toState = fsa.createState(new Point());
//					//update ids
//					ids.put(to, toState.getID());
////					ID = toState.getID();
//				}
				
				//add transition
				FSATransition t = new FSATransition(fromState,toState,label);
				fsa.addTransition(t);
			}
		}
		
		//add final states
		Iterator<State> ac_it = ltl_ba.accepting().iterator();
		while(ac_it.hasNext()){
			State a = ac_it.next();
			automata.State as = ids.get(a);
//			int ID = ids.get(a);
//			automata.State as = fsa.getStateWithID(ID);
			fsa.addFinalState(as);
		}
		
//		System.out.println(fsa);
		
		NFAToDFA determinizer = new NFAToDFA();
        FiniteStateAutomaton dfa = determinizer.convertToDFA(fsa);
//        AutomatonChecker ac = new AutomatonChecker();
//		if (ac.isNFA(dfa))throw new RuntimeException("automata2RE: dfa must be deterministic.");
//        System.out.println(dfa.toString());

		Minimizer min = new Minimizer();
        min.initializeMinimizer();
	    automata.Automaton minimizable = min.getMinimizeableAutomaton(dfa);
	    DefaultTreeModel tree = min.getDistinguishableGroupsTree(minimizable);
	    minimizable = min.getMinimumDfa(minimizable, tree);
//    	System.out.println(dfa_minimized.toString());	
//    	min.printTree(tree, (MinimizeTreeNode)tree.getRoot());
//    	System.out.println();
		
        FSAToRegularExpressionConverter.convertToSimpleAutomaton(minimizable);
//        System.out.println(dfa_minimized.toString());
        String re = FSAToRegularExpressionConverter.convertToRegularExpression(minimizable);
//        String[] arr = Discretizer.or(re);
//        for (int i=0;i<arr.length;i++)
//        	System.out.print(arr[i].length()+" ");
        return re;
	}


	public  automata.Automaton nbaTodfa(Nba ltl_ba){
//		System.out.println(ltl_ba);
		FiniteStateAutomaton fsa = new FiniteStateAutomaton();

		//Map nodes to states ids
		java.util.Map<State,automata.State> ids = new HashMap<>();
		Iterator<State> states_it = ltl_ba.states().iterator();
		while(states_it.hasNext()) {
			State s = states_it.next();
//			System.out.println(s.toString());
//			int stateID = Integer.valueOf(s.name().substring(1));
//			System.out.println(stateID);
//			automata.State state = fsa.createStateWithId(new Point(),stateID);
			automata.State state = fsa.createState(new Point());
			//initial node ids
			ids.put(s, state);
		}


		//get initial node
		if(ltl_ba.start().size() > 1) throw new RuntimeException("automata2RE: more than 1 initial state found");
		State in = ltl_ba.start().head(); //CUIDADO:que pasa si tenemos varios estados iniciales.
		//create and set initial state
//		automata.State is = fsa.createState(new Point());
		automata.State is = ids.get(in);
		fsa.setInitialState(is);

		//initial node ids
//		ids.put(in, is.getID());

		Map<Tuple2<State,Sign>, List<DirectedState>> trans = (Map<Tuple2<State, Sign>, List<DirectedState>>) ltl_ba.transitions();
		Vector<Tuple2<Tuple2<State,Sign>,List<DirectedState>>> vector =  trans.toVector();
		VectorIterator<Tuple2<Tuple2<State,Sign>,List<DirectedState>>> ltl_ba_it = vector.iterator();
		while(ltl_ba_it.hasNext()){
			Tuple2<Tuple2<State,Sign>,List<DirectedState>> o = ltl_ba_it.next();
			State from = o._1()._1();
			automata.State fromState = ids.get(from);
			//checks if ID exists
//			int ID = 0;
//			automata.State fromState = null;
//			if (ids.containsKey(from)){
//				ID = ids.get(from);
//				fromState = fsa.getStateWithID(ID);
//			}
//			else{
//				//create new state
//				fromState = fsa.createState(new Point());
//				//update ids
//				ids.put(from, fromState.getID());
////				ID = fromState.getID();
//			}

			//get Label
			String l = o._1()._2().toString().replace("\"", "");

//			if(encoded_alphabet==-1)
//				setLabel(l);
//			else
//				setLabelEncoded(l);
//			System.out.println(l);
			String label = labelIDs.get(l);

			Iterator<DirectedState> listIt = o._2().iterator();
			while(listIt.hasNext()){
				State to = listIt.next().state();
				automata.State toState = ids.get(to);
				//check if toState exists
//				automata.State toState = null;
//
//				if (ids.containsKey(to)){
//					ID = ids.get(to);
//					toState = fsa.getStateWithID(ID);
//				}
//				else{
//					//create new state
//					toState = fsa.createState(new Point());
//					//update ids
//					ids.put(to, toState.getID());
////					ID = toState.getID();
//				}

				//add transition
				FSATransition t = new FSATransition(fromState,toState,label);
				fsa.addTransition(t);
			}
		}

		//add final states
		Iterator<State> ac_it = ltl_ba.accepting().iterator();
		while(ac_it.hasNext()){
			State a = ac_it.next();
			automata.State as = ids.get(a);
//			int ID = ids.get(a);
//			automata.State as = fsa.getStateWithID(ID);
			fsa.addFinalState(as);
		}

//		System.out.println(fsa);

		NFAToDFA determinizer = new NFAToDFA();
		FiniteStateAutomaton dfa = determinizer.convertToDFA(fsa);
//        AutomatonChecker ac = new AutomatonChecker();
//		if (ac.isNFA(dfa))throw new RuntimeException("automata2RE: dfa must be deterministic.");
//        System.out.println(dfa.toString());

//		Minimizer min = new Minimizer();
//		min.initializeMinimizer();
//		automata.Automaton minimizable = min.getMinimizeableAutomaton(dfa);
//		DefaultTreeModel tree = min.getDistinguishableGroupsTree(minimizable);
//		minimizable = min.getMinimumDfa(minimizable, tree);
//    	System.out.println(dfa_minimized.toString());
//    	min.printTree(tree, (MinimizeTreeNode)tree.getRoot());
//    	System.out.println();

		return dfa;
	}


	public String toABClanguage(String re){
		String abcStr = "";
		abcStr = re.replace("λ", "\"\"");
		abcStr = abcStr.replace("+", "|");
		return abcStr;
	}
	
	int base = 48;//start with char 0
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
	
	public int encoded_alphabet = -1;

	
	public int[]state = {48,48};//start with char 0
	public void setLabelEncoded(String l) throws RuntimeException{
		if(labelIDs.containsKey(l)){
			return;
		}
		String label = "";
		if(this.encoded_alphabet==1)
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
