package org.acme;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import jakarta.ws.rs.Produces;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/local-users")
public class UserResource {

    @Inject 
    @RestClient 
    IUserClient userClient;

    @GET 
    @Produces(MediaType.APPLICATION_JSON)
    public List<User> listLocalUsers() {
        return userClient.listUsers();
    }

}
