package solvers;

import main.Settings;
import utils.SolverUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Runs an external LTL satisfiability solver on one formula.
 *
 * <p>Each call runs in a fresh temporary directory, which is deleted at the
 * end of the call, also after a timeout or an exception. aalta writes a work
 * file ({@code cnf.dimacs}) in its current directory, so calls sharing a
 * directory overwrite each other's file and give wrong answers.</p>
 *
 * <p>The solver is {@link Settings#SAT_SOLVER}: {@code aalta}, {@code black},
 * or {@code custom}, which runs {@link Settings#SAT_CMD} and reads the answer
 * with {@link Settings#SAT_SAT_PATTERN} and {@link Settings#SAT_UNSAT_PATTERN}.
 * {@link SolverResult#UNSAT} is returned only when the solver exits with 0 and
 * prints an unsatisfiability verdict; any other outcome is {@code ERROR},
 * {@code UNKNOWN} or {@code TIMEOUT}.</p>
 */
public class LTLSolver {

    public static int numOfTimeout = 0;
    public static int numOfError = 0;
    public static int numOfCalls = 0;

    /** Placeholder for the formula, as one argument, in a command template. */
    public static final String FORMULA_PLACEHOLDER = "{formula}";
    /** Placeholder for the absolute path of a file holding the formula. */
    public static final String FILE_PLACEHOLDER = "{file}";

    private static final String FORMULA_FILE = "formula.ltl";
    private static final String STDOUT_FILE = "solver.out";
    private static final String STDERR_FILE = "solver.err";

    /** How to run a solver and read its answer. */
    static final class SolverConfig {
        final String name;
        final List<String> command;
        final boolean blackSyntax;
        final Pattern sat;
        final Pattern unsat;
        final Pattern unknown;
        final boolean stderrIsError;

        SolverConfig(String name, List<String> command, boolean blackSyntax,
                     Pattern sat, Pattern unsat, Pattern unknown, boolean stderrIsError) {
            this.name = name;
            this.command = command;
            this.blackSyntax = blackSyntax;
            this.sat = sat;
            this.unsat = unsat;
            this.unknown = unknown;
            this.stderrIsError = stderrIsError;
        }
    }

    private static synchronized void count(boolean call, boolean timeout, boolean error) {
        if (call) numOfCalls++;
        if (timeout) numOfTimeout++;
        if (error) numOfError++;
    }

    private static String defaultAalta() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("mac") ? "lib/aalta" : "lib/aalta_linux";
    }

    /**
     * The configuration selected by {@link Settings}.
     *
     * @throws IllegalArgumentException if the settings do not describe a usable solver
     */
    static SolverConfig currentConfig() {
        String solver = Settings.SAT_SOLVER.toLowerCase();
        switch (solver) {
            case "aalta": {
                String bin = Settings.SAT_BIN.isEmpty() ? defaultAalta() : Settings.SAT_BIN;
                // A stderr line from aalta has always made the answer an error.
                return new SolverConfig("aalta", List.of(bin, FORMULA_PLACEHOLDER), false,
                        Pattern.compile("sat|.*Formula 1: satisfiable.*"),
                        Pattern.compile("unsat|.*Formula 1: unsatisfiable.*"),
                        null, true);
            }
            case "black": {
                String bin = Settings.SAT_BIN.isEmpty() ? "black" : Settings.SAT_BIN;
                // Infinite-trace semantics is BLACK's default and has no flag of its
                // own: it holds as long as --finite is never passed. The backend is
                // pinned so that macOS and the Linux package use the same one.
                // W and M must be rewritten first (SolverSyntaxOperatorReplacer):
                // BLACK 26.05.0 accepts them but answers wrongly.
                return new SolverConfig("black", List.of(bin, "solve", "-B", "z3", "-f", FORMULA_PLACEHOLDER), true,
                        Pattern.compile("SAT"), Pattern.compile("UNSAT"), Pattern.compile("UNKNOWN.*"), false);
            }
            case "custom": {
                List<String> command = splitCommand(Settings.SAT_CMD);
                if (command.isEmpty())
                    throw new IllegalArgumentException("-sat=custom needs -sat-cmd");
                if (command.stream().noneMatch(t -> t.contains(FORMULA_PLACEHOLDER) || t.contains(FILE_PLACEHOLDER)))
                    throw new IllegalArgumentException("-sat-cmd must contain " + FORMULA_PLACEHOLDER
                            + " or " + FILE_PLACEHOLDER + ": " + Settings.SAT_CMD);
                if (!Settings.SAT_BIN.isEmpty()) {
                    command = new ArrayList<>(command);
                    command.set(0, Settings.SAT_BIN);
                }
                String syntax = Settings.SAT_SYNTAX.toLowerCase();
                if (!syntax.equals("aalta") && !syntax.equals("black"))
                    throw new IllegalArgumentException("unknown -sat-syntax: " + Settings.SAT_SYNTAX);
                return new SolverConfig("custom", command, syntax.equals("black"),
                        Pattern.compile(Settings.SAT_SAT_PATTERN), Pattern.compile(Settings.SAT_UNSAT_PATTERN),
                        null, false);
            }
            default:
                throw new IllegalArgumentException("unknown SAT solver: " + Settings.SAT_SOLVER);
        }
    }

    /**
     * Applies one satisfiability-solver option ({@code -sat=}, {@code -sat-bin=},
     * {@code -sat-cmd=}, {@code -sat-syntax=}, {@code -sat-sat-pattern=},
     * {@code -sat-unsat-pattern=}) to {@link Settings}. Shared by every command line.
     *
     * @param arg one command-line argument
     * @return {@code true} iff the argument was a solver option
     * @throws IllegalArgumentException if its value is invalid
     */
    public static boolean applyOption(String arg) {
        String value;
        if ((value = valueOf(arg, "-sat=")) != null) {
            String sat = value.toLowerCase();
            if (!sat.equals("aalta") && !sat.equals("black") && !sat.equals("custom"))
                throw new IllegalArgumentException("unknown value for -sat: '" + value
                        + "' (expected 'aalta', 'black' or 'custom')");
            Settings.SAT_SOLVER = sat;
        } else if ((value = valueOf(arg, "-sat-bin=")) != null) {
            Settings.SAT_BIN = value;
        } else if ((value = valueOf(arg, "-sat-cmd=")) != null) {
            Settings.SAT_CMD = value;
            Settings.SAT_SOLVER = "custom";
        } else if ((value = valueOf(arg, "-sat-syntax=")) != null) {
            Settings.SAT_SYNTAX = value;
        } else if ((value = valueOf(arg, "-sat-sat-pattern=")) != null) {
            Settings.SAT_SAT_PATTERN = value;
        } else if ((value = valueOf(arg, "-sat-unsat-pattern=")) != null) {
            Settings.SAT_UNSAT_PATTERN = value;
        } else {
            return false;
        }
        return true;
    }

    /**
     * Checks that the solver settings describe a usable solver, so that a bad
     * {@code -sat-cmd} or pattern stops the run before the search starts.
     *
     * @throws IllegalArgumentException with the reason otherwise
     */
    public static void validateSettings() {
        try {
            currentConfig();
        } catch (java.util.regex.PatternSyntaxException e) {
            throw new IllegalArgumentException("invalid solver pattern: " + e.getMessage(), e);
        }
    }

    private static String valueOf(String arg, String prefix) {
        return arg.startsWith(prefix) ? arg.substring(prefix.length()) : null;
    }

    /** Splits a command template on whitespace; no shell is involved. */
    public static List<String> splitCommand(String command) {
        List<String> tokens = new ArrayList<>();
        for (String token : command.trim().split("\\s+")) {
            if (!token.isEmpty()) tokens.add(token);
        }
        return tokens;
    }

    /**
     * A path with a directory part is resolved against the AuRUS directory
     * (the JVM's working directory), because the solver runs in a temporary
     * one. A bare name is left to the {@code PATH} lookup.
     */
    private static String resolveExecutable(String bin) {
        if (!bin.contains("/")) return bin;
        Path path = Paths.get(bin);
        return path.isAbsolute() ? bin : Paths.get(System.getProperty("user.dir")).resolve(path).normalize().toString();
    }

    /**
     * Decides whether a formula, in {@link SolverUtils#toSolverSyntax} form, has a model.
     *
     * @param formula the formula in aalta syntax; BLACK receives {@link SolverUtils#toBlackSyntax} of it
     * @return {@code UNSAT} only on an explicit unsatisfiability verdict
     */
    public static SolverResult isSAT(String formula) throws IOException, InterruptedException {
        count(true, false, false);
        if (formula == null) {
            System.out.println("ERR: no formula");
            count(false, false, true);
            return SolverResult.ERROR;
        }
        SolverConfig config;
        try {
            config = currentConfig();
        } catch (IllegalArgumentException e) { // includes PatternSyntaxException
            System.out.println("ERR: " + e.getMessage());
            count(false, false, true);
            return SolverResult.ERROR;
        }
        return run(config, formula);
    }

    static SolverResult run(SolverConfig config, String formula) throws IOException, InterruptedException {
        String input = config.blackSyntax ? SolverUtils.toBlackSyntax(formula) : formula;
        Path dir = Files.createTempDirectory("aurus-sat-");
        Process p = null;
        try {
            Path formulaFile = dir.resolve(FORMULA_FILE);
            List<String> command = new ArrayList<>();
            for (int i = 0; i < config.command.size(); i++) {
                String token = config.command.get(i);
                if (i == 0) {
                    command.add(resolveExecutable(token));
                    continue;
                }
                if (token.contains(FILE_PLACEHOLDER)) {
                    Files.writeString(formulaFile, input + "\n", StandardCharsets.UTF_8);
                    token = token.replace(FILE_PLACEHOLDER, formulaFile.toString());
                }
                command.add(token.replace(FORMULA_PLACEHOLDER, input));
            }
            ProcessBuilder builder = new ProcessBuilder(command)
                    .directory(dir.toFile())
                    .redirectOutput(dir.resolve(STDOUT_FILE).toFile())
                    .redirectError(dir.resolve(STDERR_FILE).toFile());
            try {
                p = builder.start();
            } catch (IOException e) {
                System.out.println("ERR: " + e.getMessage());
                count(false, false, true);
                return SolverResult.ERROR;
            }

            if (!p.waitFor(Settings.SAT_TIMEOUT, TimeUnit.SECONDS)) {
                kill(p);
                count(false, true, false);
                return SolverResult.TIMEOUT;
            }
            List<String> out = Files.readAllLines(dir.resolve(STDOUT_FILE), StandardCharsets.UTF_8);
            List<String> err = Files.readAllLines(dir.resolve(STDERR_FILE), StandardCharsets.UTF_8);
            return verdict(config, p.exitValue(), out, err, formula);
        } finally {
            // Listing descendants scans the whole process table, so it is only
            // done for a process still running (timeout or exception).
            if (p != null && p.isAlive()) kill(p);
            deleteRecursively(dir);
        }
    }

    /** Only an exact verdict on a clean exit counts: anything else is inconclusive. */
    static SolverResult verdict(SolverConfig config, int exit, List<String> out, List<String> err, String formula) {
        for (String line : err) {
            System.out.println("ERR: " + line);
        }
        boolean sat = false;
        boolean unsat = false;
        boolean unknown = false;
        for (String raw : out) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (config.unsat.matcher(line).matches()) unsat = true;
            else if (config.sat.matcher(line).matches()) sat = true;
            else if (config.unknown != null && config.unknown.matcher(line).matches()) unknown = true;
        }
        boolean clean = exit == 0 && !(config.stderrIsError && !err.isEmpty());
        if (clean && sat != unsat && !unknown) return sat ? SolverResult.SAT : SolverResult.UNSAT;
        if (clean && unknown && !sat && !unsat) return SolverResult.UNKNOWN;
        String answer = out.stream().map(String::trim).filter(l -> !l.isEmpty()).collect(Collectors.joining(" | "));
        String reason;
        if (exit != 0) reason = "non-zero exit";
        else if (!clean) reason = "output on stderr";
        else if (sat && unsat) reason = "both a sat and an unsat line";
        else reason = "no output line matches the sat pattern '" + config.sat.pattern()
                    + "' or the unsat pattern '" + config.unsat.pattern() + "'";
        System.out.println(config.name + ": " + reason + "; exit value = " + exit + ", answer = '" + answer + "'");
        System.out.println(formula);
        count(false, false, true);
        return SolverResult.ERROR;
    }

    /** Kills the process and every process it started, and waits for them. */
    private static void kill(Process p) {
        List<ProcessHandle> descendants = p.descendants().collect(Collectors.toList());
        descendants.forEach(ProcessHandle::destroyForcibly);
        p.destroyForcibly();
        boolean interrupted = false;
        List<ProcessHandle> all = new ArrayList<>(descendants);
        all.add(p.toHandle());
        for (ProcessHandle handle : all) {
            try {
                handle.onExit().get(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                interrupted = true;
            } catch (Exception e) {
                System.out.println("ERR: process " + handle.pid() + " still alive after kill");
            }
        }
        if (interrupted) Thread.currentThread().interrupt();
    }

    private static void deleteRecursively(Path dir) {
        try (Stream<Path> files = Files.walk(dir)) {
            files.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    System.out.println("ERR: could not delete " + path + ": " + e.getMessage());
                }
            });
        } catch (IOException e) {
            System.out.println("ERR: could not delete " + dir + ": " + e.getMessage());
        }
    }

    public enum SolverResult {
        SAT,
        UNSAT,
        TIMEOUT,
        ERROR,
        UNKNOWN;

        public boolean inconclusive() {
            return this == TIMEOUT || this == ERROR || this == UNKNOWN;
        }
    }
}
