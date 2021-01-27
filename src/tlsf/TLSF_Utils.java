package tlsf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import main.Settings;
import owl.ltl.BooleanConstant;
import owl.ltl.Conjunction;
import owl.ltl.Disjunction;
import owl.ltl.FOperator;
import owl.ltl.Formula;
import owl.ltl.GOperator;
import owl.ltl.LabelledFormula;
import owl.ltl.parser.SpectraParser;
import owl.ltl.parser.TlsfParser;
import owl.ltl.spectra.Spectra;
import owl.ltl.tlsf.Tlsf;
import owl.ltl.tlsf.Tlsf.Semantics;
import owl.ltl.visitors.SolverSyntaxOperatorReplacer;

public class TLSF_Utils {
	static Map<String, String> replacements = new HashMap<String, String>(){
		private static final long serialVersionUID = 1L;
	{
        put("&", "&&");
        put("|", "||");
    }};
    
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
		return cmd;
	}
	
	public static Tlsf toBasicTLSF(String spec) throws IOException, InterruptedException {
		File file = null;
		try {
			//System.out.println(spec);
			/*file = new File("out2.tlsf");
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.write(spec);
			fileWriter.flush();
			fileWriter.close();
			
			if (hasSyfcoSintax(file)) {
				if (isBasic(file)) {
					return TlsfParser.parse(new FileReader(file));
				}
				else {
					String cmd = getCommand();
					cmd += " -o out2.tlsf -f basic -m pretty -s0 out2.tlsf"; 
					Process p = Runtime.getRuntime().exec(cmd);
					p.waitFor();
				}
			}
			else {
				String tlsfAdapted = adaptTLSFSpec(TlsfParser.parse(new FileReader(file)));
				file = new File("out2.tlsf");
				FileWriter fileWriter2 = new FileWriter(file);
				fileWriter2.write(tlsfAdapted);
				fileWriter2.flush();
				fileWriter2.close();
			}
			System.out.println(p.waitFor());*/
			String directoryName =  Settings.STRIX_PATH;
			File outfolder = new File(directoryName);
			if (!outfolder.exists())
				outfolder.mkdirs();
			file = new File(Settings.STRIX_PATH,"/out2.tlsf");
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.write(spec);
			fileWriter.flush();
			fileWriter.close();
			return toBasicTLSF(file);

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		

	}
	
	
	private static boolean hasSyfcoSintax(File spec) throws InterruptedException, IOException {
		String cmd = getCommand();
		cmd += " "+spec.getPath();
		Process pr = Runtime.getRuntime().exec(cmd);
		int res = pr.waitFor();
		return (res == 0);
	}
	
	private static boolean isBasic(File spec) throws IOException, InterruptedException {
		String cmd = getCommand();
		cmd += " -p "+ spec.getPath();
		Process pr = Runtime.getRuntime().exec(cmd);
		
		if (pr.waitFor() == 1) return false;
		
		InputStream in = pr.getInputStream();
    	InputStreamReader inread = new InputStreamReader(in);
    	BufferedReader bufferedreader = new BufferedReader(inread);
    	String aux;
    	aux = bufferedreader.readLine();
    	return aux.length() == 0;
	}
	
	

	public static Tlsf toBasicTLSF(File spec) throws IOException, InterruptedException {
//		System.out.println(hasSyfcoSintax(spec) + "and "+isBasic(spec) + "File: " + spec.getPath() );
		if (spec.getAbsolutePath().endsWith("spectra")) {
			Spectra spectra = SpectraParser.parse(new FileReader(spec));
			Tlsf tlsf = TLSF_Utils.fromSpectra(spectra);
			String tlsf_name = spec.getAbsolutePath().replace(".spectra", ".tlsf");
			FileWriter out = new FileWriter(tlsf_name);
			out.write(adaptTLSFSpec(tlsf));
			out.close();
			spec = new File(tlsf_name);
		}
		
		if (hasSyfcoSintax(spec)) {
			if (isBasic(spec))
				return TlsfParser.parse(new FileReader(spec));
		}
		else {
			String tlsfAdapted = adaptTLSFSpec(TlsfParser.parse(new FileReader(spec)));
			FileWriter fileWriter2 = new FileWriter(spec);
			fileWriter2.write(tlsfAdapted);
			fileWriter2.flush();
			fileWriter2.close();
		}
		
		String cmd = getCommand();
		String tlsfBasic = spec.getAbsolutePath().replace(".tlsf","_basic.tlsf");
		cmd += " -o "+ tlsfBasic +" -f basic -m pretty -s0 " +spec.getAbsolutePath();
		Process p = Runtime.getRuntime().exec(cmd);

		p.waitFor();
		
		String tlsf =  adaptTLSFSpec(TlsfParser.parse(new FileReader(new File(tlsfBasic))));
		return TlsfParser.parse(tlsf);
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
			    + LabelledFormula.of(spec.initially().accept(new SolverSyntaxOperatorReplacer()),spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}
		if (spec.preset().compareTo(BooleanConstant.TRUE) != 0) {
			tlsf_spec += "  PRESET {\n"
				+ "    "
			    + LabelledFormula.of(spec.preset().accept(new SolverSyntaxOperatorReplacer()),spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}
		if (spec.require().compareTo(BooleanConstant.TRUE) != 0) {
			tlsf_spec += "  REQUIRE {\n"
				+ "    "
			    + LabelledFormula.of(spec.require().accept(new SolverSyntaxOperatorReplacer()),spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}
			  
		if (!spec.assert_().isEmpty()) {
			tlsf_spec += "  ASSERT {\n";
			for (Formula f : spec.assert_()) {
				tlsf_spec += "    " + LabelledFormula.of(f.accept(new SolverSyntaxOperatorReplacer()),spec.variables()) + ";\n"	;
			}
			tlsf_spec += "  }\n"
			    + '\n';
		}

		if (spec.assume().compareTo(BooleanConstant.TRUE) != 0) {
			tlsf_spec += "  ASSUMPTIONS {\n"
				+ "    "
			    + LabelledFormula.of(spec.assume().accept(new SolverSyntaxOperatorReplacer()),spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}

		if (!spec.guarantee().isEmpty()) {
			tlsf_spec += "  GUARANTEES {\n";
			
		    for (Formula f : spec.guarantee()) {
		    	tlsf_spec += "    " + LabelledFormula.of(f.accept(new SolverSyntaxOperatorReplacer()),spec.variables()) + ";\n"	;
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
					new_tlsf_spec += "  SEMANTICS:   Mealy,Strict\n";
				else if (spec.semantics().equals(Semantics.MOORE))
					new_tlsf_spec += "  SEMANTICS:   Moore\n";
				else
					new_tlsf_spec += "  SEMANTICS:   Moore,Strict\n";
										
				if (spec.target().equals(Semantics.MEALY))
					new_tlsf_spec += "  TARGET:   Mealy\n";
				else if (spec.target().equals(Semantics.MEALY_STRICT)) 
					new_tlsf_spec += "  TARGET:   Mealy,Strict\n";
				else if (spec.target().equals(Semantics.MOORE))
					new_tlsf_spec += "  TARGET:   Moore\n";
				else
					new_tlsf_spec += "  TARGET:   Moore,Strict\n";
				
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
					new_tlsf_spec += "  SEMANTICS:   Mealy,Strict\n";
				else if (spec.semantics().equals(Semantics.MOORE))
					new_tlsf_spec += "  SEMANTICS:   Moore\n";
				else
					new_tlsf_spec += "  SEMANTICS:   Moore,Strict\n";
										
				if (spec.target().equals(Semantics.MEALY))
					new_tlsf_spec += "  TARGET:   Mealy\n";
				else if (spec.target().equals(Semantics.MEALY_STRICT)) 
					new_tlsf_spec += "  TARGET:   Mealy,Strict\n";
				else if (spec.target().equals(Semantics.MOORE))
					new_tlsf_spec += "  TARGET:   Moore\n";
				else
					new_tlsf_spec += "  TARGET:   Moore,Strict\n";
				
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
					new_tlsf_spec += "  SEMANTICS:   Mealy,Strict\n";
				else if (spec.semantics().equals(Semantics.MOORE))
					new_tlsf_spec += "  SEMANTICS:   Moore\n";
				else
					new_tlsf_spec += "  SEMANTICS:   Moore,Strict\n";
										
				if (spec.target().equals(Semantics.MEALY))
					new_tlsf_spec += "  TARGET:   Mealy\n";
				else if (spec.target().equals(Semantics.MEALY_STRICT)) 
					new_tlsf_spec += "  TARGET:   Mealy,Strict\n";
				else if (spec.target().equals(Semantics.MOORE))
					new_tlsf_spec += "  TARGET:   Moore\n";
				else
					new_tlsf_spec += "  TARGET:   Moore,Strict\n";
				
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
					new_tlsf_spec += "  SEMANTICS:   Mealy,Strict\n";
				else if (spec.semantics().equals(Semantics.MOORE))
					new_tlsf_spec += "  SEMANTICS:   Moore\n";
				else
					new_tlsf_spec += "  SEMANTICS:   Moore,Strict\n";
										
				if (spec.target().equals(Semantics.MEALY))
					new_tlsf_spec += "  TARGET:   Mealy\n";
				else if (spec.target().equals(Semantics.MEALY_STRICT)) 
					new_tlsf_spec += "  TARGET:   Mealy,Strict\n";
				else if (spec.target().equals(Semantics.MOORE))
					new_tlsf_spec += "  TARGET:   Moore\n";
				else
					new_tlsf_spec += "  TARGET:   Moore,Strict\n";
				
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
	
	public static String adaptTLSFSpec(Tlsf spec) {
		String new_tlsf_spec = "INFO {\n"
			    + "  TITLE:       " + spec.title() + "\n"
			    + "  DESCRIPTION: " + spec.description() + "\n";			    
				if (spec.semantics().equals(Semantics.MEALY))
					new_tlsf_spec += "  SEMANTICS:   Mealy\n";
				else if (spec.semantics().equals(Semantics.MEALY_STRICT)) 
					new_tlsf_spec += "  SEMANTICS:   Mealy,Strict\n";
				else if (spec.semantics().equals(Semantics.MOORE))
					new_tlsf_spec += "  SEMANTICS:   Moore\n";
				else
					new_tlsf_spec += "  SEMANTICS:   Moore,Strict\n";
										
				if (spec.target().equals(Semantics.MEALY))
					new_tlsf_spec += "  TARGET:   Mealy\n";
				else if (spec.target().equals(Semantics.MEALY_STRICT)) 
					new_tlsf_spec += "  TARGET:   Mealy,Strict\n";
				else if (spec.target().equals(Semantics.MOORE))
					new_tlsf_spec += "  TARGET:   Moore\n";
				else
					new_tlsf_spec += "  TARGET:   Moore,Strict\n";
				
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
		if (spec.initially().compareTo(BooleanConstant.TRUE) != 0) {
			new_tlsf_spec += "  INITIALLY {\n"
					+ "    "
				    + toSolverSyntax((LabelledFormula.of(spec.initially(),spec.variables()))) + ";\n"
				    + "  }\n"
				    + '\n';
		}
		
		if (spec.preset().compareTo(BooleanConstant.TRUE) != 0) {
			new_tlsf_spec += "  PRESET {\n"
				+ "    "
			    + toSolverSyntax((LabelledFormula.of(spec.preset(),spec.variables()))) + ";\n"
			    + "  }\n"
			    + '\n';
		}
		if (spec.require().compareTo(BooleanConstant.TRUE) != 0) {
			new_tlsf_spec += "  REQUIRE {\n"
				+ "    "
			    + toSolverSyntax((LabelledFormula.of(spec.require(),spec.variables()))) + ";\n"
			    + "  }\n"
			    + '\n';
		}
			  
		if (!spec.assert_().isEmpty()) {
			new_tlsf_spec += "  ASSERT {\n";
			for (Formula f : spec.assert_()) {
				new_tlsf_spec += "    " +  toSolverSyntax((LabelledFormula.of(f,spec.variables()))) + ";\n"	;
			}
			new_tlsf_spec += "  }\n"
			    + '\n';
		}

		if (spec.assume().compareTo(BooleanConstant.TRUE) != 0) {
			new_tlsf_spec += "  ASSUMPTIONS {\n" ;
			for (Formula as : Formula_Utils.splitConjunction(spec.assume())) {
				new_tlsf_spec += "    " + toSolverSyntax(LabelledFormula.of(as, spec.variables())) + ";\n";
			}
			new_tlsf_spec += "  }\n" + '\n';
		}

		if (!spec.guarantee().isEmpty()) {
			new_tlsf_spec += "  GUARANTEES {\n";
			
		    for (Formula f : spec.guarantee()) {
		    	new_tlsf_spec += "    " + toSolverSyntax(((LabelledFormula.of(f,spec.variables())))) + ";\n";
		    }
		    new_tlsf_spec += "  }\n";
		}
		new_tlsf_spec += '}';
		
		for (String v : spec.variables()) 
			new_tlsf_spec = new_tlsf_spec.replaceAll(v, v.toLowerCase());	
		
		return  (new_tlsf_spec);
	}
	
	private static String toSolverSyntax(LabelledFormula f) {
		String LTLFormula = f.toString();
		
		for (String v : f.variables()) 
			LTLFormula = LTLFormula.replaceAll(v, v.toLowerCase());			

		LTLFormula = LTLFormula.replaceAll("([A-Z])", " $1 ");
		
		return new String(replaceLTLConstructions(LTLFormula)); 
	}
	
	private static String replaceLTLConstructions(String line) {
		Set<String> keys = replacements.keySet();
	    for(String key: keys)
	        line = line.replace(key, replacements.get(key));    
		return line;
	}
	
	public static Tlsf fromSpectra(Spectra spec) {
		List<Formula> additionalAssumptions = new LinkedList<Formula>();
		List<Formula> additionalGuarantee = new LinkedList<Formula>();
		String new_tlsf_spec = "INFO {\n"
			    + "  TITLE:       " + "\"" + spec.title() + "\"" + "\n"
			    + "  DESCRIPTION: " + "\""+ "empty description" + "\""+ "\n";			    
					new_tlsf_spec += "  SEMANTICS:   Mealy,Strict\n";
					new_tlsf_spec += "  TARGET:   Mealy\n";
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
		//init
		
		List<Formula> lst= new LinkedList<Formula>();
		if (!spec.thetaE().isEmpty()) {
			for (Formula f : spec.thetaE()) {
				if (hasGFPattern(f)) 
					additionalAssumptions.add(f);
				else if (Formula_Utils.numOfTemporalOperators(f) > 0) 
					additionalAssumptions.add(f);
				else
					lst.add(f);
			}
			new_tlsf_spec += "  INITIALLY {\n"
				+ "    "
			    + LabelledFormula.of(Conjunction.of(lst),spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}
		if (!spec.thetaS().isEmpty()) {
			lst.clear();
			for (Formula f : spec.thetaS()) {
				if (hasGFPattern(f)) 
					additionalGuarantee.add(f);
				else if (Formula_Utils.numOfTemporalOperators(f) > 0) 
					additionalGuarantee.add(f);
				else
					lst.add(f);
			}
			new_tlsf_spec += "  PRESET {\n"
				+ "    "
			    + LabelledFormula.of(Conjunction.of(lst),spec.variables()) + ";\n"
			    + "  }\n"
			    + '\n';
		}
		if (!spec.psiE().isEmpty()) {
			new_tlsf_spec += "  REQUIRE {\n"
				+ "    ";
				for (Formula f : spec.psiE()) {
					new_tlsf_spec += "    " + LabelledFormula.of(GOperator.of(f),spec.variables()) + ";\n"	;
				}
				new_tlsf_spec += "  }\n"
			    + '\n';
		}
			  
		if (!spec.psiS().isEmpty()) {
			new_tlsf_spec += "  ASSERT {\n"
			+ "    ";
			for (Formula f : spec.psiS()) {
				new_tlsf_spec += "    " + LabelledFormula.of(GOperator.of(f),spec.variables()) + ";\n"	;
			}
			new_tlsf_spec += "  }\n"
			+ '\n';
		}

		additionalAssumptions.addAll(spec.phiE());
		if (!additionalAssumptions.isEmpty()) {
			new_tlsf_spec += "  ASSUMPTIONS {\n";
		    for(Formula a : additionalAssumptions) {
		    	if (Formula_Utils.numOfTemporalOperators(a)>0)
		    		new_tlsf_spec += "    " + LabelledFormula.of(a,spec.variables()) + ";\n";
		    	else
			    	new_tlsf_spec += "    " + LabelledFormula.of(GOperator.of(FOperator.of(a)),spec.variables()) + ";\n"	;
		    }
			new_tlsf_spec += "  }\n"
							+ '\n';
		}
		additionalGuarantee.addAll(spec.phiS());
		if (!additionalGuarantee.isEmpty()) {
			new_tlsf_spec += "  GUARANTEES {\n";
			
		    for (Formula f : additionalGuarantee) {
		    	if (Formula_Utils.numOfTemporalOperators(f)>0)
		    		new_tlsf_spec += "    " + LabelledFormula.of(f,spec.variables()) + ";\n";
		    	else
			    	new_tlsf_spec += "    " + LabelledFormula.of(GOperator.of(FOperator.of(f)),spec.variables()) + ";\n"	;
		    }
		    new_tlsf_spec += "  }\n";
		}
		else {
			new_tlsf_spec += "  GUARANTEES {\n    true; \n}\n";
		}
		
		new_tlsf_spec += '}';
		
		return TlsfParser.parse(new_tlsf_spec);
	}
	
	public static String tlsf2spectra(Tlsf tlsf) {
		String spectra_spec = "module TLSF2SPECTRA \n";
		// set variables
		int i = 0;
		while (tlsf.inputs().get(i))
			spectra_spec += "env boolean " + tlsf.variables().get(i++) + ";\n";
		while (tlsf.outputs().get(i))
			spectra_spec += "sys boolean " + tlsf.variables().get(i++) + ";\n";
		
		// set domain properties
		Formula domain_properties = Conjunction.of(tlsf.initially(),tlsf.require(), tlsf.assume());
		for (Formula domainP : Formula_Utils.splitConjunction(domain_properties)) {
			if (hasGFPattern(domainP))
				spectra_spec += "assumption\n GF "
						+ toSpectraFormat(LabelledFormula.of(getFormulaWOGFpattern(domainP), tlsf.variables()), tlsf.variables()) + ";\n";
			else
				spectra_spec += "assumption\n"
						+ toSpectraFormat(LabelledFormula.of(domainP, tlsf.variables()), tlsf.variables()) + ";\n";
		}
		
		// set system properties
		List<List<Formula>> system_properties = List.of(tlsf.guarantee(), tlsf.assert_());
		for (i = 0; i < system_properties.size(); i++) {
			List<Formula> system = system_properties.get(i);
			for (Formula systemP : system) {
				if (hasGFPattern(systemP))
					spectra_spec += "guarantee\n GF " 
							+ toSpectraFormat(LabelledFormula.of(getFormulaWOGFpattern(systemP), tlsf.variables()), tlsf.variables()) + ";\n";
				else
					spectra_spec += "guarantee\n"
							+ toSpectraFormat(LabelledFormula.of(systemP, tlsf.variables()), tlsf.variables()) + ";\n";
			}
		}
		if (hasGFPattern(tlsf.preset()))
			spectra_spec += "guarantee\n GF " 
					+ toSpectraFormat(LabelledFormula.of(getFormulaWOGFpattern(tlsf.preset()), tlsf.variables()), tlsf.variables()) + ";\n";
		else
			spectra_spec += "guarantee\n"
					+ toSpectraFormat(LabelledFormula.of(tlsf.preset(), tlsf.variables()), tlsf.variables()) + ";\n";
		
		return spectra_spec.replaceAll("X", "next");
	}

	private static String toSpectraFormat(LabelledFormula base, List<String> variables) {
		String form = base.toString();
		int i = 0;
		for (i = variables.size() -1 ; i >= 0; i--) {
			form = form.replaceAll("!"+variables.get(i),"!nagatedvar"+i);
			form = form.replaceAll("\\b"+variables.get(i)+"\\b",variables.get(i)+"=true");
		}
		for (i = variables.size() -1 ; i >= 0; i--) {
			form = form.replaceAll("!nagatedvar"+i,variables.get(i)+"=false");
		}
		return form;
	}
	
	public static boolean hasGFPattern(Formula source) {
		if (source instanceof GOperator) {
			var child = ((GOperator) source).operand;
			if (child instanceof FOperator) {
				return true;
			}
			else if (child instanceof Conjunction || child instanceof Disjunction) {
				for (Formula c : child.children())
					if (!(c instanceof FOperator))
						continue;
				return true;
			}
		}
		return false;
	}
	
	public static Formula getFormulaWOGFpattern(Formula source) {
		List<Formula> res = new ArrayList<Formula>();
		if (source instanceof GOperator) {
			var child = ((GOperator) source).operand;
			if (child instanceof FOperator) {
				return ((FOperator) child).operand;
			}
			else if (child instanceof Conjunction || child instanceof Disjunction) {
				child.children().forEach(c -> {
					if (c instanceof FOperator) res.add(((FOperator)c).operand);
				});
				if (child instanceof Conjunction) return Conjunction.of(res);
				else return Disjunction.of(res);
			}
		}
		return null;
	}
	
}
