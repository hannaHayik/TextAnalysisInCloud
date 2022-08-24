import software.amazon.awssdk.services.sqs.model.Message;

public class ExtendTime implements Runnable {
    private Message msg;
    public volatile boolean terminate = false;
    private String queueInUse;

    public ExtendTime(Message mess, String queueName) {
        this.queueInUse = queueName;
        this.msg = mess;
    }

    @Override
    public void run() {
        while (true) {
            if (terminate) {
                SqsUtils.deleteMessage(msg, queueInUse);
                break;
            } else {
            	SqsUtils.changeVisibilityTime(SqsUtils.getQueueUrl(queueInUse), this.msg, 25);
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
}
