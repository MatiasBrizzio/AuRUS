package solvers;

import org.junit.jupiter.api.Test;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.TlsfParser;
import owl.ltl.tlsf.Tlsf;
import tlsf.TLSF_Utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EmersonLeiAutomatonBasedStrongSATSolverTest {

    @Test
    <S> void testAutomataSimpleUnreal() throws IOException {
        String filename = "examples/simple2.tlsf";
        FileReader f = new FileReader(filename);
        Tlsf tlsf = TlsfParser.parse(f);
        LabelledFormula f0 = tlsf.toFormula();
        System.out.println(f0);
        EmersonLeiAutomatonBasedStrongSATSolver s = new EmersonLeiAutomatonBasedStrongSATSolver(f0);
        boolean res = s.isStrongSatisfiable();
        System.out.println(res);
        assertFalse(res);
    }

    @Test
    <S> void testAutomataMinepumpUnreal() throws IOException {
        String filename = "examples/minepump.tlsf";
        FileReader f = new FileReader(filename);
        Tlsf tlsf = TlsfParser.parse(f);
        LabelledFormula f0 = tlsf.toFormula();
        System.out.println(f0);
        EmersonLeiAutomatonBasedStrongSATSolver s = new EmersonLeiAutomatonBasedStrongSATSolver(f0);
        boolean res = s.isStrongSatisfiable();
        System.out.println(res);
        assertFalse(res);
    }

    @Test
    <S> void testAutomataMinepump2Real() throws IOException {
        String filename = "examples/minepump-2.tlsf";
        FileReader f = new FileReader(filename);
        Tlsf tlsf = TlsfParser.parse(f);
        LabelledFormula f0 = tlsf.toFormula();
        System.out.println(f0);
        EmersonLeiAutomatonBasedStrongSATSolver s = new EmersonLeiAutomatonBasedStrongSATSolver(f0);
        boolean res = s.isStrongSatisfiable();
        System.out.println(res);
        assertTrue(res);
    }

    @Test
    <S> void testAutomataMinepump3Real() throws IOException {
        String filename = "examples/minepump-3.tlsf";
        FileReader f = new FileReader(filename);
        Tlsf tlsf = TlsfParser.parse(f);
        LabelledFormula f0 = tlsf.toFormula();
        System.out.println(f0);
        EmersonLeiAutomatonBasedStrongSATSolver s = new EmersonLeiAutomatonBasedStrongSATSolver(f0);
        boolean res = s.isStrongSatisfiable();
        System.out.println(res);
        assertTrue(res);
    }

    @Test
    <S> void testAutomataMinepump4Real() throws IOException {
        String filename = "examples/minepump-4.tlsf";
        FileReader f = new FileReader(filename);
        Tlsf tlsf = TlsfParser.parse(f);
        LabelledFormula f0 = tlsf.toFormula();
        System.out.println(f0);
        EmersonLeiAutomatonBasedStrongSATSolver s = new EmersonLeiAutomatonBasedStrongSATSolver(f0);
        boolean res = s.isStrongSatisfiable();
        System.out.println(res);
        assertTrue(res);
    }

    @Test
    <S> void testAutomata_full_arbiter_unreal1_3_4() throws IOException, InterruptedException {
        String filename = "examples/syntcomp2019/unreal/9158599/full_arbiter_unreal1_3_4.tlsf";
        Tlsf tlsf = TLSF_Utils.toBasicTLSF(new File(filename));
        LabelledFormula f0 = tlsf.toFormula();
        System.out.println(f0);
        EmersonLeiAutomatonBasedStrongSATSolver s = new EmersonLeiAutomatonBasedStrongSATSolver(f0);
        boolean res = s.isStrongSatisfiable();
        System.out.println(res);
        assertFalse(res);
    }

    @Test
    <S> void testAyntCompUnreal() throws IOException {
        try (Stream<Path> walk = Files.walk(Paths.get("examples/syntcomp2019/unreal/"))) {

            List<String> specifications = walk.map(x -> x.toString())
                    .filter(f -> f.endsWith(".tlsf")).collect(Collectors.toList());

            for (String filename : specifications) {
                System.out.println(filename);
                FileReader f = new FileReader(filename);
                Tlsf tlsf = TLSF_Utils.toBasicTLSF(new File(filename));
                LabelledFormula f0 = tlsf.toFormula();
//                System.out.println(f0);
                EmersonLeiAutomatonBasedStrongSATSolver s = new EmersonLeiAutomatonBasedStrongSATSolver(f0);
                boolean res = s.isStrongSatisfiable();
                System.out.println(res);
                assertFalse(res);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
