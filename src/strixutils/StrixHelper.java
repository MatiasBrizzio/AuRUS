package strixutils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import owl.ltl.tlsf.Tlsf;
import tlsf.TLSF_Utils;

public class StrixHelper {

	private static FileWriter writer;
	private static String TARGET = "TARGET: Mealy";
	private static String SEMANTICS = "SEMANTICS:   Mealy";
	private static String DESC = "DESCRIPTION: \"Example of unrealizability\" ";
	private static String TITLE = "TITLE:       \"BC example\"";
	private static final String[] params = new String[]{"lib/strix/bin/strix", "BC.tlsf" };

	/**
	 * Checks the realizability of the system specified by the TLSF file passed as parameter
	 * @param path path to TLSF file
	 * @return true iff the TLSF file in path is realizable
	 * @throws IOException
	 */
	public static boolean checkRealizability(String path) throws IOException {
		return executeStrix(path);
	}
	
	/**
	 * Checks the realizability of the system specified by the TLSF file passed as parameter
	 * @param tlsf tlsf file containing all specification.
	 * @return true iff the tlsf file is realizable
	 * @throws IOException
	 */
	public static boolean checkRealizability(Tlsf tlsf) throws IOException {
		//Writes the tlsf object into file...
		String tlsf_string = TLSF_Utils.toTLSF(tlsf);
		File file = new File(tlsf.title().replace("\"", "")+".tlsf");
		//Create the file
		try {
			if (file.createNewFile()){
			  //System.out.println("File is created!");
			}else{
			  //System.out.println("File already exists.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			writer = new FileWriter(file);
			writer.write(tlsf_string);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return executeStrix(file.getPath());
	}

	/**
	 * Calls strix program with the file in path
	 * @param path path to TLSF file
	 * @return
	 * @throws IOException
	 */
	private static boolean executeStrix(String path) throws IOException {
		Process pr = Runtime.getRuntime().exec( new String[]{"lib/strix/bin/strix", "."+path});
		InputStream in = new BufferedInputStream( pr.getInputStream());
		
		int i = 0;
		boolean flag = false;
		byte[] buffer = new byte[1024];
		String res;
		while ( in.read(buffer) != -1 && i++<1) {
		    res = new String(buffer).trim();
		    if (res.equals("REALIZABLE")) flag = true;
		}  
		return flag;
	}

	
	/**
	 * Generate a TLSF file containing all information passed as a parameter.
	 * @param bc boundary condition
	 * @param inputs a set containing all inputs propositions
	 * @param outputs a set containing all outputs propositions
	 * @param goals	a set containing all system goals 
	 * @param dom a set representing the system domain
	 * @return the file's path is returned 
	 */
	public static String generateTlsfFile(String bc, List<String> inputs, List<String> outputs,
			List<String> goals, List<String> dom) {
		
		File file = new File("BC.tlsf");
		
		//Create the file
		try {
			if (file.createNewFile()){
			  //System.out.println("File is created!");
			}else{
			  //System.out.println("File already exists.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		 
		try {
			writer = new FileWriter(file);
			writer.write("");
			writer.write("INFO{\n" + TITLE +"\n" + DESC +"\n" + SEMANTICS +"\n" + TARGET+"\n}\n" );
			writer.write("MAIN{\n");
			writeIo(file, inputs, true);
			writeIo(file, outputs, false);
			writeAssume(file,bc,dom);
			writeGoals(file,goals);
			writer.write("}\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return file.getPath();
	}

	/**
	 * Writes all goals into file
	 * @param file where the goals will be written.
	 * @param goals the list of all system goals.
	 * @throws IOException
	 */
	private static void writeGoals(File file, List<String> goals) throws IOException {
		writer.write("GUARANTEE{\n");
		for (String ltl : goals ) 
			writer.write(ltl+";\n");
		writer.write("\n}\n");
	}

	/**
	 * Write all system assumes.
	 * @param file the file where the assumes will be written
	 * @param bc boundary condition to be added into new domain.
	 * @param dom the system domain.
	 * @throws IOException
	 */
	private static void writeAssume(File file, String bc, List<String> dom) throws IOException {
		writer.write("ASSUME{\n");
		for (String ltl : dom ) 
			writer.write(ltl+";\n");
		//add !BC to new domain
		writer.write("!("+bc+");");
		writer.write("\n}\n");
	}

	/**
	 * Write and inputs or outputs proposition.
	 * @param file 
	 * @param io
	 * @param isInput
	 * @throws IOException
	 */
	private static void writeIo(File file, List<String> io, boolean isInput) throws IOException {
		if (isInput) writer.write("INPUTS{\n");
		else writer.write("OUTPUTS{\n");
		for (String in : io ) {
			writer.write(in);
		}
		writer.write("\n}\n");
	}
	
	/**
	 * Call a strix program
	 * @return a boolean value indicating if the strix call gives as a result a "realizable" is returned
	 * @throws IOException
	 */
	public static boolean executeStrix() throws IOException {
		Process pr = Runtime.getRuntime().exec(params);
		InputStream in = new BufferedInputStream( pr.getInputStream());
		int i = 0;
		boolean flag = false;
		byte[] buffer = new byte[1024];
		String res;
		while ( in.read(buffer) != -1 && i++<1) {
		    res = new String(buffer).trim();
		    if (res.equals("REALIZABLE")) flag = true;
		}  
		return flag;
	}

	public static boolean checkRealizability(List<String> dom, List<String> goals, List<String> inputs,
			List<String> outputs) throws IOException {
		StrixHelper.generateTlsfFile("false",inputs,outputs,goals,dom);
		return StrixHelper.executeStrix();
	}
}
