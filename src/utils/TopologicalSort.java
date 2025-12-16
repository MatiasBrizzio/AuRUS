package utils;

import java.io.IOException;
import java.util.*;

import owl.ltl.BooleanConstant;
import owl.ltl.Conjunction;
import owl.ltl.Formula;
import owl.ltl.GOperator;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.visitors.SolverSyntaxOperatorReplacer;
import solvers.LTLSolver;

public class TopologicalSort {
    private int vertices;
    private LinkedList<Integer>[] adj;

    @SuppressWarnings("unchecked")
    public TopologicalSort(int v) {
        vertices = v;
        adj = new LinkedList[v];
        for (int i = 0; i < v; ++i)
            adj[i] = new LinkedList<>();
    }

    void addEdge(int u, int v) {
        adj[u].add(v);
    }

    void dfs(int v, boolean[] visited, Stack<Integer> stack) {
        visited[v] = true;
        for (int neighbor : adj[v]) {
            if (!visited[neighbor])
                dfs(neighbor, visited, stack);
        }
        stack.push(v);
    }

    private static boolean implies(Formula f1, Formula f2) throws IOException, InterruptedException {
        SolverSyntaxOperatorReplacer visitor = new SolverSyntaxOperatorReplacer();
        Formula implication = GOperator.of(Conjunction.of(f1, f2.not())).accept(visitor);
        LTLSolver.SolverResult res = LTLSolver.isSAT(SolverUtils.toSolverSyntax(implication));
        return res.equals(LTLSolver.SolverResult.UNSAT);
    }

    private void addSpecs(List<Tlsf> specs) throws IOException, InterruptedException {
        List<Formula> formulae = new ArrayList<>();
        for (Tlsf spec : specs) {
            formulae.add(spec.toFormula().formula());
        }
        for (int i = 0; i < specs.size(); i++) {
            for (int j = 0; j < specs.size(); j++) {
                if (i != j && implies(formulae.get(i), formulae.get(j))) {
                    addEdge(i, j);
                }
                System.out.print("Comparing " + (i+1) + " and " + (j+1) + "\r");
            }
            System.out.println("Completed implication checks for spec " + (i+1) + "/" + specs.size());
        }
    }

    public void sort(List<Tlsf> specs) throws IOException, InterruptedException {
        addSpecs(specs);
        Stack<Integer> stack = new Stack<>();
        boolean[] visited = new boolean[vertices];
        for (int i = 0; i < vertices; i++) {
            if (!visited[i])
                dfs(i, visited, stack);
        }

        while (!stack.isEmpty()) {
            System.out.print(stack.pop() + " ");
        }
    }
}
