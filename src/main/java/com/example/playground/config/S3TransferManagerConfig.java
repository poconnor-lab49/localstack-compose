package com.example.playground.config;

import io.smallrye.config.ConfigMapping;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

/**
 * Config for {@link S3TransferManager}.
 *
 */
@ConfigMapping(prefix = "s3")
public interface S3TransferManagerConfig {

  String region();

  String bucket();

  String endpointOverride();

}
