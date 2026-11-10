package main;

import modelcounter.AutomatonDump;
import modelcounter.CountResult;
import modelcounter.ModelCounter;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.LtlParser;
import solvers.LTLSolver;
import utils.TlsfUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Command-line model counter: {@code ./count-models.sh [options] FORMULA|FILE.tlsf}.
 * Prints one JSON line on standard output.
 */
public final class CountModels {

    private static final int EXIT_OK = 0;
    private static final int EXIT_FAILURE = 1;
    private static final int EXIT_USAGE = 2;

    private CountModels() {
    }

    public static void main(String[] args) {
        // Solvers and the counter log to standard output; keep it for the JSON.
        PrintStream json = System.out;
        System.setOut(System.err);
        int exit = run(args, json);
        json.flush();
        // Timed-out and idle counter threads would otherwise keep the JVM alive.
        System.exit(exit);
    }

    static int run(String[] args, PrintStream out) {
        int bound = Settings.MC_BOUND;
        List<String> vars = null;
        boolean dump = false;
        String input = null;
        boolean countOnly = Arrays.asList(args).contains("-c") || Arrays.asList(args).contains("-count");
        // With -c, standard output holds a number or nothing at all.
        PrintStream errors = countOnly ? System.err : out;

        try {
            for (String arg : args) {
                if (arg.equals("--help") || arg.equals("-h")) {
                    printHelp(out);
                    return EXIT_OK;
                } else if (arg.startsWith("-k=")) {
                    bound = Integer.parseInt(arg.substring(3));
                } else if (arg.startsWith("-vars=")) {
                    vars = Arrays.stream(arg.substring(6).split(",")).map(String::trim).collect(Collectors.toList());
                } else if (arg.startsWith("-sat=") || arg.startsWith("-sat-")) {
                    try {
                        if (!LTLSolver.applyOption(arg))
                            return usage(errors, "unknown option: " + arg);
                    } catch (IllegalArgumentException e) {
                        return usage(errors, e.getMessage());
                    }
                } else if (arg.startsWith("--timeout=")) {
                    int seconds = Integer.parseInt(arg.substring(10));
                    Settings.PARSING_TIMEOUT = seconds;
                    Settings.MC_TIMEOUT = seconds;
                } else if (arg.equals("--dump")) {
                    dump = true;
                } else if (arg.equals("-c") || arg.equals("-count")) {
                    continue;
                } else if (arg.startsWith("-")) {
                    return usage(errors, "unknown option: " + arg);
                } else if (input != null) {
                    return usage(errors, "more than one formula given");
                } else {
                    input = arg;
                }
            }
        } catch (NumberFormatException e) {
            return usage(errors, "invalid number: " + e.getMessage());
        }
        if (input == null)
            return usage(errors, "no formula given; see --help");
        try {
            LTLSolver.validateSettings();
        } catch (IllegalArgumentException e) {
            return usage(errors, e.getMessage());
        }
        if (countOnly && dump)
            return usage(errors, "-c and --dump cannot be combined");
        if (input.endsWith(".tlsf") && vars != null)
            return usage(errors, "-vars cannot be used with a TLSF file, which declares its own variables");

        long start = System.nanoTime();
        try {
            LabelledFormula formula = input.endsWith(".tlsf") ? fromTlsf(new File(input))
                    : vars != null ? LtlParser.parse(input, vars) : LtlParser.parse(input);
            if (dump && formula.variables().size() > AutomatonDump.MAX_VARIABLES)
                return failure(errors, "alphabet too large to dump: " + formula.variables().size()
                        + " variables (maximum " + AutomatonDump.MAX_VARIABLES + ")", start);

            CountResult result = ModelCounter.count(formula, bound);
            if (countOnly && !result.isFailure()) {
                out.println(result.count());
                return EXIT_OK;
            }
            StringBuilder line = new StringBuilder("{\"status\":\"").append(result.status().label()).append('"');
            if (result.isFailure()) {
                line.append(",\"detail\":\"").append(result.status().name().toLowerCase()).append('"');
            } else {
                line.append(",\"count\":\"").append(result.count()).append('"');
            }
            line.append(",\"ms\":").append(millisSince(start));
            if (dump && result.status() == CountResult.Status.OK) {
                Optional<AutomatonDump> automaton = ModelCounter.dump(formula);
                automaton.ifPresent(a -> line.append(",\"dump\":").append(toJson(a)));
            }
            errors.println(line.append('}'));
            return result.isFailure() ? EXIT_FAILURE : EXIT_OK;
        } catch (IOException | InterruptedException | RuntimeException e) {
            if (e instanceof InterruptedException)
                Thread.currentThread().interrupt();
            return failure(errors,e.toString(), start);
        }
    }

    /** Translates a copy, because the syfco step writes next to its input. */
    private static LabelledFormula fromTlsf(File spec) throws IOException, InterruptedException {
        if (!spec.isFile())
            throw new IOException("file not found: " + spec);
        Path dir = Files.createTempDirectory("count-models");
        try {
            Path copy = dir.resolve("spec.tlsf");
            Files.copy(spec.toPath(), copy);
            return TlsfUtils.toBasicTLSF(copy.toFile()).toFormula();
        } finally {
            try (Stream<Path> files = Files.walk(dir)) {
                files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }

    private static long millisSince(long start) {
        return (System.nanoTime() - start) / 1_000_000L;
    }

    private static int failure(PrintStream out, String message, long start) {
        out.println("{\"status\":\"error\",\"detail\":" + quote(message) + ",\"ms\":" + millisSince(start) + "}");
        return EXIT_FAILURE;
    }

    private static int usage(PrintStream out, String message) {
        out.println("{\"status\":\"error\",\"detail\":" + quote(message) + ",\"ms\":0}");
        return EXIT_USAGE;
    }

    private static String toJson(AutomatonDump dump) {
        String transitions = dump.transitions().stream()
                .map(t -> "{\"state\":" + t.state + ",\"letter\":" + t.letter + ",\"successor\":"
                        + (t.successor == AutomatonDump.NO_SUCCESSOR ? "null" : String.valueOf(t.successor))
                        + ",\"accepting\":" + t.accepting + "}")
                .collect(Collectors.joining(","));
        String variables = dump.variables().stream().map(CountModels::quote).collect(Collectors.joining(","));
        return "{\"variables\":[" + variables + "],\"states\":" + dump.stateCount()
                + ",\"initial\":" + dump.initialStates().toString().replace(" ", "")
                + ",\"final\":" + dump.finalStates().toString().replace(" ", "")
                + ",\"transitions\":[" + transitions + "]}";
    }

    private static String quote(String text) {
        StringBuilder quoted = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            if (c == '"' || c == '\\') {
                quoted.append('\\').append(c);
            } else if (c < 0x20) {
                quoted.append(String.format("\\u%04x", (int) c));
            } else {
                quoted.append(c);
            }
        }
        return quoted.append('"').toString();
    }

    private static void printHelp(PrintStream out) {
        out.println("Usage: ./count-models.sh [options] 'FORMULA' | FILE.tlsf");
        out.println();
        out.println("Counts the words of length k accepted by the formula's automaton and prints");
        out.println("one JSON line: {\"status\":\"ok\",\"count\":\"...\",\"ms\":...}");
        out.println("status is ok, unsat_simplified, unsat_solver, timeout or error.");
        out.println("The exit code is 0 for ok and unsat_*, 1 on a failure, 2 on bad usage.");
        out.println();
        out.println("  -k=N            word length (default " + Settings.MC_BOUND + ")");
        out.println("  -vars=a,b,...   declared variables; they set the alphabet and so the count.");
        out.println("                  Default: the variables that appear in the formula.");
        out.println("                  Not allowed with TLSF files, which declare their own.");
        out.println("  -sat=NAME       satisfiability solver: aalta (default), black or custom");
        out.println("  -sat-bin=PATH   override the satisfiability solver binary");
        out.println("  -sat-cmd='CMD'  any solver: CMD with {formula} or {file}; selects -sat=custom");
        out.println("  -sat-syntax=NAME          formula syntax for -sat-cmd: aalta (default) or black");
        out.println("  -sat-sat-pattern=RE       output line meaning satisfiable (default: sat, any case)");
        out.println("  -sat-unsat-pattern=RE     output line meaning unsatisfiable (default: unsat, any case)");
        out.println("  --timeout=S     translation and counting timeouts, in seconds");
        out.println("  --dump          add the automaton to the output (up to "
                + AutomatonDump.MAX_VARIABLES + " variables, status ok only)");
        out.println("  -c, -count      print only the number; on a failure print nothing on standard");
        out.println("                  output, report on standard error and exit non-zero");
        out.println("  --help          show this help");
    }
}
