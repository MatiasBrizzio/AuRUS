package solvers;

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
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

import org.junit.Test;

public class PotentiallyRealizabilityCheckerTest {

    @Test
    public void testAutomataSimple2Unreal() throws IOException {
        String filename = "examples/simple2.tlsf";
        FileReader f = new FileReader(filename);
        Tlsf tlsf = TlsfParser.parse(f);
        LabelledFormula f0 = tlsf.toFormula();
        System.out.println(f0);
        PotentiallyRealizabilityChecker s = new PotentiallyRealizabilityChecker(f0);
        boolean res = s.checkPotentiallyRealizability();
        System.out.println(res);
        assertFalse(res);
    }

    @Test
    public void testAutomataSimple2Real() throws IOException {
        String filename = "examples/simple2-real.tlsf";
        FileReader f = new FileReader(filename);
        Tlsf tlsf = TlsfParser.parse(f);
        LabelledFormula f0 = tlsf.toFormula();
        System.out.println(f0);
        PotentiallyRealizabilityChecker s = new PotentiallyRealizabilityChecker(f0);
        boolean res = s.checkPotentiallyRealizability();
        System.out.println(res);
        assertTrue(res);
    }

    @Test
    public void testAutomataMinepumpUnreal() throws IOException {
        String filename = "examples/minepump.tlsf";
        FileReader f = new FileReader(filename);
        Tlsf tlsf = TlsfParser.parse(f);
        LabelledFormula f0 = tlsf.toFormula();
        System.out.println(f0);
        PotentiallyRealizabilityChecker s = new PotentiallyRealizabilityChecker(f0);
        boolean res = s.checkPotentiallyRealizability();
        System.out.println(res);
        assertFalse(res);
    }

    @Test
    public void testAutomataMinepump2Real() throws IOException {
        String filename = "examples/minepump-2.tlsf";
        FileReader f = new FileReader(filename);
        Tlsf tlsf = TlsfParser.parse(f);
        LabelledFormula f0 = tlsf.toFormula();
        System.out.println(f0);
        PotentiallyRealizabilityChecker s = new PotentiallyRealizabilityChecker(f0);
        boolean res = s.checkPotentiallyRealizability();
        System.out.println(res);
        assertTrue(res);
    }

    @Test
    public void testAutomataMinepump3Real() throws IOException {
        String filename = "examples/minepump-3.tlsf";
        FileReader f = new FileReader(filename);
        Tlsf tlsf = TlsfParser.parse(f);
        LabelledFormula f0 = tlsf.toFormula();
        System.out.println(f0);
        PotentiallyRealizabilityChecker s = new PotentiallyRealizabilityChecker(f0);
        boolean res = s.checkPotentiallyRealizability();
        System.out.println(res);
        assertTrue(res);
    }

    @Test
    public void testAutomataMinepump4Real() throws IOException {
        String filename = "examples/minepump-4.tlsf";
        FileReader f = new FileReader(filename);
        Tlsf tlsf = TlsfParser.parse(f);
        LabelledFormula f0 = tlsf.toFormula();
        System.out.println(f0);
        PotentiallyRealizabilityChecker s = new PotentiallyRealizabilityChecker(f0);
        boolean res = s.checkPotentiallyRealizability();
        System.out.println(res);
        assertTrue(res);
    }

    @Test
    public void testAutomata_full_arbiter_unreal1_3_4() throws IOException, InterruptedException {
        String filename = "examples/syntcomp2019/unreal/9158599/full_arbiter_unreal1_3_4.tlsf";
        Tlsf tlsf = TLSF_Utils.toBasicTLSF(new File(filename));
        LabelledFormula f0 = tlsf.toFormula();
        System.out.println(f0);
        PotentiallyRealizabilityChecker s = new PotentiallyRealizabilityChecker(f0);
        boolean res = s.checkPotentiallyRealizability();
        System.out.println(res);
        assertFalse(res);
    }


    @Test
    public void testAutomata_round_robin_arbiter_unreal1_2_3() throws IOException, InterruptedException {
        String filename = "examples/syntcomp2019/unreal/9158546/round_robin_arbiter_unreal1_2_3.tlsf";
        Tlsf tlsf = TLSF_Utils.toBasicTLSF(new File(filename));
        LabelledFormula f0 = tlsf.toFormula();
        System.out.println(f0);
        PotentiallyRealizabilityChecker s = new PotentiallyRealizabilityChecker(f0);
        boolean res = s.checkPotentiallyRealizability();
        System.out.println(res);
        assertFalse(res);
    }

    @Test
    public void testSyntCompUnreal() throws IOException {
        try (Stream<Path> walk = Files.walk(Paths.get("examples/syntcomp2019/unreal/"))) {

            List<String> specifications = walk.map(x -> x.toString())
                    .filter(f -> f.endsWith(".tlsf") && !f.endsWith("_basic.tlsf")).collect(Collectors.toList());
            int numOfTimeout = 0;
            int numOfStrongSAT = 0;
            int numOf_NOT_StrongSAT = 0;

            for (String filename : specifications) {
                System.out.println(filename);
                FileReader f = new FileReader(filename);
                Tlsf tlsf = TLSF_Utils.toBasicTLSF(new File(filename));
                LabelledFormula f0 = tlsf.toFormula();
//                System.out.println(f0);
                PotentiallyRealizabilityChecker s = new PotentiallyRealizabilityChecker(f0);
                Boolean res = s.checkPotentiallyRealizability();
                //EXPECTED: false
                System.out.println(res);
                if (res == null)
                    numOfTimeout++;
                else if (res.booleanValue())
                    numOfStrongSAT++;
                else //false
                    numOf_NOT_StrongSAT++;
            }
            System.out.printf("True:%d  False:%d  TIMEOUT:%d", numOfStrongSAT,numOf_NOT_StrongSAT,numOfTimeout);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testAutomata_SensorSelector_real() throws IOException, InterruptedException {
        String filename = "examples/syntcomp2019/LTL/9127916/SensorSelector.tlsf";
        Tlsf tlsf = TLSF_Utils.toBasicTLSF(new File(filename));
        LabelledFormula f0 = tlsf.toFormula();
        System.out.println(f0);
        PotentiallyRealizabilityChecker s = new PotentiallyRealizabilityChecker(f0);
        boolean res = s.checkPotentiallyRealizability();
        System.out.println(res);
        assertTrue(res);
    }

    @Test
    public void testSyntCompReal() throws IOException {
        try (Stream<Path> walk = Files.walk(Paths.get("examples/syntcomp2019/LTL/"))) {

            List<String> specifications = walk.map(x -> x.toString())
                    .filter(f -> f.endsWith(".tlsf") && !f.endsWith("_basic.tlsf")).collect(Collectors.toList());
            int numOfTimeout = 0;
            int numOfStrongSAT = 0;
            int numOf_NOT_StrongSAT = 0;

            for (String filename : specifications) {
                Instant initialExecutionTime = Instant.now();
                System.out.println(filename);
                FileReader f = new FileReader(filename);
                Tlsf tlsf = TLSF_Utils.toBasicTLSF(new File(filename));
                LabelledFormula f0 = tlsf.toFormula();
//                System.out.println(f0);
                PotentiallyRealizabilityChecker s = new PotentiallyRealizabilityChecker(f0);
                Boolean res = s.checkPotentiallyRealizability();
//                System.out.println(res);
//                Instant finalExecutionTime = Instant.now();
//                Duration duration = Duration.between(initialExecutionTime, finalExecutionTime);
//                System.out.printf("Time: %s m  %s s\n",duration.toMinutes(), duration.toSecondsPart());
                //EXPECTED: true
                System.out.println(res);
                if (res == null)
                    numOfTimeout++;
                else if (res.booleanValue())
                    numOfStrongSAT++;
                else //false
                    numOf_NOT_StrongSAT++;
            }
            System.out.printf("True:%d  False:%d  TIMEOUT:%d", numOfStrongSAT,numOf_NOT_StrongSAT,numOfTimeout);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
