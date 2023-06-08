package dev.ancaghenade.shipmentpicturelambdavalidator;

import lombok.Getter;

@Getter
public enum Location {


  REGION(software.amazon.awssdk.regions.Region.US_EAST_1);

  private final software.amazon.awssdk.regions.Region region;
  Location(software.amazon.awssdk.regions.Region region) {
    this.region = region;
  }
}
