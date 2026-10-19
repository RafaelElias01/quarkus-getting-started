package org.acme;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

// Exercicio 2: readiness DOWN de proposito, sem consultar nada externo.
// Enquanto esta classe existir, /q/health/ready responde 503.
@Readiness
public class ReadyDown implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.down("ready-down");
    }
}
