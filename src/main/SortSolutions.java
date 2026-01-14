package main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import owl.ltl.tlsf.Tlsf;
import utils.TlsfUtils;
import utils.TopologicalSort;

/* Sort solutions based on the partial order of implication
   Uses a topological sort algorithm to sort the specifications
   such that if spec A implies spec B, then A appears before B in the sorted list
*/

public class SortSolutions {
    private static String dotEscape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static List<Integer> getMaximalSpecs(HashMap<Integer, HashSet<Integer>> adjList) {
        Set<Integer> allNodes = new HashSet<>(adjList.keySet());
        Set<Integer> nonMaximalNodes = new HashSet<>();
        for (HashSet<Integer> neighbors : adjList.values()) {
            nonMaximalNodes.addAll(neighbors);
        }
        allNodes.removeAll(nonMaximalNodes);
        return new ArrayList<>(allNodes);
    }

    private static List<Tlsf> parseTlsfFiles(List<String> specifications_filenames) throws IOException, InterruptedException {
        List<Tlsf> specifications = new LinkedList<>();
        for (String filename : specifications_filenames) {
            Tlsf spec = TlsfUtils.toBasicTLSF(new File(filename));
            specifications.add(spec);
        }
        return specifications;
    }

    private static void renderDot(List<String> specifications_filenames, String directoryName, String outputName,
                                  List<Integer> sortedIndices,
                                  HashMap<Integer, HashSet<Integer>> adjList) throws IOException {
        // Build DOT graph using only deduplicated specs (from adjList keys)
        Set<Integer> dedupedIndices = adjList.keySet();
        Map<Integer, String> labels = new HashMap<>();
        for (int index : dedupedIndices) {
            labels.put(index, new File(specifications_filenames.get(sortedIndices.get(index))).getName());
        }
        StringBuilder dot = new StringBuilder();
        dot.append("digraph Implication {\n");
        dot.append("  rankdir=LR;\n");
        dot.append("  node [shape=box];\n");
        // Ensure all deduplicated nodes appear even if isolated
        for (int i : dedupedIndices) {
            dot.append("  \"")
               .append(dotEscape(labels.get(i)))
               .append("\";\n");
        }
        // Add edges from adjacency list
        for (int u : dedupedIndices) {
            for (Integer v : adjList.get(u)) {
                dot.append("  \"")
                   .append(dotEscape(labels.get(u)))
                   .append("\" -> \"")
                   .append(dotEscape(labels.get(v)))
                   .append("\";\n");
            }
        }
        dot.append("}\n");
        // Determine output path (use provided -out=... or default to implication.dot under directory)
        Path outPath = outputName.isEmpty()
            ? Paths.get(directoryName).resolve("implication.dot")
            : Paths.get(outputName);

        Files.createDirectories(outPath.getParent() == null ? Paths.get(".") : outPath.getParent());
        Files.write(outPath, dot.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("Wrote DOT graph to: " + outPath.toAbsolutePath());
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        String directoryName = "";
        String outputName = "";
        for (String arg : args) {
            if (arg.startsWith("-d=")) {
                directoryName = arg.replace("-d=", "");
            } else if (arg.startsWith("-out=")) {
                outputName = arg.replace("-out=", "");
            }
        }
        if (directoryName.isEmpty()) {
            System.out.println("directory name is missing.");
            System.exit(0);
        }

        Path dirPath = Paths.get(directoryName);
        Stream<Path> walk = Files.walk(dirPath);
        List<String> specifications_filenames = walk.map(Path::toString)
                .filter(f -> f.endsWith(".tlsf") && !f.endsWith("_basic.tlsf")).collect(Collectors.toList());
        walk.close();

        System.out.println("Found " + specifications_filenames.size() + " specifications, converting to TLSF...");
        List<Tlsf> specifications = parseTlsfFiles(specifications_filenames);

        System.out.println("Starting topological sort based on implication...");
        TopologicalSort topoSort = new TopologicalSort(specifications.size());
        List<Integer> sortedIndices = topoSort.sort(specifications);

        HashMap<Integer, HashSet<Integer>> adjList = topoSort.getAdjacencyList();

        // renderDot(specifications_filenames, directoryName, outputName, sortedIndices, adjList);
    }
}
