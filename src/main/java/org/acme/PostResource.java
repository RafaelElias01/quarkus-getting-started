package org.acme;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/local-posts")
public class PostResource {

    @Inject
    @RestClient
    IPostClient postClient;

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Post getLocalPost(@PathParam("id") Long id) {
        return postClient.getPost(id);
    }

}
