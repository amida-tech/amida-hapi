package com.amida.hapi.domain;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientRepresentation {
    @JsonProperty
    private String clientId;
    @JsonProperty
    private String rootUrl;
    @JsonProperty
    private String[] redirectUris;
    @JsonProperty
    private String[] webOrigins;
    @JsonProperty
    private String id;

    /*@JsonIgnore
    private String secretKey;
    @JsonIgnore
    private TokenBean token;*/

    public ClientRepresentation() {

    }

    public ClientRepresentation(String name) {
        this.clientId = name;
        rootUrl = "http://localhost:8080";
        redirectUris = new String[1];
        redirectUris[0] = "http://localhost:8080/*";
        webOrigins = new String[1];
        webOrigins[0] = "http://hapi-fhir:8080/*";
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRootUrl() {
        return rootUrl;
    }

    public void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl;
    }

    public String[] getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(String[] redirectUris) {
        this.redirectUris = redirectUris;
    }

    public String[] getWebOrigins() {
        return webOrigins;
    }

    public void setWebOrigins(String[] webOrigins) {
        this.webOrigins = webOrigins;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /*public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public TokenBean getToken() {
        return token;
    }

    public void setToken(TokenBean token) {
        this.token = token;
    }*/
}
