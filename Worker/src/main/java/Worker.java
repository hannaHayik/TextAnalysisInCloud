import com.google.gson.Gson;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.util.List;
import java.io.*;
import java.net.URL;

public class Worker {
    
	/**
	 * Downloads a file from the given url, and names it as outputFileName
	 * @param url url of the file
	 * @param outputFileName chose name of the downloaded file
	 * @throws IOException
	 */
    public static void downloadFile(URL url, String outputFileName) throws IOException
    {
        try (InputStream in = url.openStream();
            ReadableByteChannel rbc = Channels.newChannel(in);
            FileOutputStream fos = new FileOutputStream(outputFileName)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
    }
    
    /**
     * parses a file according to given type with stanford-parser 3.6.0
     * @param type parse type: POS, DEPENDENCY, penn
     * @param fileURL 
     * @return a result line containing: parseType: inputFileURL outputS3Url
     */
    public static String parseTxt(String type, String fileURL) {
    	//obtain file name
    	String[] tmp = fileURL.split("/");
    	String fileName = tmp[tmp.length-1];

    	//choose proper parse type to pass to the stanford-parser
    	String parseType = "penn";
    	if(type.equalsIgnoreCase("POS"))
    		parseType = "wordsAndTags";
    	if(type.equalsIgnoreCase("DEPENDENCY"))
    		parseType = "typedDependencies";

    	String outputS3Url = type + ": " + fileURL;
    	
    	//downloading the input file
    	try {
    		URL linkFile = new URL(fileURL);
    		downloadFile(linkFile, fileName);
    	}
    	catch (Exception e) {
    		System.out.println("FAILED to download file: " + fileURL);
    		e.printStackTrace();
    		return outputS3Url + " " + e.toString();
    	}
    	
    	System.out.println("Launching Stanford Parser");
    	String[] parserArgs = {"-sentences", "newline", "-outputFormat", parseType, "-writeOutputFiles", "englishPCFG.ser.gz", fileName}; 
    	long startT = System.currentTimeMillis();
    	try {
    		
    	     LexicalizedParser.main(parserArgs);
    		//Runtime.getRuntime().exec("java edu.stanford.nlp.parser.lexparser.LexicalizedParser -outputFormat \"" + parseType + "\" -writeOutputFiles englishPCFG.ser.gz " + fileName).waitFor();
    	}
    	catch (Exception e) {
    		System.out.println("FAILED to parse file: " + fileName);
    		e.printStackTrace();
    		return outputS3Url + " " + e.toString();
    	}
    	long endT = System.currentTimeMillis();
    	System.out.println("Finished parsing successfully");
    	
    	//stanford-parser outputs the content in the same filename plus .stp at the end, ex: 1660-0.txt.stp
    	String outputName = fileName +".stp";
    	String keyName = parseType + "-" + outputName; //if we upload same file with diff dependency, it should have unique name
    	System.out.println("Trying to upload " + outputName + " to S3ResultBucket");
    	S3Utils.putObjectPublic(Paths.get(outputName), keyName, Defs.AWS_result_bucket);
    	outputS3Url += " https://" + Defs.AWS_result_bucket + ".s3.us-east-1.amazonaws.com/" + keyName + " " + (String.valueOf(endT-startT));
    	return outputS3Url;
    }

    public static void main(String[] args) throws InterruptedException {
        Thread visibilityThread = null;
        ExtendTime theExtender = null;
        
        while (true) {
            System.out.println("Worker checking for incoming messages");
            
            List<Message> messages = SqsUtils.receiveMessages(Defs.manager_to_workers_queue, 1);
            if (messages != null && messages.size() > 0) {
                try {
                	theExtender = new ExtendTime(messages.get(0), Defs.manager_to_workers_queue);
                    visibilityThread = new Thread(theExtender);
                    visibilityThread.start();
                    
                    //construct the received message
                    SqsMessage msg = new Gson().fromJson(messages.get(0).body(), SqsMessage.class);
                    Job j = new Gson().fromJson(msg.body, Job.class);
                    
                    //initiate parsing operation
                    System.out.println("trying to parse the text file");
                    String res = parseTxt(j.type,j.URL);
                    
                    //send the results back to manager
                    System.out.println("Message to be sent back:\n" + res + "\n");
                    SqsMessage toSendBack = new SqsMessage("NO_REPLY_NEEDED", res, false); 
                    SqsUtils.sendMessage(toSendBack, SqsUtils.getQueueUrl(msg.sqsReplyQueue));
                    theExtender.terminate = true;
                } catch (Exception e) {
                    System.out.println("Something went wrong in Worker: " + e);
                    e.printStackTrace();
                    if (visibilityThread != null)
                    	visibilityThread.interrupt();
                }
            }else {
                Thread.sleep(10000);
            }
        }
    }
}
