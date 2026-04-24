package com.smartcampus.config;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/")
public class ApplicationConfig extends Application {
<<<<<<< HEAD
    // empty body — web.xml handles routing
=======
    // This class configures the base URI for all JAX-RS REST resources
    // Empty class is sufficient; the server will scan for @Path annotations.
>>>>>>> 469379c8b91dc4899f60d48b5bfe01912f29bbce
}