package modelcounter;

import gov.nasa.ltl.graph.Edge;
import gov.nasa.ltl.graph.Graph;
import gov.nasa.ltl.graph.Guard;
import gov.nasa.ltl.graph.Node;
import gov.nasa.ltl.graphio.Writer;
import org.apache.commons.math3.Field;
import org.apache.commons.math3.fraction.BigFraction;
import org.apache.commons.math3.fraction.Fraction;
import org.apache.commons.math3.linear.Array2DRowFieldMatrix;
import org.apache.commons.math3.linear.FieldMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.ejml.data.DMatrixRMaj;
import owl.ltl.LabelledFormula;
import solvers.SolverUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;


public class MatrixBigIntegerModelCounting {
	private FieldMatrix<BigFraction> T = null;
	private FieldMatrix<BigFraction> I = null;
	private Graph<String> nba = null;
	private boolean exhaustive = true;
	public static int TIMEOUT = 300;

	public MatrixBigIntegerModelCounting(LabelledFormula formula, boolean exhaustive) throws IOException, InterruptedException {

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
		BigFraction[][] pData = new BigFraction[n][n];
		for (int i = 0; i<n; i++){
			for (int j = 0; j<n; j++){
				if (i != j)
					pData[i][j] = new BigFraction(0);
				else
					pData[i][j] = new BigFraction(1);
			}
		}
		I = new Array2DRowFieldMatrix<BigFraction>(pData, false);

	  }

	


	public  BigInteger count(int k){
		//We compute uTkv, where u is the row vector such that ui = 1 if and only if i is the start state and 0 otherwise,
		// and v is the column vector where vi = 1 if and only if i is an accepting state and 0 otherwise.
		if (nba == null || nba.getNodeCount() == 0)
			return BigInteger.ZERO;
		int n = T.getRowDimension();

		//set initial states
		FieldMatrix u = createMatrix(1,n);
		for (int i = 0; i<n; i++){
			if (i == nba.getInit().getId())
				u.addToEntry(0,i, new BigFraction(1));
			else
				u.addToEntry(0,i, new BigFraction(0));
		}

		//set final states
		FieldMatrix v =  createMatrix(n,1);
		for (Node<String> node : nba.getNodes()) {
			if (node.getBooleanAttribute("accepting"))
				v.addToEntry(node.getId(),0, new BigFraction(1));
		}

		int bound = exhaustive ? k+1 : k;
		FieldMatrix T_res = T.power(bound);
		FieldMatrix reachable = u.multiply(T_res);
//		System.out.println("reachable: " + reachable.toString());
		FieldMatrix result = reachable.multiply(v);
//		System.out.println("result: " + result.toString());
		BigFraction value = (BigFraction)result.getEntry(0,0);
		BigInteger count = value.getNumerator();
		return count;
	}

		/**
	   * Build the Transfer Matrix for the given DFA 
	   * @param nba is the DFA
	   * @return a n x n matrix M where M[i,j] is the number of transitions from state si to state sj 
	   */
	  public  FieldMatrix buildTransferMatrix(Graph<String> nba) {
		  int n = nba.getNodeCount();
		  BigFraction[][] pData = new BigFraction[n][n];
		  for (int i=0;i<n;i++) {
			  Node<String> si = nba.getNodes().get(i);
			  for (int j=0;j<n;j++) {
				  Node<String> sj = nba.getNodes().get(j);
				  long transitions = 0;
				  for (Edge<String> edge : si.getOutgoingEdges()) {
					  if (edge.getNext().getId()==sj.getId())
						  transitions++;
				  }
				  BigFraction v = new BigFraction(transitions);
				  pData[i][j] = v;
			  }
		  }
		  return new Array2DRowFieldMatrix<BigFraction>(pData, false);
	  }


	  
	  public FieldMatrix createMatrix(int row, int column) {
		  BigFraction[][] pData = new BigFraction[row][column];
		  for (int i = 0; i<row; i++){
			  for (int j = 0; j<column; j++){
				  pData[i][j] = new BigFraction(0);
			  }
		  }
		  return new Array2DRowFieldMatrix<BigFraction>(pData, false);
	  }
	  
	 
}	

