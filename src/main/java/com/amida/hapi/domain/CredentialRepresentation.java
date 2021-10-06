package com.amida.hapi.domain;

public class CredentialRepresentation {
    private long createdDate;
    private String credentialData;
    private String id;
    private int priority;
    private String secretData;
    private boolean temporary;
    private String type;
    private String userLabel;
    private String value;

    public long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(long createdDate) {
        this.createdDate = createdDate;
    }

    public String getCredentialData() {
        return credentialData;
    }

    public void setCredentialData(String credentialData) {
        this.credentialData = credentialData;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getSecretData() {
        return secretData;
    }

    public void setSecretData(String secretData) {
        this.secretData = secretData;
    }

    public boolean isTemporary() {
        return temporary;
    }

    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUserLabel() {
        return userLabel;
    }

    public void setUserLabel(String userLabel) {
        this.userLabel = userLabel;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "CredentialRepresentation{" +
                "createdDate=" + createdDate +
                ", credentialData='" + credentialData + '\'' +
                ", id='" + id + '\'' +
                ", priority=" + priority +
                ", secretData='" + secretData + '\'' +
                ", temporary=" + temporary +
                ", type='" + type + '\'' +
                ", userLabel='" + userLabel + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
