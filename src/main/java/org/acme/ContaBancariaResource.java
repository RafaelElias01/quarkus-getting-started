package org.acme;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/conta")
public class ContaBancariaResource {

    public record Resultado(double resultado) { }

    public record Erro(String mensagem) { }

    @GET
    @Path("/deposito")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deposito(@QueryParam("saldo") double saldo, @QueryParam("valor") double valor) {
        return Response.ok(new Resultado(saldo + valor)).build();
    }

    @GET
    @Path("/saque")
    @Produces(MediaType.APPLICATION_JSON)
    public Response saque(@QueryParam("saldo") double saldo, @QueryParam("valor") double valor) {
        if (valor > saldo) {
            return Response.status(400).entity(new Erro("Saldo insuficiente")).build();
        }
        return Response.ok(new Resultado(saldo - valor)).build();
    }

    @POST
    @Path("/fatura")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response fatura(List<Double> compras) {
        if (compras == null || compras.isEmpty()) {
            return Response.status(400)
                    .entity(new Erro("A lista de compras não pode ser vazia"))
                    .build();
        }
        double total = compras.stream().mapToDouble(Double::doubleValue).sum();
        return Response.ok(new Resultado(total)).build();
    }

    @GET
    @Path("/rendimento/{meses}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response rendimento(@PathParam("meses") int meses) {
        double valorInicial = 1000;
        double taxaMensal = 0.01;
        double montante = valorInicial + (valorInicial * taxaMensal * meses);
        return Response.ok(new Resultado(montante)).build();
    }

    @GET
    @Path("/extrato")
    @Produces(MediaType.APPLICATION_JSON)
    public Response extrato(
            @QueryParam("saldo") double saldo,
            @HeaderParam("Authorization") String token) {
        if (token == null || token.isBlank()) {
            return Response.status(401).entity(new Erro("Autenticação obrigatória")).build();
        }
        double taxaManutencao = saldo * 0.02;
        return Response.ok(new Resultado(saldo - taxaManutencao)).build();
    }
}
