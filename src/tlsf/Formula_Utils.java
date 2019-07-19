package tlsf;

import java.util.LinkedList;
import java.util.List;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.LtlParser;

public class Formula_Utils {
	
	public static List<LabelledFormula> subformulas (LabelledFormula f, List<String> vars) {
		List<LabelledFormula> s = new LinkedList();
		
		for (Formula c : f.formula().children()) {
			LabelledFormula sf = LabelledFormula.of(c, vars);
			for(LabelledFormula e : subformulas(sf, vars))
				s.add(e);		
		}
		s.add(LabelledFormula.of(f.formula(),vars));
		return s;
	}

}
