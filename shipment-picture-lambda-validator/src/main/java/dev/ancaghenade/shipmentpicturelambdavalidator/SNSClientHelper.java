package dev.ancaghenade.shipmentpicturelambdavalidator;

import software.amazon.awssdk.services.sns.SnsClient;

import java.net.URI;
import java.util.Objects;

public class SNSClientHelper {

  private static final String AWS_ENDPOINT_URL = System.getenv("AWS_ENDPOINT_URL");
  private static String snsTopicArn;

  public static SnsClient getSnsClient() {

    var clientBuilder = SnsClient.builder();

    if (Objects.nonNull(AWS_ENDPOINT_URL)) {
      snsTopicArn = System.getenv("SNS_TOPIC_ARN_DEV");

      return clientBuilder
              .region(Location.REGION.getRegion())
              .endpointOverride(URI.create(AWS_ENDPOINT_URL))
              .build();
    } else {
      snsTopicArn = System.getenv("SNS_TOPIC_ARN_PROD");
      return clientBuilder.build();
    }
  }
  public static String topicARN() {
    return snsTopicArn;
  }

}
