package dev.ancaghenade.shipmentpicturelambdavalidator;

import java.io.IOException;
import java.net.URI;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class S3ClientHelper {

  private static final String ENVIRONMENT = System.getenv("ENVIRONMENT");
  private static final String s3Endpoint = System.getenv("s3.endpoint");

  private static PropertiesProvider properties = new PropertiesProvider();

  public static S3Client getS3Client() throws IOException {

    var clientBuilder = S3Client.builder();
    if (properties.getProperty("environment.dev").equals(ENVIRONMENT)) {
      System.out.println("Using dev environment.");
      return clientBuilder
          .region(Region.of("us-east-1"))
          .endpointOverride(URI.create(s3Endpoint))
          .forcePathStyle(true)
          .build();
    } else {
      return clientBuilder.build();
    }
  }

}
