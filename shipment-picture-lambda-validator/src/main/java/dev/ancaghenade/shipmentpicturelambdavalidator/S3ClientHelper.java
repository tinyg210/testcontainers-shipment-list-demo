package dev.ancaghenade.shipmentpicturelambdavalidator;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import software.amazon.awssdk.services.s3.S3Client;

public class S3ClientHelper {

  private static final String AWS_ENDPOINT_URL = System.getenv("AWS_ENDPOINT_URL");

  public static S3Client getS3Client() throws IOException {

    var clientBuilder = S3Client.builder();
    if (Objects.nonNull(AWS_ENDPOINT_URL)) {
      return clientBuilder
              .region(Location.REGION.getRegion())
              .endpointOverride(URI.create(AWS_ENDPOINT_URL))
              .forcePathStyle(true)
              .build();
    } else {
      return clientBuilder.build();
    }
  }

}
