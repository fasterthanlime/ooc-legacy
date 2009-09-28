package org.ooc.compiler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * Utility class to read environment variables even under GCJ/GNU classpath
 * The System.getenv() method has been deprecated in Java 1.4 and reinstated
 * in 1.5, but apparently 
 * 
 * Taken from http://www.rgagnon.com/javadetails/java-0150.html
 * 
 * @author Amos Wenger
 */
public class ReadEnv {

	/**
	 * @return a property object containing all the environment variables
	 * @throws IOException 
	 * @throws Throwable
	 */
	public static Properties getEnvVars() throws IOException {
		Process p = null;
		Properties envVars = new Properties();
		Runtime r = Runtime.getRuntime();
		String OS = System.getProperty("os.name").toLowerCase();
		
		//System.out.println("OS: "+OS);
		
		if (OS.indexOf("windows 9") > -1) {
			p = r.exec("command.com /c set");
		} else if ((OS.indexOf("nt") > -1) || (OS.indexOf("windows 20") > -1)
				|| (OS.indexOf("windows xp") > -1)) {
			// thanks to JuanFran for the xp fix!
			p = r.exec("cmd.exe /c set");
		} else {
			// our last hope, we assume Unix (thanks to H. Ware for the fix)
			p = r.exec("env");
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(p
				.getInputStream()));
		String line;
		while ((line = br.readLine()) != null) {
			int idx = line.indexOf('=');
			if(idx != -1) {
				String key = line.substring(0, idx);
				String value = line.substring(idx + 1);
				envVars.setProperty(key, value);
				//System.out.println( key + " = " + value );
			} else {
				//System.out.println("While trying to get environment variables, got: " + line + "\n");
			}
		}
		return envVars;
	}

}