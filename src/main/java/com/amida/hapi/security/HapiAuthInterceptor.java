package com.amida.hapi.security;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.interceptor.executor.InterceptorService;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.apache.http.HttpResponse;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HapiAuthInterceptor extends InterceptorService {//extends ScopeBasedAuthorizationInterceptor implements IInterceptorService {
    public HapiAuthInterceptor(TokenStore tokenStore, OAuth2RestTemplate oAuth2RestTemplate) {
        //super(tokenStore, oAuth2RestTemplate);
    }

    public HapiAuthInterceptor() {
        this(new InMemoryTokenStore(), new OAuth2RestTemplate(new AuthorizationCodeResourceDetails()));
    }

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
    public void authorize(RequestDetails theRequestDetails) {
        System.out.println("Start interceptor1");
        //buildRuleList(theRequestDetails);
    }

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)
    public boolean authorize2(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("Start interceptor1.1");
        //buildRuleList(theRequestDetails);
        return true;
    }

    //@Override
    @Hook(Pointcut.CLIENT_REQUEST)
    public void interceptRequest(IHttpRequest theRequest) {
        System.out.println("Start interceptor2");
        Set<Map.Entry<String, List<String>>> entries = theRequest.getAllHeaders().entrySet();
        for (Map.Entry<String, List<String>> entry : entries) {
            System.out.println("Key = " + entry.getKey());
            for (String value : entry.getValue()) {
                System.out.println("Value = " + value);
                /*if (value.equals("state")) {
                    DefaultOAuth2AccessToken defaultOAuth2AccessToken = SecurityConfig.getInMemTokenStore().get("12345");

                }*/
            }
        }

        //buildRuleList(null);
    }

    //@Override
    @Hook(Pointcut.CLIENT_RESPONSE)
    public void interceptResponse(IHttpResponse theResponse) throws IOException {
        System.out.println("Start interceptor3");
        System.out.println(theResponse.getStatusInfo());
        HttpResponse response = (HttpResponse) theResponse.getResponse();
        //response.setE
        System.out.println(response.getEntity().getContentType());
        //SecurityConfig.getInMemTokenStore().containsKey("12345")
    }
}
