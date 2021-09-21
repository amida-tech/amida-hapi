package com.amida.hapi.domain;

public class HapiFhirClient {
    private ClientRepresentation clientRep;
    private TokenBean token;
    private String clientSecret;

    public ClientRepresentation getClientRep() {
        return clientRep;
    }

    public void setClientRep(ClientRepresentation clientRep) {
        this.clientRep = clientRep;
    }

    public TokenBean getToken() {
        return token;
    }

    public void setToken(TokenBean token) {
        this.token = token;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
