package main;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CountModelsTest {

    /** A small strict JSON reader: numbers stay BigDecimal, so no digit can be lost. */
    private static final class Json {
        private final String text;
        private int at;

        private Json(String text) {
            this.text = text;
        }

        static Object parse(String text) {
            Json json = new Json(text);
            Object value = json.value();
            assertEquals(text.length(), json.at, "trailing characters in: " + text);
            return value;
        }

        private Object value() {
            char c = text.charAt(at);
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == '"') return string();
            for (String word : List.of("true", "false", "null")) {
                if (text.startsWith(word, at)) {
                    at += word.length();
                    return word.equals("null") ? null : Boolean.valueOf(word);
                }
            }
            int start = at;
            while (at < text.length() && "-+.eE0123456789".indexOf(text.charAt(at)) >= 0) at++;
            return new BigDecimal(text.substring(start, at));
        }

        private Map<String, Object> object() {
            Map<String, Object> map = new LinkedHashMap<>();
            at++;
            while (text.charAt(at) != '}') {
                String key = string();
                assertEquals(':', text.charAt(at++));
                map.put(key, value());
                if (text.charAt(at) == ',') at++;
            }
            at++;
            return map;
        }

        private List<Object> array() {
            List<Object> list = new ArrayList<>();
            at++;
            while (text.charAt(at) != ']') {
                list.add(value());
                if (text.charAt(at) == ',') at++;
            }
            at++;
            return list;
        }

        private String string() {
            assertEquals('"', text.charAt(at++));
            StringBuilder s = new StringBuilder();
            while (text.charAt(at) != '"') {
                if (text.charAt(at) == '\\') at++;
                s.append(text.charAt(at++));
            }
            at++;
            return s.toString();
        }
    }

    private int exit;
    private String savedSolver;
    private String savedBin;
    private int savedParsingTimeout;
    private int savedCountTimeout;

    @BeforeEach
    void save() {
        savedSolver = Settings.SAT_SOLVER;
        savedBin = Settings.SAT_BIN;
        savedParsingTimeout = Settings.PARSING_TIMEOUT;
        savedCountTimeout = Settings.MC_TIMEOUT;
    }

    @AfterEach
    void restore() {
        Settings.SAT_SOLVER = savedSolver;
        Settings.SAT_BIN = savedBin;
        Settings.PARSING_TIMEOUT = savedParsingTimeout;
        Settings.MC_TIMEOUT = savedCountTimeout;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> run(String... args) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        exit = CountModels.run(args, new PrintStream(bytes));
        String output = bytes.toString().trim();
        System.out.println("\n" + String.join(" ", args) + " => exit " + exit + ": " + output);
        assertEquals(1, output.lines().count(), "expected one line: " + output);
        return (Map<String, Object>) Json.parse(output);
    }

    private String runCountOnly(String... args) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        exit = CountModels.run(args, new PrintStream(bytes));
        return bytes.toString();
    }

    @Test
    void countOnlyPrintsJustTheNumber() {
        assertEquals("78732", runCountOnly("-c", "-k=10", "-vars=a,b", "G (a -> X b)").trim());
        assertEquals(0, exit);
        assertEquals("78732", runCountOnly("-count", "-k=10", "-vars=a,b", "G (a -> X b)").trim());
        assertEquals("0", runCountOnly("-c", "-k=10", "-vars=a,b", "G (a -> X b) & !G (a -> F b)").trim());
        assertEquals(0, exit);
        assertEquals(BigInteger.valueOf(8).pow(30).toString(),
                runCountOnly("-c", "-k=30", "-vars=a,b,c", "G F a").trim());
    }

    @Test
    void countOnlyPrintsNothingOnAFailure() {
        assertEquals("", runCountOnly("-c", "-k=10", "-vars=a,b", "-sat=black", "-sat-bin=/nonexistent/black",
                "G (a -> X b) & !G (a -> F b)"));
        assertEquals(1, exit);
        assertEquals("", runCountOnly("-c", "-vars=a,b", "--dump", "G a"));
        assertEquals(2, exit);
    }

    @Test
    void countsAFormula() {
        Map<String, Object> json = run("-k=10", "-vars=a,b", "G (a -> X b)");
        assertEquals("ok", json.get("status"));
        assertEquals("78732", json.get("count"));
        assertTrue(json.get("ms") instanceof BigDecimal);
        assertEquals(0, exit);
    }

    @Test
    void reportsUnsatisfiability() {
        Map<String, Object> solver = run("-k=10", "-vars=a,b", "G (a -> X b) & !G (a -> F b)");
        assertEquals("unsat_solver", solver.get("status"));
        assertEquals("0", solver.get("count"));
        assertEquals(0, exit);

        assertEquals("unsat_simplified", run("-k=10", "-vars=a,b", "a & !a").get("status"));
        assertEquals(0, exit);
    }

    @Test
    void variablesDefaultToThoseOfTheFormula() {
        assertEquals("1", run("-k=10", "G a").get("count"));
        assertEquals("1024", run("-k=10", "-vars=a,b", "G a").get("count"));
    }

    @Test
    void countsAboveSixtyFourBitsKeepEveryDigit() {
        Map<String, Object> json = run("-k=30", "-vars=a,b,c", "G F a");
        BigInteger count = new BigInteger((String) json.get("count"));
        assertTrue(count.bitLength() > 64, "count too small for this test: " + count);
        assertEquals(BigInteger.valueOf(8).pow(30), count);
    }

    @Test
    void aTimeoutFailsWithoutACount() {
        String vars = IntStream.range(0, 18).mapToObj(i -> "v" + i).collect(Collectors.joining(","));
        Map<String, Object> json = run("-k=10", "-vars=" + vars, "--timeout=0", "G v0");
        assertEquals("timeout", json.get("status"));
        assertFalse(json.containsKey("count"));
        assertTrue(exit != 0);
    }

    @Test
    void aBrokenSolverFailsWithoutACount() {
        Map<String, Object> json = run("-k=10", "-vars=a,b", "-sat=black", "-sat-bin=/nonexistent/black",
                "G (a -> X b) & !G (a -> F b)");
        assertEquals("error", json.get("status"));
        assertFalse(json.containsKey("count"));
        assertTrue(exit != 0);
    }

    @Test
    void badUsageIsReported() {
        assertEquals("error", run("-nope", "G a").get("status"));
        assertEquals(2, exit);
        assertEquals("error", run().get("status"));
        assertEquals(2, exit);
    }

    @Test
    void aFormulaThatDoesNotParseIsAnError() {
        assertEquals("error", run("-k=10", "G (").get("status"));
        assertEquals(1, exit);
    }

    @Test
    void countsATlsfFileWithoutWritingNextToIt(@TempDir Path dir) throws Exception {
        Path spec = dir.resolve("spec.tlsf");
        Files.writeString(spec, "INFO {\n  TITLE: \"t\"\n  DESCRIPTION: \"d\"\n  SEMANTICS: Mealy\n  TARGET: Mealy\n}\n"
                + "MAIN {\n  INPUTS { a; }\n  OUTPUTS { b; }\n  GUARANTEES { G (a -> X b); }\n}\n");
        assertEquals("78732", run("-k=10", spec.toString()).get("count"));
        assertEquals(0, exit);
        try (Stream<Path> files = Files.list(dir)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void dumpIsPartOfTheJson() {
        Map<String, Object> json = run("-k=10", "-vars=a,b", "--dump", "G a");
        Map<String, Object> dump = (Map<String, Object>) json.get("dump");
        assertEquals(List.of("a", "b"), dump.get("variables"));
        assertEquals(4, ((List<Object>) dump.get("transitions")).size());
    }

    @Test
    void dumpOfALargeAlphabetIsAClearError() {
        String vars = IntStream.range(0, 11).mapToObj(i -> "v" + i).collect(Collectors.joining(","));
        Map<String, Object> json = run("-vars=" + vars, "--dump", "G v0");
        assertEquals("error", json.get("status"));
        assertTrue(((String) json.get("detail")).contains("too large"));
        assertEquals(1, exit);
    }
}
