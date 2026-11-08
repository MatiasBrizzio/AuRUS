package geneticalgorithm;

import main.Settings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/** Runs every {@link CountModelsSatCheckTest} case with BLACK as the SAT solver. */
class CountModelsSatCheckBlackTest extends CountModelsSatCheckTest {

    private String savedSolver;

    @BeforeEach
    void useBlack() {
        savedSolver = Settings.SAT_SOLVER;
        Settings.SAT_SOLVER = "black";
    }

    @AfterEach
    void restoreSolver() {
        Settings.SAT_SOLVER = savedSolver;
    }
}
