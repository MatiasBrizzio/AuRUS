package modelcounter;

import geneticalgorithm.AutomataBasedModelCountingSpecificationFitness;
import main.Settings;
import modelcounter.CountResult.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.LtlParser;
import owl.ltl.parser.TlsfParser;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelCounterTest {

    private static final List<String> AB = List.of("a", "b");
    private static final String UNSAT = "G (a -> X b) & !G (a -> F b)";
    /** Large enough that translating and counting cannot finish within a zero timeout. */
    private static final List<String> WIDE =
            IntStream.range(0, 18).mapToObj(i -> "v" + i).collect(Collectors.toList());

    private String savedSolver;
    private String savedBin;
    private int savedSatTimeout;
    private int savedParsingTimeout;
    private int savedCountTimeout;
    private int savedBound;
    private boolean savedRealizability;

    @BeforeEach
    void save() {
        savedSolver = Settings.SAT_SOLVER;
        savedBin = Settings.SAT_BIN;
        savedSatTimeout = Settings.SAT_TIMEOUT;
        savedParsingTimeout = Settings.PARSING_TIMEOUT;
        savedCountTimeout = Settings.MC_TIMEOUT;
        savedBound = Settings.MC_BOUND;
        savedRealizability = Settings.check_REALIZABILITY;
    }

    @AfterEach
    void restore() {
        Settings.SAT_SOLVER = savedSolver;
        Settings.SAT_BIN = savedBin;
        Settings.SAT_TIMEOUT = savedSatTimeout;
        Settings.PARSING_TIMEOUT = savedParsingTimeout;
        Settings.MC_TIMEOUT = savedCountTimeout;
        Settings.MC_BOUND = savedBound;
        Settings.check_REALIZABILITY = savedRealizability;
    }

    private static CountResult count(String formula, List<String> vars, int k) {
        CountResult result = ModelCounter.count(LtlParser.parse(formula, vars), k);
        System.out.printf("%nformula: %s vars=%s k=%d => %s%n", formula, vars, k, result);
        return result;
    }

    private static void assertCount(long expected, String formula) {
        CountResult result = count(formula, AB, 10);
        assertEquals(Status.OK, result.status(), formula);
        assertEquals(BigInteger.valueOf(expected), result.count(), formula);
    }

    private static void assertFailure(Status expected, CountResult result) {
        assertEquals(expected, result.status());
        assertTrue(result.isFailure());
        assertThrows(IllegalStateException.class, result::count);
    }

    // The table of expected values, measured before this change.

    @Test
    void knownCounts() {
        assertCount(1024, "G a");
        assertCount(1023, "G a & F b");
        assertCount(1, "!(G a & F b) & G a");
        assertCount(1048576, "G F a");
        assertCount(78732, "G (a -> X b)");
    }

    @Test
    void unsatisfiableBySolver() {
        CountResult result = count(UNSAT, AB, 10);
        assertEquals(Status.UNSAT_SOLVER, result.status());
        assertEquals(BigInteger.ZERO, result.count());
    }

    @Test
    void unsatisfiableBySimplifier() {
        CountResult result = count("a & !a", AB, 10);
        assertEquals(Status.UNSAT_SIMPLIFIED, result.status());
        assertEquals(BigInteger.ZERO, result.count());
    }

    // The public path and the fitness path agree.

    private static String randomFormula(Random random, int depth) {
        String[] atoms = {"a", "b", "!a", "!b"};
        if (depth == 0 || random.nextInt(4) == 0) {
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
    void agreesWithTheFitnessPath() throws Exception {
        Settings.check_REALIZABILITY = false;
        Settings.MC_BOUND = 10;
        AutomataBasedModelCountingSpecificationFitness fitness = new AutomataBasedModelCountingSpecificationFitness(
                TlsfParser.parse("INFO {\n  TITLE: \"t\"\n  DESCRIPTION: \"d\"\n  SEMANTICS: Mealy\n  TARGET: Mealy\n}\n"
                        + "MAIN {\n  INPUTS { a; }\n  OUTPUTS { b; }\n  GUARANTEES { G (a -> X b); }\n}\n"));
        Method countModels = AutomataBasedModelCountingSpecificationFitness.class
                .getDeclaredMethod("countModels", LabelledFormula.class);
        countModels.setAccessible(true);

        Random random = new Random(20261109L);
        for (int i = 0; i < 80; i++) {
            String formula = randomFormula(random, 3);
            LabelledFormula labelled = LtlParser.parse(formula, AB);
            CountResult result = ModelCounter.count(labelled, 10);
            assertFalse(result.isFailure(), formula);
            assertEquals(countModels.invoke(fitness, labelled), result.count(), formula);
        }
    }

    // Failures are never a count of zero.

    @Test
    void translationTimeoutIsAFailure() {
        Settings.PARSING_TIMEOUT = 0;
        CountResult result = count("G v0", WIDE, 10);
        assertFailure(Status.TRANSLATION_TIMEOUT, result);
        assertEquals(Optional.empty(), result.fitnessCount());
    }

    @Test
    void countTimeoutIsAFailure() {
        Settings.MC_TIMEOUT = 0;
        CountResult result = count("G v0", WIDE, 10);
        assertFailure(Status.COUNT_TIMEOUT, result);
        assertEquals(Optional.empty(), result.fitnessCount());
    }

    @Test
    void solverTimeoutIsAFailureButTheFitnessKeepsTheAutomatonCount() {
        Settings.SAT_TIMEOUT = 0;
        CountResult result = count(UNSAT, AB, 10);
        assertFailure(Status.SOLVER_TIMEOUT, result);
        assertEquals(Optional.of(BigInteger.valueOf(13122)), result.fitnessCount());
    }

    @Test
    void missingSolverBinaryIsAFailure() {
        for (String solver : List.of("aalta", "black")) {
            Settings.SAT_SOLVER = solver;
            Settings.SAT_BIN = "/nonexistent/solver";
            CountResult result = count(UNSAT, AB, 10);
            assertFailure(Status.ERROR, result);
            assertEquals(Optional.of(BigInteger.valueOf(13122)), result.fitnessCount());
        }
    }

    private static String script(Path dir, String body) throws IOException {
        Path file = Files.createTempFile(dir, "solver", ".sh");
        Files.writeString(file, "#!/bin/sh\n" + body + "\n");
        assertTrue(file.toFile().setExecutable(true));
        return file.toString();
    }

    @Test
    void crashingSolverIsAFailure(@TempDir Path dir) throws Exception {
        for (String solver : List.of("aalta", "black")) {
            Settings.SAT_SOLVER = solver;
            Settings.SAT_BIN = script(dir, "exit 3");
            assertFailure(Status.ERROR, count(UNSAT, AB, 10));
        }
    }

    @Test
    void garbageFromBlackIsAFailure(@TempDir Path dir) throws Exception {
        Settings.SAT_SOLVER = "black";
        Settings.SAT_BIN = script(dir, "echo garbage");
        assertFailure(Status.ERROR, count(UNSAT, AB, 10));
    }

    // Declared variables are part of the question.

    @Test
    void anUnusedPropositionMultipliesTheCountByTwoToTheK() {
        BigInteger one = count("G a", List.of("a"), 10).count();
        BigInteger two = count("G a", AB, 10).count();
        BigInteger three = count("G a", List.of("a", "b", "c"), 10).count();
        assertEquals(one.shiftLeft(10), two);
        assertEquals(two.shiftLeft(10), three);
    }

    // The dump describes the automaton that was counted.

    private static BigInteger enumerate(int state, int remaining,
                                        Map<Integer, List<AutomatonDump.Transition>> byState, AutomatonDump dump) {
        if (remaining == 0) {
            return dump.finalStates().contains(state) ? BigInteger.ONE : BigInteger.ZERO;
        }
        BigInteger words = BigInteger.ZERO;
        for (AutomatonDump.Transition t : byState.get(state)) {
            if (t.successor != AutomatonDump.NO_SUCCESSOR) {
                words = words.add(enumerate(t.successor, remaining - 1, byState, dump));
            }
        }
        return words;
    }

    @Test
    void enumeratingAllWordsOverTheDumpGivesTheCount() {
        for (String formula : List.of("G a", "G a & F b", "!(G a & F b) & G a", "G F a", "G (a -> X b)", "a W b")) {
            AutomatonDump dump = ModelCounter.dump(LtlParser.parse(formula, AB)).orElseThrow();
            Map<Integer, List<AutomatonDump.Transition>> byState = new HashMap<>();
            for (AutomatonDump.Transition t : dump.transitions()) {
                byState.computeIfAbsent(t.state, s -> new ArrayList<>()).add(t);
            }
            BigInteger words = BigInteger.ZERO;
            for (int initial : dump.initialStates()) {
                words = words.add(enumerate(initial, 10, byState, dump));
            }
            assertEquals(count(formula, AB, 10).count(), words, formula);
        }
    }

    @Test
    void dumpRejectsLargeAlphabets() {
        assertThrows(IllegalArgumentException.class, () -> ModelCounter.dump(LtlParser.parse("G v0", WIDE)));
    }
}
