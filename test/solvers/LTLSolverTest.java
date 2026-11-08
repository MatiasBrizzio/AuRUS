package solvers;

import org.junit.jupiter.api.Test;
import owl.ltl.Conjunction;
import owl.ltl.Disjunction;
import owl.ltl.Formula;
import owl.ltl.parser.LtlParser;
import utils.SolverUtils;

import java.io.IOException;
import java.util.List;


class LTLSolverTest {
    @Test
    public void test_sat() throws IOException, InterruptedException {
        List<String> vars = List.of("p", "q", "r", "s");
        Formula phi = LtlParser.parse("G (p)", vars).formula();
        Formula psi = LtlParser.parse("G !p", vars).formula();
        Formula impl = Disjunction.of(psi.not(), phi);
        Formula impl2 = Disjunction.of(phi.not(), psi);
        System.out.println(LTLSolver.isSAT(impl.toString()));
        System.out.println(LTLSolver.isSAT(impl2.toString()));
    }


    @Test
    public void test_weird_case() throws IOException, InterruptedException {
        List<String> vars = List.of("a", "b");
        Formula phi = LtlParser.parse("G (a -> X b)", vars).formula();
        Formula psi = LtlParser.parse("!G(a -> F b)", vars).formula();
        System.out.println(LTLSolver.isSAT(SolverUtils.toSolverSyntax(Conjunction.of(phi,psi)).toString()));
    }


}
