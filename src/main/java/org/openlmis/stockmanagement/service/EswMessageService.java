package org.openlmis.stockmanagement.service;

import org.openlmis.stockmanagement.i18n.ExposedMessageSourceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class EswMessageService {

    @Autowired
    private ExposedMessageSourceImpl exposedMessageSourceImpl;

    @PostConstruct
    public void postConstruct() {
        exposedMessageSourceImpl.addBasenames("classpath:messages/esw-stock-event-post-extension/messages");
    }

    public String getMessage(String key) {
        return exposedMessageSourceImpl.getMessage(key, null, LocaleContextHolder.getLocale());
    }
}
