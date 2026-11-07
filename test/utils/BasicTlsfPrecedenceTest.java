package utils;

import org.junit.jupiter.api.Test;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.LtlParser;
import owl.ltl.tlsf.Tlsf;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class BasicTlsfPrecedenceTest {

    /** A basic TLSF file whose implication has no parentheses around its antecedent. */
    private static File basicSpec() throws IOException {
        File f = File.createTempFile("precedence", ".tlsf");
        f.deleteOnExit();
        Files.writeString(f.toPath(),
                "INFO {\n"
                        + "  TITLE:       \"precedence\"\n"
                        + "  DESCRIPTION: \"-> binds weaker than ||\"\n"
                        + "  SEMANTICS:   Mealy\n"
                        + "  TARGET:      Mealy\n"
                        + "}\n"
                        + "MAIN {\n"
                        + "  INPUTS { a; b; }\n"
                        + "  OUTPUTS { p; }\n"
                        + "  ASSUME { G (!a || a && !b -> X a); }\n"
                        + "  GUARANTEES { G p; }\n"
                        + "}\n");
        return f;
    }

    @Test
    public void basicTlsfIsReadWithSyfcoPrecedence() throws IOException, InterruptedException {
        Tlsf tlsf = TlsfUtils.toBasicTLSF(basicSpec());

        // syfco's reading: the whole disjunction is the antecedent.
        LabelledFormula expected = LtlParser.parse("G ((!a | (a & !b)) -> X a)", tlsf.variables());
        // The reading the shortcut produced: the implication sits inside the disjunction.
        LabelledFormula wrong = LtlParser.parse("G (!a | ((a & !b) -> X a))", tlsf.variables());

        // The two readings are different formulas, so the test can tell them apart.
        assertNotEquals(expected.formula(), wrong.formula());
        assertEquals(expected.formula(), tlsf.assume());
    }
}
