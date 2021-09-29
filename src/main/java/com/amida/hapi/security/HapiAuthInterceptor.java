package com.amida.hapi.security;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.interceptor.executor.InterceptorService;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import com.amida.hapi.domain.HapiFhirClient;
import org.apache.tomcat.websocket.AuthenticationException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HapiAuthInterceptor extends InterceptorService {

    private int count = 0;

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
    public boolean authorize(RequestDetails theRequestDetails, ServletRequestDetails servletRequestDetails,
                          HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        System.out.println("Start interceptor");
        if (count == 0) {
            count++;
            return true;
        }

        String client_id = theRequestDetails.getHeader("client_id");
        if (client_id == null || "".equals(client_id)) {
            throw new AuthenticationException("Bypassed client selection.");
        }

        if ("startup".equals(client_id)) {
            return true;
        }

        HapiFhirClient hapiFhirClient = SecurityConfig.getInMemTokenStore().get(client_id);
        if (hapiFhirClient == null) {
            throw new AuthenticationException("Client has not authenticated yet.");
        }

        if (TokenUtil.verifyToken(hapiFhirClient)) {
            return true;
        }
        else {
            throw new AuthenticationException("Client is not authenticated.");
        }
    }
}
