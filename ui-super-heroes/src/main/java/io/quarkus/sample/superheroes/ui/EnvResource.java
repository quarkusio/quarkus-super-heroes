package io.quarkus.sample.superheroes.ui;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.common.annotation.NonBlocking;

/**
 * JAX-RS API endpoints to serve configuration to a front-end.
 */
@Path("/env.js")
public class EnvResource {

    @ConfigProperty(name = "api.base.url", defaultValue = "http://localhost:8082")
    String url;
    @ConfigProperty(name = "auth.base.url", defaultValue = "http://localhost:8086")
    String authUrl;

    @ConfigProperty(name = "calculate.api.base.url", defaultValue = "false")
    boolean calculateApiBaseUrl;

    @Inject
    ObjectMapper objectMapper;

    @GET
    @Produces(APPLICATION_JSON)
    @NonBlocking
    public String getConfig() throws JsonProcessingException {
        var config = new Config(this.url, this.authUrl, this.calculateApiBaseUrl);

        // We could just return the Config object, but that would be json, and we want a
        // javascript snippet we can include with <script src="..."/>
        return "window.APP_CONFIG=" + this.objectMapper.writeValueAsString(config);
    }


}
