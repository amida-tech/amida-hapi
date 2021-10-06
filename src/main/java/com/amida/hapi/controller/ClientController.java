package com.amida.hapi.controller;

import com.amida.hapi.domain.ClientRepresentation;
import com.amida.hapi.domain.CredentialRepresentation;
import com.amida.hapi.domain.HapiFhirClient;
import com.amida.hapi.security.SecurityConfig;
import com.amida.hapi.security.TokenUtil;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.client.*;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

@RestController
public class ClientController {
    private final WebTarget keycloakTarget;

    @Autowired
    TokenUtil tokenUtil;

    @Value("${saraswati.keycloak.user.username}")
    private String username;

    @Value("${saraswati.keycloak.user.password}")
    private String password;

    private final ObjectMapper objectMapper;

    public ClientController(@Value("${saraswati.keycloak.internal}") String keycloakBaseUrl) {
        Client client = ClientBuilder.newClient();
        keycloakTarget = client.target(keycloakBaseUrl);
        objectMapper = new ObjectMapper();
    }

    @GetMapping(value = "registerClient/{clientName}")
    public String createClient(@PathVariable String clientName) {
        WebTarget createClient = keycloakTarget.path("/auth/admin/realms/igia/clients");

        String authToken = getAuthToken();

        Invocation.Builder request = createClient.request();
        request.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken);

        Response response = request.post(Entity.entity(new ClientRepresentation(clientName), MediaType.APPLICATION_JSON));

        if (response.getStatus() == 201) {
            return saveClient(clientName, authToken);
        }

        return response.getStatusInfo().getReasonPhrase();
    }

    private String saveClient(String name, String authToken) {
        String json = viewClient(name);

        ObjectMapper mapper = new ObjectMapper();
        try {
            List<ClientRepresentation> myObjects = mapper.readValue(json, new TypeReference<List<ClientRepresentation>>(){});
            String uuid = myObjects.get(0).getId();

            String secretKey = getSecretKey(uuid, authToken);

            ClientRepresentation clientRep = myObjects.get(0);
            HapiFhirClient hapiClient = new HapiFhirClient();
            hapiClient.setClientRep(clientRep);
            hapiClient.setClientSecret(secretKey);

            SecurityConfig.getInMemTokenStore().put(name, hapiClient);

            return clientRep.getClientId() + " client created ";
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException();
        }
    }

    private String getSecretKey(String uuid, String authToken) {
        Invocation.Builder request = keycloakTarget.path("auth/admin/realms/igia/clients/" + uuid + "/client-secret").request(MediaType.APPLICATION_JSON_TYPE);
        request.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken);

        CredentialRepresentation post = request.post(null, CredentialRepresentation.class);

        return post.getValue();
    }

    private String viewClient(@PathVariable String clientName) {
        WebTarget path = keycloakTarget.path("/auth/admin/realms/igia/clients").queryParam("clientId", clientName);

        Invocation.Builder request = path.request(MediaType.APPLICATION_JSON_TYPE);
        request.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + getAuthToken());

        return request.get(String.class);
    }

    private String getAuthToken() {
        Invocation.Builder request = keycloakTarget.path("/auth/realms/master/protocol/openid-connect/token").request(MediaType.APPLICATION_JSON_TYPE);
        Form form = new Form();
        form.param("client_id", "admin-cli");
        form.param("grant_type", "password");
        form.param("username", username);
        form.param("password", password);

        String json = request
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), String.class);

        String accessToken;
        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            accessToken = jsonNode.get("access_token").asText();
        } catch (IOException e) {
            e.printStackTrace();
            return "error";
        }
        return accessToken;
    }
}
