package main;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class MainTest {

	@Test
	void testMinePump() throws IOException, InterruptedException {
		String [] args = {"examples/minepump.tlsf"};
		Main.main(args);
	}

	@Test
	void testSpectra() throws IOException, InterruptedException {
		String [] args = {"examples/icse2019/Simple/RG1.spectra"};
		Main.main(args);
	}
}
