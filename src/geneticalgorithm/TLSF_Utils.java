package geneticalgorithm;

import owl.ltl.BooleanConstant;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.tlsf.Tlsf.Semantics;

public class TLSF_Utils {
	
	public static String toTLSF(Tlsf spec) {
		String tlsf_spec = "INFO {\n"
			    + "  TITLE:       " + spec.title() + "\n"
			    + "  DESCRIPTION: " + spec.description() + "\n";			    
				if (spec.semantics().equals(Semantics.MEALY))
					tlsf_spec += "  SEMANTICS:   Mealy\n";
				else if (spec.semantics().equals(Semantics.MEALY_STRICT)) 
					tlsf_spec += "  SEMANTICS:   Mealy_Strict\n";
				else if (spec.semantics().equals(Semantics.MOORE))
					tlsf_spec += "  SEMANTICS:   Moore\n";
				else
					tlsf_spec += "  SEMANTICS:   Moore_Strict\n";
										
				if (spec.target().equals(Semantics.MEALY))
					tlsf_spec += "  TARGET:   Mealy\n";
				else if (spec.target().equals(Semantics.MEALY_STRICT)) 
					tlsf_spec += "  TARGET:   Mealy_Strict\n";
				else if (spec.target().equals(Semantics.MOORE))
					tlsf_spec += "  TARGET:   Moore\n";
				else
					tlsf_spec += "  TARGET:   Moore_Strict\n";
				
				tlsf_spec += "}\n"
			    + '\n'
			    + "MAIN {\n"
			    + "  INPUTS {\n"
			    + "    ";
		int i = 0;
		while (spec.inputs().get(i)) {
			tlsf_spec += spec.variables().get(i) + ";";
			i++;
		}
		tlsf_spec += "\n"
			    + "  }\n"
			    + "  OUTPUTS {\n"
			    + "    ";
		while (spec.outputs().get(i)) {
			tlsf_spec += spec.variables().get(i) + ";";
			i++;
		}
		tlsf_spec += "\n"
			    + "  }\n"
			    + '\n';
		if (spec.initially().compareTo(BooleanConstant.TRUE) != 0) {
			tlsf_spec += "  INITIALLY {\n"
				+ "    "
			    + LabelledFormula.of(spec.initially(),spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}
		if (spec.preset().compareTo(BooleanConstant.TRUE) != 0) {
			tlsf_spec += "  PRESET {\n"
				+ "    "
			    + LabelledFormula.of(spec.preset(),spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}
		if (spec.require().compareTo(BooleanConstant.TRUE) != 0) {
			tlsf_spec += "  REQUIRE {\n"
				+ "    "
			    + LabelledFormula.of(spec.require(),spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}
			  
		if (!spec.assert_().isEmpty()) {
			tlsf_spec += "  ASSERT {\n";
			for (Formula f : spec.assert_()) {
				tlsf_spec += "    " + LabelledFormula.of(f,spec.variables()) + ";\n"	;
			}
			tlsf_spec += "  }\n"
			    + '\n';
		}

		if (spec.assume().compareTo(BooleanConstant.TRUE) != 0) {
			tlsf_spec += "  ASSUMPTIONS {\n"
				+ "    "
			    + LabelledFormula.of(spec.assume(),spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}

		if (!spec.guarantee().isEmpty()) {
			tlsf_spec += "  GUARANTEES {\n";
			
		    for (Formula f : spec.guarantee()) {
		    	tlsf_spec += "    " + LabelledFormula.of(f,spec.variables()) + ";\n"	;
		    }
		    tlsf_spec += "  }\n";
		}
		tlsf_spec += '}';
		
		return tlsf_spec;
	}

}
