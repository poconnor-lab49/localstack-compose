package com.example.playground.service;

import static java.lang.String.format;

import com.example.playground.config.S3TransferManagerConfig;
import java.net.URI;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.DownloadRequest;

/**
 * S3 Services.
 *
 */
@ApplicationScoped
public class S3Service {
  private static final Logger LOG = LoggerFactory.getLogger(S3Service.class);

  private final S3TransferManager s3TransferManager;
  private final S3Client s3Client;

  @ConfigProperty(name = "aws.s3.bucket", defaultValue = "test-bucket")
  String bucket;

  /**
   * Constructor.
   *
   * @param s3Config transfer manager configuration
   */
  public S3Service(S3TransferManagerConfig s3Config, S3AsyncClient s3Client) {
    var region = s3Config.region();
    var endpoint = s3Config.client().endpointOverride()
        .orElseThrow(() -> new RuntimeException("Requiring an endpoint override for now"));

    LOG.info("Configuring S3TranferManger with region={} and endpoint={}", region, endpoint);

    this.s3TransferManager = S3TransferManager.builder().s3Client(s3Client).build();

    this.s3Client = S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .credentialsProvider(() -> AwsBasicCredentials.create("localstack", "localstack"))
        .region(Region.of(region))
        .build();
  }

  /**
   * Fetch a file from S3.
   *
   * @param filename the filename to get
   * @return the response bytes
   */
  public ResponseBytes<GetObjectResponse> getFile(String filename) {
    LOG.info("Fetching {} from {}", filename, bucket);

    var d = DownloadRequest.builder().getObjectRequest(t -> t.bucket(bucket)
          .key(filename)
          .build())
        .responseTransformer(AsyncResponseTransformer.toBytes()).build();

    var download = s3TransferManager.download(d);

    return download.completionFuture().join().result();
  }

  /**
   * Fetch the tags for an object.
   *
   * @param filename object to get tags from
   * @return the tags
   */
  public List<Tag> getTags(String filename) {

    var response = s3Client.getObjectTagging(otr -> otr.bucket(this.bucket).key(filename));

    return response.tagSet();
  }

  /**
   * Save a file to S3 and tag.
   *
   * @param filename file to save
   * @return eTag
   */
  public String putFile(String filename, String format) {
    String requestBody = getRequestBody(format);

    var putRequest = PutObjectRequest.builder()
        .bucket(this.bucket)
        .key(filename)
        .tagging("MyTag=TaggingWorks")
        .build();

    LOG.info("Saving {} to {}", filename, this.bucket);

    var upload = s3TransferManager.upload(ub -> ub.putObjectRequest(putRequest)
        .requestBody(AsyncRequestBody.fromString(requestBody)));

    return upload.completionFuture().join().response().eTag();
  }

  private String getRequestBody(String format) {
    LOG.info("Selected body format {}", format);
    String defaultBody = "Test file contents";

    if (format == null) {
      LOG.warn("No format specified. Using .txt");
      return defaultBody;
    }

    return switch (format) {
      case "xml" -> """
        <?xml version="1.0"?>
        <body>I think this is a valid xml file</body>
        """;
      case "txt" -> defaultBody;
      default -> throw new RuntimeException(format("Unsupported file type %s", format));
    };
  }

  static class S3AsyncClientProducer {
    private static final Logger LOG = LoggerFactory.getLogger(S3AsyncClientProducer.class);

    String endpointOverride;

    public S3AsyncClientProducer(S3TransferManagerConfig s3Config) {
      LOG.info("Creating producer");
      this.endpointOverride = s3Config.client().endpointOverride()
          .orElseThrow(() -> new RuntimeException("Requiring an endpoint override for now"));
    }

    @ApplicationScoped
    S3AsyncClient s3CrtAsyncClient(
        @ConfigProperty(name = "aws.s3.client.type", defaultValue = "classic") String clientType) {
      LOG.info("Supplying S3 client using endpoint {}", endpointOverride);

      var defaultClient = S3AsyncClient.builder()
          .credentialsProvider(() -> AwsBasicCredentials.create("localstack", "localstack"))
          .endpointOverride(URI.create(endpointOverride)).build();

      if (clientType == null) {
        LOG.warn("No client type selected. Using default client.");
        return defaultClient;
      }
      
      return switch (clientType) {
        case "crt" -> {
          LOG.info("Using CRT Client");
          yield S3AsyncClient.crtBuilder()
            .credentialsProvider(() -> AwsBasicCredentials.create("localstack", "localstack"))
            .endpointOverride(URI.create(endpointOverride)).build();
        }
        case "classic" -> defaultClient;
        default -> throw new RuntimeException(format("Unknown client type %s", clientType));
      };
    }

  }
}
