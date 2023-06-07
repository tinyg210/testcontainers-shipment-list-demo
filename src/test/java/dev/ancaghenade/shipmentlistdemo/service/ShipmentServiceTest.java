package dev.ancaghenade.shipmentlistdemo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import dev.ancaghenade.shipmentlistdemo.entity.Shipment;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ShipmentServiceTest extends LocalStackSetupConfigurations {


  @Test
  @Order(1)
  void testFileUploadToS3() throws Exception {

    // Prepare the file to upload
    byte[] imageData = new byte[0];
    try {
      imageData = Files.readAllBytes(Path.of("src/test/java/resources/cat.jpg"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    ByteArrayResource resource = new ByteArrayResource(imageData) {
      @Override
      public String getFilename() {
        return "cat.jpg";
      }
    };

    String shipmentId = "3317ac4f-1f9b-4bab-a974-4aa9876d5547";
    // build the URL with the id as a path variable
    String url = "/api/shipment/" + shipmentId + "/image/upload";
    // set the request headers
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    // request body with the file resource and headers
    MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
    requestBody.add("file", resource);
    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(requestBody,
        headers);

    ResponseEntity<String> responseEntity = restTemplate.exchange(BASE_URL + url,
        HttpMethod.POST, requestEntity, String.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    ExecResult execResult = executeInContainer(
        "awslocal s3api list-objects --bucket shipment-picture-bucket --query length(Contents[])");
    assertEquals(String.valueOf(1), execResult.getStdout().trim());
  }

  @Test
  @Order(2)
  void testFileDownloadFromS3() {

    String shipmentId = "3317ac4f-1f9b-4bab-a974-4aa9876d5547";
    // build the URL with the id as a path variable
    String url = "/api/shipment/" + shipmentId + "/image/download";

    ResponseEntity<byte[]> responseEntity = restTemplate.exchange(BASE_URL + url,
        HttpMethod.GET, null, byte[].class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    // object is not empty
    assertNotNull(responseEntity.getBody());
  }

  @Test
  @Order(3)
  void testFileDownloadFromS3FailsOnWrongId() {

    String shipmentId = "3317ac4f-1f9b-4bab-a974-4aa987wrong";
    // build the URL with the id as a path variable
    String url = "/api/shipment/" + shipmentId + "/image/download";
    ResponseEntity<byte[]> responseEntity = restTemplate.exchange(BASE_URL + url,
        HttpMethod.GET, null, byte[].class);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
  }

  @Test
  @Order(4)
  void testGetShipmentFromDynamoDB() throws IOException {

    String url = "/api/shipment";
    // set the request headers
    ResponseEntity<List<Shipment>> responseEntity = restTemplate.exchange(BASE_URL + url,
        HttpMethod.GET, null, new ParameterizedTypeReference<>() {
        });

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertNotNull(responseEntity.getBody());

    if (responseEntity.getStatusCode().is2xxSuccessful()) {
      File json = new File("src/test/java/resources/shipment.json");
      Shipment shipment = objectMapper.readValue(json, Shipment.class);
      List<Shipment> shipmentList = responseEntity.getBody();
      Shipment shipmentWithoutLink = shipmentList.get(0);
      shipmentWithoutLink.setImageLink(null);
      assertEquals(shipment, shipmentWithoutLink);
    }
  }

  @Test
  @Order(5)
  void testAddShipmentToDynamoDB() throws IOException {

    String url = "/api/shipment";
    // set the request headers

    File json = new File("src/test/java/resources/shipmentToUpload.json");
    Shipment shipment = objectMapper.readValue(json, Shipment.class);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.valueOf(MediaType.APPLICATION_JSON_VALUE));

    HttpEntity<Shipment> requestEntity = new HttpEntity<>(shipment,
        headers);

    ResponseEntity<String> responseEntity = restTemplate.exchange(BASE_URL + url,
        HttpMethod.POST, requestEntity, String.class);

    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

  }

  @Test
  @Order(6)
  void testGetTwoShipmentsFromDynamoDB() {

    String url = "/api/shipment";
    // set the request headers
    ResponseEntity<List<Shipment>> responseEntity = restTemplate.exchange(BASE_URL + url,
        HttpMethod.GET, null, new ParameterizedTypeReference<>() {
        });

    if (responseEntity.getStatusCode().is2xxSuccessful()) {
      List<Shipment> shipmentList = responseEntity.getBody();
      assertEquals(2, shipmentList.size());
    }
  }

  @Test
  @Order(7)
  void testDeleteShipmentFromDynamoDB() {

    String url = "/api/shipment";
    String shipmentId = "/3317ac4f-1f9b-4bab-a974-4aa9876d5547";

    // set the request headers
    ResponseEntity<String> deleteResponseEntity = restTemplate.exchange(BASE_URL + url + shipmentId,
        HttpMethod.DELETE, null, String.class);

    assertEquals(HttpStatus.OK, deleteResponseEntity.getStatusCode());
    assertEquals("Shipment has been deleted", deleteResponseEntity.getBody());

    ResponseEntity<List<Shipment>> getResponseEntity = restTemplate.exchange(BASE_URL + url,
        HttpMethod.GET, null, new ParameterizedTypeReference<>() {
        });

    if (getResponseEntity.getStatusCode().is2xxSuccessful()) {
      List<Shipment> shipmentList = getResponseEntity.getBody();
      assertEquals(1, shipmentList.size());
    }
  }

//  @Test
//  @Order(8)
//  void testSQSMessage() {
//
//    String url = "/push-endpoint";
//    // set the request headers
//    ResponseEntity<String> responseEntity = restTemplate.exchange(BASE_URL + url,
//        HttpMethod.GET, null, String.class);
//    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
//
//  }


}
