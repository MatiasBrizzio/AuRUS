package geneticalgorithm;

import java.util.Random;

public class Settings {
	
	// level == 0 implements random swap (no guarantee consistency); 
	// level == 1 implements random merge of the assumptions and guarantees (no guarantee consistency);
	// level == 2 swaps assumptions and guarantees preserving consistency; and
	// level == 3 merges the assumptions and guarantees preserving consistency.
	public static int CROSSOVER_LEVEL = 0;
	
	public static Random RANDOM_GENERATOR = new Random(1);
	

}
