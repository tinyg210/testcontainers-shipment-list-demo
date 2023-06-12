package dev.ancaghenade.shipmentlistdemo.integrationtests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

public class MessageReceiverIntegrationTest extends LocalStackSetupConfigurations {

  @BeforeAll
  static void setup() throws Exception {
    localStack.followOutput(logConsumer);

    s3Client = S3Client.builder()
        .region(region)
        .endpointOverride(localStack.getEndpointOverride(LocalStackContainer.Service.S3))
        .build();
    dynamoDbClient = DynamoDbClient.builder()
        .region(region)
        .endpointOverride(localStack.getEndpointOverride(Service.DYNAMODB))
        .build();
    lambdaClient = LambdaClient.builder()
        .region(region)
        .endpointOverride(localStack.getEndpointOverride(Service.LAMBDA))
        .build();
    sqsClient = SqsClient.builder()
        .region(region)
        .endpointOverride(localStack.getEndpointOverride(Service.SQS))
        .build();
    snsClient = SnsClient.builder()
        .region(region)
        .endpointOverride(localStack.getEndpointOverride(Service.SNS))
        .build();
    iamClient = IamClient.builder()
        .region(Region.AWS_GLOBAL)
        .endpointOverride(localStack.getEndpointOverride(Service.IAM))
        .build();

    createS3Bucket();
    createDynamoDBResources();
    createIAMRole();
    createLambdaResources();
    createBucketNotificationConfiguration();
    createSNS();
    createSQS();
    createSNSSubscription();

    lambdaClient.close();
    snsClient.close();
    sqsClient.close();
    iamClient.close();

  }
  @Test
  void testSNSSQSMessageReceiver() {
    var imageData = new byte[0];
    try {
      imageData = Files.readAllBytes(Path.of("src/test/java/resources/cat.jpg"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    var resource = new ByteArrayResource(imageData) {
      @Override
      public String getFilename() {
        return "cat.jpg";
      }
    };

    var shipmentId = "3317ac4f-1f9b-4bab-a974-4aa9876d5547";
    // build the URL with the id as a path variable
    var url = "/api/shipment/" + shipmentId + "/image/upload";
    // set the request headers
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    // request body with the file resource and headers
    MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
    requestBody.add("file", resource);
    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(requestBody,
        headers);

    ResponseEntity<String> responseEntity = restTemplate.exchange(BASE_URL + url,
        HttpMethod.POST, requestEntity, String.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    // give the Lambda time to start up and process the image + send the message to SQS
    try {
      Thread.sleep(10000);

    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    String sseUrl = "/push-endpoint";

    ResponseEntity<String> sseEndpointResponse = restTemplate.getForEntity(BASE_URL + sseUrl,
        String.class);
    assertEquals(HttpStatus.OK, sseEndpointResponse.getStatusCode());

    assertTrue(sseEndpointResponse.getBody().contains("3317ac4f-1f9b-4bab-a974-4aa9876d5547"));

  }

}
