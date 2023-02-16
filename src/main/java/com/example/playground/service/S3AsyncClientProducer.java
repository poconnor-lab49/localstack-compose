package com.example.playground.service;

import static java.lang.String.format;

import com.example.playground.config.S3TransferManagerConfig;
import java.net.URI;
import java.time.Duration;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@ApplicationScoped
class S3AsyncClientProducer {
  private static final Logger LOG = LoggerFactory.getLogger(S3AsyncClientProducer.class);

  String endpointOverride;

  public S3AsyncClientProducer(S3TransferManagerConfig s3Config) {
    LOG.info("Creating producer");
    this.endpointOverride = s3Config.client().endpointOverride()
        .orElseThrow(() -> new RuntimeException("Requiring an endpoint override for now"));
  }

  @Produces
  S3AsyncClient s3CrtAsyncClient(
      @ConfigProperty(name = "aws.s3.client.type", defaultValue = "legacy") String clientType) {
    LOG.info("Supplying S3 client using endpoint {}", endpointOverride);

    if (clientType == null) {
      LOG.warn("No client type selected. Using default client.");
      return legacyClient();
    }
    
    return switch (clientType) {
      case "crt" -> crtClient();
      case "legacy" -> legacyClient();
      default -> throw new RuntimeException(format("Unknown client type %s", clientType));
    };
  }

  private S3AsyncClient legacyClient() {
    LOG.info("Using Legacy Client");
    return S3AsyncClient.builder()
        .credentialsProvider(() -> AwsBasicCredentials.create("localstack", "localstack"))
        .httpClientBuilder(AwsCrtAsyncHttpClient.builder()
            .connectionTimeout(Duration.ofSeconds(3))
            .maxConcurrency(100))
        .endpointOverride(URI.create(endpointOverride)).build();
  }

  private S3AsyncClient crtClient() {
    LOG.info("Using CRT Client");
    return S3AsyncClient.crtBuilder()
        .credentialsProvider(() -> AwsBasicCredentials.create("localstack", "localstack"))
        .endpointOverride(URI.create(endpointOverride)).build();
  }

}