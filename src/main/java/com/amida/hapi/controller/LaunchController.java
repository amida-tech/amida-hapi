package com.amida.hapi.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.amida.hapi.domain.HapiFhirClient;
import com.amida.hapi.domain.TokenBean;
import com.amida.hapi.security.SecurityConfig;
import com.amida.hapi.security.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.client.*;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;

@RestController
@Component
public class LaunchController {

    @Autowired
    TokenUtil tokenUtil;

    @Value("${saraswati.keycloak.external}")
    private String keycloakExternalUrl;

    @Value("${saraswati.url.external}")
    private String hapiExternalUrl;

    @Value("${saraswati.url.internal}")
    private String hapiInternalUrl;

    @Value("${hspc.platform.authorization.authorizeUrlPath}")
    private String authUrl;

    @Value("${hspc.platform.authorization.tokenUrlPath}")
    private String tokenUrl;

    private final WebTarget keycloakTarget;

    public LaunchController(@Value("${saraswati.keycloak.internal}") String keycloakBaseUrl) {
        Client client = ClientBuilder.newClient();
        keycloakTarget = client.target(keycloakBaseUrl);
    }

    private String getPatient(String clientId, String patientId) {
        String fullPatientId = "Patient/" + patientId;

        FhirContext ctx = SecurityConfig.getFhirContext();
        IGenericClient client = ctx.newRestfulGenericClient(hapiInternalUrl + "/fhir");
        Patient pt = client.read()
                .resource(Patient.class)
                .withId(fullPatientId)
                .withAdditionalHeader("client_id", clientId)
                .execute();

        IParser parser = ctx.newJsonParser().setPrettyPrint(true);
        return parser.encodeResourceToString(pt);
    }

    private TokenBean getToken(String code, HapiFhirClient hapiFhirClient, String patientId) {
        WebTarget resource = keycloakTarget
                .path(tokenUrl)
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

        return new ModelAndView("redirect:" + hapiExternalUrl + "/start/" + clientId + "/" + patientId);
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
        return getPatient(clientId, patientId);
    }

    private boolean verifyAccessToken(HapiFhirClient hapiFhirClient) {
        return tokenUtil.verifyToken(keycloakTarget, hapiFhirClient);
    }

    private Object createAccessToken(HapiFhirClient hapiFhirClient, String clientId, String patientId, RedirectAttributes redirectAttrs) {
        redirectAttrs.addFlashAttribute("clientId", clientId).addFlashAttribute("patientId", patientId);
        return new ModelAndView("redirect:" +
                createURL(clientId, hapiFhirClient.getClientRep().getRootUrl() +
                        "/authorize/{clientId}/{patientId}"));
    }

    private String refreshAccessToken(HapiFhirClient hapiFhirClient) {
        WebTarget resource = keycloakTarget.path(tokenUrl);

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
        return keycloakExternalUrl + authUrl +
                "?response_type=code&client_id=" + clientId + "&state=" + SecurityConfig.createState()
                + "&scope=openid&redirect_uri=" + redirect_url;
    }
}
