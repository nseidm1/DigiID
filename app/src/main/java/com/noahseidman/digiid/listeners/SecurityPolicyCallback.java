package com.noahseidman.digiid.listeners;

public interface SecurityPolicyCallback {
    void proceed();
    void failed();
    String getDescription();
    String getTitle();
}
