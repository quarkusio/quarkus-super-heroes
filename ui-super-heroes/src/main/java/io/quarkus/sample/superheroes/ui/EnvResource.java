package io.quarkus.sample.superheroes.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * JAX-RS API endpoints to serve configuration to a front-end.
 */
@Path("/")
public class EnvResource {

    @ConfigProperty(name = "api.base.url", defaultValue = "http://localhost:8082")
    String url;

    @ConfigProperty(name = "calculate.api.base.url", defaultValue = "false")
    boolean calculateApiBaseUrl;

    @Inject
    ObjectMapper objectMapper;

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/env.js")
    public String getConfig() throws JsonProcessingException {
        Config config = new Config(url,
            calculateApiBaseUrl);
        // We could just return the Config object, but that would be json, and we want a
        // javascript snippet we can include with <script src="..."/>
        return "window.NG_CONFIG=" + objectMapper.writeValueAsString(config);
    }


}
