package com.example.playground.config;

import io.smallrye.config.ConfigMapping;
import java.util.Optional;

/**
 * Config for AWS S3.
 *
 */
@ConfigMapping(prefix = "aws.s3")
public interface S3TransferManagerConfig {

  String region();

  String bucket();

  Client client();

  /**
   * S3 Client configuration.
   *
   */
  interface Client {
    Optional<String> endpointOverride();

    Optional<String> type();
  }

}
