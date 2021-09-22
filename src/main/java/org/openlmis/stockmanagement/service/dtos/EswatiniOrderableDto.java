package org.openlmis.stockmanagement.service.dtos;

import java.util.UUID;

public class EswatiniOrderableDto {
  private UUID id;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return "EswatiniOrderableDto{" +
            "id=" + id +
            '}';
  }
}