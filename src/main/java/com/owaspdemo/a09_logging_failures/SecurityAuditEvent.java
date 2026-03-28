package com.owaspdemo.a09_logging_failures;

import org.springframework.context.ApplicationEvent;

public class SecurityAuditEvent extends ApplicationEvent {

    public enum Action { LOGIN_SUCCESS, LOGIN_FAILURE, SUSPICIOUS_ACTIVITY }

    private final Action action;
    private final String username;
    private final String sourceIp;
    private final String details;

    public SecurityAuditEvent(Object source, Action action, String username, String sourceIp, String details) {
        super(source);
        this.action = action;
        this.username = username;
        this.sourceIp = sourceIp;
        this.details = details;
    }

    public Action getAction() { return action; }
    public String getUsername() { return username; }
    public String getSourceIp() { return sourceIp; }
    public String getDetails() { return details; }
}
