import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.io.*;
import java.nio.file.Paths;
import java.util.List;

public class Local {
    final static String manager_to_local_reply_queue_name = Defs.manager_to_local_queue + ((int)(Math.random() * 1000000)) + "q" + ((int)(Math.random() * 100));
    final static String local_to_manager_bucket_name = Defs.local_to_manager_bucket + ((int)(Math.random() * 1000000)) + "b" + ((int)(Math.random() * 100));

    public static void main(String[] args) throws InterruptedException, IOException {
        System.out.println("Local Application starting\n");
        
        //String cwd = "C:\\Users\\Hanna\\Downloads\\stanford-parser-4.2.0\\";
    	String cwd = Paths.get(System.getProperty("user.dir")) + File.separator;
    	
        if (args.length == 6) {
        	System.out.println("   ===================Warning===================");
        	System.out.println("   You've choosen the portable version of the program!");
        	System.out.println("   This is a beta version and may contain some errors.");
        	System.out.println("   If you are not sure of the 2 bucket names you entered,");
        	System.out.println("   this may crash the program.\n");
        	System.out.println("   NOTE: crashing the program might leave some resources running!");
        	Defs.AWS_result_bucket = args[5];
        	Defs.jar_bucket = args[4];
        }
        //the Jar bucket will be created once for all locals, and we won't delete it
        if (!S3Utils.checkIfBucketExistsAndHasAccessToIt(Defs.jar_bucket)) 
            S3Utils.createBucket(Defs.jar_bucket);
        
        //First we start by uploading the MOST UPDATED jar file for the Manager/Worker to run
        System.out.println("Uploading assignment JAR"); 
        //S3Utils.putObjectPublic(Paths.get(cwd + "Local-1.0-SNAPSHOT.jar"), "AssJar", Defs.jar_bucket); // Upload parser to S3
        System.out.println("Upload Done!");
        
        //Starting the manager
        Ec2Utils.startEc2IfNotExist("Manager", Scripts.initManager);
        
        //flag if terminate is included in args
        boolean terminationFlag = false;
        if (args.length == 4 && args[3].equalsIgnoreCase("terminate")) {
        	terminationFlag = true;
        }
        
        SqsUtils.createSqsQueue(Defs.local_to_manager_queue);
        
        /*
        if(!S3Utils.doesObjectExist(Defs.jar_bucket, "StanfordParser")) {
        	//S3Utils.putObject(Paths.get("C:\\Users\\Hanna\\Downloads\\stanford-parser-4.2.0\\stanford-parser.jar"), "StanfordParser", Defs.jar_bucket); // Upload parser to S3
        	System.out.println("Uploaded parser");
        }
        */
        
       //System.out.println("ParserModule already uploaded, Nice!");
        if(!S3Utils.doesObjectExist(Defs.jar_bucket, "ParserModule")) {
        	S3Utils.putObjectPublic(Paths.get(cwd + "englishPCFG.ser.gz"), "ParserModule", Defs.jar_bucket); // Upload parser to S3
        	System.out.println("Uploaded module");
        }
        
        if (!S3Utils.checkIfBucketExistsAndHasAccessToIt(local_to_manager_bucket_name))
            S3Utils.createBucket(local_to_manager_bucket_name);
        if (!S3Utils.checkIfBucketExistsAndHasAccessToIt(Defs.AWS_result_bucket))
            S3Utils.createBucket(Defs.AWS_result_bucket);
        SqsUtils.createSqsQueue(manager_to_local_reply_queue_name);


    	System.out.println("Uploading input file to S3...");
        S3Utils.putObject(Paths.get(args[0]), "InputFile", local_to_manager_bucket_name); 
        
        //create message that will be sent to manager,
        //1st arg: which queue should the manager reply to, 2nd which queue to send this msg to, 3rd if manager should terminate
        SqsMessage sqsMessage = new SqsMessage(manager_to_local_reply_queue_name, local_to_manager_bucket_name, terminationFlag);
        
        sqsMessage.workerMessageRatio = Integer.parseInt(args[2]);
        SqsUtils.sendMessage(sqsMessage, SqsUtils.getQueueUrl(Defs.local_to_manager_queue));
        System.out.println("File uploaded successfully.");
        
        FileWriter fw;
        ResponseInputStream workersOutputStream = null;
        Writer writer;
        BufferedReader reader = null;
    	long startT = System.currentTimeMillis();
        while (true) {
            try {
                List<Message> messages = SqsUtils.receiveMessages(manager_to_local_reply_queue_name);
                if (messages.size() != 0) {
                    System.out.println("Message content: " + messages.get(0).body());

                    SqsUtils.deleteMessage(messages.get(0), manager_to_local_reply_queue_name);
                    workersOutputStream = S3Utils.getObjectS3("WorkersOutputFile.txt", local_to_manager_bucket_name);
                    
                    //Writing to output file
                    reader = new BufferedReader(new InputStreamReader(workersOutputStream));
                    fw = new FileWriter(args[1]);
                    writer = new BufferedWriter(fw);
                    writer.write("<!DOCTYPE html>");
                    writer.write("<html>");
                    writer.write("<body>");
                    String Line;
                    String[] splits = {};
                    String str = "";
                    while ((Line = reader.readLine()) != null) {
                    	splits = Line.split(" ");	//this is for time measurements, our DEBUG game is on point :) !!
                    	str = splits[0] + " " + splits[1] + " " + splits[2];
                    	System.out.println("Parse Type: " + splits[0] + ",   File: " + splits[1] + ",   Elapsed Time: " + splits[3]);
                        writer.write("<p>" + str + "</p>");
                        writer.write('\n');
                    }
                    writer.write("</body>");
                    writer.write("</html>");
                    writer.close();
                    
                	System.out.println("Elapsed Time: " + ((System.currentTimeMillis()-startT)/1000) + " seconds");
                    //delete our queue & buckets to not waste resources
                    SqsUtils.deleteQueue(manager_to_local_reply_queue_name);
                    S3Utils.deleteBucket(local_to_manager_bucket_name);
                    //S3Utils.deleteBucket(Defs.AWS_result_bucket); //if we delete this bucket, we can't see the output...
                    return;
                }
                System.out.print("Waiting for messages from Manager (" + ((System.currentTimeMillis()-startT)/1000) + " seconds)...\r");
                Thread.sleep(11000);
            } catch (SdkClientException ignored) {
                ;
            }
        }
    }
}
