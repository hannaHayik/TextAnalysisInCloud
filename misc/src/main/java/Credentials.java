import software.amazon.awssdk.auth.credentials.*;
import java.io.*;

public class Credentials {
    public static String accessKey;
    public static String secretKey;
    public static String sessionToken;
    
    /**
     * Loads the AWS credentials from the file in the given path
     * @param path : path to AWS credentials file
     */
    public static void loadCredentials(String path){
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String line = reader.readLine();
            line = reader.readLine();
            accessKey = line.split("=", 2)[1];
            line = reader.readLine();
            secretKey = line.split("=", 2)[1];
            line = reader.readLine();
            if (line!=null) 
            	sessionToken = line.split("=", 2)[1];
            reader.close();
        }
        catch (IOException e)
        {
            System.out.println(e);
        }

    }
    
    /**
     * 
     * @return AwsBasicCredentials OR AwsSessionCredentials object from the existing info
     */
    static public AwsCredentials getCredentials()
    {
        if (sessionToken==null)
            return AwsBasicCredentials.create(accessKey, secretKey);
        else
            return AwsSessionCredentials.create(accessKey,secretKey,sessionToken);
    }
    
    /**
     * 
     * @return AwsCredentials object
     */
    static public AwsCredentials create()
    {
        return new CredentialsObject(accessKey, secretKey, sessionToken).getCredentials();
    }

}
