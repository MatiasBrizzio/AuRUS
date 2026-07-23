package main;

import geneticalgorithm.SpecificationChromosome;
import geneticalgorithm.SpecificationGeneticAlgorithm;
import owl.ltl.tlsf.Tlsf;
import utils.TlsfUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Command-line entry point of AuRUS.
 *
 * <p>Orchestrates a complete repair run: parses the command-line flags,
 * transfers them into {@link Settings}, loads the input specification (TLSF,
 * or Spectra with {@code -use-spectra}), launches the genetic search
 * ({@link SpecificationGeneticAlgorithm#run(Tlsf, double, double, double)},
 * or the random baseline with {@code -random}), and finally writes the
 * results:</p>
 * <ul>
 *   <li>one {@code specN.tlsf} file per realisable repair found, annotated
 *       with its fitness and its syntactic/semantic similarity to the
 *       original specification;</li>
 *   <li>an {@code out.txt} summary with solution counts, fitness statistics,
 *       timing and the full configuration — the file consumed by
 *       {@code read-results.sh} (its line format is load-bearing for the
 *       experiment scripts and must not be changed);</li>
 *   <li>optionally, when genuine reference repairs are supplied with
 *       {@code -ref=...}, a comparison of the found solutions against them
 *       (equivalent / weaker / stronger, via
 *       {@link GenuineSolutionsAnalysis}).</li>
 * </ul>
 *
 * <p>Run {@code ./unreal-repair.sh -help} for the full flag reference; the
 * same information, with defaults, is documented in the project README.</p>
 *
 * <p>Reference implementation of: <i>Brizzio, Cordy, Papadakis,
 * S&aacute;nchez, Aguirre, Degiovanni. "Automated Repair of Unrealisable LTL
 * Specifications Guided by Model Counting", GECCO 2023
 * (<a href="https://doi.org/10.1145/3583131.3590454">doi:10.1145/3583131.3590454</a>).</i></p>
 *
 * @author Mat&iacute;as Brizzio
 * @see Settings
 * @see SpecificationGeneticAlgorithm
 */
public class Main {

    /**
     * Parses the arguments, configures {@link Settings}, runs the search and
     * writes the repairs and the summary report.
     *
     * <p>Exits with status {@code 0} on success (including {@code -help}),
     * and with status {@code 1} on malformed arguments or unreadable input
     * files, after printing a targeted error message.</p>
     *
     * @param args the command-line arguments (see {@link #printUsage()})
     * @throws IOException          if writing the output files fails
     * @throws InterruptedException if an external solver call is interrupted
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        List<Tlsf> referenceSolutions = new LinkedList<>();
        int popSize = 0;
        int maxNumOfInd = 0;
        int crossoverRate = 0;
        int mutationRate = 0;
        int gene_mutationRate = 0;
        int gene_num_of_mutations = 0;
        int guaranteePreferenceRate = -1;
        boolean random_GA_selector = false;
        int generations = 0;
        boolean randomGen = false;
        double status_factor = -1.0d;
        double syntactic_factor = -1.0d;
        double semantic_factor = -1.0d;
        boolean allowGuaranteesRemoval = false;
        boolean allowAssumptionsAddition = false;
        boolean onlyInputsInAssumptions = false;
        boolean no_check_realizability = false;
        boolean strong_SAT = false;
        int bound = 0;
        boolean precise = false;
        int ga_timeout = 0;
        int real_timeout = 0;
        int sat_timeout = 0;
        int mc_timeout = 0;
        double threshold = 0.0d;
        String filename = "";
        String outname = "";
        for (String arg : args) {
            if (arg.equals("-h") || arg.equals("-help") || arg.equals("--help")) {
                printBanner();
                printUsage();
                System.exit(0);
            } else if (arg.startsWith("-Gen=")) {
                generations = parseIntArg("-Gen", arg.replace("-Gen=", ""));
            } else if (arg.startsWith("-Pop=")) {
                popSize = parseIntArg("-Pop", arg.replace("-Pop=", ""));
            } else if (arg.startsWith("-Max=")) {
                maxNumOfInd = parseIntArg("-Max", arg.replace("-Max=", ""));
            } else if (arg.startsWith("-COR=")) {
                crossoverRate = parseIntArg("-COR", arg.replace("-COR=", ""));
            } else if (arg.startsWith("-GPR=")) {
                guaranteePreferenceRate = parseIntArg("-GPR", arg.replace("-GPR=", ""));
            } else if (arg.startsWith("-MR=")) {
                mutationRate = parseIntArg("-MR", arg.replace("-MR=", ""));
            } else if (arg.startsWith("-geneMR=")) {
                gene_mutationRate = parseIntArg("-geneMR", arg.replace("-geneMR=", ""));
            } else if (arg.startsWith("-geneNUM=")) {
                gene_num_of_mutations = parseIntArg("-geneNUM", arg.replace("-geneNUM=", ""));
            } else if (arg.startsWith("-k=")) {
                bound = parseIntArg("-k", arg.replace("-k=", ""));
            } else if (arg.startsWith("-precise")) {
                precise = true;
            } else if (arg.equals("-docker")) {
                Settings.USE_DOCKER = true;
            } else if (arg.startsWith("-no-docker")) {
                Settings.USE_DOCKER = false;
            } else if (arg.startsWith("-use-spectra")) {
                Settings.USE_SPECTRA = true;
            } else if (arg.startsWith("-synth-bin=")) {
                Settings.SYNTH_BIN = arg.replace("-synth-bin=", "");
            } else if (arg.startsWith("-synth=")) {
                String synth = arg.replace("-synth=", "");
                if (!synth.equalsIgnoreCase("strix") && !synth.equalsIgnoreCase("ltlsynt")) {
                    System.err.println("ERROR: unknown value for -synth: '" + synth + "' (expected 'strix' or 'ltlsynt').");
                    System.exit(1);
                }
                Settings.SYNTH_TOOL = synth;
            } else if (arg.startsWith("-random")) {
                randomGen = true;
            } else if (arg.startsWith("-GA_random_selector")) {
                random_GA_selector = true;
            } else if (arg.startsWith("-onlySAT")) {
                no_check_realizability = true;
            } else if (arg.startsWith("-strongSAT")) {
                strong_SAT = true;
            } else if (arg.startsWith("-removeG")) {
                allowGuaranteesRemoval = true;
            } else if (arg.startsWith("-addA")) {
                allowAssumptionsAddition = true;
            } else if (arg.startsWith("-onlyInputsA")) {
                onlyInputsInAssumptions = true;
            } else if (arg.startsWith("-GATO=")) {
                ga_timeout = parseIntArg("-GATO", arg.replace("-GATO=", ""));
            } else if (arg.startsWith("-RTO=")) {
                real_timeout = parseIntArg("-RTO", arg.replace("-RTO=", ""));
            } else if (arg.startsWith("-SatTO=")) {
                sat_timeout = parseIntArg("-SatTO", arg.replace("-SatTO=", ""));
            } else if (arg.startsWith("-MCTO=")) {
                mc_timeout = parseIntArg("-MCTO", arg.replace("-MCTO=", ""));
            } else if (arg.startsWith("-sol=")) {
                threshold = parseDoubleArg("-sol", arg.replace("-sol=", ""));
            } else if (arg.startsWith("-factors")) {
                String[] factors = arg.replace("-factors=", "").split(",");
                if (factors.length != 3) {
                    System.err.println("ERROR: -factors expects exactly three comma-separated values, e.g. -factors=0.7,0.1,0.2 (got: " + arg + ")");
                    System.exit(1);
                }
                status_factor = parseDoubleArg("-factors", factors[0]);
                syntactic_factor = parseDoubleArg("-factors", factors[1]);
                semantic_factor = parseDoubleArg("-factors", factors[2]);
            } else if (arg.startsWith("-ref=")) {
                String ref_name = arg.replace("-ref=", "");
                File ref_file = new File(ref_name);
                if (!ref_file.exists()) {
                    System.err.println("ERROR: reference solution not found: " + ref_name);
                    System.exit(1);
                }
                Tlsf ref_sol = TlsfUtils.toBasicTLSF(ref_file);
                referenceSolutions.add(ref_sol);
            } else if (arg.startsWith("-out=")) {
                outname = arg.replace("-out=", "");
            } else if (arg.startsWith("-") || (!arg.endsWith(".tlsf") && !arg.endsWith(".spectra"))) {
                System.err.println("ERROR: unknown or malformed argument: " + arg);
                System.err.println("Run with -help to see the available options.");
                System.exit(1);
            } else {
                filename = arg;
            }
        }
        if (filename.isEmpty()) {
            System.err.println("ERROR: no input specification provided (expected a .tlsf or .spectra file).");
            printUsage();
            System.exit(1);
        }
        File input_file = new File(filename);
        if (!input_file.exists()) {
            System.err.println("ERROR: input specification not found: " + filename);
            System.exit(1);
        }

        printBanner();
        System.out.println("Input specification: " + filename);
        System.out.println();

        Tlsf tlsf = TlsfUtils.toBasicTLSF(input_file);
        SpecificationGeneticAlgorithm ga = new SpecificationGeneticAlgorithm();
        if (popSize > 0) Settings.GA_POPULATION_SIZE = popSize;
        if (maxNumOfInd > 0) Settings.GA_MAX_NUM_INDIVIDUALS = maxNumOfInd;
        if (crossoverRate > 0) Settings.GA_CROSSOVER_RATE = crossoverRate;
        if (mutationRate > 0) Settings.GA_MUTATION_RATE = mutationRate;
        if (gene_mutationRate > 0) Settings.GA_GENE_MUTATION_RATE = gene_mutationRate;
        if (gene_num_of_mutations >= 0) Settings.GA_GENE_NUM_OF_MUTATIONS = gene_num_of_mutations;
        if (threshold > 0.0d) Settings.GA_THRESHOLD = threshold;
        if (guaranteePreferenceRate >= 0) Settings.GA_GUARANTEES_PREFERENCE_FACTOR = guaranteePreferenceRate;
        if (generations > 0) Settings.GA_GENERATIONS = generations;
        if (ga_timeout > 0) Settings.GA_EXECUTION_TIMEOUT = ga_timeout;
        if (real_timeout > 0) Settings.STRIX_TIMEOUT = real_timeout;
        if (sat_timeout > 0) Settings.SAT_TIMEOUT = sat_timeout;
        if (mc_timeout > 0) Settings.MC_TIMEOUT = mc_timeout;
        if (bound > 0) Settings.MC_BOUND = bound;
        if (precise) Settings.MC_EXHAUSTIVE = false;
        if (allowAssumptionsAddition) Settings.allowAssumptionAddition = true;
        if (allowGuaranteesRemoval) Settings.allowGuaranteeRemoval = true;
        if (onlyInputsInAssumptions) Settings.only_inputs_in_assumptions = true;
        if (no_check_realizability) Settings.check_REALIZABILITY = false;
        if (strong_SAT) Settings.check_STRONG_SAT = true;
        if (random_GA_selector) Settings.GA_RANDOM_SELECTOR = true;

        if (!outname.isEmpty()) Settings.setStrixName(outname);

        if (randomGen)
            ga.runRandom(tlsf);
        else
            ga.run(tlsf, status_factor, syntactic_factor, semantic_factor);

        //compute statistics
        double bestFitness = 0.0d;
        double sumFitness = 0.0d;

        String directoryName = filename.substring(0, filename.lastIndexOf('.'));
        if (!outname.isEmpty()) {
            directoryName = outname;
        }

        File outfolder = new File(directoryName);
        if (!outfolder.exists() && !outfolder.mkdirs()) {
            System.err.println("Failed to create directory: " + directoryName);
        }
        List<Tlsf> solutions = new LinkedList<>();
        for (int i = 0; i < ga.solutions.size(); i++) {
            SpecificationChromosome sol = ga.solutions.get(i);
            String sol_name = directoryName + "/spec" + i + ".tlsf";
            File file = new File(sol_name);
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(TlsfUtils.adaptTLSFSpec(sol.spec));
            bw.write("\n//fitness: " + sol.fitness);
            bw.write("\n//syntactic: " + sol.syntactic_distance);
            bw.write("\n//semantic: " + sol.semantic_distance);
            bw.close();

            if (bestFitness < sol.fitness)
                bestFitness = sol.fitness;

            sumFitness += sol.fitness;

            solutions.add(sol.spec);
        }

        System.out.println();
        System.out.println("==================== AuRUS summary ====================");
        System.out.println("Num. of Solutions: " + solutions.size());
        System.out.printf("Best fitness:      %.2f%n", bestFitness);
        System.out.printf("AVG fitness:       %.2f%n", (!ga.solutions.isEmpty()) ? (sumFitness / (double) ga.solutions.size()) : 0);
        if (!solutions.isEmpty())
            System.out.println("Repairs written to: " + outfolder.getAbsolutePath()
                    + " (spec0.tlsf .. spec" + (solutions.size() - 1) + ".tlsf)");
        double genuineBestFitness = 0.0d;
        double genuineAvgFitness = 0.0d;
        double moregeneralBestFitness = 0.0d;
        double moregeneralAvgFitness = 0.0d;
        double lessgeneralBestFitness = 0.0d;
        double lessgeneralAvgFitness = 0.0d;
        double genuinesSumFitness = 0.0d;
        double moregeneralSumFitness = 0.0d;
        double lessgeneralSumFitness = 0.0d;
        if (!referenceSolutions.isEmpty()) {
            System.out.println();
            System.out.println("Comparing against " + referenceSolutions.size() + " genuine reference solution(s)...");
            //check if some genuine solution has been found
            GenuineSolutionsAnalysis.calculateGenuineStatistics(referenceSolutions, solutions);

            for (Integer index : GenuineSolutionsAnalysis.genuineSolutionsFound) {
                SpecificationChromosome c = ga.solutions.get(index);
                genuinesSumFitness += c.fitness;
                if (c.fitness > genuineBestFitness)
                    genuineBestFitness = c.fitness;
            }
            genuineAvgFitness = genuinesSumFitness / (double) GenuineSolutionsAnalysis.genuineSolutionsFound.size();
            for (Integer index : GenuineSolutionsAnalysis.moreGeneralSolutions) {
                SpecificationChromosome c = ga.solutions.get(index);
                moregeneralSumFitness += c.fitness;
                if (c.fitness > moregeneralBestFitness)
                    moregeneralBestFitness = c.fitness;
            }
            moregeneralAvgFitness = moregeneralSumFitness / (double) GenuineSolutionsAnalysis.moreGeneralSolutions.size();
            for (Integer index : GenuineSolutionsAnalysis.lessGeneralSolutions) {
                SpecificationChromosome c = ga.solutions.get(index);
                lessgeneralSumFitness += c.fitness;
                if (c.fitness > lessgeneralBestFitness)
                    lessgeneralBestFitness = c.fitness;
            }
            lessgeneralAvgFitness = lessgeneralSumFitness / (double) GenuineSolutionsAnalysis.lessGeneralSolutions.size();

            System.out.println("Genuine  (equivalent): " + GenuineSolutionsAnalysis.genuineSolutionsFound.size()
                    + " " + GenuineSolutionsAnalysis.genuineSolutionsFound.toString());
            System.out.printf("         best %.2f | avg %.2f%n", genuineBestFitness, genuineAvgFitness);
            System.out.println("Weaker   (more general): " + GenuineSolutionsAnalysis.moreGeneralSolutions.size()
                    + " " + GenuineSolutionsAnalysis.moreGeneralSolutions.toString());
            System.out.printf("         best %.2f | avg %.2f%n", moregeneralBestFitness, moregeneralAvgFitness);
            System.out.println("Stronger (less general): " + GenuineSolutionsAnalysis.lessGeneralSolutions.size()
                    + " " + GenuineSolutionsAnalysis.lessGeneralSolutions.toString());
            System.out.printf("         best %.2f | avg %.2f%n", lessgeneralBestFitness, lessgeneralAvgFitness);
            System.out.printf("Genuine precision: %.2f%n", ((double) GenuineSolutionsAnalysis.genuineSolutionsFound.size() / (double) referenceSolutions.size()));

        }
        System.out.println("=======================================================");

        //saving the time execution and configuration details
        File file = new File(directoryName + "/out.txt");
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write("Num. of Solutions:   " + solutions.size() + "\n");
        bw.write(String.format("Best fitness: %.2f\n", bestFitness));
        bw.write(String.format("AVG fitness: %.2f\n", (!ga.solutions.isEmpty()) ? (sumFitness / (double) ga.solutions.size()) : 0));
        if (!referenceSolutions.isEmpty()) {
            bw.write("Genuine Solutions:   " + GenuineSolutionsAnalysis.genuineSolutionsFound.size() + "\n");
            bw.write("Genuine Solutions found:" + GenuineSolutionsAnalysis.genuineSolutionsFound.toString() + "\n");
            bw.write(String.format("Best Genuine fitness: %.2f\n", genuineBestFitness));
            bw.write(String.format("AVG Genuine fitness: %.2f\n", genuineAvgFitness));
            bw.write("Weaker Solutions:   " + GenuineSolutionsAnalysis.moreGeneralSolutions.size() + "\n");
            bw.write("Weaker Solutions found:" + GenuineSolutionsAnalysis.moreGeneralSolutions.toString() + "\n");
            bw.write(String.format("Best Weaker fitness: %.2f\n", moregeneralBestFitness));
            bw.write(String.format("AVG Weaker fitness: %.2f\n", moregeneralAvgFitness));
            bw.write("Stronger Solutions:    " + GenuineSolutionsAnalysis.lessGeneralSolutions.size() + "\n");
            bw.write("Stronger Solutions found:" + GenuineSolutionsAnalysis.lessGeneralSolutions.toString() + "\n");
            bw.write(String.format("Best Stronger fitness: %.2f\n", lessgeneralBestFitness));
            bw.write(String.format("AVG Stronger fitness: %.2f\n", lessgeneralAvgFitness));
            bw.write(String.format("Genuine precision: %.2f \n", ((double) GenuineSolutionsAnalysis.genuineSolutionsFound.size() / (double) referenceSolutions.size())));
        }

        bw.write(ga.print_execution_time() + "\n");
        bw.write(ga.print_config() + "\n");
        bw.write("\n");
        bw.write(Settings.print_settings() + "\n");
        bw.close();

        System.exit(0);
    }

    /**
     * Parses an integer flag value, exiting with a targeted error message —
     * instead of an unhandled {@link NumberFormatException} stack trace —
     * when the value is not a valid integer.
     *
     * @param flag  the flag name, used in the error message (e.g. {@code -Gen})
     * @param value the raw value to parse
     * @return the parsed integer
     */
    private static int parseIntArg(String flag, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println("ERROR: invalid value for " + flag + ": '" + value + "' (expected an integer).");
            System.exit(1);
            return 0; // unreachable
        }
    }

    /**
     * Parses a floating-point flag value, exiting with a targeted error
     * message when the value is not a valid number.
     *
     * @param flag  the flag name, used in the error message (e.g. {@code -sol})
     * @param value the raw value to parse
     * @return the parsed double
     */
    private static double parseDoubleArg(String flag, String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            System.err.println("ERROR: invalid value for " + flag + ": '" + value + "' (expected a number).");
            System.exit(1);
            return 0.0d; // unreachable
        }
    }

    /** Prints the tool banner with the reference to the paper. */
    private static void printBanner() {
        System.out.println("=======================================================");
        System.out.println(" AuRUS - Automated Repair of Unrealisable LTL Specs");
        System.out.println(" Brizzio et al., GECCO 2023 - doi:10.1145/3583131.3590454");
        System.out.println("=======================================================");
    }

    /** Prints the usage message, with the flags grouped by concern. */
    private static void printUsage() {
        System.out.println("Usage: ./unreal-repair.sh [flags] input-file.{tlsf|spectra}");
        System.out.println();
        System.out.println("Search budget & population:");
        System.out.println("  -Gen=N               number of generations");
        System.out.println("  -Pop=N               population size per generation");
        System.out.println("  -Max=N               maximum number of individuals to generate");
        System.out.println("  -GATO=s              overall GA timeout (seconds)");
        System.out.println("  -sol=T               discard solutions with fitness below T");
        System.out.println();
        System.out.println("Genetic operators:");
        System.out.println("  -COR=r               crossover rate (% of the population)");
        System.out.println("  -MR=r                specification mutation probability (%)");
        System.out.println("  -geneMR=r            sub-formula (gene) mutation probability (%)");
        System.out.println("  -geneNUM=n           max sub-formulas mutated per formula");
        System.out.println("  -GPR=r               preference for mutating guarantees over assumptions (%)");
        System.out.println("  -addAssumptions      allow the GA to add new assumptions (-addA)");
        System.out.println("  -removeGuarantees    allow the GA to remove guarantees (-removeG)");
        System.out.println("  -onlyInputsA         restrict new assumptions to input variables");
        System.out.println("  -GA_random_selector  use a random selector instead of the best selector");
        System.out.println();
        System.out.println("Fitness function:");
        System.out.println("  -factors=S,SYN,SEM   weights of status, syntactic and semantic distance");
        System.out.println("  -k=N                 bound for the model-counting approach");
        System.out.println("  -onlySAT             check realizability only on final candidates");
        System.out.println("  -strongSAT           also check strong satisfiability in the fitness");
        System.out.println("  -precise             use the exact bounded model counter (slower)");
        System.out.println("  -random              baseline: random mutants, realizability checked at the end");
        System.out.println();
        System.out.println("External solvers & timeouts:");
        System.out.println("  -RTO=s               Strix (realizability) timeout per query");
        System.out.println("  -SatTO=s             LTL SAT-solving timeout per query");
        System.out.println("  -MCTO=s              model-counting timeout per query");
        System.out.println("  -docker              run Strix through the Docker image (recommended on macOS)");
        System.out.println("  -no-docker           use the local Strix installation (default)");
        System.out.println("  -use-spectra         treat the input as a Spectra specification");
        System.out.println("  -synth=NAME          synthesiser to use: strix (default) or ltlsynt (Docker-free)");
        System.out.println("  -synth-bin=PATH      override the synthesiser binary path/name");
        System.out.println("  -ref=file.tlsf       genuine reference solution (repeatable)");
        System.out.println("  -out=dir             output directory for the generated repairs");
        System.out.println();
        System.out.println("  -help                show this message");
    }

}