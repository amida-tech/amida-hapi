package com.amida.hapi;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class HapiApplication extends SpringBootServletInitializer {

    private FhirContext dstu2 = FhirContext.forDstu2Hl7Org();
    private IParser jsonParser = dstu2.newJsonParser();
    private FhirValidator validator = dstu2.newValidator();
    private String serverBase = "http://hapi-fhir:9081/fhir";
    private IGenericClient client = dstu2.newRestfulGenericClient(serverBase);

    public static void main(String[] args) {

        SpringApplication.run(HapiApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
//            FileResource fs =
            File dir = new File("/var/hapi/init");
            if (!dir.exists()) {
                dir = new File(ctx.getClassLoader().getResource("samples").getFile());
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
                    String fileBody = FileUtils.readFileToString(f);
                    for (String tempId : idMap.keySet()) {
                        fileBody = fileBody.replaceAll("\"" + tempId + "\"", "\"" + idMap.get(tempId) + "\"");
                    }
                    IBaseResource resource = jsonParser.parseResource(fileBody);
                    ValidationResult validation = validator.validateWithResult(resource);

                    if (validation.isSuccessful()) {
                        executeResource(resource, idMap);
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

    private void executeResource(IBaseResource resource, HashMap<String, String> idMap) {
        String origId = resource.getIdElement().toString();
        System.out.println(">>" + origId);
        MethodOutcome outcome = client.create()
                .resource(resource)
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
