package com.amida.hapi.domain;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.Date;

public class TokenBean {
    @JsonProperty("access_token")
    private String accessToken;
    private int expires_in;
    private int refresh_expires_in;
    private String refresh_token;
    private String token_type;
    private String id_token;
    private int notBeforePolicy;
    private String session_state;
    private String scope;

    @JsonIgnore
    private Date expirationDate;
    @JsonIgnore
    private Date refreshExpirationDate;

    public TokenBean(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(json);
            accessToken = jsonNode.get("access_token").asText();
            expires_in = jsonNode.get("expires_in").asInt();
            System.out.println("Token expires in " + expires_in);
            refresh_expires_in = jsonNode.get("refresh_expires_in").asInt();
            System.out.println("Refresh expires in " + refresh_expires_in);
            refresh_token = jsonNode.get("refresh_token").asText();
            token_type = jsonNode.get("token_type").asText();
            id_token = jsonNode.get("id_token").asText();
            session_state = jsonNode.get("session_state").asText();
            scope = jsonNode.get("scope").asText();

            expirationDate = new Date(new Date().getTime() + (expires_in * 1000L));
            refreshExpirationDate = new Date(new Date().getTime() + (refresh_expires_in * 1000L));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public int getExpires_in() {
        return expires_in;
    }

    public void setExpires_in(int expires_in) {
        this.expires_in = expires_in;
    }

    public int getRefresh_expires_in() {
        return refresh_expires_in;
    }

    public void setRefresh_expires_in(int refresh_expires_in) {
        this.refresh_expires_in = refresh_expires_in;
    }

    public String getRefresh_token() {
        return refresh_token;
    }

    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    public String getToken_type() {
        return token_type;
    }

    public void setToken_type(String token_type) {
        this.token_type = token_type;
    }

    public String getSession_state() {
        return session_state;
    }

    public void setSession_state(String session_state) {
        this.session_state = session_state;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @JsonProperty("not-before-policy")
    public int getNotBeforePolicy() {
        return notBeforePolicy;
    }

    public void setNotBeforePolicy(int notBeforePolicy) {
        this.notBeforePolicy = notBeforePolicy;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public Date getRefreshExpirationDate() {
        return refreshExpirationDate;
    }

    public String getId_token() {
        return id_token;
    }

    public void setId_token(String id_token) {
        this.id_token = id_token;
    }
}
