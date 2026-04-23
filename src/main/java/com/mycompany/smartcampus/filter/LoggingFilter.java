package com.mycompany.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/*Request & Response Logging Filter
 every incoming request and outgoing response is logged in one class*/

public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    /**
     * Runs BEFORE the resource method.
     * Logs the HTTP method and full request URI.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info(String.format(
            "[REQUEST]  %s %s",
            requestContext.getMethod(),
            requestContext.getUriInfo().getRequestUri()
        ));
    }

    /**
     * Runs AFTER the resource method (and after exception mappers).
     * Logs the HTTP status code of the outgoing response.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOGGER.info(String.format(
            "[RESPONSE] %s %s → HTTP %d",
            requestContext.getMethod(),
            requestContext.getUriInfo().getRequestUri(),
            responseContext.getStatus()
        ));
    }
}
