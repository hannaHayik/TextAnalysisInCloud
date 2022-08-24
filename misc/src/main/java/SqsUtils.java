import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

//import java.util.HashMap;
import java.util.List;
//import java.util.Map;

public class SqsUtils {
	private static final SqsClient sqs = SqsClient.builder().region(Region.US_EAST_1)
			.credentialsProvider(StaticCredentialsProvider.create(Credentials.getCredentials())).build();

	public static boolean checkIfSqsIsOpened(String sqsName) {
		try {
			sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(sqsName).build()).queueUrl();
			return true;
		} catch (QueueDoesNotExistException e) {
			return false;
		}
	}

	public static void createSqsQueue(String queueName) {
		try {
			sqs.createQueue(CreateQueueRequest.builder().queueName(queueName).build());
			System.out.println(queueName + " Queue has been created.");
		} catch (Exception e) {
			System.out.println(e + "\nCannot create queue: " + queueName + "!!");
			return;
		}
	}

	public static String getQueueUrl(String sqsName) {
		return sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(sqsName).build()).queueUrl();
	}

	public static void deleteQueue(String sqsName) {
		try {
			sqs.deleteQueue(DeleteQueueRequest.builder()
					.queueUrl(sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(sqsName).build()).queueUrl())
					.build());
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public static void sendMessage(SqsMessage message, String url) {
		sqs.sendMessage(SendMessageRequest.builder().messageBody(message.toString()).queueUrl(url).build());
	}

	public static Message receiveOneMessage(String sqsName) {
		List<Message> messageList = sqs.receiveMessage(ReceiveMessageRequest.builder().maxNumberOfMessages(1)
				.queueUrl(sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(sqsName).build()).queueUrl()).build())
				.messages();
		if (messageList.size() != 0)
			return messageList.get(0);
		else
			return null;
	}

	public static List<Message> receiveMessages(String sqsName) {
		return sqs.receiveMessage(ReceiveMessageRequest.builder()
				.queueUrl(sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(sqsName).build()).queueUrl()).build())
				.messages();
	}

	public static List<Message> receiveMessages(String sqsName, int max) {
		return sqs.receiveMessage(ReceiveMessageRequest.builder()
				.queueUrl(sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(sqsName).build()).queueUrl())
				.maxNumberOfMessages(max).build()).messages();
	}

	public static void deleteMessage(Message message, String sqsName) {
		sqs.deleteMessage(DeleteMessageRequest.builder().receiptHandle(message.receiptHandle())
				.queueUrl(getQueueUrl(sqsName)).build());
	}

	public static void changeVisibilityTime(String url, Message message, Integer time) {
		sqs.changeMessageVisibility(ChangeMessageVisibilityRequest.builder().visibilityTimeout(time).queueUrl(url)
				.receiptHandle(message.receiptHandle()).build());
	}

}
