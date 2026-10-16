package org.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ContaBancariaResourceTest {

    @Test
    public void testDeposito() {
        given()
            .queryParam("saldo", 100)
            .queryParam("valor", 50)
        .when()
            .get("/conta/deposito")
        .then()
            .statusCode(200)
            .body("resultado", equalTo(150.0f));
    }

    @Test
    public void testSaqueSaldoInsuficiente() {
        given()
            .queryParam("saldo", 100)
            .queryParam("valor", 150)
        .when()
            .get("/conta/saque")
        .then()
            .statusCode(400)
            .body("mensagem", equalTo("Saldo insuficiente"));
    }

    @Test
    public void testSaqueOk() {
        given()
            .queryParam("saldo", 100)
            .queryParam("valor", 40)
        .when()
            .get("/conta/saque")
        .then()
            .statusCode(200)
            .body("resultado", equalTo(60.0f));
    }

    @Test
    public void testFatura() {
        given()
            .contentType("application/json")
            .body("[20.0, 35.5, 10.0]")
        .when()
            .post("/conta/fatura")
        .then()
            .statusCode(200)
            .body("resultado", greaterThan(60.0f));
    }

    @Test
    public void testFaturaVazia() {
        given()
            .contentType("application/json")
            .body("[]")
        .when()
            .post("/conta/fatura")
        .then()
            .statusCode(400);
    }

    @Test
    public void testRendimento() {
        given()
        .when()
            .get("/conta/rendimento/5")
        .then()
            .statusCode(200)
            .body("resultado", equalTo(1050.0f));
    }

    @Test
    public void testExtratoComToken() {
        given()
            .header("Authorization", "Bearer abc.123.xyz")
            .queryParam("saldo", 500)
        .when()
            .get("/conta/extrato")
        .then()
            .statusCode(200)
            .body("resultado", equalTo(490.0f));
    }

    @Test
    public void testExtratoSemToken() {
        given()
            .queryParam("saldo", 500)
        .when()
            .get("/conta/extrato")
        .then()
            .statusCode(401);
    }

    @Test
    public void testDepositoEExtrato() {
        // pega o saldo do deposito e usa no extrato
        float saldo = given()
            .queryParam("saldo", 100)
            .queryParam("valor", 400)
        .when()
            .get("/conta/deposito")
        .then()
            .statusCode(200)
            .extract().path("resultado");

        given()
            .header("Authorization", "Bearer abc.123.xyz")
            .queryParam("saldo", saldo)
        .when()
            .get("/conta/extrato")
        .then()
            .statusCode(200)
            .body("resultado", equalTo(490.0f));
    }
}
