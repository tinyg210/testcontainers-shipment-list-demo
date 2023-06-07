package dev.ancaghenade.shipmentlistdemo.service;

import dev.ancaghenade.shipmentlistdemo.buckets.BucketName;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
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
import software.amazon.awssdk.services.iam.model.CreatePolicyRequest;
import software.amazon.awssdk.services.iam.model.CreatePolicyResponse;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.GetRoleRequest;
import software.amazon.awssdk.services.iam.model.GetRoleResponse;
import software.amazon.awssdk.services.iam.model.IamException;
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
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

public class LocalStackSetupConfigurations {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalStackSetupConfigurations.class);
  private static Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(LOGGER);
  protected TestRestTemplate restTemplate = new TestRestTemplate();

  protected static final String BASE_URL = "http://localhost:8081";
  @Container
  protected static LocalStackContainer localStack =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:2.1.0"))
          // .withEnv("DNS_LOCAL_NAME_PATTERNS", ".*s3.*.amazonaws.com")
          .withEnv("DNS_ADDRESS", "0")
          .withEnv("DEBUG", "1");
  private static Region region = Region.of(localStack.getRegion());
  private static S3Client s3Client;
  private static DynamoDbClient dynamoDbClient;
  private static LambdaClient lambdaClient;
  private static SqsClient sqsClient;
  private static SnsClient snsClient;
  private static IamClient iamClient;
  private static Logger logger = LoggerFactory.getLogger(ShipmentServiceTest.class);

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

    S3Client.builder();

    Properties props = new Properties();
    FileInputStream fis = new FileInputStream("src/test/java/resources/commands.properties");
    props.load(fis);
    fis.close();

    createS3Bucket();
    createIAMRole();
    createDynamoDBResources();
    createLambdaResources();
    createBucketNotificationConfiguration();
    createSNS();
    createSQS();
    createSNSSubscription();

    s3Client.close();
    dynamoDbClient.close();
    lambdaClient.close();
    snsClient.close();
    sqsClient.close();

  }

  private static void createIAMRole() {
    String roleName = "lambda_exec_role";
    String assumeRolePolicyDocument = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"Service\":\"lambda.amazonaws.com\"},\"Action\":\"sts:AssumeRole\"}]}";

    iamClient.createRole(CreateRoleRequest.builder()
        .roleName(roleName)
        .assumeRolePolicyDocument(assumeRolePolicyDocument)
        .build());

    String policyArn1 = "arn:aws:iam::aws:policy/AmazonS3FullAccess";
    String policyArn2 = "arn:aws:iam::aws:policy/AWSLambda_FullAccess";

    iamClient.attachRolePolicy(
        AttachRolePolicyRequest.builder()
            .roleName(roleName)
            .policyArn(policyArn1)
            .build());

    iamClient.attachRolePolicy(
        AttachRolePolicyRequest.builder()
            .roleName(roleName)
            .policyArn(policyArn2)
            .build());

    String policyDocument = "{" +
        "    \"Version\": \"2012-10-17\"," +
        "    \"Statement\": [" +
        "        {" +
        "            \"Effect\": \"Allow\"," +
        "            \"Action\": [" +
        "                \"logs:CreateLogGroup\"," +
        "                \"logs:CreateLogStream\"," +
        "                \"logs:PutLogEvents\"" +
        "            ]," +
        "            \"Resource\": \"arn:aws:logs:*:*:*\"" +
        "        }," +
        "        {" +
        "            \"Effect\": \"Allow\"," +
        "            \"Action\": [" +
        "                \"s3:GetObject\"," +
        "                \"s3:PutObject\"," +
        "                \"sns:Publish\"" +
        "            ]," +
        "            \"Resource\": [" +
        "                \"arn:aws:s3:::shipment-picture-bucket\"," +
        "                \"arn:aws:s3:::shipment-picture-bucket/*\"" +
        "            ]" +
        "        }" +
        "    ]" +
        "}";

    // Create the IAM role policy
    try {
      CreatePolicyResponse createPolicyResponse = iamClient.createPolicy(
          CreatePolicyRequest.builder()
              .policyName("lambda_exec_policy")
              .policyDocument(policyDocument)
              .build());

      String policyArn = createPolicyResponse.policy().arn();

      // Attach the policy to the IAM role
      iamClient.attachRolePolicy(AttachRolePolicyRequest.builder()
          .roleName(roleName)
          .policyArn(policyArn)
          .build());

    } catch (IamException e) {
      System.err.println(e.awsErrorDetails().errorMessage());
      System.exit(1);
    }
  }

  private static String getQueueUrl(SqsClient sqsClient, String queueName) {
    return sqsClient.getQueueUrl(r -> r.queueName(queueName)).queueUrl();
  }

  private static void createSNSSubscription() {
    String topicArn = snsClient.listTopics().topics().get(0).topicArn();
    // Get the queue URL
    String queueName = "update_shipment_picture_queue";

    GetQueueAttributesRequest request = GetQueueAttributesRequest.builder()
        .queueUrl(getQueueUrl(sqsClient, queueName))
        .attributeNames(QueueAttributeName.QUEUE_ARN)
        .build();

    GetQueueAttributesResponse response = sqsClient.getQueueAttributes(request);
    String queueArn = response.attributes().get(QueueAttributeName.QUEUE_ARN);

    SubscribeRequest subscribeRequest = SubscribeRequest.builder()
        .topicArn(topicArn)
        .protocol("sqs")
        .endpoint(queueArn)
        .build();

    snsClient.subscribe(subscribeRequest);
  }

  private static void createSQS() {
    String queueName = "update_shipment_picture_queue";

    CreateQueueRequest request = CreateQueueRequest.builder()
        .queueName(queueName)
        .build();

    sqsClient.createQueue(request);
  }

  private static void createSNS() {
    String topicName = "update_shipment_picture_topic";

    CreateTopicRequest request = CreateTopicRequest.builder()
        .name(topicName)
        .build();

    snsClient.createTopic(request);
  }

  private static void createBucketNotificationConfiguration()
      throws IOException, InterruptedException {

    ExecResult result = localStack.execInContainer(formatCommand(
        "awslocal lambda get-function --function-name shipment-picture-lambda-validator"));
    JSONObject obj = new JSONObject(result.getStdout()).getJSONObject("Configuration");
    String state = obj.getString("State");
    while (!state.equals("Active")) {
      result = localStack.execInContainer(formatCommand(
          "awslocal lambda get-function --function-name shipment-picture-lambda-validator"));
      obj = new JSONObject(result.getStdout()).getJSONObject("Configuration");
      state = obj.getString("State");
    }

    NotificationConfiguration notificationConfiguration = NotificationConfiguration.builder()
        .lambdaFunctionConfigurations(
            LambdaFunctionConfiguration.builder().id("shipment-picture-lambda-validator")
                .lambdaFunctionArn(
                    "arn:aws:lambda:" + region
                        + ":000000000000:function:shipment-picture-lambda-validator")
                .events(Event.S3_OBJECT_CREATED).build()).build();

    // Create the request
    PutBucketNotificationConfigurationRequest request = PutBucketNotificationConfigurationRequest.builder()
        .bucket(BucketName.SHIPMENT_PICTURE.getBucketName())
        .notificationConfiguration(notificationConfiguration)
        .build();

    // Call the PutBucketNotificationConfiguration API
    s3Client.putBucketNotificationConfiguration(request);
  }

  private static void createLambdaResources() {
    String functionName = "shipment-picture-lambda-validator";
    String runtime = "java11";
    String handler = "dev.ancaghenade.shipmentpicturelambdavalidator.ServiceHandler::handleRequest";
    String zipFilePath = "shipment-picture-lambda-validator/target/shipment-picture-lambda-validator.jar";
    String sourceArn = "arn:aws:s3:000000000000:shipment-picture-bucket";
    String statementId = "AllowExecutionFromS3Bucket";
    String action = "lambda:InvokeFunction";
    String principal = "s3.amazonaws.com";

    GetRoleResponse getRoleResponse = iamClient.getRole(GetRoleRequest.builder()
        .roleName("lambda_exec_role")
        .build());

    String roleArn = getRoleResponse.role().arn();

    try {
      byte[] zipFileBytes = Files.readAllBytes(Paths.get(zipFilePath));
      ByteBuffer zipFileBuffer = ByteBuffer.wrap(zipFileBytes);

      var env = new HashMap<String, String>();
      env.put("ENVIRONMENT", "dev");
      env.put("s3.endpoint", String.valueOf(localStack.getEndpointOverride(Service.S3)));

      CreateFunctionRequest createFunctionRequest = CreateFunctionRequest.builder()
          .functionName(functionName)
          .runtime(runtime)
          .handler(handler)
          .code(FunctionCode.builder().zipFile(SdkBytes.fromByteBuffer(zipFileBuffer)).build())
          .role(roleArn)
          .timeout(60)
          .memorySize(512)
          .environment(Environment.builder().variables(env).build())
          .build();

      lambdaClient.createFunction(
          createFunctionRequest);

      AddPermissionRequest request = AddPermissionRequest.builder()
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
    String tableName = "shipment";

    // attribute definitions
    AttributeDefinition attributeDefinition = AttributeDefinition.builder()
        .attributeName("shipmentId")
        .attributeType(ScalarAttributeType.S)
        .build();

    // create key schema
    KeySchemaElement keySchemaElement = KeySchemaElement.builder()
        .attributeName("shipmentId")
        .keyType(KeyType.HASH)
        .build();

    // CreateTableRequest with table name, attribute definitions, key schema, and billing mode
    CreateTableRequest createTableRequest = CreateTableRequest.builder()
        .tableName(tableName)
        .attributeDefinitions(attributeDefinition)
        .keySchema(keySchemaElement)
        .billingMode(BillingMode.PAY_PER_REQUEST)
        .build();

    // createTable operation to create the table
    dynamoDbClient.createTable(createTableRequest);

    // Create attribute values for the item
    AttributeValue shipmentId = AttributeValue.builder().s("3317ac4f-1f9b-4bab-a974-4aa9876d5547")
        .build();
    AttributeValue recipientName = AttributeValue.builder().s("Harry Potter").build();
    // Add other attributes as needed

    // Create a map to hold the item attribute values
    Map<String, AttributeValue> item = new HashMap<>();
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

    AttributeValue senderName = AttributeValue.builder().s("Warehouse of Unicorns").build();

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
    PutItemRequest putItemRequest = PutItemRequest.builder()
        .tableName(tableName)
        .item(item)
        .build();

    // Call the PutItem operation to add the item to the table
    dynamoDbClient.putItem(putItemRequest);
  }

  private static void createS3Bucket() {
    // bucket name
    String bucketName = BucketName.SHIPMENT_PICTURE.getBucketName();
    // CreateBucketRequest with the bucket name
    CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
        .bucket(bucketName)
        .build();
    // createBucket operation to create the bucket
    s3Client.createBucket(createBucketRequest);

    PutBucketPolicyRequest putBucketPolicyRequest = PutBucketPolicyRequest.builder()
        .bucket(bucketName)
        .policy(
            "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Sid\":\"AllowLambdaInvoke\",\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"*\"},\"Action\":\"s3:GetObject\",\"Resource\":\"arn:aws:s3:::"
                + BucketName.SHIPMENT_PICTURE.getBucketName() + "/*\"}]}")
        .build();

    s3Client.putBucketPolicy(putBucketPolicyRequest);
  }


  protected static ExecResult executeInContainer(String command) throws Exception {

    final ExecResult execResult = localStack.execInContainer(formatCommand(command));
    // assertEquals(0, execResult.getExitCode());

    final String logs = execResult.getStdout() + execResult.getStderr();
    logger.info(logs);
    logger.error(execResult.getExitCode() != 0 ? execResult + " - DOES NOT WORK" : "");
    return execResult;
  }

  private static String[] formatCommand(String command) {
    return command.split(" ");
  }
}
