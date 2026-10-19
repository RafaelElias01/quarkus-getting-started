package org.acme;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.inject.Inject;

// Exercício 5: readiness depende de outro serviço (IUserClient já existente)
@Readiness
public class ReadyCatalog implements HealthCheck {

    @Inject
    @RestClient
    IUserClient userClient;

    @Override
    public HealthCheckResponse call() {
        try {
            userClient.listUsers();
            return HealthCheckResponse.up("catalog-service");
        } catch (Exception e) {
            return HealthCheckResponse.down("catalog-service");
        }
    }
}
