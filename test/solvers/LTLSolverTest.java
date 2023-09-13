package solvers;

import org.junit.jupiter.api.Test;
import owl.ltl.*;
import owl.ltl.parser.LtlParser;
import owl.ltl.visitors.CounterOfXs;
import owl.ltl.visitors.GeneratePyAigerInput;
import owl.ltl.visitors.PushDownXsVisitor;
import owl.ltl.visitors.ToPastLTLVisitor;

import java.io.IOException;
import java.util.List;


class LTLSolverTest {
    @Test
    public void test_projection_m2() {
        List<String> vars = List.of("st", "rs", "controllablecntst", "controllablehalt", "controllablejump", "controllablete",
                "controllablecnt1", "controllablecnt2", "controllablecnt3", "controllablecnt4", "first");
        Formula theta_e = LtlParser.parse("true", vars).formula();
        Formula theta_s = LtlParser.parse("((controllablecnt1 || rs) && !controllablejump && !controllablehalt)", vars).formula();
        Formula phi_e = LtlParser.parse("!(rs && st)", vars).formula();
        Formula phi_s = LtlParser.parse("(" +
                "           ((rs && !controllablejump && !controllablehalt) -> X controllablejump)\n" +
                "           && (!controllablete)\n" +
                "           && ((controllablecnt1 && !controllablejump && !controllablehalt) -> X(controllablecnt2 || rs))\n" +
                "           && ((controllablecnt2 && !controllablejump && !controllablehalt) -> X(controllablecnt3 || rs))\n" +
                "           && ((controllablecnt3 && !controllablejump && !controllablehalt) -> X(controllablecnt4 || rs))\n" +
                "           && ((controllablecnt4 && !controllablejump && !controllablehalt) -> X(controllablejump))\n" +
                "           && (controllablejump -> Xcontrollablehalt)\n" +
                "           && ((!controllablejump && !controllablehalt) -> X!controllablehalt))", vars).formula();

//        Formula first = ZOperator.of(LtlParser.parse("first",vars).formula().not());
        Formula first = LtlParser.parse("first", vars).formula();
        Formula ant = HOperator.of(Disjunction.of(first.not(), theta_e));
        Formula cons = Conjunction.of((HOperator.of(Disjunction.of(first.not(), theta_s))),
                Disjunction.of(phi_e.not(), phi_s));
        Formula projection_m2 = Disjunction.of(ant.not(), cons);
        PushDownXsVisitor visitor = new PushDownXsVisitor();
        CounterOfXs counter = new CounterOfXs();
        Formula projection_m2_1 = projection_m2.accept(visitor);
        int max_deep = counter.get_max_x_deep(projection_m2_1);
        ToPastLTLVisitor visitor1 = new ToPastLTLVisitor(max_deep, vars);
        Formula res = projection_m2_1.accept(visitor1);
        String str = GeneratePyAigerInput.toString(LabelledFormula.of(res, vars), false);
        System.out.println(str);
        str = str.replaceAll("first", "Z ~asddsa");
        System.out.println(str);
    }


    @Test
    public void test_sat() throws IOException, InterruptedException {
        List<String> vars = List.of("p", "q", "r", "s");
        Formula phi = LtlParser.parse("p && q", vars).formula();
        Formula psi = LtlParser.parse("p", vars).formula();
        Formula impl = Disjunction.of(psi.not(), phi);
        Formula impl2 = Disjunction.of(phi.not(), psi);
        System.out.println(LTLSolver.isSAT(impl.toString()));
        System.out.println(LTLSolver.isSAT(impl2.toString()));
    }


}
