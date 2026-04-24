package com.smartcampus.resources;

import com.smartcampus.exceptions.LinkedResourceNotFoundException;
import com.smartcampus.models.Room;
import com.smartcampus.models.Sensor;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private DataStore store = DataStore.getInstance();

    /**
     * GET /api/v1/sensors
     * Supports optional @QueryParam filtering by type (e.g., ?type=CO2).
     */
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> sensors = new ArrayList<>(store.getSensors().values());

        // Distinction Filter Logic: Dynamic Search, case-insensitive
        if (type != null && !type.trim().isEmpty()) {
            sensors = sensors.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type.trim()))
                    .collect(Collectors.toList());
        }

        return Response.ok(sensors).build();
    }

    /**
     * POST /api/v1/sensors
     * Verifies that the roomId specified in the request body exists.
     */
    @POST
    public Response registerSensor(Sensor sensor, @Context UriInfo uriInfo) {
        // Dependency Validation (422 Unprocessable Entity)
        String roomId = sensor.getRoomId();
        if (roomId == null || !store.getRooms().containsKey(roomId)) {
            throw new LinkedResourceNotFoundException("Cannot register sensor. The specified roomId '" + roomId + "' does not exist.");
        }

        // Generate ID and default status if missing
        if (sensor.getId() == null || sensor.getId().trim().isEmpty()) {
            sensor.setId("SENS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (sensor.getStatus() == null) {
            sensor.setStatus("ACTIVE"); // Default
        }

        // Save sensor to the store
        store.getSensors().put(sensor.getId(), sensor);

        // Update the parent room's sensorIds list
        Room parentRoom = store.getRooms().get(roomId);
        if (parentRoom.getSensorIds() == null) {
            parentRoom.setSensorIds(new ArrayList<>());
        }
        parentRoom.getSensorIds().add(sensor.getId());

        // Build 201 Created Location URI
        URI locationUri = uriInfo.getAbsolutePathBuilder().path(sensor.getId()).build();
        return Response.created(locationUri).entity(sensor).build();
    }

    /**
     * The Sub-Resource Locator Pattern.
     * Maps requests starting with /sensors/{sensorId}/readings to a dedicated class.
     * Architectural Benefit: Keeps Controller classes modular and small.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
<<<<<<< HEAD
        return new SensorReadingResource(sensorId);
    }

    /**
     * DELETE /api/v1/sensors/{sensorId}
     * Delete a sensor by its ID.
     */
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Sensor not found\"}")
                    .build();
        }

        // Remove from parent room's sensorIds list
        String roomId = sensor.getRoomId();
        if (roomId != null) {
            Room parentRoom = store.getRooms().get(roomId);
            if (parentRoom != null && parentRoom.getSensorIds() != null) {
                parentRoom.getSensorIds().remove(sensorId);
            }
        }

        // Remove from store
        store.getSensors().remove(sensorId);
        
        return Response.noContent().build();
    }
=======
        // Pass the contextual sensorId to the newly instantiated sub-resource
        return new SensorReadingResource(sensorId);
    }
>>>>>>> 469379c8b91dc4899f60d48b5bfe01912f29bbce
}