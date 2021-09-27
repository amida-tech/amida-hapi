package com.amida.hapi.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import com.amida.hapi.domain.HapiFhirClient;
import com.amida.hapi.domain.TokenBean;
import com.amida.hapi.security.SecurityConfig;
import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.hl7.fhir.dstu2.model.Patient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.client.*;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
public class LaunchController {

    private final FhirContext ctx;
    private final IGenericClient client;

    private final WebTarget keycloakTarget;

    private String redirect_url = "http://localhost:9080/auth/realms/igia/protocol/openid-connect/auth" +
            "?response_type=code&client_id=igia-fhir-api-example&state=12345&scope=openid&redirect_uri=http://localhost:8080/hello";

    public LaunchController() {
        ctx = FhirContext.forDstu2Hl7Org();

        client = ctx.newRestfulGenericClient("http://hapi-fhir:8080/fhir");

        Client client = ClientBuilder.newClient();
        keycloakTarget = client.target(SecurityConfig.getKeycloakBaseUrl());
    }

    private String getPatient(String patientId) {
        String fullPatientId = "Patient/" + patientId;

        Patient pt = client.read().resource(Patient.class).withId(fullPatientId).execute();

        IParser parser = ctx.newJsonParser();
        return parser.encodeResourceToString(pt);
    }

    private TokenBean getToken(String code, HapiFhirClient hapiFhirClient, String patientId) {
        WebTarget resource = keycloakTarget
                .path("/auth/realms/igia/protocol/openid-connect/token")
                .queryParam("scope", "openid");

        Form form = new Form();
        form.param("grant_type", "authorization_code");
        form.param("client_id", hapiFhirClient.getClientRep().getClientId());
        form.param("client_secret", hapiFhirClient.getClientSecret());
        form.param("code", code);
        form.param("redirect_uri", hapiFhirClient.getClientRep().getRootUrl() +
                "/authorize/" + hapiFhirClient.getClientRep().getClientId()
                + "/" + patientId);

        String json = resource.request(MediaType.APPLICATION_JSON_TYPE)
                                 .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), String.class);

        return new TokenBean(json);
    }

    @GetMapping(value = "/authorize/{clientId}/{patientId}", produces = MediaType.APPLICATION_JSON)
    public Object authorize(@PathVariable String clientId,
                            @PathVariable String patientId,
                            @RequestParam(required = false) String state,
                            @RequestParam(required = false) String session_state,
                            @RequestParam(required = false) String code) {

        HapiFhirClient hapiFhirClient = SecurityConfig.getInMemTokenStore().get(clientId);
        TokenBean accessToken = getToken(code, hapiFhirClient, patientId);

        hapiFhirClient.setToken(accessToken);

        return new ModelAndView("redirect:http://localhost:8080/start/" + clientId + "/" + patientId);
    }

    @GetMapping(value = "/start/{clientId}/{patientId}")
    public Object startApp(@PathVariable String clientId, @PathVariable String patientId, RedirectAttributes redirectAttrs) {
        HapiFhirClient hapiFhirClient = SecurityConfig.getInMemTokenStore().get(clientId);
        if (hapiFhirClient ==  null) {
            return "Client " + clientId + " does not exist.";
        }
        TokenBean token = hapiFhirClient.getToken();
        if (token == null || token.getAccessToken() == null) {
            return createAccessToken(hapiFhirClient, clientId, patientId, redirectAttrs);
        }

        if (!verifyAccessToken(hapiFhirClient)) {
            String json = refreshAccessToken(hapiFhirClient);
            if ("401".equals(json)) {
                return createAccessToken(hapiFhirClient, clientId, patientId, redirectAttrs);
            }
            else {
                hapiFhirClient.setToken(new TokenBean(json));
            }
        }
        return getPatient(patientId);
    }

    private boolean verifyAccessToken(HapiFhirClient hapiFhirClient) {
        WebTarget resource = keycloakTarget
                .path("/auth/realms/igia/protocol/openid-connect/userinfo");
        Invocation.Builder request = resource.request(MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + hapiFhirClient.getToken().getAccessToken());

        Response response = request.get();
        System.out.println(response.getStatus());
        System.out.println(response.getStatusInfo().getReasonPhrase());

        return response.getStatus() == 200;
    }

    private Object createAccessToken(HapiFhirClient hapiFhirClient, String clientId, String patientId, RedirectAttributes redirectAttrs) {
        redirectAttrs.addFlashAttribute("clientId", clientId).addFlashAttribute("patientId", patientId);
        return new ModelAndView("redirect:" +
                createURL(clientId, hapiFhirClient.getClientRep().getRootUrl() +
                        "/authorize/{clientId}/{patientId}"));
    }

    private String refreshAccessToken(HapiFhirClient hapiFhirClient) {
        WebTarget resource = keycloakTarget
                .path("/auth/realms/igia/protocol/openid-connect/token");

        Form form = new Form();
        form.param("grant_type", "refresh_token");
        form.param("client_id", hapiFhirClient.getClientRep().getClientId());
        form.param("client_secret", hapiFhirClient.getClientSecret());
        form.param("refresh_token", hapiFhirClient.getToken().getRefresh_token());

        try {
            return resource.request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), String.class);
        } catch (NotAuthorizedException e) {
            return "401";
        }
    }

    private String createURL(String clientId, String redirect_url) {
        return "http://localhost:9080/auth/realms/igia/protocol/openid-connect/auth" +
                "?response_type=code&client_id=" + clientId + "&state=" + SecurityConfig.createState()
                + "&scope=openid&redirect_uri=" + redirect_url;
    }
}
