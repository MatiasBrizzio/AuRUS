package main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* Parse DOT file and find maximal specifications (those with no incoming edges) */

public class MaximalSpecs {

    private static List<String> getMaximalSpecsFromDot(String dotFilePath) throws IOException {
        String dotContent = Files.readString(Paths.get(dotFilePath), StandardCharsets.UTF_8);
        Set<String> allNodes = new HashSet<>();
        Set<String> nodesWithIncomingEdges = new HashSet<>();
        // Pattern to match node declarations: "spec123.tlsf";
        Pattern nodePattern = Pattern.compile("\"([^\"]+\\.tlsf)\"\\s*;");
        Matcher nodeMatcher = nodePattern.matcher(dotContent);
        while (nodeMatcher.find()) {
            allNodes.add(nodeMatcher.group(1));
        }
        // Pattern to match edges: "source.tlsf" -> "target.tlsf";
        Pattern edgePattern = Pattern.compile("\"([^\"]+\\.tlsf)\"\\s*->\\s*\"([^\"]+\\.tlsf)\"");
        Matcher edgeMatcher = edgePattern.matcher(dotContent);
        while (edgeMatcher.find()) {
            String target = edgeMatcher.group(2);
            nodesWithIncomingEdges.add(target);
        }
        // Maximal specs are those with no incoming edges
        allNodes.removeAll(nodesWithIncomingEdges);
        return new ArrayList<>(allNodes);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        String dotFilePath = "";
        for (String arg : args) {
            if (arg.startsWith("-dot=")) {
                dotFilePath = arg.replace("-dot=", "");
            }
        }
        if (dotFilePath.isEmpty()) {
            System.out.println("DOT file path is missing. Use -dot=<path_to_dot_file>");
            System.exit(0);
        }
        List<String> maximalSpecs = getMaximalSpecsFromDot(dotFilePath);
        System.out.println("Maximal specifications (no incoming edges):");
        for (String spec : maximalSpecs) {
            System.out.println("  " + spec);
        }
        System.out.println("\nTotal: " + maximalSpecs.size() + " maximal specifications");
    }
}
