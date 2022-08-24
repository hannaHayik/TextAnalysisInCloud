
public class Defs {
	//Queues names should contain: alphanumeric characters, hyphens, or underscores
	//Bucket names can't contain underscores or uppercase letters
    public static String local_to_manager_queue = "local_to_manager_queue";
    public static String manager_to_workers_queue = "manager_to_worker_queue";
    public static String manager_to_local_queue = "manager_to_local_queue";
    public static String worker_to_manager_queue = "worker_to_manager_queue";
    public static String local_to_manager_bucket = "hannalocaltomanagerbucket";
    public static String AWS_result_bucket = "hannaresultbucketdps";
    public static String jar_bucket = "mybuckethannadpscourse";
}
