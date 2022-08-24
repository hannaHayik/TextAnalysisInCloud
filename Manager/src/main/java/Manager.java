import com.google.gson.Gson;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Manager {
    static boolean terminate = false;

    public static void main(String[] args) throws FileNotFoundException {
    	System.out.println("Starting Manager");
    	
        SqsUtils.createSqsQueue(Defs.manager_to_workers_queue);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        
        Ec2Utils.startEc2s("Worker", Scripts.initEmployee, 1);
        System.out.println("Manager Created 1st Worker, Good!");
        
        while(true){
            List<Message> inbox;
            if (terminate) {
                pool.shutdown();
                while (!pool.isTerminated()) {
                    try {
						Thread.sleep(6000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
                }
                
                //delete all resources
                SqsUtils.deleteQueue(Defs.local_to_manager_queue);
                SqsUtils.deleteQueue(Defs.manager_to_workers_queue);
                Ec2Utils.terminateAll("Worker");
                Ec2Utils.terminateAll("Manager");
                break;
            }
            inbox = SqsUtils.receiveMessages(Defs.local_to_manager_queue,1);
            if (inbox != null && inbox.size() != 0) {
                SqsMessage sqsmessage = new Gson().fromJson(inbox.get(0).body(), SqsMessage.class);
                terminate = sqsmessage.terminate;
                
                ManagerTask task = new ManagerTask(sqsmessage);
                SqsUtils.deleteMessage(inbox.get(0), Defs.local_to_manager_queue); 
                pool.execute(task);
            }
        }
    }
}
