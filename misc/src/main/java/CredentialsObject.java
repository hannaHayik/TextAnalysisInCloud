
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;

public class CredentialsObject {
    public  String accessKey;
    public  String secretKey;
    public  String sessionToken;
    public CredentialsObject(String accessKey, String secretKey, String sessionToken)
    {
        this.accessKey=accessKey;
        this.secretKey=secretKey;
        this.sessionToken=sessionToken;
    }
    public AwsCredentials getCredentials()
    {
        if (sessionToken==null)
            return AwsBasicCredentials.create(accessKey, secretKey);
        else
            return AwsSessionCredentials.create(accessKey,secretKey,sessionToken);
    }
}
