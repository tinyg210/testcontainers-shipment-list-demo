package dev.ancaghenade.shipmentlistdemo.integrationtests;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.AttachRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.GetRoleRequest;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.AddPermissionRequest;
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest;
import software.amazon.awssdk.services.lambda.model.Environment;
import software.amazon.awssdk.services.lambda.model.FunctionCode;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.Event;
import software.amazon.awssdk.services.s3.model.LambdaFunctionConfiguration;
import software.amazon.awssdk.services.s3.model.NotificationConfiguration;
import software.amazon.awssdk.services.s3.model.PutBucketNotificationConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

public class LocalStackSetupConfigurations {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalStackSetupConfigurations.class);
  private static Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(LOGGER);
  protected TestRestTemplate restTemplate = new TestRestTemplate();

  protected static final String BUCKET_NAME = "shipment-picture-bucket";
  protected static final String BASE_URL = "http://localhost:8081";

  @Container
  protected static LocalStackContainer localStack =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:2.1.0"))
          .withExposedPorts(4566)
          //  .withEnv("DNS_LOCAL_NAME_PATTERNS", ".*s3.*.amazonaws.com")
          //  .withEnv("DNS_ADDRESS", "1")
          .withEnv("DEBUG", "1");
  private static Region region = Region.of(localStack.getRegion());
  protected static S3Client s3Client;
  protected static DynamoDbClient dynamoDbClient;
  private static LambdaClient lambdaClient;
  private static SqsClient sqsClient;
  private static SnsClient snsClient;
  private static IamClient iamClient;
  private static Logger logger = LoggerFactory.getLogger(ShipmentServiceIntegrationTest.class);

  protected static ObjectMapper objectMapper = new ObjectMapper();


  @DynamicPropertySource
  static void overrideConfigs(DynamicPropertyRegistry registry) {

    registry.add("aws.s3.endpoint",
        () -> localStack.getEndpointOverride(LocalStackContainer.Service.S3));
    registry.add(
        "aws.dynamodb.endpoint", () -> localStack.getEndpointOverride(Service.DYNAMODB));
    registry.add(
        "aws.sqs.endpoint", () -> localStack.getEndpointOverride(Service.SQS));
    registry.add(
        "aws.sns.endpoint", () -> localStack.getEndpointOverride(Service.SNS));
    registry.add("aws.credentials.secret-key", localStack::getSecretKey);
    registry.add("aws.credentials.access-key", localStack::getAccessKey);
    registry.add("aws.region", () -> localStack.getRegion());
    registry.add("shipment-picture-bucket", () -> BUCKET_NAME);
  }

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
    createIAMRole();
    createDynamoDBResources();
    createLambdaResources();
    createBucketNotificationConfiguration();
    createSNS();
    createSQS();
    createSNSSubscription();

    lambdaClient.close();
    snsClient.close();
    sqsClient.close();

  }

  private static void createIAMRole() {
    var roleName = "lambda_exec_role";
    var assumeRolePolicyDocument = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"Service\":\"lambda.amazonaws.com\"},\"Action\":\"sts:AssumeRole\"}]}";

    iamClient.createRole(CreateRoleRequest.builder()
        .roleName(roleName)
        .assumeRolePolicyDocument(assumeRolePolicyDocument)
        .build());

    var policyArn1 = "arn:aws:iam::aws:policy/AmazonS3FullAccess";

    iamClient.attachRolePolicy(
        AttachRolePolicyRequest.builder()
            .roleName(roleName)
            .policyArn(policyArn1)
            .build());

  }

  private static String getQueueUrl(SqsClient sqsClient, String queueName) {
    return sqsClient.getQueueUrl(r -> r.queueName(queueName)).queueUrl();
  }

  private static void createSNSSubscription() {
    String topicArn = snsClient.listTopics().topics().get(0).topicArn();
    // Get the queue URL
    String queueName = "update_shipment_picture_queue";

    var request = GetQueueAttributesRequest.builder()
        .queueUrl(getQueueUrl(sqsClient, queueName))
        .attributeNames(QueueAttributeName.QUEUE_ARN)
        .build();

    var response = sqsClient.getQueueAttributes(request);
    String queueArn = response.attributes().get(QueueAttributeName.QUEUE_ARN);

    var subscribeRequest = SubscribeRequest.builder()
        .topicArn(topicArn)
        .protocol("sqs")
        .endpoint(queueArn)
        .build();

    snsClient.subscribe(subscribeRequest);
  }

  private static void createSQS() {
    var queueName = "update_shipment_picture_queue";

    var request = CreateQueueRequest.builder()
        .queueName(queueName)
        .build();

    sqsClient.createQueue(request);
  }

  private static void createSNS() {
    var topicName = "update_shipment_picture_topic";

    var request = CreateTopicRequest.builder()
        .name(topicName)
        .build();

    snsClient.createTopic(request);
  }

  private static void createBucketNotificationConfiguration()
      throws IOException, InterruptedException {

    var result = localStack.execInContainer(formatCommand(
        "awslocal lambda get-function --function-name shipment-picture-lambda-validator"));
    var obj = new JSONObject(result.getStdout()).getJSONObject("Configuration");
    var state = obj.getString("State");
    while (!state.equals("Active")) {
      result = localStack.execInContainer(formatCommand(
          "awslocal lambda get-function --function-name shipment-picture-lambda-validator"));
      obj = new JSONObject(result.getStdout()).getJSONObject("Configuration");
      state = obj.getString("State");
    }

    var notificationConfiguration = NotificationConfiguration.builder()
        .lambdaFunctionConfigurations(
            LambdaFunctionConfiguration.builder().id("shipment-picture-lambda-validator")
                .lambdaFunctionArn(
                    "arn:aws:lambda:" + region
                        + ":000000000000:function:shipment-picture-lambda-validator")
                .events(Event.S3_OBJECT_CREATED).build()).build();

    // Create the request
    var request = PutBucketNotificationConfigurationRequest.builder()
        .bucket(BUCKET_NAME)
        .notificationConfiguration(notificationConfiguration)
        .build();

    // Call the PutBucketNotificationConfiguration API
    s3Client.putBucketNotificationConfiguration(request);
  }

  private static void createLambdaResources() {
    var functionName = "shipment-picture-lambda-validator";
    var runtime = "java11";
    var handler = "dev.ancaghenade.shipmentpicturelambdavalidator.ServiceHandler::handleRequest";
    var zipFilePath = "shipment-picture-lambda-validator/target/shipment-picture-lambda-validator.jar";
    var sourceArn = "arn:aws:s3:000000000000:" + BUCKET_NAME;
    var statementId = "AllowExecutionFromS3Bucket";
    var action = "lambda:InvokeFunction";
    var principal = "s3.amazonaws.com";

    var getRoleResponse = iamClient.getRole(GetRoleRequest.builder()
        .roleName("lambda_exec_role")
        .build());

    var roleArn = getRoleResponse.role().arn();

    try {
      var zipFileBytes = Files.readAllBytes(Paths.get(zipFilePath));
      var zipFileBuffer = ByteBuffer.wrap(zipFileBytes);

      var createFunctionRequest = CreateFunctionRequest.builder()
          .functionName(functionName)
          .runtime(runtime)
          .handler(handler)
          .code(FunctionCode.builder().zipFile(SdkBytes.fromByteBuffer(zipFileBuffer)).build())
          .role(roleArn)
          .timeout(60)
          .memorySize(512)
          .environment(
              Environment.builder().variables(Collections.singletonMap("BUCKET", BUCKET_NAME))
                  .build())
          .build();

      lambdaClient.createFunction(
          createFunctionRequest);

      var request = AddPermissionRequest.builder()
          .functionName(functionName)
          .statementId(statementId)
          .action(action)
          .principal(principal)
          .sourceArn(sourceArn)
          .sourceAccount("000000000000")
          .build();

      // Call the AddPermission API
      lambdaClient.addPermission(request);

    } catch (Exception e) {
      System.err.println("Error creating Lambda function: " + e.getMessage());
    }
  }

  private static void createDynamoDBResources() {

    // table name
    var tableName = "shipment";

    // attribute definitions
    var attributeDefinition = AttributeDefinition.builder()
        .attributeName("shipmentId")
        .attributeType(ScalarAttributeType.S)
        .build();

    // create key schema
    var keySchemaElement = KeySchemaElement.builder()
        .attributeName("shipmentId")
        .keyType(KeyType.HASH)
        .build();

    // CreateTableRequest with table name, attribute definitions, key schema, and billing mode
    var createTableRequest = CreateTableRequest.builder()
        .tableName(tableName)
        .attributeDefinitions(attributeDefinition)
        .keySchema(keySchemaElement)
        .billingMode(BillingMode.PAY_PER_REQUEST)
        .build();

    // createTable operation to create the table
    dynamoDbClient.createTable(createTableRequest);

    // Create attribute values for the item
    var shipmentId = AttributeValue.builder().s("3317ac4f-1f9b-4bab-a974-4aa9876d5547")
        .build();
    var recipientName = AttributeValue.builder().s("Harry Potter").build();
    // Add other attributes as needed

    // Create a map to hold the item attribute values
    var item = new HashMap<String, AttributeValue>();
    item.put("shipmentId", shipmentId);
    item.put("recipient", AttributeValue.builder()
        .m(Map.of(
            "name", recipientName,
            "address", AttributeValue.builder()
                .m(Map.of(
                    "postalCode", AttributeValue.builder().s("LNDNGB").build(),
                    "street", AttributeValue.builder().s("Privet Drive").build(),
                    "number", AttributeValue.builder().s("4").build(),
                    "city", AttributeValue.builder().s("Little Whinging").build(),
                    "additionalInfo", AttributeValue.builder().s("").build()
                ))
                .build()
        ))
        .build());

    var senderName = AttributeValue.builder().s("Warehouse of Unicorns").build();

    item.put("sender", AttributeValue.builder()
        .m(Map.of(
            "name", senderName,
            "address", AttributeValue.builder()
                .m(Map.of(
                    "postalCode", AttributeValue.builder().s("98653").build(),
                    "street", AttributeValue.builder().s("47th Street").build(),
                    "number", AttributeValue.builder().s("5").build(),
                    "city", AttributeValue.builder().s("Townsville").build(),
                    "additionalInfo", AttributeValue.builder().s("").build()
                ))
                .build()
        ))
        .build());
    item.put("weight", AttributeValue.builder().s("2.3").build());

    // Create a PutItemRequest with the table name and item
    var putItemRequest = PutItemRequest.builder()
        .tableName(tableName)
        .item(item)
        .build();

    // Call the PutItem operation to add the item to the table
    dynamoDbClient.putItem(putItemRequest);
  }

  private static void createS3Bucket() {
    // bucket name
    var bucketName = BUCKET_NAME;
    // CreateBucketRequest with the bucket name
    var createBucketRequest = CreateBucketRequest.builder()
        .bucket(bucketName)
        .build();
    // createBucket operation to create the bucket
    s3Client.createBucket(createBucketRequest);

    var putBucketPolicyRequest = PutBucketPolicyRequest.builder()
        .bucket(bucketName)
        .policy(
            "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Sid\":\"AllowLambdaInvoke\",\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"*\"},\"Action\":\"s3:GetObject\",\"Resource\":\"arn:aws:s3:::"
                + BUCKET_NAME + "/*\"}]}")
        .build();

    s3Client.putBucketPolicy(putBucketPolicyRequest);
  }


  protected static ExecResult executeInContainer(String command) throws Exception {

    final var execResult = localStack.execInContainer(formatCommand(command));
    // assertEquals(0, execResult.getExitCode());

    final var logs = execResult.getStdout() + execResult.getStderr();
    logger.info(logs);
    logger.error(execResult.getExitCode() != 0 ? execResult + " - DOES NOT WORK" : "");
    return execResult;
  }

  private static String[] formatCommand(String command) {
    return command.split(" ");
  }
}
