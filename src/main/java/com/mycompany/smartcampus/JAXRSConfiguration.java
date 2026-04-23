package com.mycompany.smartcampus;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * Bootstraps the JAX-RS application.
 *
 * @ApplicationPath sets the base URI for all REST endpoints.
 * Every resource class path is appended after "/api/v1".

 */
@ApplicationPath("/api/v1")
public class JAXRSConfiguration extends Application {
    // No overrides needed — Jersey auto-discovers resources and providers
}
