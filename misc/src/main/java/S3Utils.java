import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class S3Utils {
	public static final S3Client s3 = S3Client.builder().region(Region.US_EAST_1)
			.credentialsProvider(StaticCredentialsProvider.create(Credentials.getCredentials())).build();

	/**
	 * Creates a bucket in S3
	 * @param bucket name of the bucket
	 */
	public static void createBucket(String bucket) {
		s3.createBucket(CreateBucketRequest.builder().bucket(bucket)
				.createBucketConfiguration(CreateBucketConfiguration.builder().build()).build());
		System.out.println(bucket + " Has been created.");
	}

	/**
	 * Deletes the bucket from S3
	 * @param bucket name of bucket
	 */
	public static void deleteBucket(String bucket) {
		deleteBucketObjects(bucket);
		DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucket).build();
		s3.deleteBucket(deleteBucketRequest);
	}

	/**
	 * Deletes all objects of bucket
	 * @param bucketName
	 */
	public static void deleteBucketObjects(String bucketName) {

		try {
			ListObjectsRequest req = ListObjectsRequest.builder().bucket(bucketName).build();
			ListObjectsResponse res = s3.listObjects(req);
			List<S3Object> objects = res.contents();

			for (S3Object x : objects) {
				deleteBucketObject(bucketName, x.key());
			}

		} catch (S3Exception e) {
			System.err.println(e.awsErrorDetails().errorMessage());
		}
	}

	/**
	 * Deletes object from bucket
	 * @param bucketName
	 * @param objectName
	 */
	public static void deleteBucketObject(String bucket, String obj) {

		ArrayList<ObjectIdentifier> lstOfObjects = new ArrayList<ObjectIdentifier>();
		lstOfObjects.add(ObjectIdentifier.builder().key(obj).build());

		try {
			DeleteObjectsRequest req = DeleteObjectsRequest.builder().bucket(bucket)
					.delete(Delete.builder().objects(lstOfObjects).build()).build();
			s3.deleteObjects(req);
		} catch (S3Exception e) {
			System.err.println(e.awsErrorDetails().errorMessage());
		}
	}

	/**
	 * check if bucket exists
	 * @param bucket name of bucket
	 * @return true if bucket exists, false otherwise
	 */
	public static boolean checkIfBucketExistsAndHasAccessToIt(String bucket) {
		HeadBucketRequest req = HeadBucketRequest.builder().bucket(bucket).build();

		try {
			s3.headBucket(req);
			return true;
		} catch (NoSuchBucketException e) {
			return false;
		}
	}

	/**
	 * Uploads data to S3 bucket and pairs it to key
	 * @param data data to upload
	 * @param key name of key the data is going to be paired to in S3 bucket
	 * @param bucketName
	 */
	public static void putObject(String data, String key, String bucketName) {
		s3.putObject(PutObjectRequest.builder().key(key).bucket(bucketName).build(),
				RequestBody.fromBytes(data.getBytes(StandardCharsets.UTF_8)));
	}
	
	/**
	 * Uploads data to S3 bucket and pairs it to key, makes file public
	 * @param data data to upload
	 * @param key name of key the data is going to be paired to in S3 bucket
	 * @param bucketName
	 */
	public static void putObjectPublic(String data, String key, String bucketName) {
		s3.putObject(PutObjectRequest.builder().key(key).bucket(bucketName).acl(ObjectCannedACL.PUBLIC_READ).build(),
				RequestBody.fromBytes(data.getBytes(StandardCharsets.UTF_8)));
	}
	
	/**
	 * Uploads file from path to S3 bucket and pairs it to key, makes file public
	 * @param data data to upload
	 * @param key name of key the data is going to be paired to in S3 bucket
	 * @param bucketName
	 */
	public static void putObjectPublic(Path path, String key, String bucketName) {
		s3.putObject(PutObjectRequest.builder().key(key).bucket(bucketName).acl(ObjectCannedACL.PUBLIC_READ).build(), RequestBody.fromFile(path));
	}

	/**
	 * Uploads file from path to S3 bucket
	 * @param path path of file
	 * @param key
	 * @param bucketName
	 */
	public static void putObject(Path path, String key, String bucketName) {
		s3.putObject(PutObjectRequest.builder().key(key).bucket(bucketName).build(), RequestBody.fromFile(path));
	}

	
	/**
	 * Downloads file from S3 bucket and returns it as String
	 * @param key name of file in S3 bucket
	 * @param bucketName
	 * @return String
	 */
	public static String getObject(String key, String bucketName) {
		BufferedReader reader;
		ResponseInputStream<GetObjectResponse> s3Obj = s3
				.getObject(GetObjectRequest.builder().key(key).bucket(bucketName).build());
		reader = new BufferedReader(new InputStreamReader(s3Obj));
		String line;
		StringBuilder obj = new StringBuilder();
		try {

			while ((line = reader.readLine()) != null) {
				obj.append(line).append("\n");
			}
		} catch (IOException e) {
			System.out.println(e);
		}
		return obj.toString();
	}
	
	/**
	 * Check if key exists in bucket
	 * @param bucket
	 * @param key
	 * @return True if exists, False otherwise
	 */
	public static boolean doesObjectExist(String bucket, String key) {
	    	ListObjectsResponse resp = s3.listObjects(ListObjectsRequest.builder().bucket(bucket).build());
	    	for(S3Object obj : resp.contents())
	    		if(obj.key().equalsIgnoreCase(key))
	    				return true;
	        return false;
	
	}

	/**
	 * Returns object from S3 bucket as stream
	 * @param key
	 * @param bucketName
	 * @return Data as stream
	 */
	public static ResponseInputStream getObjectS3(String key, String bucketName) {
		return s3.getObject(GetObjectRequest.builder().key(key).bucket(bucketName).build());
	}

	/**
	 * Deletes object from S3 bucket
	 * @param key
	 * @param bucketName
	 */
	public static void deleteObject(String key, String bucketName) {
		s3.deleteObject(DeleteObjectRequest.builder().key(key).bucket(bucketName).build());
	}
}
