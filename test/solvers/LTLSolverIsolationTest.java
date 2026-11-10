package solvers;

import main.Settings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import owl.ltl.parser.LtlParser;
import owl.ltl.visitors.SolverSyntaxOperatorReplacer;
import solvers.LTLSolver.SolverResult;
import utils.SolverUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link LTLSolver}: one temporary directory per call, any solver through
 * {@code -sat-cmd}, and {@code UNSAT} only on an explicit verdict.
 */
class LTLSolverIsolationTest {

    private static final List<String> VARS = List.of("a", "b");
    private static final String MAIN_UNSAT = "G(a -> X b) & !G(a -> F b)";
    private static final int CONCURRENT_CALLS = 40;

    private String savedSolver;
    private String savedBin;
    private String savedCmd;
    private String savedSyntax;
    private String savedSatPattern;
    private String savedUnsatPattern;
    private int savedTimeout;

    @BeforeEach
    void save() {
        savedSolver = Settings.SAT_SOLVER;
        savedBin = Settings.SAT_BIN;
        savedCmd = Settings.SAT_CMD;
        savedSyntax = Settings.SAT_SYNTAX;
        savedSatPattern = Settings.SAT_SAT_PATTERN;
        savedUnsatPattern = Settings.SAT_UNSAT_PATTERN;
        savedTimeout = Settings.SAT_TIMEOUT;
        Settings.SAT_SOLVER = "aalta";
        Settings.SAT_BIN = "";
    }

    @AfterEach
    void restore() {
        Settings.SAT_SOLVER = savedSolver;
        Settings.SAT_BIN = savedBin;
        Settings.SAT_CMD = savedCmd;
        Settings.SAT_SYNTAX = savedSyntax;
        Settings.SAT_SAT_PATTERN = savedSatPattern;
        Settings.SAT_UNSAT_PATTERN = savedUnsatPattern;
        Settings.SAT_TIMEOUT = savedTimeout;
    }

    private static String input(String formula) {
        return SolverUtils.toSolverSyntax(
                LtlParser.parse(formula, VARS).formula().accept(new SolverSyntaxOperatorReplacer()));
    }

    private static String script(Path dir, String body) throws IOException {
        Path file = Files.createTempFile(dir, "solver", ".sh");
        Files.writeString(file, "#!/bin/sh\n" + body + "\n");
        assertTrue(file.toFile().setExecutable(true));
        return file.toString();
    }

    private static String aaltaBinary() {
        String bin = System.getProperty("os.name").toLowerCase().contains("mac") ? "lib/aalta" : "lib/aalta_linux";
        return Paths.get(bin).toAbsolutePath().toString();
    }

    private static <T> List<T> runConcurrently(int n, Callable<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(n);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private static List<Path> solverTempDirs() throws IOException {
        try (Stream<Path> entries = Files.list(Paths.get(System.getProperty("java.io.tmpdir")))) {
            return entries.filter(p -> p.getFileName().toString().startsWith("aurus-sat-")).collect(Collectors.toList());
        }
    }

    // 1. Concurrency.

    @Test
    void concurrentCallsWithAaltaAllAnswerUnsat() throws Exception {
        String formula = input(MAIN_UNSAT);
        List<SolverResult> results = runConcurrently(CONCURRENT_CALLS, () -> LTLSolver.isSAT(formula));
        long wrong = results.stream().filter(r -> r != SolverResult.UNSAT).count();
        System.out.printf("%nISOLATED %d concurrent calls: %d not UNSAT %s%n", CONCURRENT_CALLS, wrong, results);
        assertEquals(0, wrong);
    }

    /**
     * Control: the same 40 calls, made directly to the binary in one shared
     * directory, as before this change. It shows that the test above would
     * see the interference, by finding at least one wrong answer.
     */
    @Test
    void controlSharedDirectoryGivesWrongAnswers(@TempDir Path shared) throws Exception {
        String formula = input(MAIN_UNSAT);
        int rounds = 0;
        long worst = 0;
        while (rounds < 10 && worst == 0) {
            rounds++;
            List<String> answers = runConcurrently(CONCURRENT_CALLS, () -> {
                Process p = new ProcessBuilder(aaltaBinary(), formula).directory(shared.toFile())
                        .redirectErrorStream(true).start();
                String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                p.waitFor();
                return out;
            });
            long wrong = answers.stream().filter(a -> !a.equals("unsat")).count();
            worst = Math.max(worst, wrong);
            System.out.printf("%nCONTROL round %d, shared directory: %d of %d not 'unsat'; sat=%d%n", rounds, wrong,
                    CONCURRENT_CALLS, answers.stream().filter(a -> a.equals("sat")).count());
        }
        assertTrue(worst > 0, "the shared-directory control never interfered in " + rounds + " rounds");
    }

    // 2. Cleanup.

    @Test
    void noTemporaryDirectoryOrProcessIsLeft(@TempDir Path dir) throws Exception {
        List<Path> before = solverTempDirs();
        String formula = input(MAIN_UNSAT);
        for (int i = 0; i < 30; i++) {
            assertEquals(SolverResult.UNSAT, LTLSolver.isSAT(formula));
        }
        runConcurrently(20, () -> LTLSolver.isSAT(formula));

        // A forced timeout, with a solver that leaves a child process running.
        Settings.SAT_TIMEOUT = 1;
        LTLSolver.applyOption("-sat-cmd=" + script(dir, "sleep 60 &\nsleep 60\necho unsat") + " {formula}");
        assertEquals(SolverResult.TIMEOUT, LTLSolver.isSAT(formula));
        Settings.SAT_SOLVER = "aalta";
        Settings.SAT_BIN = "/nonexistent/aalta";
        assertEquals(SolverResult.ERROR, LTLSolver.isSAT(formula));

        List<Path> after = solverTempDirs();
        after.removeAll(before);
        assertEquals(List.of(), after, "temporary directories left behind");
        List<String> alive = ProcessHandle.current().descendants().filter(ProcessHandle::isAlive)
                .map(h -> h.pid() + " " + h.info().commandLine().orElse("?")).collect(Collectors.toList());
        assertEquals(List.of(), alive, "processes left alive");
    }

    @Test
    void theSolverRunsOutsideTheAurusDirectory(@TempDir Path dir) throws Exception {
        String here = Paths.get(System.getProperty("user.dir")).toRealPath().toString();
        LTLSolver.applyOption("-sat-cmd=" + script(dir,
                "if [ \"$(pwd -P)\" = \"$1\" ]; then echo shared; else echo unsat; fi") + " " + here + " {formula}");
        assertEquals(SolverResult.UNSAT, LTLSolver.isSAT("a"));
    }

    // 4. Known cases, with both built-in solvers.

    @Test
    void knownCases() throws Exception {
        for (String solver : List.of("aalta", "black")) {
            Settings.SAT_SOLVER = solver;
            assertEquals(SolverResult.UNSAT, LTLSolver.isSAT(input("F a & G !a")), solver);
            assertEquals(SolverResult.SAT, LTLSolver.isSAT(input("G F (a & b)")), solver);
            assertEquals(SolverResult.SAT, LTLSolver.isSAT(input("G a & F b")), solver);
            assertEquals(SolverResult.UNSAT, LTLSolver.isSAT(input(MAIN_UNSAT)), solver);
        }
    }

    // 5. Errors are never UNSAT.

    @Test
    void errorsAreNeverUnsat(@TempDir Path dir) throws Exception {
        String formula = input(MAIN_UNSAT);
        List<String> bodies = List.of("exit 0", "echo garbage", "exit 3", "echo unsat; exit 3",
                "echo unsat; echo oops >&2", "echo sat; echo unsat", "echo");
        for (String solver : List.of("aalta", "black")) {
            Settings.SAT_SOLVER = solver;
            Settings.SAT_BIN = "/nonexistent/" + solver;
            assertEquals(SolverResult.ERROR, LTLSolver.isSAT(formula), solver + " missing binary");
            for (String body : bodies) {
                Settings.SAT_BIN = script(dir, body);
                SolverResult result = LTLSolver.isSAT(formula);
                assertNotEquals(SolverResult.UNSAT, result, solver + ": " + body);
                assertTrue(result.inconclusive(), solver + ": " + body + " => " + result);
            }
        }
        Settings.SAT_BIN = "";
        LTLSolver.applyOption("-sat-cmd=/nonexistent/solver {formula}");
        assertEquals(SolverResult.ERROR, LTLSolver.isSAT(formula));
        for (String body : bodies) {
            if (body.contains(">&2")) continue; // see below
            LTLSolver.applyOption("-sat-cmd=" + script(dir, body) + " {formula}");
            SolverResult result = LTLSolver.isSAT(formula);
            assertNotEquals(SolverResult.UNSAT, result, "custom: " + body);
            assertTrue(result.inconclusive(), "custom: " + body + " => " + result);
        }
        // Like BLACK, and unlike aalta, a custom solver may warn on stderr: an
        // explicit verdict with exit 0 still counts.
        LTLSolver.applyOption("-sat-cmd=" + script(dir, "echo unsat; echo warning >&2") + " {formula}");
        assertEquals(SolverResult.UNSAT, LTLSolver.isSAT(formula));
    }

    @Test
    void aaltaSyntaxErrorIsNotUnsat() throws Exception {
        assertEquals(SolverResult.ERROR, LTLSolver.isSAT("(a &&"));
    }

    // 6. Any solver.

    @Test
    void genericSolverAnswersByItsArgument(@TempDir Path dir) throws Exception {
        String solver = script(dir, "[ \"$1\" = --flag ] || exit 9\n"
                + "case \"$2\" in *p0*) echo 'RESULT: UNSAT';; *) echo 'RESULT: SAT';; esac");
        assertTrue(LTLSolver.applyOption("-sat-cmd=" + solver + " --flag {formula}"));
        assertTrue(LTLSolver.applyOption("-sat-sat-pattern=RESULT: SAT"));
        assertTrue(LTLSolver.applyOption("-sat-unsat-pattern=RESULT: UNSAT"));
        assertEquals("custom", Settings.SAT_SOLVER);
        LTLSolver.validateSettings();
        assertEquals(SolverResult.UNSAT, LTLSolver.isSAT("G p0"));
        assertEquals(SolverResult.SAT, LTLSolver.isSAT("G p1"));
    }

    @Test
    void blackAsACustomSolverNeedsNoPatterns() throws Exception {
        // BLACK prints SAT/UNSAT in capitals; the default patterns ignore case.
        LTLSolver.applyOption("-sat-cmd=black solve -f {formula}");
        assertEquals(SolverResult.UNSAT, LTLSolver.isSAT(input(MAIN_UNSAT)));
        assertEquals(SolverResult.SAT, LTLSolver.isSAT(input("G a & F b")));
        LTLSolver.applyOption("-sat-syntax=black");
        assertEquals(SolverResult.UNSAT, LTLSolver.isSAT("(a && false)"));
    }

    @Test
    void genericSolverReadingAFile(@TempDir Path dir) throws Exception {
        String solver = script(dir, "if grep -q False \"$1\"; then echo unsat; else echo sat; fi");
        LTLSolver.applyOption("-sat-cmd=" + solver + " {file}");
        LTLSolver.applyOption("-sat-syntax=black");
        assertEquals(SolverResult.UNSAT, LTLSolver.isSAT("(a && false)"));
        assertEquals(SolverResult.SAT, LTLSolver.isSAT("a"));
    }

    @Test
    void badGenericSettingsAreRejected() {
        LTLSolver.applyOption("-sat-cmd=my-solver --flag");
        assertThrows(IllegalArgumentException.class, LTLSolver::validateSettings);
        LTLSolver.applyOption("-sat-cmd=my-solver {formula}");
        LTLSolver.applyOption("-sat-unsat-pattern=(");
        assertThrows(IllegalArgumentException.class, LTLSolver::validateSettings);
        assertThrows(IllegalArgumentException.class, () -> LTLSolver.applyOption("-sat=nope"));
    }
}
