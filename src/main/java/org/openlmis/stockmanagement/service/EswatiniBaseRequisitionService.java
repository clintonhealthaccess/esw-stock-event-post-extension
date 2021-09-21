package org.openlmis.stockmanagement.service;

import org.openlmis.stockmanagement.util.RequestParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.UUID;

import static org.openlmis.stockmanagement.util.RequestHelper.createUri;

public abstract class EswatiniBaseRequisitionService<T> extends EswatiniBaseCommunicationService<T> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${requisition.url}")
    private String requisitionUrl;

    /**
     * Return one object from Reference data service.
     *
     * @param id UUID of requesting object.
     * @return Requesting reference data object.
     */
    public T findOne(UUID id) {
        String url = getServiceUrl() + getUrl() + id;

        try {
            ResponseEntity<T> responseEntity = restTemplate.exchange(
                    buildUri(url), HttpMethod.GET, createEntity(), getResultClass());
            return responseEntity.getBody();
        } catch (HttpStatusCodeException ex) {
            // rest template will handle 404 as an exception, instead of returning null
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                logger.warn("{} with id {} does not exist. ", getResultClass().getSimpleName(), id);
                return null;
            } else {
                throw buildDataRetrievalException(ex);
            }
        }
    }

    <P> P get(Class<P> type, String resourceUrl, RequestParameters parameters) {
        String url = getServiceUrl() + getUrl() + resourceUrl;

        ResponseEntity<P> response = restTemplate.exchange(createUri(url, parameters), HttpMethod.GET,
                createEntity(), type);

        return response.getBody();
    }

    protected String getServiceUrl() {
        return requisitionUrl;
    }

    protected abstract String getUrl();

    protected abstract Class<T> getResultClass();

    protected abstract Class<T[]> getArrayResultClass();
}
