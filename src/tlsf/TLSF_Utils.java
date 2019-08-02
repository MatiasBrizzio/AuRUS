package tlsf;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import owl.ltl.BooleanConstant;
import owl.ltl.Formula;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.TlsfParser;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.tlsf.Tlsf.Semantics;

public class TLSF_Utils {
	
	public static String TLSF_EXAMPLE_SPEC = "INFO {\n"
		    + "  TITLE:       \"TLSF - Empty Specification\"\n"
		    + "  DESCRIPTION: \"Empty Specification\"\n"
		    + "  SEMANTICS:   Mealy\n"
		    + "  TARGET:      Mealy\n"
		    + "}\n"
		    + '\n'
		    + "MAIN {\n"
		    + '\n'
		    + "  INPUTS {\n"
		    + "    a; b; c;\n"
		    + "  }\n"
		    + "  OUTPUTS {\n"
		    + "    d;e;\n"
			+ "  }\n"
		    + '\n'
		    + '\n'
		    + "  ASSUMPTIONS {\n"
		    + "    true;\n"
		    + "  }\n"
		    + '\n'
		    + "  GUARANTEES {\n"
		    + "    true;\n"
		    + "  }  \n"
		    + '}';
	
	private static String getCommand(){
		String cmd = "";
		String currentOS = System.getProperty("os.name");
		if (currentOS.startsWith("Mac"))
				cmd = "./lib/syfco_macos";
		else
			cmd = "./lib/syfco";
		System.out.println(cmd);
		return cmd;
	}
	
	public static Tlsf toBasicTLSF(String spec) throws IOException, InterruptedException {
		File file = null;
		try {
			file = new File("out2.tlsf");
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.write(spec);
			fileWriter.flush();
			fileWriter.close();
			String cmd = getCommand();
			cmd += " -o out2.tlsf -f basic -m pretty -s0 out2.tlsf"; 
			Process p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
			return toBasicTLSF(file);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		

	}
	
	public static Tlsf toBasicTLSF(File spec) throws IOException, InterruptedException {
		String cmd = getCommand();
		String tlsfBasic = spec.getAbsolutePath().replace(".tlsf","_basic.tlsf");
		cmd += " -o "+ tlsfBasic +" -f basic -m pretty -s0 " +spec.getAbsolutePath();
		Process p = Runtime.getRuntime().exec(cmd);
		p.waitFor();
		return TlsfParser.parse(new FileReader(new File(tlsfBasic)));
	}
	
	public static String toTLSF(Tlsf spec) {
		String tlsf_spec = "INFO {\n"
			    + "  TITLE:       " + spec.title() + "\n"
			    + "  DESCRIPTION: " + spec.description() + "\n";			    
				if (spec.semantics().equals(Semantics.MEALY))
					tlsf_spec += "  SEMANTICS:   Mealy\n";
				else if (spec.semantics().equals(Semantics.MEALY_STRICT)) 
					tlsf_spec += "  SEMANTICS:   Mealy,Strict\n";
				else if (spec.semantics().equals(Semantics.MOORE))
					tlsf_spec += "  SEMANTICS:   Moore\n";
				else
					tlsf_spec += "  SEMANTICS:   Moore,Strict\n";
										
				if (spec.target().equals(Semantics.MEALY))
					tlsf_spec += "  TARGET:   Mealy\n";
				else if (spec.target().equals(Semantics.MEALY_STRICT)) 
					tlsf_spec += "  TARGET:   Mealy,Strict\n";
				else if (spec.target().equals(Semantics.MOORE))
					tlsf_spec += "  TARGET:   Moore\n";
				else
					tlsf_spec += "  TARGET:   Moore,Strict\n";
				
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

	public static Tlsf empty_spec(Tlsf spec) {
		String TLSF_EMPTY_SPEC = "INFO {\n"
			    + "  TITLE:       \"TLSF - Empty Specification\"\n"
			    + "  DESCRIPTION: \"Empty Specification\"\n"
			    + "  SEMANTICS:   Mealy\n"
			    + "  TARGET:      Mealy\n"
			    + "}\n"
			    + '\n'
			    + "MAIN {\n"
			    + '\n'
			    + "  INPUTS {\n";
			    int i = 0;
				while (spec.inputs().get(i)) {
					TLSF_EMPTY_SPEC += spec.variables().get(i) + ";";
					i++;
				}
				TLSF_EMPTY_SPEC += "\n"
					    + "  }\n"
					    + "  OUTPUTS {\n"
					    + "    ";
				while (spec.outputs().get(i)) {
					TLSF_EMPTY_SPEC += spec.variables().get(i) + ";";
					i++;
				}
				TLSF_EMPTY_SPEC += "\n"
			    + "  }\n"
			    + '\n'
			    + '\n'
			    + "  ASSUMPTIONS {\n"
			    + "    true;\n"
			    + "  }\n"
			    + '\n'
			    + "  GUARANTEES {\n"
			    + "    true;\n"
			    + "  }  \n"
			    + '}';
		
		return  TlsfParser.parse(TLSF_EMPTY_SPEC);
	}
	
	public static Tlsf fromSpec (Tlsf spec) {
		return TlsfParser.parse(toTLSF(spec));
	}
	
	public static Tlsf change_initially(Tlsf spec, Formula new_initially) {
		String new_tlsf_spec = "INFO {\n"
			    + "  TITLE:       " + spec.title() + "\n"
			    + "  DESCRIPTION: " + spec.description() + "\n";			    
				if (spec.semantics().equals(Semantics.MEALY))
					new_tlsf_spec += "  SEMANTICS:   Mealy\n";
				else if (spec.semantics().equals(Semantics.MEALY_STRICT)) 
					new_tlsf_spec += "  SEMANTICS:   Mealy_Strict\n";
				else if (spec.semantics().equals(Semantics.MOORE))
					new_tlsf_spec += "  SEMANTICS:   Moore\n";
				else
					new_tlsf_spec += "  SEMANTICS:   Moore_Strict\n";
										
				if (spec.target().equals(Semantics.MEALY))
					new_tlsf_spec += "  TARGET:   Mealy\n";
				else if (spec.target().equals(Semantics.MEALY_STRICT)) 
					new_tlsf_spec += "  TARGET:   Mealy_Strict\n";
				else if (spec.target().equals(Semantics.MOORE))
					new_tlsf_spec += "  TARGET:   Moore\n";
				else
					new_tlsf_spec += "  TARGET:   Moore_Strict\n";
				
				new_tlsf_spec += "}\n"
			    + '\n'
			    + "MAIN {\n"
			    + "  INPUTS {\n"
			    + "    ";
		int i = 0;
		while (spec.inputs().get(i)) {
			new_tlsf_spec += spec.variables().get(i) + ";";
			i++;
		}
		new_tlsf_spec += "\n"
			    + "  }\n"
			    + "  OUTPUTS {\n"
			    + "    ";
		while (spec.outputs().get(i)) {
			new_tlsf_spec += spec.variables().get(i) + ";";
			i++;
		}
		new_tlsf_spec += "\n"
			    + "  }\n"
			    + '\n';
		// set new initially
		if (new_initially.compareTo(BooleanConstant.TRUE) != 0) {
			new_tlsf_spec += "  INITIALLY {\n"
					+ "    "
				    + LabelledFormula.of(new_initially,spec.variables()) + ";\n"
				    + "  }\n"
				    + '\n';
		}
		
		if (spec.preset().compareTo(BooleanConstant.TRUE) != 0) {
			new_tlsf_spec += "  PRESET {\n"
				+ "    "
			    + LabelledFormula.of(spec.preset(),spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}
		if (spec.require().compareTo(BooleanConstant.TRUE) != 0) {
			new_tlsf_spec += "  REQUIRE {\n"
				+ "    "
			    + LabelledFormula.of(spec.require(),spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}
			  
		if (!spec.assert_().isEmpty()) {
			new_tlsf_spec += "  ASSERT {\n";
			for (Formula f : spec.assert_()) {
				new_tlsf_spec += "    " + LabelledFormula.of(f,spec.variables()) + ";\n"	;
			}
			new_tlsf_spec += "  }\n"
			    + '\n';
		}

		if (spec.assume().compareTo(BooleanConstant.TRUE) != 0) {
			new_tlsf_spec += "  ASSUMPTIONS {\n"
				+ "    "
			    + LabelledFormula.of(spec.assume(),spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}

		if (!spec.guarantee().isEmpty()) {
			new_tlsf_spec += "  GUARANTEES {\n";
			
		    for (Formula f : spec.guarantee()) {
		    	new_tlsf_spec += "    " + LabelledFormula.of(f,spec.variables()) + ";\n"	;
		    }
		    new_tlsf_spec += "  }\n";
		}
		new_tlsf_spec += '}';
		
		return  TlsfParser.parse(new_tlsf_spec);
	}

	public static Tlsf change_preset(Tlsf spec, Formula new_preset) {
		String new_tlsf_spec = "INFO {\n"
			    + "  TITLE:       " + spec.title() + "\n"
			    + "  DESCRIPTION: " + spec.description() + "\n";			    
				if (spec.semantics().equals(Semantics.MEALY))
					new_tlsf_spec += "  SEMANTICS:   Mealy\n";
				else if (spec.semantics().equals(Semantics.MEALY_STRICT)) 
					new_tlsf_spec += "  SEMANTICS:   Mealy_Strict\n";
				else if (spec.semantics().equals(Semantics.MOORE))
					new_tlsf_spec += "  SEMANTICS:   Moore\n";
				else
					new_tlsf_spec += "  SEMANTICS:   Moore_Strict\n";
										
				if (spec.target().equals(Semantics.MEALY))
					new_tlsf_spec += "  TARGET:   Mealy\n";
				else if (spec.target().equals(Semantics.MEALY_STRICT)) 
					new_tlsf_spec += "  TARGET:   Mealy_Strict\n";
				else if (spec.target().equals(Semantics.MOORE))
					new_tlsf_spec += "  TARGET:   Moore\n";
				else
					new_tlsf_spec += "  TARGET:   Moore_Strict\n";
				
				new_tlsf_spec += "}\n"
			    + '\n'
			    + "MAIN {\n"
			    + "  INPUTS {\n"
			    + "    ";
		int i = 0;
		while (spec.inputs().get(i)) {
			new_tlsf_spec += spec.variables().get(i) + ";";
			i++;
		}
		new_tlsf_spec += "\n"
			    + "  }\n"
			    + "  OUTPUTS {\n"
			    + "    ";
		while (spec.outputs().get(i)) {
			new_tlsf_spec += spec.variables().get(i) + ";";
			i++;
		}
		new_tlsf_spec += "\n"
			    + "  }\n"
			    + '\n';
		if (spec.initially().compareTo(BooleanConstant.TRUE) != 0) {
			new_tlsf_spec += "  INITIALLY {\n"
				+ "    "
			    + LabelledFormula.of(spec.initially(),spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}
		
		// set new preset
		if (new_preset.compareTo(BooleanConstant.TRUE) != 0) {
			new_tlsf_spec += "  PRESET {\n"
				+ "    "
			    + LabelledFormula.of(new_preset,spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}
		if (spec.require().compareTo(BooleanConstant.TRUE) != 0) {
			new_tlsf_spec += "  REQUIRE {\n"
				+ "    "
			    + LabelledFormula.of(spec.require(),spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}
			  
		if (!spec.assert_().isEmpty()) {
			new_tlsf_spec += "  ASSERT {\n";
			for (Formula f : spec.assert_()) {
				new_tlsf_spec += "    " + LabelledFormula.of(f,spec.variables()) + ";\n"	;
			}
			new_tlsf_spec += "  }\n"
			    + '\n';
		}

		if (spec.assume().compareTo(BooleanConstant.TRUE) != 0) {
			new_tlsf_spec += "  ASSUMPTIONS {\n"
				+ "    "
			    + LabelledFormula.of(spec.assume(),spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}

		if (!spec.guarantee().isEmpty()) {
			new_tlsf_spec += "  GUARANTEES {\n";
			
		    for (Formula f : spec.guarantee()) {
		    	new_tlsf_spec += "    " + LabelledFormula.of(f,spec.variables()) + ";\n"	;
		    }
		    new_tlsf_spec += "  }\n";
		}
		new_tlsf_spec += '}';
		
		return TlsfParser.parse(new_tlsf_spec);
	}
		
	public static Tlsf change_require(Tlsf spec, Formula new_require) {
		String new_tlsf_spec = "INFO {\n"
			    + "  TITLE:       " + spec.title() + "\n"
			    + "  DESCRIPTION: " + spec.description() + "\n";			    
				if (spec.semantics().equals(Semantics.MEALY))
					new_tlsf_spec += "  SEMANTICS:   Mealy\n";
				else if (spec.semantics().equals(Semantics.MEALY_STRICT)) 
					new_tlsf_spec += "  SEMANTICS:   Mealy_Strict\n";
				else if (spec.semantics().equals(Semantics.MOORE))
					new_tlsf_spec += "  SEMANTICS:   Moore\n";
				else
					new_tlsf_spec += "  SEMANTICS:   Moore_Strict\n";
										
				if (spec.target().equals(Semantics.MEALY))
					new_tlsf_spec += "  TARGET:   Mealy\n";
				else if (spec.target().equals(Semantics.MEALY_STRICT)) 
					new_tlsf_spec += "  TARGET:   Mealy_Strict\n";
				else if (spec.target().equals(Semantics.MOORE))
					new_tlsf_spec += "  TARGET:   Moore\n";
				else
					new_tlsf_spec += "  TARGET:   Moore_Strict\n";
				
				new_tlsf_spec += "}\n"
			    + '\n'
			    + "MAIN {\n"
			    + "  INPUTS {\n"
			    + "    ";
		int i = 0;
		while (spec.inputs().get(i)) {
			new_tlsf_spec += spec.variables().get(i) + ";";
			i++;
		}
		new_tlsf_spec += "\n"
			    + "  }\n"
			    + "  OUTPUTS {\n"
			    + "    ";
		while (spec.outputs().get(i)) {
			new_tlsf_spec += spec.variables().get(i) + ";";
			i++;
		}
		new_tlsf_spec += "\n"
			    + "  }\n"
			    + '\n';
		if (spec.initially().compareTo(BooleanConstant.TRUE) != 0) {
			new_tlsf_spec += "  INITIALLY {\n"
				+ "    "
			    + LabelledFormula.of(spec.initially(),spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}
		if (spec.preset().compareTo(BooleanConstant.TRUE) != 0) {
			new_tlsf_spec += "  PRESET {\n"
				+ "    "
			    + LabelledFormula.of(spec.preset(),spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}
		
		//set new require
		if (new_require.compareTo(BooleanConstant.TRUE) != 0) {
			new_tlsf_spec += "  REQUIRE {\n"
				+ "    "
			    + LabelledFormula.of(new_require,spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}
		
			  
		if (!spec.assert_().isEmpty()) {
			new_tlsf_spec += "  ASSERT {\n";
			for (Formula f : spec.assert_()) {
				new_tlsf_spec += "    " + LabelledFormula.of(f,spec.variables()) + ";\n"	;
			}
			new_tlsf_spec += "  }\n"
			    + '\n';
		}

		if (spec.assume().compareTo(BooleanConstant.TRUE) != 0) {
			new_tlsf_spec += "  ASSUMPTIONS {\n"
				+ "    "
			    + LabelledFormula.of(spec.assume(),spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}

		if (!spec.guarantee().isEmpty()) {
			new_tlsf_spec += "  GUARANTEES {\n";
			
		    for (Formula f : spec.guarantee()) {
		    	new_tlsf_spec += "    " + LabelledFormula.of(f,spec.variables()) + ";\n"	;
		    }
		    new_tlsf_spec += "  }\n";
		}
		new_tlsf_spec += '}';
		
		return TlsfParser.parse(new_tlsf_spec);
	}
	
	public static Tlsf change_assert(Tlsf spec, List<Formula> new_asserts) {
		String new_tlsf_spec = "INFO {\n"
			    + "  TITLE:       " + spec.title() + "\n"
			    + "  DESCRIPTION: " + spec.description() + "\n";			    
				if (spec.semantics().equals(Semantics.MEALY))
					new_tlsf_spec += "  SEMANTICS:   Mealy\n";
				else if (spec.semantics().equals(Semantics.MEALY_STRICT)) 
					new_tlsf_spec += "  SEMANTICS:   Mealy_Strict\n";
				else if (spec.semantics().equals(Semantics.MOORE))
					new_tlsf_spec += "  SEMANTICS:   Moore\n";
				else
					new_tlsf_spec += "  SEMANTICS:   Moore_Strict\n";
										
				if (spec.target().equals(Semantics.MEALY))
					new_tlsf_spec += "  TARGET:   Mealy\n";
				else if (spec.target().equals(Semantics.MEALY_STRICT)) 
					new_tlsf_spec += "  TARGET:   Mealy_Strict\n";
				else if (spec.target().equals(Semantics.MOORE))
					new_tlsf_spec += "  TARGET:   Moore\n";
				else
					new_tlsf_spec += "  TARGET:   Moore_Strict\n";
				
				new_tlsf_spec += "}\n"
			    + '\n'
			    + "MAIN {\n"
			    + "  INPUTS {\n"
			    + "    ";
		int i = 0;
		while (spec.inputs().get(i)) {
			new_tlsf_spec += spec.variables().get(i) + ";";
			i++;
		}
		new_tlsf_spec += "\n"
			    + "  }\n"
			    + "  OUTPUTS {\n"
			    + "    ";
		while (spec.outputs().get(i)) {
			new_tlsf_spec += spec.variables().get(i) + ";";
			i++;
		}
		new_tlsf_spec += "\n"
			    + "  }\n"
			    + '\n';
		if (spec.initially().compareTo(BooleanConstant.TRUE) != 0) {
			new_tlsf_spec += "  INITIALLY {\n"
				+ "    "
			    + LabelledFormula.of(spec.initially(),spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}
		if (spec.preset().compareTo(BooleanConstant.TRUE) != 0) {
			new_tlsf_spec += "  PRESET {\n"
				+ "    "
			    + LabelledFormula.of(spec.preset(),spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}
		if (spec.require().compareTo(BooleanConstant.TRUE) != 0) {
			new_tlsf_spec += "  REQUIRE {\n"
				+ "    "
			    + LabelledFormula.of(spec.require(),spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}
			  
		if (!new_asserts.isEmpty()) {
			new_tlsf_spec += "  ASSERT {\n";
			for (Formula f : new_asserts) {
				new_tlsf_spec += "    " + LabelledFormula.of(f,spec.variables()) + ";\n"	;
			}
			new_tlsf_spec += "  }\n"
			    + '\n';
		}

		if (spec.assume().compareTo(BooleanConstant.TRUE) != 0) {
			new_tlsf_spec += "  ASSUMPTIONS {\n"
				+ "    "
			    + LabelledFormula.of(spec.assume(),spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}

		if (!spec.guarantee().isEmpty()) {
			new_tlsf_spec += "  GUARANTEES {\n";
			
		    for (Formula f : spec.guarantee()) {
		    	new_tlsf_spec += "    " + LabelledFormula.of(f,spec.variables()) + ";\n"	;
		    }
		    new_tlsf_spec += "  }\n";
		}
		new_tlsf_spec += '}';
		
		return TlsfParser.parse(new_tlsf_spec);
	}

	public static Tlsf change_assume(Tlsf spec, Formula new_assumption) {
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

		// set new assumption
		if (new_assumption.compareTo(BooleanConstant.TRUE) != 0) {
			tlsf_spec += "  ASSUMPTIONS {\n"
				+ "    "
			    + LabelledFormula.of(new_assumption,spec.variables()) + ";\n"
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
		
		return TlsfParser.parse(tlsf_spec);
	}

	public static Tlsf change_guarantees(Tlsf spec, Formula new_guarantee) {
		List<Formula> list = new LinkedList<Formula>();
		list.add(new_guarantee);
		return change_guarantees(spec, list);
	}
	
	public static Tlsf change_guarantees(Tlsf spec, List<Formula> new_guarantees) {
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

		if (!new_guarantees.isEmpty()) {
			tlsf_spec += "  GUARANTEES {\n";
			
		    for (Formula f : new_guarantees) {
		    	tlsf_spec += "    " + LabelledFormula.of(f,spec.variables()) + ";\n"	;
		    }
		    tlsf_spec += "  }\n";
		}
		tlsf_spec += '}';

		return TlsfParser.parse(tlsf_spec);
	}
		
	public static Tlsf change_assume(Tlsf spec, List<Formula> new_assumes) {
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

		if (!new_assumes.isEmpty()) {
			tlsf_spec += "  ASSUMPTIONS {\n";
			
		    for (Formula f : new_assumes) {
		    	tlsf_spec += "    " + LabelledFormula.of(f,spec.variables()) + ";\n"	;
		    }
		    tlsf_spec += "  }\n";
		}

		if (!spec.guarantee().isEmpty()) {
			tlsf_spec += "  GUARANTEES {\n";
			
		    for (Formula f : spec.guarantee()) {
		    	tlsf_spec += "    " + LabelledFormula.of(f,spec.variables()) + ";\n"	;
		    }
		    tlsf_spec += "  }\n";
		}
		tlsf_spec += '}';

		return TlsfParser.parse(tlsf_spec);
	}
	
	public static boolean equals(Tlsf tlsf1, Tlsf tlsf2) {
		if (!tlsf1.assume().equals(tlsf2.assume())) return false;

		if (!tlsf1.initially().equals(tlsf2.initially())) return false;

		if (!tlsf1.preset().equals(tlsf2.preset())) return false;

		if (!tlsf1.require().equals(tlsf2.require())) return false;
		
		for (Formula f1 : tlsf1.assert_()) {
			if (tlsf2.assert_().contains(f1)) continue;
			else return false;
		}
		
		for (Formula f1 : tlsf1.guarantee()) {
			if (tlsf2.guarantee().contains(f1)) continue;
			else return false;
		}

		List<String> vars = new ArrayList<String>();
		int i = 0;
		while (tlsf1.inputs().get(i)) {
			vars.add(tlsf1.variables().get(i));
			i++;
		}

		i = 0;
		while (tlsf2.inputs().get(i)) {
			if (!vars.contains(tlsf2.variables().get(i))) return false;
			i++;
		}
		
		i = 0;
		List<String> outs = new ArrayList<String>();
		while (tlsf1.outputs().get(i)) {
			outs.add(tlsf1.variables().get(i));
			i++;
		}
		i = 0;
		while (tlsf2.outputs().get(i)) {
			if (!outs.contains(tlsf2.variables().get(i))) return false;
			i++;
		}
		
		return true;
		
	}
	
}
