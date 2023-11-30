package modelcounter;

import gov.nasa.ltl.graph.Edge;
import gov.nasa.ltl.graph.Graph;
import gov.nasa.ltl.graph.Node;
import gov.nasa.ltl.graphio.Writer;
import org.apache.commons.math3.fraction.BigFraction;
import org.apache.commons.math3.linear.Array2DRowFieldMatrix;
import org.apache.commons.math3.linear.FieldMatrix;
import owl.ltl.LabelledFormula;

import java.io.IOException;
import java.math.BigInteger;


public class MatrixBigIntegerModelCounting {
    private final FieldMatrix<BigFraction> T;
    private final Graph<String> nba;
    private final boolean exhaustive;

    public MatrixBigIntegerModelCounting(LabelledFormula formula, boolean exhaustive) throws IOException, InterruptedException {
        this.exhaustive = exhaustive;
        Writer<String> w = Writer.getWriter(Writer.Format.FSP, System.out);
        // Convert the ltl formula to an automaton with OWL
        nba = Buchi2Graph.LTL2Graph(formula);
        if (exhaustive) {
            //We first apply a transformation and add an extra state, sn+1. The resulting
            //automaton is a DFA A0 with λ-transitions from each of the accepting states of A
            //to sn+1 where λ is a new padding symbol that is not in the alphabet of A.
            Node<String> sn1 = new Node<>(nba);
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

        }
        //From A0 we construct the (n + 1) × (n + 1) transfer matrix T. A0 has n + 1
        //states s1, s2, . . . sn+1. The matrix entry Ti,j is the number of transitions from
        //state si to state sj
        T = buildTransferMatrix(nba);
    }


    public BigInteger count(int k) {
        //We compute uTkv, where u is the row vector such that ui = 1 if and only if i is the start state and 0 otherwise,
        // and v is the column vector where vi = 1 if and only if i is an accepting state and 0 otherwise.
        if (nba == null || nba.getNodeCount() == 0)
            return BigInteger.ZERO;
        int n = T.getRowDimension();

        //set initial states
        FieldMatrix<BigFraction> u = createMatrix(1, n);
        for (int i = 0; i < n; i++) {
            if (i == nba.getInit().getId())
                u.addToEntry(0, i, new BigFraction(1));
            else
                u.addToEntry(0, i, new BigFraction(0));
        }

        //set final states
        FieldMatrix<BigFraction> v = createMatrix(n, 1);
        for (Node<String> node : nba.getNodes()) {
            if (node.getBooleanAttribute("accepting"))
                v.addToEntry(node.getId(), 0, new BigFraction(1));
        }

        int bound = exhaustive ? k + 1 : k;
        FieldMatrix<BigFraction> T_res = T.power(bound);
        FieldMatrix<BigFraction> reachable = u.multiply(T_res);
        FieldMatrix<BigFraction> result = reachable.multiply(v);
        BigFraction value = result.getEntry(0, 0);
        return value.getNumerator();
    }

    /**
     * Build the Transfer Matrix for the given DFA
     *
     * @param nba is the DFA
     * @return a n x n matrix M where M[i,j] is the number of transitions from state si to state sj
     */
    public FieldMatrix<BigFraction> buildTransferMatrix(Graph<String> nba) {
        int n = nba.getNodeCount();
        BigFraction[][] pData = new BigFraction[n][n];
        for (int i = 0; i < n; i++) {
            Node<String> si = nba.getNodes().get(i);
            for (int j = 0; j < n; j++) {
                Node<String> sj = nba.getNodes().get(j);
                long transitions = 0;
                for (Edge<String> edge : si.getOutgoingEdges()) {
                    if (edge.getNext().getId() == sj.getId())
                        transitions++;
                }
                BigFraction v = new BigFraction(transitions);
                pData[i][j] = v;
            }
        }
        return new Array2DRowFieldMatrix<>(pData, false);
    }


    public FieldMatrix<BigFraction> createMatrix(int row, int column) {
        BigFraction[][] pData = new BigFraction[row][column];
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                pData[i][j] = new BigFraction(0);
            }
        }
        return new Array2DRowFieldMatrix<>(pData, false);
    }


}	

