package dev.ancaghenade.shipmentlistdemo.controller;


import dev.ancaghenade.shipmentlistdemo.entity.Shipment;
import dev.ancaghenade.shipmentlistdemo.service.ShipmentService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/shipment")
@CrossOrigin("http://localhost:3000")
public class ShipmentController {

  private final ShipmentService shipmentService;

  @Autowired
  public ShipmentController(ShipmentService shipmentService) {
    this.shipmentService = shipmentService;
  }

  @GetMapping
  public List<Shipment> getAllShipments() {
    return shipmentService.getAllShipments();
  }

  @GetMapping(
      path = "{shipmentId}/image/download", produces = MediaType.IMAGE_JPEG_VALUE)
  public byte[] downloadShipmentImage(@PathVariable("shipmentId") String shipmentId) {
    return shipmentService.downloadShipmentImage(shipmentId);
  }

  @DeleteMapping("/{shipmentId}")
  public String deleteShipment(@PathVariable("shipmentId") String shipmentId) {
    return shipmentService.deleteShipment(shipmentId);
  }

  @PostMapping(
      path = "{shipmentId}/image/upload",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public void uploadShipmentImage(@PathVariable("shipmentId") String shipmentId,
      @RequestParam("file") MultipartFile file) {
    shipmentService.uploadShipmentImage(shipmentId, file);
  }

  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public void saveUpdateShipment(@RequestBody Shipment shipment) {
    shipmentService.saveShipment(shipment);
  }

}
