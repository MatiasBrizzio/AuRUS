package utils;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import owl.ltl.Conjunction;
import owl.ltl.Formula;
import owl.ltl.GOperator;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.visitors.SolverSyntaxOperatorReplacer;
import solvers.LTLSolver;

public class TopologicalSort {
    private int vertices;
    private HashMap<Integer, HashSet<Integer>> adj;
    private Map<Integer, Integer> newToOldIndex;

    public TopologicalSort(int v) {
        vertices = v;
        adj = new HashMap<>();
        for (int i = 0; i < v; ++i)
            adj.put(i, new HashSet<>());
    }

    void addEdge(int u, int v) {
        adj.get(u).add(v);
    }

    void dfs(int v, boolean[] visited, Stack<Integer> stack) {
        visited[v] = true;
        HashSet<Integer> neighbors = adj.get(v);
        if (neighbors != null) {
            for (int neighbor : neighbors) {
                if (!visited[neighbor])
                    dfs(neighbor, visited, stack);
            }
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

    private Map<Integer, Integer> removeEquivalentSpecs() {
        Map<Integer, Integer> oldToNewIndex = new HashMap<>();
        Set<Integer> toRemove = new HashSet<>();
        // Find equivalent spec pairs
        for (int i = 0; i < vertices; i++) {
            for (int j = i + 1; j < vertices; j++) {
                if (adj.containsKey(i) && adj.containsKey(j) &&
                    adj.get(i).contains(j) && adj.get(j).contains(i)) {
                    toRemove.add(j);
                }
            }
        }
        // Create mapping from old indices to new indices
        int newIndex = 0;
        for (int i = 0; i < vertices; i++) {
            if (!toRemove.contains(i)) {
                oldToNewIndex.put(i, newIndex);
                newIndex++;
            }
        }
        // Rebuild adjacency list with new indices
        HashMap<Integer, HashSet<Integer>> newAdj = new HashMap<>();
        // First, initialize all vertices with empty sets
        for (int i = 0; i < newIndex; i++) {
            newAdj.put(i, new HashSet<>());
        }
        // Then populate with edges
        for (int oldI : oldToNewIndex.keySet()) {
            int newI = oldToNewIndex.get(oldI);
            for (int oldNeighbor : adj.get(oldI)) {
                if (oldToNewIndex.containsKey(oldNeighbor)) {
                    newAdj.get(newI).add(oldToNewIndex.get(oldNeighbor));
                }
            }
        }
        adj = newAdj;
        vertices = newIndex;
        
        // Create reverse mapping (new to old)
        newToOldIndex = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : oldToNewIndex.entrySet()) {
            newToOldIndex.put(entry.getValue(), entry.getKey());
        }
        
        return oldToNewIndex;
    }

    public HashMap<Integer, HashSet<Integer>> getAdjacencyList() {
        return adj;
    }

    public List<Integer> sort(List<Tlsf> specs) throws IOException, InterruptedException {
        addSpecs(specs);
        removeEquivalentSpecs();
        Stack<Integer> stack = new Stack<>();
        System.out.println("vertices: " + vertices);
        boolean[] visited = new boolean[vertices];
        for (int i = 0; i < vertices; i++) {
            if (!visited[i])
                dfs(i, visited, stack);
        }
        List<Integer> sortedIndices = new ArrayList<>();
        while (!stack.isEmpty()) {
            int newIndex = stack.pop();
            // Map back to original index if deduplication occurred
            int originalIndex = (newToOldIndex != null) ? newToOldIndex.get(newIndex) : newIndex;
            sortedIndices.add(originalIndex);
        }
        return sortedIndices;
    }
}
