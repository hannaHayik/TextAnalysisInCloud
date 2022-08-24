import com.google.gson.Gson;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class ManagerTask implements Runnable {
	public volatile static int CurrWorkers = 1;
	public volatile static int MaxWorkers = 15;
	private int random = (int) (Math.random() * 100000);
	final SqsMessage message;

	public ManagerTask(SqsMessage msg) {
		this.message = msg;
	}

	/**
	 * Sends the job to MANAGER_TO_WORKER_QUEUE
	 * 
	 * @param m the job
	 */
	public void notifyWorkerOfJob(Job x) {
		SqsMessage msg = new SqsMessage(Defs.worker_to_manager_queue + random, new Gson().toJson(x), false);
		SqsUtils.sendMessage(msg, SqsUtils.getQueueUrl(Defs.manager_to_workers_queue));
	}

	public void run() {
		String InputFromLocal = S3Utils.getObject("InputFile", message.body);
		String[] lines = InputFromLocal.split("\n");
		int NumOfJobs = 0;
		SqsUtils.createSqsQueue(Defs.worker_to_manager_queue + random);

		for (String x : lines) {
			String[] tabSplitted = x.split("\t");
			notifyWorkerOfJob(new Job(tabSplitted[0], tabSplitted[1]));
			NumOfJobs++;
			System.out.println("Processing ==> " + x);
		}

		int workersCount = NumOfJobs / message.workerMessageRatio;
		if (workersCount > 1) {
			MoreWorkers(workersCount);
		}

		File WorkersOutputFile = new File("WorkersOutputFile.txt");
		int finishedJobs = 0;
		FileWriter fileWrite = null;
		SqsMessage temp;
		System.out.println("Opening filewriter & waiting for messages from workers...");

		try {
			fileWrite = new FileWriter("WorkersOutputFile.txt");
		} catch (IOException e) {
			System.out.println("Failed to open/write to WorkersOutputFile.txt");
			e.printStackTrace();
		}
		while (finishedJobs < NumOfJobs) {
			List<Message> messages = SqsUtils.receiveMessages(Defs.worker_to_manager_queue + random);
			if (messages.size() != 0) {
				for (Message msg : messages) {
					System.out.println("Manager receieved message: " + msg.body());
					temp = new Gson().fromJson(msg.body(), SqsMessage.class);

					try {
						assert fileWrite != null;
						fileWrite.write(temp.body);
						fileWrite.write("\n");
					} catch (IOException e) {
						e.printStackTrace();
					}

					finishedJobs++;
					SqsUtils.deleteMessage(msg, Defs.worker_to_manager_queue + random);
				}
			}
			
			if (messages.size() == 0) {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		
		try {
			fileWrite.close();
		} catch (IOException e) {
			System.out.println("Error: couldn't close FileWriter fileWrite");
			e.printStackTrace();
		}
		
		System.out.println("File sent to bucket: " + message.body);
		S3Utils.putObject(Paths.get("WorkersOutputFile.txt"), "WorkersOutputFile.txt", message.body);
		WorkersOutputFile.delete();
		System.out.println("Manager finished a task");
		
		SqsMessage mess = new SqsMessage("NO_REPLY_NEEDED", "finished", false);
		SqsUtils.sendMessage(mess, SqsUtils.getQueueUrl(message.sqsReplyQueue));
		SqsUtils.deleteQueue(Defs.worker_to_manager_queue + random);
	}

	/**
	 * Initiates more EC2 instances as workers.  
	 * Synchronized because of Manager's task pool multiple threads might use it.
	 * 
	 * @param numToAdd
	 */
	private synchronized static void MoreWorkers(int numToAdd) {
		if (numToAdd > MaxWorkers) {
			if ((MaxWorkers - CurrWorkers) > 0) {
				Ec2Utils.startEc2s("Worker", Scripts.initEmployee, (MaxWorkers - CurrWorkers));
				CurrWorkers = MaxWorkers;
			}
		} else if (numToAdd > CurrWorkers) {
			Ec2Utils.startEc2s("Worker", Scripts.initEmployee, numToAdd - CurrWorkers);
			CurrWorkers = numToAdd;
		}
		if (CurrWorkers >= MaxWorkers)
			return;
	}
}
