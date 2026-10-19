package org.acme;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

// Exercícios 2 e 3: classe criada com down() e depois modificada para up() com a versão
@Readiness
public class Ready implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("ready")
                .up()
                .withData("version", "1.0.0")
                .build();
    }
}
