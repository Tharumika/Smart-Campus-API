package com.smartcampus.resources;

import com.smartcampus.exceptions.RoomNotEmptyException;
import com.smartcampus.models.Room;
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

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    // Access our thread-safe in-memory database
    private DataStore store = DataStore.getInstance();

    /**
     * GET /api/v1/rooms
     * Returns a comprehensive list of all rooms.
     */
    @GET
    public Response getAllRooms() {
        List<Room> rooms = new ArrayList<>(store.getRooms().values());
        return Response.ok(rooms).build();
    }

    /**
     * GET /api/v1/rooms/{roomId}
     * Allow users to fetch detailed metadata for a specific room.
     */
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            // Standard 404 Not Found if room doesn't exist
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Room not found\"}")
                    .build();
        }
        return Response.ok(room).build();
    }

    /**
     * POST /api/v1/rooms
     * Enable the creation of new rooms.
     * Returns 201 Created and the URI of the new room.
     */
    @POST
    public Response createRoom(Room room, @Context UriInfo uriInfo) {
        // Generate a unique ID if one isn't provided by the client
        if (room.getId() == null || room.getId().trim().isEmpty()) {
            room.setId("ROOM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        // Initialize sensor array just in case client sent null
        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        }

        // Save to thread-safe store
        store.getRooms().put(room.getId(), room);

        // Build the Location URI pointing to the newly created resource
        URI locationUri = uriInfo.getAbsolutePathBuilder().path(room.getId()).build();

        return Response.created(locationUri).entity(room).build();
    }

    /**
     * DELETE /api/v1/rooms/{roomId}
     * Allow room decommissioning, but blocks if sensors are still attached.
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        
        if (room == null) {
            // Idempotency: If it's already gone, return a 404 Not Found.
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Room not found or already deleted\"}")
                    .build();
        }

        // BUSINESS LOGIC CONSTRAINT: Prevent Data Orphans
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            // Throw custom exception which is mapped to a 409 Conflict JSON response
            throw new RoomNotEmptyException("Cannot delete room '" + roomId + "' because it still has active sensors assigned to it.");
        }

        // Safe to delete
        store.getRooms().remove(roomId);

        // 204 No Content is standard for a successful DELETE operation
        return Response.noContent().build();
    }
}