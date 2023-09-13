package main;

import org.junit.jupiter.api.Test;

public class TestRunnerTest {

    @Test
    void testSimple() throws ClassNotFoundException {
        String[] args = {"-class=solvers.EmersonLeiAutomatonBasedStrongSATSolverTest", "-test=testAutomataSimple2Unreal"};
        TestRunner.main(args);
    }
}
