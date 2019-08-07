package main;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class MainTest {

	@Test
	void testMinePump() throws IOException {
		String [] args = {"examples/minepump.tlsf"};
		Main.main(args);
	}

}
