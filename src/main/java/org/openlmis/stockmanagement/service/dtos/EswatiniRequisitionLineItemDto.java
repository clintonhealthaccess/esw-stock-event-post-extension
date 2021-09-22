package org.openlmis.stockmanagement.service.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EswatiniRequisitionLineItemDto {
  private Integer averageConsumption;
  private UUID id;
  private EswatiniOrderableDto orderable;

  public Integer getAverageConsumption() {
    return averageConsumption;
  }

  public void setAverageConsumption(Integer averageConsumption) {
    this.averageConsumption = averageConsumption;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public EswatiniOrderableDto getOrderable() {
    return orderable;
  }

  public void setOrderable(EswatiniOrderableDto orderable) {
    this.orderable = orderable;
  }

  @Override
  public String toString() {
    return "EswatiniRequisitionLineitemDto{" +
            "averageConsumption=" + averageConsumption +
            ", id=" + id +
            ", orderable=" + orderable +
            '}';
  }
}