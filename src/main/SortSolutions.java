package main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
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

    public static void main(String[] args) throws IOException, InterruptedException {
        String directoryName = "";
        for (String arg : args) {
            if (arg.startsWith("-d=")) {
                directoryName = arg.replace("-d=", "");
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

        System.out.println("Found " + specifications_filenames.size() + " specifications.");

        List<Tlsf> specifications = new LinkedList<>();
        for (String filename : specifications_filenames) {
            Tlsf spec = TlsfUtils.toBasicTLSF(new File(filename));
            specifications.add(spec);
        }
        System.out.println("Converted all specifications to basic TLSF.");

        TopologicalSort topoSort = new TopologicalSort(specifications.size());

        System.out.println("Starting topological sort based on implication...");
        topoSort.sort(specifications);
    }
}
