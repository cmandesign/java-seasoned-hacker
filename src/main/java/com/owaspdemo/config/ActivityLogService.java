package com.owaspdemo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * Writes structured JSON activity logs to myapp/activity.log.
 * Fields use ECS naming (event.type, user.name, source.ip) for Elasticsearch compatibility.
 */
@Service
public class ActivityLogService {

    private static final Logger log = LoggerFactory.getLogger("activity.log");

    public void log(String eventType, String userName, String sourceIp, String message) {
        MDC.put("event.type", eventType);
        MDC.put("user.name", userName != null ? userName : "unknown");
        MDC.put("source.ip", sourceIp != null ? sourceIp : "unknown");
        try {
            log.info(message);
        } finally {
            MDC.clear();
        }
    }
}
