package tlsf;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import owl.ltl.LabelledFormula;
import owl.ltl.parser.LtlParser;
import owl.ltl.tlsf.Tlsf;

class CountREModelsTest {

	 @Test
    public void testSimple1() throws IOException, InterruptedException{
        List<String> vars = List.of("a", "b");
        LabelledFormula f0 =  LtlParser.parse("G F(a & (b))",vars);
        List<LabelledFormula> list = new LinkedList();
        list.add(f0);
        System.out.println(f0);
        CountREModels counter = new CountREModels();
//	        String re = counter.genABCString(f0);
//	        System.out.println(re);
        BigInteger c = counter.count(list, 5, false, true);
        System.out.println(c);
    }

	 @Test
	    public void testSimple2() throws IOException, InterruptedException{
	        List<String> vars = List.of("a", "b");
	        LabelledFormula f0 =  LtlParser.parse("G(a -> X(b))",vars);
	        List<LabelledFormula> list = new LinkedList();
	        list.add(f0);
	        System.out.println(f0);
	        CountREModels counter = new CountREModels();
//		        String re = counter.genABCString(f0);
//		        System.out.println(re);
	        BigInteger c = counter.count(list, 10, false, true);
	        System.out.println(c);
	    }
	 
	 @Test
	    public void testMinepump() throws IOException, InterruptedException{
	    	String filename = "examples/minepump.tlsf";
	  	  	Tlsf tlsf = TLSF_Utils.toBasicTLSF(new File(filename));
	        List<String> vars = tlsf.variables();
	        LabelledFormula f0 =  tlsf.toFormula();
	        List<LabelledFormula> list = new LinkedList();
	        list.add(f0);
	        System.out.println(f0);
	        CountREModels counter = new CountREModels();
//		        String re = counter.genABCString(f0);
//		        System.out.println(re);
	        BigInteger c = counter.count(list, 5, false, true);
	        System.out.println(c);
	    }
}
