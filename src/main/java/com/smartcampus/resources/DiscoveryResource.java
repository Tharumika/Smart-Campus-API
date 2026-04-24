package com.smartcampus.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("/")
public class DiscoveryResource {

    /**
     * Discovery Endpoint.
     * Demonstrates HATEOAS (Hypermedia As The Engine Of Application State).
     * Maps to GET /api/v1
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("apiName", "Smart Campus Sensor & Room Management API");
        metadata.put("version", "v1.0");
        metadata.put("W2120815Contact", "W2120815@university.edu");

        // Hypermedia links enabling self-discovery
        Map<String, String> links = new HashMap<>();
        links.put("rooms", "/api/rooms");
        links.put("sensors", "/api/sensors");

        metadata.put("_links", links);

        return Response.ok(metadata).build();
    }
}