package modelcounter;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.nasa.ltl.graphio.Writer;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import gov.nasa.ltl.graph.*;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.visitors.RltlConvSyntaxReplacer;
import solvers.SolverUtils;

public class AutomataBasedModelCounting {

	private DMatrixRMaj T = null;
	private DMatrixRMaj I = null;
	private Graph<String> nba = null;
	private boolean exhaustive = true;
	public static int TIMEOUT = 300;
	public AutomataBasedModelCounting (LabelledFormula formula, boolean exhaustive) throws IOException, InterruptedException {

		  this.exhaustive = exhaustive;
		  Writer<String> w = Writer.getWriter (Writer.Format.FSP, System.out);

		// Convert the ltl formula to an automaton with RltlConv
//		Formula clean_syntax = formula.formula().accept(new RltlConvSyntaxReplacer());
//		LabelledFormula clean_formula = LabelledFormula.of(clean_syntax, formula.variables());
//		String ltlStr = genRltlString(clean_formula);
////		System.out.println(ltlStr);
//		nba = Buchi2Graph.LTL2Graph(ltlStr);

		// Convert the ltl formula to an automaton with OWL
		nba = Buchi2Graph.LTL2Graph(formula);

		if(exhaustive) {
			//We first apply a transformation and add an extra state, sn+1. The resulting
			//automaton is a DFA A0 with λ-transitions from each of the accepting states of A
			//to sn+1 where λ is a new padding symbol that is not in the alphabet of A.

			Node<String> sn1 = new Node<>(nba);
//			Guard<String> lambda = new Guard<>();
//			lambda.add(new Literal<String>("0", false));

			for (Node<String> node : nba.getNodes()) {
				if (node.getBooleanAttribute("accepting")) {
					node.setBooleanAttribute("accepting", false);
					Edge<String> nToSn1 = new Edge<>(node, sn1);
					node.getOutgoingEdges().add(nToSn1);
					sn1.getIncomingEdges().add(nToSn1);
				}
			}

			sn1.setBooleanAttribute("accepting", true);
			Edge<String> sn1ToSn1 = new Edge<>(sn1, sn1);
			sn1.getOutgoingEdges().add(sn1ToSn1);
			sn1.getIncomingEdges().add(sn1ToSn1);

//			w.write(nba);
		}
				  
		//From A0 we construct the (n + 1) × (n + 1) transfer matrix T. A0 has n + 1
		//states s1, s2, . . . sn+1. The matrix entry Ti,j is the number of transitions from
		//state si to state sj
		T = buildTransferMatrix(nba);
//		System.out.println("T: " + T.toString());
		int n = nba.getNodeCount();
		I = CommonOps_DDRM.identity(n);

	  }

	public AutomataBasedModelCounting (Graph<String> input_graph, boolean exhaustive) {

		this.exhaustive = exhaustive;


		// Convert the ltl formula to an automaton with OWL
		nba = input_graph;

		if(exhaustive) {
			//We first apply a transformation and add an extra state, sn+1. The resulting
			//automaton is a DFA A0 with λ-transitions from each of the accepting states of A
			//to sn+1 where λ is a new padding symbol that is not in the alphabet of A.

			Node<String> sn1 = new Node<>(nba);
//			Guard<String> lambda = new Guard<>();
//			lambda.add(new Literal<String>("0", false));

			for (Node<String> node : nba.getNodes()) {
				if (node.getBooleanAttribute("accepting")) {
					node.setBooleanAttribute("accepting", false);
					Edge<String> nToSn1 = new Edge<>(node, sn1);
					node.getOutgoingEdges().add(nToSn1);
					sn1.getIncomingEdges().add(nToSn1);
				}
			}

			sn1.setBooleanAttribute("accepting", true);
			Edge<String> sn1ToSn1 = new Edge<>(sn1, sn1);
			sn1.getOutgoingEdges().add(sn1ToSn1);
			sn1.getIncomingEdges().add(sn1ToSn1);

//			w.write(nba);
		}

		//From A0 we construct the (n + 1) × (n + 1) transfer matrix T. A0 has n + 1
		//states s1, s2, . . . sn+1. The matrix entry Ti,j is the number of transitions from
		//state si to state sj
		T = buildTransferMatrix(nba);
//		System.out.println("T: " + T.toString());
		int n = nba.getNodeCount();
		I = CommonOps_DDRM.identity(n);

	}

	public String genRltlString(LabelledFormula formula) throws IOException, InterruptedException{
//		String ltl = SolverUtils.toLambConvSyntax(formula.formula().toString());
//		String alph = SolverUtils.createLambConvAlphabet(formula);
		List<String> alphabet = SolverUtils.genAlphabet(formula.variables().size());
		LabelledFormula label_formula = LabelledFormula.of(formula.formula(), alphabet);
		String ltl = SolverUtils.toLambConvSyntax(label_formula.toString());
		String alph = alphabet.toString();

		String form = "LTL="+ltl;
		if(alph!=null && alph!="")
			form += ",ALPHABET="+alph;

		return form;
	}


	public  BigInteger count(int k){
		//We compute uTkv, where u is the row vector such that ui = 1 if and only if i is the start state and 0 otherwise,
		// and v is the column vector where vi = 1 if and only if i is an accepting state and 0 otherwise.
		if (nba == null || nba.getNodeCount() == 0)
			return BigInteger.ZERO;
		int n = T.numRows;
		DMatrixRMaj u = new DMatrixRMaj(1,n);
		//TODO: Assume that the initial state is in first position
		u.set(0,nba.getInit().getId(),1);
		DMatrixRMaj v = new DMatrixRMaj(n,1);
		for (Node<String> node : nba.getNodes()) {
			if (node.getBooleanAttribute("accepting"))
				v.set(node.getId(), 0, 1);
		}
		DMatrixRMaj T_res = T.copy();
		int bound = exhaustive ? k+1 : k;
		for(int i=1; i<bound; i++) {
			long initialTime = System.currentTimeMillis();
			DMatrixRMaj T_i = T.copy();
			DMatrixRMaj T_aux = T_res.copy();
			CommonOps_DDRM.mult(T_aux, T_i, T_res);
//			System.out.println(i + ": " + T_res.toString());
			//check for timeout
			long currentTime = System.currentTimeMillis();
			long totalTime = currentTime-initialTime;
			int min = (int) (totalTime)/60000;
			int sec = (int) (totalTime - min*60000)/1000;
			if (sec > TIMEOUT) {
				System.out.print("TO ");
				return BigInteger.ZERO;
			}
		}
		DMatrixRMaj reachable = new DMatrixRMaj(1,n);
		CommonOps_DDRM.mult(u, T_res, reachable);
//		System.out.println("reachable: " + reachable.toString());
		DMatrixRMaj result = new DMatrixRMaj(1,1);
		CommonOps_DDRM.mult(reachable,v,result);
//		System.out.println("result: " + result.toString());
		long value = (long)result.get(0,0);
		BigInteger count = BigInteger.valueOf(value);
		return count;
	}

	private  BigInteger eval(int k){
		 //gA(z) = (−1)n det(I − zT : n + 1, 1) / z det(I − zT)
		  //where (M : i, j) denotes the matrix obtained by removing the ith row and jth
		  //column from M, I is the identity matrix, det M is the matrix determinant, and
		  //n is the number of states in the original DFA A
			BigInteger count = BigInteger.ZERO;
		  // Remove row n+1 and column j. After this M = I − zT : n + 1, 1
		  int n = I.numCols-1;
			for (int z = 0; z <=k; z++) {
			  DMatrixRMaj Ti = T.copy();
			  DMatrixRMaj ID = I.copy();
//			  for (int i = 0; i <=z; i++) {
				  CommonOps_DDRM.scale(z, Ti);
				  
				  // Subtract T to I. After this subtraction, I' = I - zT
				  CommonOps_DDRM.subtractEquals(ID, Ti);
				  
				  DMatrixRMaj M = removeRowAndCol(ID,n,0);
				  
				  // Calculate det0
				  BigDecimal det0 = BigDecimal.valueOf(CommonOps_DDRM.det(M));
				  System.out.println(det0);
				  // Calculate det1
				  BigDecimal det1 = BigDecimal.valueOf(CommonOps_DDRM.det(ID));
				  System.out.println(det1);
				  
				  // gA(z) = (−1)n * (det0 / z det1)
				  BigDecimal Z = BigDecimal.valueOf(z);
		//		  double gaZ = Math.pow(-1, n) * ( det0 / (z * det1));
				  BigDecimal zTimesdet1 = Z.multiply(det1);
				  BigDecimal det = BigDecimal.ZERO;
				  if (zTimesdet1.intValue() > 0)
					  det = det0.divide(zTimesdet1, 10, RoundingMode.HALF_UP);
				  BigDecimal gaZ = det.multiply(BigDecimal.valueOf(-1));
				  count = count.add(gaZ.toBigInteger());
				System.out.println("COUNT: "+ count);
//			  }
		  }
		  return count;
	}
	
	  
	  /**
	   * Build the Transfer Matrix for the given DFA 
	   * @param nba is the DFA
	   * @return a n x n matrix M where M[i,j] is the number of transitions from state si to state sj 
	   */
	  public  DMatrixRMaj buildTransferMatrix(Graph<String> nba) {
		  int n = nba.getNodeCount();
		  DMatrixRMaj M = new DMatrixRMaj(n,n);
		  for (int i=0;i<n;i++) {
			  Node<String> si = nba.getNodes().get(i);
			  for (int j=0;j<n;j++) {
				  Node<String> sj = nba.getNodes().get(j);
				  long transitions = 0;
				  for (Edge<String> edge : si.getOutgoingEdges()) {
					  if (edge.getNext().getId()==sj.getId())
						  transitions++;
				  }
				  M.add(i, j, transitions);
			  }
		  }
		  return M;
	  }	
	  
	  /**
	   * Build a new matrix out of the given matrix A removing the given row and column.
	   * @param A
	   * @param row
	   * @param col
	   * @return
	   */
	  private static DMatrixRMaj removeRowAndCol(DMatrixRMaj A,int row,int col) {
//		  System.out.println(A.toString());
		 DMatrixRMaj M = new DMatrixRMaj(A.numRows-1,A.numCols-1);
		 int k = -1;
		 for (int i=0;i<A.numRows;i++) {
			 if (i!=row) {
				 k++;
				 int r = -1;
				 for (int j=0;j<A.numCols;j++) {
					 if (j!=col) {
						 r++;
						 M.set(k, r, A.get(i, j));
					 }
				 }
			 }
		 }
//		 System.out.println(M.toString());
		 return M;
	  }
	  
	  /**
	   * Determinize the given NFA
	   */
	  private static Graph<String> toDFA(Graph<String> nfa) {
		  Graph<String> dfa = new Graph<String>();
		  // λ-closure the intial state of nfa and set that set of states as the new state of the dfa
		  Set<Integer> nodesIds = closure(nfa.getInit(),new HashSet<Integer>());
		  Node<String> dfaInitial = new Node<String>(dfa);
		  dfa.setInit(dfaInitial);
		  Map<Set<Integer>,Node<String>> dfaNodes = new HashMap<Set<Integer>,Node<String>>();
		  dfaNodes.put(nodesIds, dfaInitial);
		  
		  // Find the states that can be traversed from the present for each input symbol
		  List<Set<Integer>> workSet = new LinkedList<Set<Integer>>();
		  Set<Set<Integer>> visited = new HashSet<Set<Integer>>();
		  workSet.add(nodesIds);
		  visited.add(nodesIds);
		  while (!workSet.isEmpty()) {
			  Set<Integer> currentStates = workSet.remove(0);
			  Node<String> currentDFANode = dfaNodes.get(currentStates);
			  Map<Guard<String>,Set<Integer>> statesByGuard = getStatesByGuard(currentStates,nfa);
			  for (Guard<String> guard : statesByGuard.keySet()) {
				  Set<Integer> guardStates = statesByGuard.get(guard);
				  // Create a new edge in the DFA with the current guard and to the state that represents guardStates
				  Node<String> n;
				  if (dfaNodes.containsKey(guardStates)) {
					  n = dfaNodes.get(guardStates);
				  } else {
					  n = new Node<String>(dfa);
					  dfaNodes.put(guardStates, n);
				  }
				  Edge<String> newEdge = new Edge<String>(currentDFANode,n,guard);
				  currentDFANode.getOutgoingEdges().add(newEdge);
				  if (visited.add(guardStates)) {
					  workSet.add(guardStates);
				  }
			  }
			  
		  }
		  
		  // Mark the states of DFA which contains final state of NFA as final states of DFA
		  for (Set<Integer> statesInNfa : dfaNodes.keySet()) {
			  Node<String> dfaNode = dfaNodes.get(statesInNfa);
			  boolean hasKeyInNfa = false;
			  for (Integer nodeId : statesInNfa) {
				  if (nfa.getNode(nodeId).getBooleanAttribute("accepting")) {
					  hasKeyInNfa = true;
					  break;
				  }
			  }
			  if (hasKeyInNfa)
				  dfaNode.setBooleanAttribute("accepting", true);
		  }
		  
		  return dfa;
	  }
	  
	  /**
	   * λ-closure of the given node with respect to the nfa
	   */
	  private static Set<Integer> closure(Node<String> node,Set<Integer> visitedNodes) {
		  Set<Integer> closureSet = new HashSet<Integer>();
		  closureSet.add(node.getId());
		  for (Edge<String> edge : node.getOutgoingEdges()) {
			  if (edge.getGuard().isTrue()) {
				  if (visitedNodes.add(edge.getNext().getId())) {
					  closureSet.addAll(closure(edge.getNext(),visitedNodes));
				  }
			  }
		  }
		  return closureSet;
	  }
	  
	  /**
	   * Given a set of states, get a map between the guards and the reached states
	   */
	  private static Map<Guard<String>,Set<Integer>> getStatesByGuard(Set<Integer> stateIds,Graph<String> nfa) {
		  Map<Guard<String>,Set<Integer>> statesByGuard = new HashMap<Guard<String>,Set<Integer>>();
		  for (Integer nodeId : stateIds) {
			  Node<String> currentNode = nfa.getNode(nodeId);
			  for (Edge<String> currentEdge : currentNode.getOutgoingEdges()) {
				  Guard<String> currentGuard = currentEdge.getGuard();
				  if (!currentGuard.isTrue()) {
					  if (statesByGuard.keySet().contains(currentGuard)) {
						  statesByGuard.get(currentGuard).add(currentEdge.getNext().getId());
					  } else {
						  HashSet<Integer> guardStates = new HashSet<Integer>();
						  guardStates.add(currentEdge.getNext().getId());
						  statesByGuard.put(currentGuard, guardStates);
					  }
					  statesByGuard.get(currentGuard).addAll(closure(currentEdge.getNext(),new HashSet<Integer>()));
				  }
				  
			  }
		  }
		  return statesByGuard;
	  }
	  
	  
	 
}	

