/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation either
 * version 3 of the License or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 */

package org.openlmis.stockmanagement.service;

import org.openlmis.stockmanagement.service.dtos.EswatiniRequisitionDto;
import org.openlmis.stockmanagement.service.dtos.EswatiniStatusLogEntryDto;
import org.openlmis.stockmanagement.util.RequestParameters;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EswatiniRequisitionService extends EswatiniBaseRequisitionService<EswatiniRequisitionDto> {

    @Override
    protected String getUrl() {
        return "/api/requisitions/";
    }

    @Override
    protected Class<EswatiniRequisitionDto> getResultClass() {
        return EswatiniRequisitionDto.class;
    }

    @Override
    protected Class<EswatiniRequisitionDto[]> getArrayResultClass() {
        return EswatiniRequisitionDto[].class;
    }

    public List<EswatiniRequisitionDto> searchAndFilter(RequestParameters parameters) {
        List<EswatiniRequisitionDto> requisitions = getPage("search", parameters, null, HttpMethod.GET, getResultClass()).getContent();
        return filter(requisitions);
    }

    List<EswatiniRequisitionDto> filter(List<EswatiniRequisitionDto> requisitions) {
        return requisitions.stream().filter(r -> {
            Map<String, EswatiniStatusLogEntryDto> statusChanges = r.getStatusChanges();
            return statusChanges.containsKey("SUBMITTED");
        }).collect(Collectors.toList());
    }
}
