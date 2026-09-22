package org.acme;

import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RegisterRestClient 
@Path("/users") 
public interface IUserClient {

    @GET 
    @Produces(MediaType.APPLICATION_JSON)
    List<User> listUsers();

}
