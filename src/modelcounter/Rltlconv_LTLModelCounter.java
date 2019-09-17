package modelcounter;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import javax.management.RuntimeErrorException;

import automata.fsa.FSAToRegularExpressionConverter;
import automata.fsa.FSATransition;
import automata.fsa.FiniteStateAutomaton;
import de.uni_luebeck.isp.buchi.BuchiAutomaton;
import de.uni_luebeck.isp.buchi.Transition;
import de.uni_luebeck.isp.rltlconv.automata.DirectedState;
import de.uni_luebeck.isp.rltlconv.automata.Nba;
import de.uni_luebeck.isp.rltlconv.automata.Nfa;
import de.uni_luebeck.isp.rltlconv.automata.Sign;
import de.uni_luebeck.isp.rltlconv.automata.State;
import de.uni_luebeck.isp.rltlconv.cli.Conversion;
import de.uni_luebeck.isp.rltlconv.cli.Main;
import de.uni_luebeck.isp.rltlconv.cli.RltlConv;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.immutable.List;
import scala.collection.immutable.Map;
import scala.collection.immutable.Set;
import scala.collection.immutable.Vector;
import scala.collection.immutable.VectorIterator;
import de.uni_luebeck.isp.rltlconv.cli.Conversion.ConversionVal;
import scala.collection.immutable.VectorIterator;


public class Rltlconv_LTLModelCounter {

	public static int TIMEOUT = 60;
	
	private static void writeFile(String fname,String text) throws IOException{
		
        try {
            File file = new File(fname);
            FileWriter fw = new FileWriter(file);
            BufferedWriter output = new BufferedWriter(fw);
            output.write(text);
            output.flush();
            output.close();
            fw.close();
        } catch ( IOException e ) {
            e.printStackTrace();
        } 
	}
	
	private static void runCommand(String cmd) throws IOException, InterruptedException{
		
		Process p = Runtime.getRuntime().exec(cmd);
		
		boolean timeout = false;
		if(!p.waitFor(TIMEOUT, TimeUnit.SECONDS)) {
		    timeout = true; //kill the process. 
			p.destroy(); // consider using destroyForcibly instead
		}
		
		if (timeout)
			throw new IllegalStateException("rltlconv timeout exception.");
		
		//empty out file
		Process p1 = Runtime.getRuntime().exec("rm rltlconv-out.txt");
		p1.waitFor(TIMEOUT, TimeUnit.SECONDS);
		
		InputStream in = p.getInputStream();
    	InputStreamReader inread = new InputStreamReader(in);
    	BufferedReader bufferedreader = new BufferedReader(inread);
    	String aux;
    	String out = "";
	    while ((aux = bufferedreader.readLine()) != null) {
	    	out += aux+"\n";
	    }
	    if(out!="")
	    	writeFile("rltlconv-out.txt",out);
	    else
	    	throw new IllegalStateException("rltlconv error: out empty file");
	 // Leer el error del programa.
    	InputStream err = p.getErrorStream();
    	InputStreamReader errread = new InputStreamReader(err);
    	BufferedReader errbufferedreader = new BufferedReader(errread);
    	
	    while ((aux = errbufferedreader.readLine()) != null) {
	    	System.out.println("ERR: " + aux);
	    }
	   
	    // Check for failure
		if (p.waitFor() != 0) {
			System.out.println("exit value = " + p.exitValue());
		}
  
		// Close the InputStream
    	bufferedreader.close();
    	inread.close();
    	in.close();
   		// Close the ErrorStream
   		errbufferedreader.close();
   		errread.close();
   		err.close();
    		
   		if (p!=null) {
//   			InputStream is = p.getInputStream();
 //  			InputStream es = p.getErrorStream();
  			OutputStream os = p.getOutputStream();
//   				if (is!=null) is.close();
//   				if (es!=null) es.close();
			if (os!=null) os.close();
   		}
	}
	
	public static void reset() {
		base = 48;
		labelIDs.clear();
		encoded_alphabet = -1;
	}
	public static boolean props = true;
	public static Nfa ltl2dfa(String formula) throws IOException, InterruptedException{
//		ConversionVal[] conv = {Conversion.PROPS(), Conversion.FORMULA(),Conversion.APA(),Conversion.NBA(), Conversion.MIN(), Conversion.NFA(), Conversion.DFA()};
//		Object res = RltlConv.convert(formula, conv);
		
		//write results to file
		String fname = "rltlconv.txt";
		//empty rltlconv file
		Process p0 = Runtime.getRuntime().exec("rm rltlconv.txt");
		p0.waitFor(TIMEOUT, TimeUnit.SECONDS);
		writeFile(fname,formula);
		if (props)
			runCommand("./rltlconv.sh @rltlconv.txt --formula --props --nba --min --nfa --dfa");
		else
			runCommand("./rltlconv.sh @rltlconv.txt --formula --apa --nba --min --nfa --dfa");
			
		Object res = Main.load("@rltlconv-out.txt");
		
		Nfa fsa = (Nfa) RltlConv.convert(res, Conversion.DFA());
		return fsa.toNamedNfa();
	}
	
	public static Nba ltl2nba(String formula) throws IOException, InterruptedException{
		
//		ConversionVal[] conv = {Conversion.FORMULA(),Conversion.PROPS(),Conversion.APA(), Conversion.NBA(), Conversion.MIN()};
		String [] conv = { "--formula","--props", "--apa", "--reduce", "--nba", "--min"};
		Object res = RltlConv.convert(formula, conv);
		Nba nba = (Nba) res;
		
////		labelIDs.clear();
//		//write results to file
//		String fname = "rltlconv.txt";
//		//empty rltlconv file
//		Process p0 = Runtime.getRuntime().exec("rm rltlconv.txt");
//		p0.waitFor(TIMEOUT, TimeUnit.SECONDS);
//		writeFile(fname,formula);
//		
//		if (props)
//			runCommand("./rltlconv.sh @rltlconv.txt --formula --props --nba --min");
//		else
//			runCommand("./rltlconv.sh @rltlconv.txt --formula --nba --min");
//		Object res = Main.load("@rltlconv-out.txt");
//		
//		Nba nba = (Nba) RltlConv.convert(res, Conversion.NBA());
		return nba.toNamedNba();
	}
	

			
	public static String automata2RE(Nfa ltl_ba){
		
		FiniteStateAutomaton fsa = new FiniteStateAutomaton();
	
		//Map nodes to states ids
		java.util.Map<String,Integer> ids = new HashMap<>();
		//get initial node
		State in = ltl_ba.start().head(); //CUIDADO:que pasa si tenemos varios estados iniciales.

		//create and set initial state
		automata.State is = fsa.createState(new Point());
		fsa.setInitialState(is);
		
		//Map labels to ids
//		java.util.Map<String,Integer> labelIDs = new HashMap<>();
		
		Iterator<String> lit = ltl_ba.alphabet().iterator();
		while(lit.hasNext()){
			String l = lit.next();
//			System.out.println(l);
			if(!labelIDs.containsKey(l)){
				labelIDs.put("\""+l+"\"", ""+labelIDs.keySet().size());
			}
		}
		
		//initial node ids
		ids.put(in.name(), is.getID());
			
		Map<Tuple2<State,Sign>, List<DirectedState>> trans = (Map<Tuple2<State, Sign>, List<DirectedState>>) ltl_ba.transitions();
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
			
//			String label = getLabel(l);
//			int base = 97;//a
//			System.out.println("l:" +l.toString());
//			String label = ""+Character.toChars(base+labelIDs.get(l))[0];
			String label = labelIDs.get(l).toString();
			
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
		Iterator<State> ac_it = ltl_ba.accepting().iterator();
		while(ac_it.hasNext()){
			State a = ac_it.next();
			int ID = ids.get(a.name());
			automata.State as = fsa.getStateWithID(ID);
			fsa.addFinalState(as);
		}
		
		//convertToDFA
		//FiniteStateAutomaton dfa = (new NFAToDFA()).convertToDFA(fsa);
		
		//minimize automaton
		//Automaton m = (new Minimizer()).getMinimizeableAutomaton(dfa);
		
//		System.out.println(labelIDs);
		
		//removeEmptyTransitions(fsa);
//		System.out.println(fsa.toString());

		FSAToRegularExpressionConverter.convertToSimpleAutomaton(fsa);
//		System.out.println(fsa.toString());
		return FSAToRegularExpressionConverter.convertToRegularExpression(fsa);
	}
	
	//Map labels to ids
	public static java.util.Map<String,String> labelIDs = new HashMap<>();
	
public static String automata2RE(Nba ltl_ba){

		FiniteStateAutomaton fsa = new FiniteStateAutomaton();
	
		//Map nodes to states ids
		java.util.Map<String,Integer> ids = new HashMap<>();
		//get initial node
		State in = ltl_ba.start().head(); //CUIDADO:que pasa si tenemos varios estados iniciales.

		//create and set initial state
		automata.State is = fsa.createState(new Point());
		fsa.setInitialState(is);

		//initial node ids
		ids.put(in.name(), is.getID());
			
		Map<Tuple2<State,Sign>, List<DirectedState>> trans = (Map<Tuple2<State, Sign>, List<DirectedState>>) ltl_ba.transitions();
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
			
			if(encoded_alphabet==-1)
				setLabel(l);
			else
				setLabelEncoded(l);
//			if(!labelIDs.containsKey(l)){
//				labelIDs.put(l, labelIDs.keySet().size());
//			}
			
//			String label = getLabel(l);
			
//			int base = 97;//a
//			String label = l; //""+Character.toChars(base+labelIDs.get(l))[0];
//			String label = ""+Character.toChars(labelIDs.get(l))[0];
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
		Iterator<State> ac_it = ltl_ba.accepting().iterator();
		while(ac_it.hasNext()){
			State a = ac_it.next();
			int ID = ids.get(a.name());
			automata.State as = fsa.getStateWithID(ID);
			fsa.addFinalState(as);
		}
//		System.out.println("lablesID: "+ labelIDs.size());
//		System.out.println("FSA: " + fsa.getStates().length +  "(" + fsa.getFinalStates().length + ") " + fsa.getTransitions().length);
		FSAToRegularExpressionConverter.convertToSimpleAutomaton(fsa);
//		System.out.println("FSA: " + fsa.getStates().length +  "(" + fsa.getFinalStates().length + ") " + fsa.getTransitions().length);
//		System.out.println(fsa.toString());
		return FSAToRegularExpressionConverter.convertToRegularExpression(fsa);
	}
	
	public static String toABClanguage(String re){
		String abcStr = "";
		abcStr = re.replace("λ", "\"\"");
		abcStr = abcStr.replace("+", "|");
		return abcStr;
	}
	
	static int base = 48;//start with char 0
	public static void setLabel(String l) throws RuntimeException{
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
	
	public static int encoded_alphabet = -1;

	
	public static int[]state = {48,48};//start with char 0
	public static void setLabelEncoded(String l) throws RuntimeException{
		if(labelIDs.containsKey(l)){
			return;
		}
		String label = "";
		if(Rltlconv_LTLModelCounter.encoded_alphabet==1)
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
