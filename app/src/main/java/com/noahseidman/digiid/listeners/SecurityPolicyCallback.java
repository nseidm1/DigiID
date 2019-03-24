package com.noahseidman.digiid.listeners;

public interface SecurityPolicyCallback {
    void proceed();
    String getDescription();
    String getTitle();
}
