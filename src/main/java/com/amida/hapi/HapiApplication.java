package com.amida.hapi;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import com.amida.hapi.security.HapiAuthInterceptor;
import com.amida.hapi.security.SecurityConfig;
import com.amida.hapi.security.TokenUtil;
import io.igia.config.fhir.interceptor.IgiaExceptionHandlingInterceptor;
import io.igia.config.fhir.interceptor.ScopeBasedAuthorizationInterceptor;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.DefaultUserInfoRestTemplateFactory;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class HapiApplication extends SpringBootServletInitializer {

    @Autowired
    RestfulServer restfulServer;

    @Autowired
    TokenUtil tokenUtil;

    @Value("${saraswati.enableAuth}")
    private boolean enableAuth;

    @Value("${saraswati.keycloak.internal}")
    private String keycloakBaseUrl;

    @Value("${saraswati.url.internal}")
    private String hapiInternalUrl;

    private IParser jsonParser;
    private FhirValidator validator;
    private IGenericClient client;

    public static void main(String[] args) {

        SpringApplication.run(HapiApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        if (enableAuth) {
            HapiAuthInterceptor hapiAuthInterceptor = new HapiAuthInterceptor(
                    new InMemoryTokenStore(), new SecurityConfig().restTemplate(), tokenUtil);
            hapiAuthInterceptor.setKeycloakBaseUrl(keycloakBaseUrl);
            restfulServer.getInterceptorService().registerInterceptor(hapiAuthInterceptor);
        }
        restfulServer.registerInterceptor(new IgiaExceptionHandlingInterceptor());


        FhirContext r4 = FhirContext.forR4();
        jsonParser = r4.newJsonParser();
        validator = r4.newValidator();
        client = r4.newRestfulGenericClient(hapiInternalUrl + "/fhir");

        SecurityConfig.setFhirContext(r4);

        return args -> {
//            FileResource fs =
            File dir = new File("/var/hapi/init");
            if (!dir.exists()) {
                dir = new File(ctx.getClassLoader().getResource("hypertension").getFile());
            }

            if (dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    processFiles(files);
                }
            }
        };
    }

    private void processFiles(File[] files) {
        Arrays.sort(files);
        HashMap<String, String> idMap = new HashMap<>();
        for (File f : files) {
            if (f.getName().endsWith(".json")) {

                System.out.println(f.getName());
                try {
//                            FileReader reader = new FileReader(f);
                    String fileBody = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
                    for (String tempId : idMap.keySet()) {
                        fileBody = fileBody.replaceAll("\"" + tempId + "\"", "\"" + idMap.get(tempId) + "\"");
                    }
                    IBaseResource resource = jsonParser.parseResource(fileBody);
                    ValidationResult validation = validator.validateWithResult(resource);

                    if (validation.isSuccessful()) {
                        createResource(resource, idMap);
                    } else {
                        System.out.println(validation.getMessages());
                    }
                } catch (Exception e) {

                    System.out.println(f.getName());
                    e.printStackTrace();
                }
            }
        }
    }

    private void createResource(IBaseResource resource, HashMap<String, String> idMap) {
        String origId = resource.getIdElement().toString();
        System.out.println(">>" + origId);
        MethodOutcome outcome = client.create()
                .resource(resource)
                .withAdditionalHeader("type", "startup")
                .execute();
        System.out.println(outcome.getCreated());
        String id = outcome.getId().toUnqualifiedVersionless().toString();
        if (!id.equalsIgnoreCase(origId)) {
            System.out.println("adding: " + origId + " " + id);
            idMap.put(origId, id);
        }
        System.out.println(id);
    }
}
