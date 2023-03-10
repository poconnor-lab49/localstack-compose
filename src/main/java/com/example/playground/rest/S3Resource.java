package com.example.playground.rest;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;

import com.example.playground.service.S3Service;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.model.Tag;

/**
 * Expose some S3 functionality.
 *
 */
@Path("s3/{filename}")
public class S3Resource {

  private static final Logger LOG = LoggerFactory.getLogger(S3Resource.class);

  private final S3Service s3Service;

  public S3Resource(S3Service s3Service) {
    this.s3Service = s3Service;
  }

  /**
   * Fetch a file from S3.
   *
   * @param filename the filename to get
   * @return contents of the file
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String getFile(@RestPath String filename) {
    LOG.info("Fetching {}", filename);

    var result = s3Service.getFile(filename);

    return result.asUtf8String();
  }

  /**
   * Fetch a file from S3.
   *
   * @param filename the filename to get
   * @return contents of the file
   */
  @GET
  @Path("tags")
  @Produces(MediaType.TEXT_PLAIN)
  public List<Tag> getTags(@RestPath String filename) {
    LOG.info("Fetching tags for {}", filename);

    var tags = s3Service.getTags(filename);

    return tags;
  }

  /**
   * Save a file to S3.
   *
   * @param filename the filename to get
   * @return contents of the file
   */
  @PUT
  @Produces(MediaType.TEXT_PLAIN)
  public String putFile(@RestPath String filename) {
    requireNonNull(filename);
    LOG.info("Saving a test file {}", filename);

    var extension = getExtension(filename);

    var etag = s3Service.putFile(filename, extension.orElse("txt"));

    return etag;
  }

  private Optional<String> getExtension(String filename) {
    int dotIndex = filename.lastIndexOf('.');
    if (dotIndex == -1) {
      return empty();
    }
    return Optional.of(filename.substring(dotIndex + 1));
  }

}
