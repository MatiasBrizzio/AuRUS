package modelcounter;

import regular.Discretizer;

import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedList;

public class CountMain {

    public static void main(String[] args) throws IOException, InterruptedException {
        for (String arg : args) {
            System.out.println(arg);
        }
        if (args.length != 2)
            throw new RuntimeException("CountMain.main: it takes the formula and the bound as input: " + args.length);
        String ltl = args[0];
        int bound = Integer.parseInt(args[1]);
        Rltlconv_LTLModelCounter translatorLTLtoRE = new Rltlconv_LTLModelCounter();
        String abcRE = translatorLTLtoRE.ltl2RE(ltl);
        String[] arr = Discretizer.or(abcRE);
        BigInteger result = BigInteger.ZERO;
        for (String string : arr) {

            LinkedList<String> abcStrs = new LinkedList<>();
            String s = translatorLTLtoRE.toABClanguage(string);
            abcStrs.add(s);
            ABC abc = new ABC();
            BigInteger count = abc.count(abcStrs, bound, false, true);
            result = result.add(count);
        }
        System.out.println(result);
    }
}
