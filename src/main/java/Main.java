import java.net.URI;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.DownloadRequest;

/**
 * Minimal reproducer for bug
 *
 */
public class Main {
  private static final String BUCKET = "test-bucket";
  private static final String KEY = "testfile.txt";

  public static void main(String... args) {
    // resolves to 127.0.0.1
    var endpoint = "http://s3.localhost.localstack.cloud:4566";

    // change this to `S3AsyncClient.builder()` and everything works
//    var client = S3AsyncClient.builder()
    var client = S3AsyncClient.crtBuilder()
        .credentialsProvider(() -> AwsBasicCredentials.create("localstack", "localstack"))
        .endpointOverride(URI.create(endpoint))
        .build();

    var putRequest = PutObjectRequest.builder()
        .bucket(BUCKET).key(KEY)
        .tagging("MyTag=TaggingWorks")
        .build();

    var s3TransferManager = S3TransferManager.builder()
        .s3Client(client)
        .build();

    var upload = s3TransferManager.upload(ub -> ub.putObjectRequest(putRequest)
        .requestBody(AsyncRequestBody.fromString("File contents")));

    upload.completionFuture().join();

    var d = DownloadRequest.builder().getObjectRequest(t -> t.bucket(BUCKET).key(KEY).build())
        .responseTransformer(AsyncResponseTransformer.toBytes())
        .build();

    var download = s3TransferManager.download(d);

    System.out.println(download.completionFuture().join().result().asUtf8String());
  }

}
