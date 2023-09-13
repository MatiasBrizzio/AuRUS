package solvers;

import org.junit.jupiter.api.Test;
import owl.ltl.Formula;
import owl.ltl.parser.LtlParser;
import owl.ltl.parser.TlsfParser;
import owl.ltl.tlsf.Tlsf;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

class PreciseLTLModelCounterTest {

    @Test
    void testMinePump() throws IOException, InterruptedException {
        String filename = "examples/minepump.tlsf";
        FileReader f = new FileReader(filename);
        Tlsf tlsf = TlsfParser.parse(f);
        System.out.println(tlsf.toFormula().formula());
        PreciseLTLModelCounter counter = new PreciseLTLModelCounter();
        BigInteger res = counter.count(tlsf.toFormula().formula(), 3);
        System.out.println(res);
    }


    @Test
    void testSimpleCount() throws IOException, InterruptedException {
        List<String> vars = List.of("a", "b");
        Formula f0 = LtlParser.syntax("G(a & b)", vars);
        System.out.println(f0);
        Formula f1 = LtlParser.syntax("G(a | b)", vars);
        int bound = 10;
        List<BigInteger> f0_precise = countModels(f0, vars.size(), bound, true);
        List<BigInteger> f0_not_precise = countModels(f0, vars.size(), bound, false);
        System.out.println(f0_precise);
        System.out.println(f0_not_precise);
        List<BigInteger> f1_precise = countModels(f1, vars.size(), bound, true);
        List<BigInteger> f1_not_precise = countModels(f1, vars.size(), bound, false);
        System.out.println(f1_precise);
        System.out.println(f1_not_precise);
    }

    List<BigInteger> countModels(Formula f, int vars, int bound, boolean precise) throws IOException, InterruptedException {
        List<BigInteger> res = new LinkedList<>();
        for (int k = 1; k <= bound; k++) {
            PreciseLTLModelCounter counter = new PreciseLTLModelCounter();
            counter.BOUND = k;
            if (!precise)
                counter.modelcounter = PreciseLTLModelCounter.MODEL_COUNTER.CACHET;
            BigInteger r = counter.count(f, vars);
            res.add(r);
        }
        return res;
    }
}
