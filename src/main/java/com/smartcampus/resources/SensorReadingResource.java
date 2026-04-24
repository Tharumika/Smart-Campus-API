package com.smartcampus.resources;

import com.smartcampus.exceptions.SensorUnavailableException;
import com.smartcampus.models.Sensor;
import com.smartcampus.models.SensorReading;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Sub-Resource Locator Class.
 * Notice there is NO class-level @Path annotation. It is dynamically routed from SensorResource.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private String sensorId;
    private DataStore store = DataStore.getInstance();

    // Constructor receives the contextual sensor ID from the parent resource
    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    /**
     * GET /api/v1/sensors/{sensorId}/readings
     * Fetch historical readings.
     */
    @GET
    public Response getHistoricalReadings() {
        // Verify sensor exists first
        if (!store.getSensors().containsKey(sensorId)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Sensor not found\"}")
                    .build();
        }

        List<SensorReading> history = store.getReadingsForSensor(sensorId);
        return Response.ok(history).build();
    }

    /**
     * POST /api/v1/sensors/{sensorId}/readings
     * Append new readings for a specific sensor context.
     * Side Effect: Updates the parent Sensor's currentValue.
     */
    @POST
    public Response postReading(SensorReading reading, @Context UriInfo uriInfo) {
        Sensor parentSensor = store.getSensors().get(sensorId);

        // 1. Validation (Not Found)
        if (parentSensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Sensor not found\"}")
                    .build();
        }

        // 2. State Constraint (403 Forbidden)
        String status = parentSensor.getStatus();
        if ("MAINTENANCE".equalsIgnoreCase(status) || "OFFLINE".equalsIgnoreCase(status)) {
            throw new SensorUnavailableException("Sensor '" + sensorId + "' is currently " + status + " and cannot accept new readings.");
        }

        // Generate UUID and Timestamp if missing
        if (reading.getId() == null || reading.getId().trim().isEmpty()) {
            reading.setId("RDG-" + UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() <= 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // 3. Save the Reading
        store.getReadingsForSensor(sensorId).add(reading);

        // 4. SIDE EFFECT: Update the currentValue on the corresponding parent Sensor object
        parentSensor.setCurrentValue(reading.getValue());

        // Location URI construction
        URI locationUri = uriInfo.getAbsolutePathBuilder().path(reading.getId()).build();
        return Response.created(locationUri).entity(reading).build();
    }
}