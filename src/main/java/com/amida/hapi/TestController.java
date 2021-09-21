package com.amida.hapi;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IParam;
import com.amida.hapi.domain.HapiFhirClient;
import com.amida.hapi.security.HapiAuthInterceptor;
import com.amida.hapi.security.SecurityConfig;
import com.amida.hapi.domain.TokenBean;
import io.igia.config.fhir.interceptor.ScopeBasedAuthorizationInterceptor;
import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.hl7.fhir.dstu2.model.Bundle;
import org.hl7.fhir.dstu2.model.Patient;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.ws.rs.client.*;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;

@RestController
public class TestController {

    private final FhirContext ctx;
    private final IGenericClient client;

    private final WebTarget keycloakTarget;

    private String redirect_url = "http://localhost:9080/auth/realms/igia/protocol/openid-connect/auth" +
            "?response_type=code&client_id=igia-fhir-api-example&state=12345&scope=openid&redirect_uri=http://localhost:8080/hello";

    public TestController() {
        ctx = FhirContext.forDstu2Hl7Org();

        client = ctx.newRestfulGenericClient("http://hapi-fhir:8080/fhir");
        //client.registerInterceptor(getAuthenticationInterceptor());
        AdditionalRequestHeadersInterceptor headersInterceptor = new AdditionalRequestHeadersInterceptor();
        headersInterceptor.addHeaderValue("state", SecurityConfig.createState());
        client.registerInterceptor(headersInterceptor);
        client.getInterceptorService().registerInterceptor(new HapiAuthInterceptor());

        Client client = ClientBuilder.newClient();
        keycloakTarget = client.target("http://keycloak:9080");
    }

    @GetMapping(value = "/hello2")
    public String helloWorld2() {
        return "Hello World2 " ;
    }

    //@GetMapping(value = "/patient")
    private Patient getPatient(String patientId) {
        String fullPatientId = "Patient/" + patientId;

        Patient pt = client.read().resource(Patient.class).withId(fullPatientId).execute();

        return pt;
    }

    //@GetMapping(value = "/test")
    public TokenBean getToken(String state, String code, HapiFhirClient hapiFhirClient, String patientId) {
        WebTarget resource = keycloakTarget
                .path("/auth/realms/igia/protocol/openid-connect/token")
                .queryParam("scope", "openid")
                .queryParam("state", state);

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
        System.out.println("State = " + state);
        System.out.println("Session State = " + session_state);
        System.out.println("Code = " + code);

        HapiFhirClient hapiFhirClient = SecurityConfig.getInMemTokenStore().get(clientId);
        TokenBean accessToken = getToken(state, code, hapiFhirClient, patientId);
        hapiFhirClient.setToken(accessToken);

        //String sofUrl = createSofUrl(state, code, accessToken);
        return getPatient(patientId);
    }

    @GetMapping(value = "/start/{clientId}/{patientId}")
    public Object startApp(@PathVariable String clientId, @PathVariable int patientId, RedirectAttributes redirectAttrs) {
        HapiFhirClient client = SecurityConfig.getInMemTokenStore().get(clientId);
        TokenBean token = client.getToken();
        if (token == null || token.getAccessToken() == null) {
            redirectAttrs.addFlashAttribute("clientId", clientId).addFlashAttribute("patientId", patientId);
            return new ModelAndView("redirect:" +
                    createURL(clientId, client.getClientRep().getRootUrl() +
                            "/authorize/{clientId}/{patientId}"));
        }
        return new Object();
    }

    private Response.ResponseBuilder createResponse(OutboundJaxrsResponse response1) {
        return Response.fromResponse(response1).status(301).location(getLocation());
    }

    private URI getLocation() {
        try {
            return new URI(redirect_url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new IllegalStateException();
        }
    }

    private String createSofUrl(String state, String code, TokenBean accessToken) {
        String token = createToken(state, code, accessToken);
        String value = "http://localhost:9080/#/patient?token=" +
                        token +
                        "&aud=http://localhost:8080/fhir" +
                        "&access_token=" + accessToken.getAccessToken();
        System.out.println("Full URL = " + value);
        return value;
    }

    private String createToken(String state, String code, TokenBean accessToken) {
        String value = "http://localhost:8080/auth/realms/igia/protocol/smart-openid-connect/smart-launch-context" +
                "?session_code=" + accessToken.getSession_state() +
                "&client_id=smart-launch-context-app" +
                "&tab_id=" + state +
                "&app-token={APP_TOKEN}";
        System.out.println("Token = " + value);
        return value;
    }

    private String createURL(String clientId, String redirect_url) {
        return "http://localhost:9080/auth/realms/igia/protocol/openid-connect/auth" +
                "?response_type=code&client_id=" + clientId + "&state=" + SecurityConfig.createState()
                + "&scope=openid&redirect_uri=" + redirect_url;
    }


}
