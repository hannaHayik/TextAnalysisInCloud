import com.google.gson.Gson;

public class SqsMessage {
    public String s3ReplyBucket;
    public String sqsReplyQueue;
    public String body;
    public Boolean terminate;
    public int workerMessageRatio;
    public int n = 0;
    public SqsMessage(String sqsReplyQueue, String body, Boolean terminate)
    {
        this.sqsReplyQueue = sqsReplyQueue;
        this.terminate=terminate;
        this.body=body;
    }

    public String toString()
    {
        return new Gson().toJson(this);
    }
}