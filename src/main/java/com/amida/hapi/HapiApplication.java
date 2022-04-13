package com.amida.hapi;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import org.apache.catalina.webresources.FileResource;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.Field;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ResourceCondition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;

@SpringBootApplication
public class HapiApplication {

    private FhirContext r4 = FhirContext.forR4();
    private IParser jsonParser = r4.newJsonParser();
    private FhirValidator validator = r4.newValidator();
    private String serverBase = "http://localhost:8080/fhir";
    private IGenericClient client = r4.newRestfulGenericClient(serverBase);

    public static void main(String[] args) {

        SpringApplication.run(HapiApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            File dir = new File("/var/hapi/init");
            if (!dir.exists())
                dir = new File(ctx.getClassLoader().getResource("hypertension").getFile());

            if (dir.isDirectory()){
                File[] files = dir.listFiles();
                Arrays.sort(files);
                HashMap<String, String> idMap = new HashMap<String, String>();
                for(File f: files){
                    if (f.getName().endsWith(".json")){

                        System.out.println(f.getName());
                        try{
                            String fileBody = FileUtils.readFileToString(f, StandardCharsets.UTF_8);
                            for (String tempId: idMap.keySet()){
                                fileBody = fileBody.replaceAll("\""+tempId+"\"", "\""+idMap.get(tempId)+"\"");
                            }
                            IBaseResource resource = jsonParser.parseResource(fileBody);
                            ValidationResult validation = validator.validateWithResult(resource);

                            if (validation.isSuccessful()){
                                String origId = resource.getIdElement().toString();
                                MethodOutcome outcome = client.create().resource(resource).execute();
                                System.out.println(outcome.getCreated());
                                String id = outcome.getId().toUnqualifiedVersionless().toString();
                                if (!id.equalsIgnoreCase(origId)){
                                    idMap.put(origId, id);
                                }

                            }else{
                                System.out.println(validation.getMessages());
                            }
                        }catch (Exception e){

                            System.out.println(f.getName());
                            e.printStackTrace();
                        }


                    }
                }
            }

        };
    }


}
