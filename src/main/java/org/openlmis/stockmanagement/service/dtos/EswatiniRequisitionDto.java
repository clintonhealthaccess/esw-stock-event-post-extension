/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.openlmis.stockmanagement.service.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EswatiniRequisitionDto {
  private UUID id;
  private Map<String, EswatiniStatusLogEntryDto> statusChanges;
  private EswatiniIdHolderDto program;
  private EswatiniIdHolderDto facility;
  private List<EswatiniRequisitionLineItemDto> requisitionLineItems;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Map<String, EswatiniStatusLogEntryDto> getStatusChanges() {
    return statusChanges;
  }

  public void setStatusChanges(Map<String, EswatiniStatusLogEntryDto> statusChanges) {
    this.statusChanges = statusChanges;
  }

  public EswatiniIdHolderDto getProgram() {
    return program;
  }

  public void setProgram(EswatiniIdHolderDto program) {
    this.program = program;
  }

  public EswatiniIdHolderDto getFacility() {
    return facility;
  }

  public void setFacility(EswatiniIdHolderDto facility) {
    this.facility = facility;
  }

  public List<EswatiniRequisitionLineItemDto> getRequisitionLineItems() {
    return requisitionLineItems;
  }

  public void setRequisitionLineItems(List<EswatiniRequisitionLineItemDto> requisitionLineItems) {
    this.requisitionLineItems = requisitionLineItems;
  }

  @Override
  public String toString() {
    return "EswatiniRequisitionDto{" +
            "id=" + id +
            ", statusChanges=" + statusChanges +
            ", program=" + program +
            ", facility=" + facility +
            ", requisitionLineItems=" + requisitionLineItems +
            '}';
  }
}
