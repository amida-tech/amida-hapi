package com.amida.hapi.controller;

import com.amida.hapi.domain.ClientRepresentation;
import com.amida.hapi.domain.HapiFhirClient;
import com.amida.hapi.security.SecurityConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

@RestController
public class ClientController {
    private final WebTarget keycloakTarget;

    public ClientController() {
        Client client = ClientBuilder.newClient();
        keycloakTarget = client.target("http://keycloak:9080");
    }

    @GetMapping(value = "registerClient/{clientName}")
    public String createClient(@PathVariable String clientName) {
        WebTarget name = keycloakTarget.path("/auth/admin/realms/igia/clients");

        String authToken = getAuthToken();

        Invocation.Builder request = name.request();
        request.header("content-type", "application/json");
        request.header("Authorization", "Bearer " + authToken);

        Response response = request.post(Entity.entity(new ClientRepresentation(clientName), MediaType.APPLICATION_JSON));

        System.out.println("Status = " + response.getStatus());
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

            String secretKeyJson = getSecretKey(uuid, authToken);
            System.out.println(secretKeyJson);

            String secretKey = mapper.readTree(secretKeyJson).get("value").asText();
            ClientRepresentation clientRep = myObjects.get(0);
            HapiFhirClient hapiClient = new HapiFhirClient();
            hapiClient.setClientRep(clientRep);
            hapiClient.setClientSecret(secretKey);

            SecurityConfig.getInMemTokenStore().put(name, hapiClient);
            System.out.println("SECRET KEY = " + secretKey);

            return clientRep.getClientId() + " client created ";
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException();
        }
    }

    private String getSecretKey(String uuid, String authToken) {
        Invocation.Builder request = keycloakTarget.path("auth/admin/realms/igia/clients/" + uuid + "/client-secret").request(MediaType.APPLICATION_JSON_TYPE);
        request.header("content-type", "application/json");
        request.header("Authorization", "Bearer " + authToken);

        request = keycloakTarget.path("auth/admin/realms/igia/clients/" + uuid + "/client-secret").request(MediaType.APPLICATION_JSON_TYPE);
        request.header("content-type", "application/json");
        request.header("Authorization", "Bearer " + authToken);

        return request.get(String.class);
    }

    @GetMapping(value = "viewClients")
    public String viewClients() {
        Invocation.Builder request = keycloakTarget.path("/auth/admin/realms/igia/clients").request(MediaType.APPLICATION_JSON_TYPE);
        request.header("content-type", "application/json");
        request.header("Authorization", "Bearer " + getAuthToken());
        return request.get(String.class);
    }

    @GetMapping(value = "viewClient/{clientName}")
    public String viewClient(@PathVariable String clientName) {
        WebTarget path = keycloakTarget.path("/auth/admin/realms/igia/clients").queryParam("clientId", clientName);

        Invocation.Builder request = path.request(MediaType.APPLICATION_JSON_TYPE);
        request.header("content-type", "application/json");
        request.header("Authorization", "Bearer " + getAuthToken());

        return request.get(String.class);
    }

    private String getAuthToken() {
        return SecurityConfig.getAuthToken(keycloakTarget);
    }
}
