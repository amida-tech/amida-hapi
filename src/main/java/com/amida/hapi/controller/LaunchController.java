package com.amida.hapi.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.amida.hapi.domain.HapiFhirClient;
import com.amida.hapi.domain.TokenBean;
import com.amida.hapi.security.SecurityConfig;
import com.amida.hapi.security.TokenUtil;

import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
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

    private String getPatient(String resourceType, String accessToken, String patientId) {
        String fullPatientId = resourceType + "/" + patientId;

        FhirContext ctx = SecurityConfig.getFhirContext();
        IGenericClient client = ctx.newRestfulGenericClient(hapiInternalUrl + "/fhir");
        switch (resourceType) {
            case "Patient":
                Patient pat = client.read()
                        .resource(Patient.class)
                        .withId(fullPatientId)
                        .withAdditionalHeader("Authorization", "Bearer " + accessToken)
                        .execute();
                return ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(pat);
            case "Organization":
                Organization org = client.read()
                        .resource(Organization.class)
                        .withId(fullPatientId)
                        .withAdditionalHeader("Authorization", "Bearer " + accessToken)
                        .execute();
                return ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(org);
            case "Observation":
                Observation obs = client.read()
                        .resource(Observation.class)
                        .withId(fullPatientId)
                        .withAdditionalHeader("Authorization", "Bearer " + accessToken)
                        .execute();
                return ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(obs);
            case "Practitioner":
                Practitioner pra = client.read()
                        .resource(Practitioner.class)
                        .withId(fullPatientId)
                        .withAdditionalHeader("Authorization", "Bearer " + accessToken)
                        .execute();
                return ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(pra);
            case "Condition":
                Condition con = client.read()
                        .resource(Condition.class)
                        .withId(fullPatientId)
                        .withAdditionalHeader("Authorization", "Bearer " + accessToken)
                        .execute();
                return ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(con);
            case "DiagnosticReport":
                DiagnosticReport dia = client.read()
                        .resource(DiagnosticReport.class)
                        .withId(fullPatientId)
                        .withAdditionalHeader("Authorization", "Bearer " + accessToken)
                        .execute();
                return ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(dia);
            case "Medication":
                Medication medi = client.read()
                        .resource(Medication.class)
                        .withId(fullPatientId)
                        .withAdditionalHeader("Authorization", "Bearer " + accessToken)
                        .execute();
                return ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(medi);
            case "MedicationRequest":
                MedicationRequest med = client.read()
                        .resource(MedicationRequest.class)
                        .withId(fullPatientId)
                        .withAdditionalHeader("Authorization", "Bearer " + accessToken)
                        .execute();
                return ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(med);

        }

        return "Not a valid resourceType";
    }

    private TokenBean getToken(String code, HapiFhirClient hapiFhirClient, String resourceType, String patientId) {
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
                + "/" + resourceType + "/" + patientId);
        String json = resource.request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), String.class);

        return new TokenBean(json);
    }

    @GetMapping(value = "/authorize/{clientId}/{resourceType}/{patientId}", produces = MediaType.APPLICATION_JSON)
    public Object authorize(@PathVariable String clientId, @PathVariable String resourceType,
            @PathVariable String patientId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String session_state,
            @RequestParam(required = false) String code) {

        HapiFhirClient hapiFhirClient = SecurityConfig.getInMemTokenStore().get(clientId);
        TokenBean accessToken = getToken(code, hapiFhirClient, resourceType, patientId);

        hapiFhirClient.setToken(accessToken);
        return new ModelAndView("redirect:" + hapiExternalUrl + "/start/" + clientId + "/" + resourceType + "/" + patientId);
    }

    @GetMapping(value = "/start/{clientId}/{resourceType}/{patientId}")
    public Object startApp(@PathVariable String clientId, @PathVariable String resourceType,
            @PathVariable String patientId, RedirectAttributes redirectAttrs) {
        HapiFhirClient hapiFhirClient = SecurityConfig.getInMemTokenStore().get(clientId);
        if (hapiFhirClient == null) {
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
            } else {
                hapiFhirClient.setToken(new TokenBean(json));
            }
        }

        return getPatient(resourceType, hapiFhirClient.getToken().getAccessToken(), patientId);
    }

    private boolean verifyAccessToken(HapiFhirClient hapiFhirClient) {
        return tokenUtil.verifyToken(keycloakTarget, hapiFhirClient);
    }

    private Object createAccessToken(HapiFhirClient hapiFhirClient, String clientId, String patientId,
            RedirectAttributes redirectAttrs) {
        redirectAttrs.addFlashAttribute("clientId", clientId).addFlashAttribute("patientId", patientId);
        return new ModelAndView("redirect:" +
                createURL(clientId, hapiFhirClient.getClientRep().getRootUrl() +
                        "/authorize/{clientId}/{resourceType}/{patientId}"));

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
