package main;

import org.junit.jupiter.api.Test;

import java.io.IOException;

class MainTest {

    @Test
    void testMinePump() throws IOException, InterruptedException {
        String[] args = {"examples/minepump.tlsf"};
        Main.main(args);
    }

    @Test
    void testMinePumpSafe() throws IOException, InterruptedException {
        String[] args = {"examples/minepump-safe.tlsf"};
        Main.main(args);
    }

    @Test
    void testSpectra() throws IOException, InterruptedException {
        String[] args = {"examples/icse2019/Simple/RG1.spectra"};
        Main.main(args);
    }
}
