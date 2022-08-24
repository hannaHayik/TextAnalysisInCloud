import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Scripts {
	
	/**
	 * Loads AWS credentials from user home/.aws/credentials file
	 * @return AWS credentials as string
	 */
    public static String loadCredentials() {
        BufferedReader reader;

        try {
            String tmp = System.getProperty("user.home") + File.separator + "/.aws/credentials";
            File credPath = new File(tmp);
            reader = new BufferedReader(new FileReader(credPath));
            StringBuilder ret = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                ret.append(line).append("\n");
            }
            reader.close();
            return ret.toString();
        } catch (IOException e) {
            System.out.println(e);
            return null;
        }
    }
    
    public static String initInstanceBash =
            		"#!/bin/bash\n" +
                    "sudo mkdir /root/.aws\n" +
                    "echo \"" + loadCredentials() + "\" > /root/.aws/credentials" + "\n" +
                    "aws s3api get-object --bucket " + Defs.jar_bucket + " --key AssJar Local-1.0-SNAPSHOT.jar\n"+
                    "sudo yum install java-1.8.0 -y\n";
    public static String initManager = initInstanceBash + "java -jar Local-1.0-SNAPSHOT.jar Manager\n";
    public static String initEmployee = initInstanceBash + 
    		"aws s3api get-object --bucket " + Defs.jar_bucket + " --key ParserModule englishPCFG.ser.gz\n" + 
    		"java -jar Local-1.0-SNAPSHOT.jar Worker\n";

}


