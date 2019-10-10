package modelcounter;

import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedList;

import regular.Discretizer;

public class CountMain {

	public static void main(String[] args) throws IOException, InterruptedException {
		for(int i = 0; i<args.length; i++) {
			System.out.println(args[i]);
		}
		if (args.length != 2)		
			throw new RuntimeException("CountMain.main: it takes the formula and the bound as input: " + args.length);
		String ltl = args[0];
		int bound = Integer.valueOf(args[1]);
		Rltlconv_LTLModelCounter translatorLTLtoRE = new Rltlconv_LTLModelCounter();
		String abcRE = translatorLTLtoRE.ltl2RE(ltl);
		String[] arr = Discretizer.or(abcRE);
		BigInteger result = BigInteger.ZERO;
        for (int i=0;i<arr.length;i++) {
        	
        	LinkedList<String> abcStrs = new LinkedList<>();
        	String s = translatorLTLtoRE.toABClanguage(arr[i]);
        	abcStrs.add(s);
//	        	System.out.print(arr[i].length()+" ");
        	ABC abc = new ABC();
			BigInteger count = abc.count(abcStrs,bound, false, true);
//				System.out.print(count+"; ");
			result = result.add(count);
        }
        System.out.println(result);
	}
}
