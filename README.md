# Text Analysis in the Cloud
real-world application to distributively process a list of text files, analyze them in various levels, upload the analyzed texts to S3, and generate an output HTML file.

Stack: AWS Java SDK, S3, EC2, S3, SQS, Stanford-Parser

Generic: This app can be suited to any Manager-Worker style application for processing data easily because of it's structure.

Execution Instructions:  
	1. Compile the source files through eclipse by specifying Maven goals as: "clean compile package"  
		or by running Maven from cmd as: mvn compile package  
	2. Go to your Home directory (ex: in Windows "C:\\Users\\Hanna"), Create a directory called ".aws" and  
		create a file called "credentials"  
	3. Open "credentials" and paste into it the AWS credentials from ur lab session.  
		NOTE: everytime you restart the lab, the credentials must be reloaded into this file.  
	4. Put the input file in the same directory with the JAR file  
	5. Put englishPCFG.ser.gz module in the same directory with the JAR file  
	6. From CMD run: "java -jar Local-1.0-SNAPSHOT.jar input-sample.txt output.html 1 terminate"  
		NOTE: check Manager/main.java for more execution options  
  
Technical Info:  
	- AMI: ami-00e95a9222311e8ed (Amazon Linux 64bit)  
	- Instance type: T2.Medium (there is an explanation for this choice)  
	- Execution time: 2006 seconds (33.4 minutes) on 9 files (Moodle input, 9 parsing operations) with 1:1 workerToFile ratio  
			  3252 seconds (54.2 minuts) on 9 files (Moodle input) with 1:2 worker to file ratio  

Workflow (simplified):  
	1. Local starts Manager instance if not started yet  
	2. Local checks/creates the persistant buckets (JAR bucket, Results bucket) if they exists, has access  
	3. Local checks/uploads if the files are there (parser module englishPCFG.ser.gz, assignment JAR...)  
	4. Local sends SQS message to the Manager notifying it about a new job  
	5. Local now awaits for a "finished" message from Manager  
	6. Manager creates proper buckets/queues for communications  
	7. Manager initaites a proper number of Workers  
	8. Manager creates an SQS message for every line in the input & sends to workers  
	9. Manager now awaits for the results from workers  
	10. Workers try to read messages from proper queues  
	11. Workers finds a job, downloads a file, parses it, uploads results to S3 and notifies Manager about the results  
	12. Manager gathers the results for the same Local in one file and uploads it to S3  
	13. Manager notifies Local about finishing the job & the location of the results file in S3  
	14. Local downloads the results file, extracts the needed info to create the HTML file  
