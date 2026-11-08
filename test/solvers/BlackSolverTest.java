package solvers;

import main.Settings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import owl.ltl.Formula;
import owl.ltl.parser.LtlParser;
import owl.ltl.visitors.SolverSyntaxOperatorReplacer;
import solvers.LTLSolver.SolverResult;
import utils.SolverUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The BLACK backend of {@link LTLSolver}. Requires a {@code black} binary on
 * the {@code PATH}; without one these tests are skipped.
 */
class BlackSolverTest {

    private static final List<String> VARS = List.of("a", "b", "c");

    private static final String UNSAT_INPUT = "( G (!p0|| X p1)&& F (p0&& G !p1))";

    private static final List<String> HAND_WRITTEN = List.of(
            "a", "!a", "a & !a", "a | !a", "a -> b", "a <-> b", "X a", "X X a", "F a", "G a",
            "F G a", "G F a", "a U b", "a W b", "a R b", "a M b", "G a & F !a", "F a & G !a",
            "G (a & X !a)", "(a U b) & G !b", "(a W b) & G !a & G !b", "(a M b) & G !a",
            "(a R b) & F !b & G !a", "!(a W b) & (G a | (a U b))", "!(a M b) & (b U (a & b))",
            "G(a -> X b) & !G(a -> F b)", "G(a -> X b) & F(a & G !b)", "G F (a & b)", "G a & F b",
            "G F a & F G !a", "G X a", "F G a & G F b & G (a -> !b)", "(a U b) U c",
            "a U (b U c)", "G (a -> F b) & G (b -> F c) & G (c -> F a)", "X (a & X (b & X c))");

    private String savedSolver;
    private String savedBin;

    @BeforeEach
    void setUp() throws Exception {
        savedSolver = Settings.SAT_SOLVER;
        savedBin = Settings.SAT_BIN;
        Settings.SAT_SOLVER = "black";
        Settings.SAT_BIN = "";
        assumeTrue(blackInstalled(), "black is not on the PATH");
    }

    @AfterEach
    void tearDown() {
        Settings.SAT_SOLVER = savedSolver;
        Settings.SAT_BIN = savedBin;
    }

    private static boolean blackInstalled() throws InterruptedException {
        try {
            return new ProcessBuilder("black", "--sat-backends").start().waitFor() == 0;
        } catch (IOException e) {
            return false;
        }
    }

    private static Formula parse(String formula) {
        return LtlParser.parse(formula, VARS).formula();
    }

    private static String input(String formula) {
        return SolverUtils.toSolverSyntax(parse(formula).accept(new SolverSyntaxOperatorReplacer()));
    }

    private static SolverResult solve(String formula) throws Exception {
        return LTLSolver.isSAT(input(formula));
    }

    private static String script(Path dir, String body) throws IOException {
        Path file = Files.createTempFile(dir, "solver", ".sh");
        Files.writeString(file, "#!/bin/sh\n" + body + "\n");
        assertTrue(file.toFile().setExecutable(true));
        return file.toString();
    }

    private static String randomFormula(Random random, int depth) {
        String[] atoms = {"a", "b", "c", "!a", "!b"};
        if (depth == 0 || random.nextInt(5) == 0) {
            return atoms[random.nextInt(atoms.length)];
        }
        String[] unary = {"!", "X ", "F ", "G "};
        String[] binary = {"&", "|", "->", "U", "W", "R", "M"};
        if (random.nextBoolean()) {
            return unary[random.nextInt(unary.length)] + "(" + randomFormula(random, depth - 1) + ")";
        }
        return "(" + randomFormula(random, depth - 1) + ") " + binary[random.nextInt(binary.length)]
                + " (" + randomFormula(random, depth - 1) + ")";
    }

    @Test
    void agreesWithAaltaRunSequentially() throws Exception {
        List<String> formulas = new ArrayList<>(HAND_WRITTEN);
        Random random = new Random(20261108L);
        for (int i = 0; i < 300; i++) {
            formulas.add(randomFormula(random, 4));
        }

        int sat = 0;
        int unsat = 0;
        List<String> differences = new ArrayList<>();
        for (String formula : formulas) {
            Settings.SAT_SOLVER = "black";
            SolverResult black = solve(formula);
            Settings.SAT_SOLVER = "aalta";
            SolverResult aalta = solve(formula);
            if (black == SolverResult.SAT) sat++;
            if (black == SolverResult.UNSAT) unsat++;
            if (black != aalta) {
                differences.add(formula + " => black=" + black + " aalta=" + aalta + " input=" + input(formula));
            }
        }
        System.out.printf("%nAGREEMENT formulas=%d blackSat=%d blackUnsat=%d differences=%d%n",
                formulas.size(), sat, unsat, differences.size());
        differences.forEach(d -> System.out.println("DIFF " + d));
        assertTrue(formulas.size() >= 200);
        assertEquals(List.of(), differences);
    }

    @Test
    void knownResults() throws Exception {
        assertEquals(SolverResult.UNSAT, solve("G(a -> X b) & !G(a -> F b)"));
        assertEquals(SolverResult.UNSAT, solve("F a & G !a"));
        assertEquals(SolverResult.SAT, solve("G F (a & b)"));
        assertEquals(SolverResult.SAT, solve("G a & F b"));
        assertEquals(SolverResult.UNSAT, solve("G F a & F G !a"));
        assertEquals(SolverResult.SAT, solve("a W b"));
        assertEquals(SolverResult.SAT, solve("a M b"));
    }

    @Test
    void rewrittenWeakUntilAndStrongReleaseHaveTheCorrectMeaning() throws Exception {
        assertEquals(SolverResult.UNSAT, solve("!(a W b) & (G a | (a U b))"));
        assertEquals(SolverResult.UNSAT, solve("(a M b) & G !a"));
    }

    @Test
    void usesInfiniteTraceSemantics() throws Exception {
        // Satisfiable on infinite traces, unsatisfiable under --finite: the last
        // state of a finite trace has no successor for the strong X.
        assertEquals(SolverResult.SAT, LTLSolver.isSAT(" G  X a"));
    }

    @Test
    void booleanConstantsAreNotReadAsPropositions() throws Exception {
        assertEquals(SolverResult.UNSAT, LTLSolver.isSAT("false"));
        assertEquals(SolverResult.SAT, LTLSolver.isSAT("true"));
        assertEquals(SolverResult.UNSAT, LTLSolver.isSAT("(a && false)"));
    }

    @Test
    void concurrentRunsInTheSameDirectoryAllAnswerUnsat() throws Exception {
        int runs = 60;
        ExecutorService pool = Executors.newFixedThreadPool(runs);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<SolverResult>> futures = new ArrayList<>();
            for (int i = 0; i < runs; i++) {
                Callable<SolverResult> task = () -> {
                    start.await();
                    return LTLSolver.isSAT(UNSAT_INPUT);
                };
                futures.add(pool.submit(task));
            }
            start.countDown();
            for (Future<SolverResult> future : futures) {
                assertEquals(SolverResult.UNSAT, future.get());
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void operatorsNeedSpacesBeforeTheirOperand() throws Exception {
        // BLACK's symbols are [a-zA-Z_][a-zA-Z0-9_]*, so "Fp1" and "Xp1" written
        // without a space are propositions. toSolverSyntax has to separate them.
        Formula eventually = parse("F a & G !a & (b | c)");
        assertTrue(SolverUtils.toSolverSyntax(eventually).contains(" F p"));
        assertEquals(SolverResult.UNSAT, LTLSolver.isSAT("F p1 && G !p1"));
        assertEquals(SolverResult.SAT, LTLSolver.isSAT("Fp1 && G !p1"));

        Formula next = parse("G(a -> X b) & !G(a -> F b)");
        assertTrue(SolverUtils.toSolverSyntax(next).contains(" X p1"));
        assertEquals(SolverResult.UNSAT, LTLSolver.isSAT(SolverUtils.toSolverSyntax(next)));
        assertEquals(SolverResult.SAT, LTLSolver.isSAT(next.toString()));
    }

    @Test
    void missingBinaryIsNotUnsat() throws Exception {
        Settings.SAT_BIN = "/nonexistent/black";
        assertEquals(SolverResult.ERROR, LTLSolver.isSAT(UNSAT_INPUT));
    }

    @Test
    void anythingButAConclusiveVerdictIsNotUnsat(@TempDir Path dir) throws Exception {
        for (String body : List.of("echo garbage", "exit 0", "echo sat", "echo unsat", "echo UNSAT; exit 3", "exit 4")) {
            Settings.SAT_BIN = script(dir, body);
            assertEquals(SolverResult.ERROR, LTLSolver.isSAT(UNSAT_INPUT), body);
        }
    }

    @Test
    void unknownIsInconclusive(@TempDir Path dir) throws Exception {
        Settings.SAT_BIN = script(dir, "echo 'UNKNOWN (stopped at k = 1)'");
        SolverResult result = LTLSolver.isSAT(UNSAT_INPUT);
        assertEquals(SolverResult.UNKNOWN, result);
        assertTrue(result.inconclusive());
    }
}
