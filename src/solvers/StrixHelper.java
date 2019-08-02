package solvers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import owl.ltl.tlsf.Tlsf;
import tlsf.TLSF_Utils;

public class StrixHelper {

	private static FileWriter writer;

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
	 * @throws InterruptedException 
	 */
	public static boolean checkRealizability(Tlsf tlsf) throws IOException, InterruptedException {
		//Writes the tlsf object into file...
		Tlsf tlsf2 = TLSF_Utils.toBasicTLSF(TLSF_Utils.toTLSF(tlsf));
		String tlsf_string = TLSF_Utils.toTLSF(tlsf2);
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
}
