# Manual de Quarkus — baseado no site da disciplina PW2

Fonte: https://pw2.rpmhub.dev (Programação para Web II, IFRS Porto Alegre, CC BY 4.0).
Regra deste projeto: toda resolução de exercício deve seguir os exemplos e padrões deste manual.

## Notas de compatibilidade (leia primeiro)

- Este projeto usa **Quarkus 3.39.4 e Java 21**. Vários exemplos do site foram escritos para versões antigas (2.x). Ao copiar um exemplo, ajuste:
  - `javax.*` vira `jakarta.*` (`jakarta.ws.rs`, `jakarta.persistence`, `jakarta.transaction`, `jakarta.inject`, `jakarta.annotation.security`).
  - Extensões `resteasy-reactive` viraram `rest` (`quarkus-rest`, `quarkus-rest-jackson`). Rest Client: `quarkus-rest-client` e `quarkus-rest-client-jackson`. As versões `resteasy` e `resteasy-jackson` são as clássicas.
  - `@RegisterRestClient` continua igual. No Quarkus 3 o registro de URL por propriedade é `quarkus.rest-client."classe.completa".url` (a forma `mp-rest/url` do site ainda funciona).
- Os trechos abaixo foram extraídos do site por um resumidor automático. Confira os detalhes na página original quando algo for crítico.

## Índice

1. Instalação e projeto
2. Microsserviços (conceito)
3. Web Services REST (JAX-RS)
4. Rest Client
5. JWT
6. Configuração
7. Health
8. Métricas
9. Tolerância a falhas
10. Trace e Log
11. Hibernate e Panache
12. Programação reativa
13. Exercícios do curso
14. Checklist para resolver um exercício

---

## 1. Instalação e projeto

Requisitos: Java e Maven (ou o Maven Wrapper `mvnw`). Verifique com `java -version` e `javac -version`. O site recomenda o **SDKMAN** para gerenciar versões do Java.

Formas de criar projeto:
1. **code.quarkus.io**: escolhe as dependências e baixa um `.zip`.
2. **GitHub Codespaces**: com `.devcontainer/devcontainer.json`, depois `quarkus dev`.
3. **Template pw2**: repositório pronto com Codespace configurado.
4. **Extensão do VSCode**: cria projeto e gerencia dependências.
5. **Quarkus CLI**: `sdk install quarkus`.

Rodar em modo desenvolvimento (hot reload):

```bash
./mvnw compile quarkus:dev
```

Adicionar extensão a projeto existente:

```bash
./mvnw quarkus:add-extension -Dextensions="smallrye-health"
```

## 2. Microsserviços (conceito)

- Cada funcionalidade é um serviço independente (ex.: catálogo, estoque, pagamento). No monólito, tudo é instalado junto.
- Vantagens: escala independente, resiliência, desacoplamento, liberdade de tecnologia, deploy independente.
- Desvantagens: complexidade operacional, latência de rede, testes de integração mais difíceis, versionamento, mais superfícies para proteger.
- **Eclipse MicroProfile** reúne as especificações usadas no curso (JAX-RS, CDI, JWT, Config, Health, Fault Tolerance, Rest Client).

## 3. Web Services REST (JAX-RS)

REST usa os verbos HTTP (GET, POST, PUT, DELETE). No Quarkus, o JAX-RS vem da extensão REST.

Criar projeto (exemplo do site, versão antiga):

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.8.2.Final:create \
    -DprojectGroupId=dev.rpmhub \
    -DprojectArtifactId=produtos \
    -DclassName="dev.rpmhub.ProdutoResource" \
    -Dpath="/produtos" \
    -Dextensions="resteasy-reactive,resteasy-reactive-jackson"
```

| Anotação | Uso | Função |
|---|---|---|
| `@Path` | classe/método | define a URI do recurso |
| `@GET @POST @PUT @DELETE` | método | associa ao verbo HTTP |
| `@PathParam` | parâmetro | valor vindo da URI |
| `@QueryParam` | parâmetro | valor da query string |
| `@FormParam` | parâmetro | campo de formulário |
| `@Consumes` | método | tipo MIME recebido |
| `@Produces` | método | tipo MIME enviado |

Exemplo base:

```java
@Path("/produtos")
public class ProdutoResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String ola() {
        return "Bem-vindo ao catálogo de produtos!";
    }
}
```

Exercícios do site: conversão de unidades (km/h para mph por POST; nós para km/h por GET), gerenciador de tarefas (criar, listar, filtrar, deletar). Exemplo completo: https://github.com/rpmhubdev/pw2-conversion

## 4. Rest Client

Consome outro serviço por uma **interface Java anotada**, reutilizando anotações JAX-RS.

Extensões: `rest-client` e `rest-client-jackson` (JSON automático).

Interface do cliente:

```java
@RegisterRestClient(baseUri = "https://localhost:8444/")
public interface IPayment {

    @POST
    @Path("/payment")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    Invoice confirmPayment(
        @FormParam("cardNumber") String cardNumber,
        @FormParam("value") String value);
}
```

- `@RegisterRestClient` torna a interface um bean CDI.
- `baseUri` define a URL remota. Alternativa: `application.properties` com `{classe completa}/mp-rest/url={url base}`.

Uso:

```java
@Inject
@RestClient
IPayment paymentService;

Invoice invoice = paymentService.confirmPayment(cardNumber, value);
```

Exemplo do exercício de empréstimo de livros (catálogo na porta 9080, empréstimos na 9081):

```java
@RegisterRestClient(baseUri = "http://localhost:9080/books")
public interface IBookCatalog {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<Book> listBooks();

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    Book getBook(@PathParam("id") Long id);

    @PUT
    @Path("/{id}/loan")
    @Produces(MediaType.APPLICATION_JSON)
    Book markAsLoaned(@PathParam("id") Long id);
}
```

Ordem de trabalho recomendada: implementar e validar o serviço 1 isolado; implementar o serviço 2 com Rest Client; subir os dois; rodar `./mvnw test`.

## 5. JWT

JWT (RFC 7519) tem três partes: cabeçalho, payload (claims) e assinatura. Fluxo: cliente envia credenciais, servidor gera o JWT, cliente manda `Authorization: Bearer <token>` em cada requisição, servidor valida a assinatura sem consultar banco.

Extensões: `smallrye-jwt` (validar) e `smallrye-jwt-build` (gerar).

Gerar chaves:

```bash
openssl genrsa -out rsaPrivateKey.pem 2048
openssl pkcs8 -topk8 -nocrypt -inform pem -in rsaPrivateKey.pem -outform pem -out privateKey.pem
openssl rsa -pubout -in rsaPrivateKey.pem -out publicKey.pem
```

`application.properties`:

```properties
smallrye.jwt.sign.key.location=privateKey.pem
mp.jwt.verify.issuer=http://localhost:8080
mp.jwt.verify.publickey.location=publicKey.pem
```

Gerar token:

```java
@POST
@Path("/getJwt")
@PermitAll
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.TEXT_PLAIN)
public String generate(final String fullName, final String email) {
    return Jwt.issuer("http://localhost:8080")
            .upn(email)
            .groups(new HashSet<>(Arrays.asList("User", "Admin")))
            .claim("full_name", fullName)
            .sign();
}
```

Proteger endpoint:

```java
@POST
@Path("/buy")
@RolesAllowed("User")
public Invoice buy(@FormParam("cardNumber") String cardNumber,
                   @FormParam("value") String value) {
    return paymentService.pay(cardNumber, value);
}
```

| Anotação | Função |
|---|---|
| `@PermitAll` | libera sem token |
| `@RolesAllowed({"User"})` | restringe por role |
| `@Inject JsonWebToken token` | injeta o token para leitura |
| `@Claim("full_name")` | acessa uma claim |

Sign + Encrypt (dados sensíveis):

```java
return Jwt.issuer("http://localhost:8080")
    .upn("user@email.com")
    .groups(new HashSet<>(Arrays.asList("User")))
    .claim("full_name", fullName)
    .innerSign()
    .encrypt();
```

```properties
smallrye.jwt.sign.key.location=privateKey.pem
smallrye.jwt.encrypt.key.location=publicKey.pem
mp.jwt.decrypt.key.location=privateKey.pem
```

Propagação entre serviços: dependência `quarkus-rest-client-oidc-token-propagation` e `@AccessToken` na interface do Rest Client. Desligue o Keycloak de dev com `quarkus.keycloak.devservices.enabled=false`.

```java
@RegisterRestClient(baseUri = "https://localhost:8445/payment")
@AccessToken
public interface IPayment { }
```

HTTPS com certificado autoassinado:

```bash
keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass password -validity 365 -keysize 2048
```

```properties
quarkus.http.ssl-port=8443
quarkus.http.ssl.certificate.key-store-file=keystore.jks
quarkus.http.ssl.certificate.key-store-password=password
quarkus.tls.trust-all=true
```

Padrão canônico de referência: módulo `users` do projeto BookStorePW2 do professor.

## 6. Configuração

Quarkus implementa MicroProfile Config. Fontes: `src/main/resources/application.properties`, variáveis de ambiente (pontos viram underscore, `PW2_MESSAGE`), `-D`, arquivo `.env` na raiz e YAML.

```java
@Inject
@ConfigProperty(name = "pw2.message", defaultValue = "")
String message;
```

Propriedade ausente: use `defaultValue`, `Optional<T>`, ou deixe falhar na inicialização.

Sobrescrever na execução:

```bash
./mvnw compile quarkus:dev -Dpw2.message=hello
```

Perfis (`dev`, `test`, `prod`) com prefixo `%`:

```properties
pw2.jdbc=jdbc:mysql://localhost:3306/pw2
%prod.pw2.jdbc=jdbc:mysql://rpmhub.dev:3307/pw2
```

Perfil customizado: `-Dquarkus.profile=nome`.

`.env`: fica ao lado do `pom.xml`, sobrescreve o `application.properties`, serve para credenciais locais e deve entrar no `.gitignore`.

YAML: dependência `quarkus-config-yaml` e arquivo `application.yaml`:

```yaml
pw2:
  message: hello
  name: world
```

## 7. Health

Extensão `smallrye-health` (MicroProfile Health). Serve para o Kubernetes decidir se descarta ou substitui instâncias.

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-health</artifactId>
</dependency>
```

Endpoints: `/q/health/live`, `/q/health/ready`, `/q/health`. Interface web: `/q/health-ui/` (dev e teste).

Liveness:

```java
@Liveness
@Startup
public class Live implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("Management is alive!");
    }
}
```

Readiness (checando outro serviço via Rest Client):

```java
@Readiness
public class Ready implements HealthCheck {

    @Inject
    @RestClient
    PingRC service;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder response =
            HealthCheckResponse.named(Ready.class.getName());
        checkCatalogService(response);
        return response.build();
    }

    private HealthCheckResponseBuilder checkCatalogService(
            HealthCheckResponseBuilder response) {
        if (service.ping().equals("Pong")) {
            response.up().withData("CatalogService", "up");
        }
        return response;
    }
}
```

## 8. Métricas (Micrometer + Prometheus)

Extensão `micrometer-registry-prometheus`. Métricas em `http://localhost:8080/q/metrics`.

Obter o `MeterRegistry`: injeção por construtor (recomendada), por `@Inject`, ou `Metrics.globalRegistry`.

```java
// Counter
registry.counter("example.prime.number", "type", "prime").increment();

// Gauge
Gauge.builder("cache.size", cache, Cache::size)
    .description("Current cache size")
    .register(registry);

// Timer
@Timed(value = "payment.processing", extraTags = {"gateway", "stripe"})
public PaymentResult processPayment(Payment payment) { ... }

// DistributionSummary
DistributionSummary.builder("order.value")
    .baseUnit("BRL")
    .register(registry)
    .record(orderValue);
```

Prometheus via Docker (`docker-compose.yml` e `prometheus.yml`):

```yaml
volumes:
    metrics:
services:
    prometheus:
      image: prom/prometheus:v3.11.3
      container_name: prometheus
      ports:
        - 9090:9090
      volumes:
        - metrics:/etc/prometheus
        - ./prometheus.yml:/etc/prometheus/prometheus.yml
      extra_hosts:
        - "host.docker.internal:host-gateway"
```

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: "quarkus-app"
    metrics_path: "/q/metrics"
    static_configs:
      - targets: ["host.docker.internal:8080"]
```

Automáticas: HTTP server, JVM e sistema, extensões instrumentadas.

## 9. Tolerância a falhas

Extensão `quarkus-smallrye-fault-tolerance`.

| Anotação | Ideia |
|---|---|
| `@Retry` | tenta de novo |
| `@Fallback` | plano B |
| `@Timeout` | limite de espera |
| `@Bulkhead` | limita concorrência |
| `@CircuitBreaker` | disjuntor |

Retry com fallback:

```java
@GET
@Path("/{name}")
@Produces(MediaType.TEXT_PLAIN)
@Retry(maxRetries = 3, delay = 2000)
@Fallback(fallbackMethod = "recover")
public String getName(@PathParam("name") String name) {
    if (name.equalsIgnoreCase("error")) {
        throw new WebApplicationException(
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("The requested was an error").build());
    }
    return name;
}

public String recover(String name) {
    return FALL_BACK_MESSAGE;
}
```

O método de fallback precisa ter a mesma assinatura do método original.

Timeout e Bulkhead:

```java
@Timeout(7000)

@Bulkhead(2)                                        // semáforo

@Asynchronous
@Bulkhead(value = 2, waitingTaskQueue = 5)          // thread pool
```

Circuit breaker:

```java
@CircuitBreaker(requestVolumeThreshold = 2)
public Integer getAvailability(Coffee coffee) {
    maybeFail();
    return new Random().nextInt(30);
}
```

Combinação recomendada:

```java
@Timeout(3000)
@Retry(maxRetries = 3, delay = 500)
@CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5)
@Fallback(fallbackMethod = "respostaCache")
public Resposta consultarServico() { ... }
```

Teste de carga com k6:

```javascript
import exec from 'k6/execution';
import http from 'k6/http';
import { sleep } from 'k6';

export const options = {
    vus: 10,
    duration: '10s',
    thresholds: { http_req_failed: ['rate<0.05'] },
};

export default function () {
    http.get('http://localhost:8080/fault/bulkhead/' + exec.vu.idInTest);
    sleep(1);
}
```

Exercício do site: `@Retry` em `/users/getJwt`; `@CircuitBreaker` e `@Timeout` em `/bookManagement/listBooks`. Código: `git clone -b dev https://github.com/rodrigoprestesmachado/pw2`

## 10. Trace e Log

- **Trace (Jaeger)**: caminho de uma requisição entre serviços; cada etapa é um *span*. Setup: Docker Compose (UI na porta 16686), extensão `quarkus-opentelemetry`, endpoint OTLP em `localhost:4317`.
- **Log (Graylog)**: eventos textuais centralizados em JSON. Setup: Elasticsearch, MongoDB e Graylog no Docker, extensão `quarkus-logging-gelf`, input GELF UDP na porta 12201.
- Depuração: achou o problema no Graylog, copie o `traceId` e consulte no Jaeger para ver qual serviço causou a lentidão. O formato de log deve incluir os campos de rastreamento.

## 11. Hibernate e Panache

Dependências:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-hibernate-orm-panache</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-jdbc-mysql</artifactId>
</dependency>
```

Entidade (herdar `PanacheEntity` dá o `id` automático; `PanacheEntityBase` deixa o `id` manual):

```java
@Entity
public class User extends PanacheEntity {
    private String name;
}
```

One-to-many:

```java
@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
@JoinColumn(name = "user_id")
private List<Message> messages;
```

Many-to-many bidirecional (`@JsonBackReference` e `@JsonManagedReference` evitam recursão infinita no JSON):

```java
// User
@ManyToMany(cascade = CascadeType.ALL)
@JsonBackReference
private List<Channel> channels;

// Channel
@ManyToMany(mappedBy = "channels", fetch = FetchType.EAGER)
@JsonManagedReference
private List<User> users;
```

Recurso REST com Panache:

```java
@Path("/user")
@Transactional
public class UserWS {

    @GET
    @Path("/save/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public User save(@PathParam("name") String name) {
        User user = new User();
        user.setName(name);
        user.persist();
        return user;
    }

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public List<User> list() {
        return User.listAll();
    }

    @GET
    @Path("/list/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public User findById(@PathParam("id") Long id) {
        return User.findById(id);
    }
}
```

Banco em `application.properties`:

```properties
quarkus.datasource.db-kind=mysql
quarkus.datasource.username=pw2
quarkus.datasource.password=pw2
quarkus.hibernate-orm.database.generation=create
%dev.quarkus.datasource.devservices.port=3306
%dev.quarkus.swagger-ui.always-include=true
```

MySQL via Docker Compose:

```yaml
version: "3.7"
volumes:
  database:
services:
  db:
    image: mysql:latest
    container_name: hibernate
    ports:
      - 3306:3306
    volumes:
      - database:/var/lib/mysql
    environment:
      - MYSQL_ROOT_PASSWORD=hibernate
      - MYSQL_DATABASE=hibernate
      - MYSQL_USER=hibernate
      - MYSQL_PASSWORD=hibernate
```

Criteria API (alternativa ao Panache):

```java
CriteriaBuilder cb = em.getCriteriaBuilder();
CriteriaQuery cq = cb.createQuery();
Root<User> e = cq.from(User.class);
cq.where(cb.equal(e.get("id"), 1));
User user = (User) em.createQuery(cq).getSingleResult();
```

Para contar: `cq.select(cb.count(e));`

## 12. Programação reativa

Modelo assíncrono, não bloqueante e orientado a eventos. O Quarkus usa Vert.x com event loops, sem uma thread por conexão.

- `Uni`: um único resultado futuro. `Multi`: fluxo de vários itens.
- Métodos: `onItem` (reage ao sucesso), `call` (executa algo assíncrono e espera), `invoke` (dispara e segue), `transform` (converte o item).

```java
repository.findById(user.id)
    .onItem().ifNotNull()
    .call(item -> {
        item.setName(user.getName());
        item.setEmail(user.getEmail());
        return repository.persistAndFlush(item);
    });
```

Referências: guias do Quarkus e a documentação do Mutiny.

## 13. Exercícios do curso

Índice em `/topicos/exercicios/exercicios-ws-rest-jwt.html` (1ª avaliação):
- **Web Services**: conversão de unidades, gerenciador de tarefas, quiz de JAX-RS.
- **Rest Client**: rede social de empréstimo de livros com dois microsserviços; teste teórico.
- **JWT**: três exercícios básicos (gerar, proteger, ler); projeto integrador com serviço `Users`; teste conceitual.
- 2ª avaliação: `/topicos/exercicios/simulado2.html`. Projeto final: `/topicos/projeto/enunciado.html` e rubrica em `/topicos/projeto/rubrica.html`.

## 14. Checklist para resolver um exercício

1. Identifique o tópico (REST, Rest Client, JWT, Config, Fault Tolerance, Panache) e releia a seção correspondente acima.
2. Confira as extensões necessárias e adicione-as ao projeto.
3. Copie o padrão do site e adapte nomes, pacotes e `jakarta.*`.
4. Configure o `application.properties` (portas, URLs, chaves).
5. Teste isolado cada serviço, depois a integração.
6. Em dúvida de padrão JWT ou `@FormParam`, siga o módulo `users` do BookStorePW2.

---

## 15. Exemplos de referência (padrões obrigatórios)

Estes são os modelos que o Claude deve seguir sempre ao resolver exercícios. Eles já estão adaptados para Quarkus 3 e `jakarta.*`, mantendo o estilo do site do professor. Regras de estilo comuns a todos:

- Interfaces de Rest Client começam com `I` (`IPayment`, `IBookCatalog`, `IUserClient`).
- Recursos REST terminam em `Resource` ou `WS`, com `@Path` na classe e `@Produces`/`@Consumes` explícitos em cada método.
- Injeção com `@Inject` (e `@RestClient` para clientes remotos).
- Formulários com `@FormParam` e `MediaType.APPLICATION_FORM_URLENCODED`.
- Configuração no `application.properties`, nunca valores fixos no código quando houver propriedade.
- Explicar em português o porquê de cada anotação, no tom de aula.

### 15.1 Recurso REST (GET, POST, PUT, DELETE)

```java
package org.acme;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/produtos")
public class ProdutoResource {

    private final List<Produto> produtos = new ArrayList<>();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Produto> listar(@QueryParam("nome") String nome) {
        if (nome == null) {
            return produtos;
        }
        return produtos.stream()
                .filter(p -> p.getNome().equalsIgnoreCase(nome))
                .toList();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Produto buscar(@PathParam("id") Long id) {
        return produtos.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new jakarta.ws.rs.NotFoundException());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response criar(Produto produto) {
        produtos.add(produto);
        return Response.status(Response.Status.CREATED).entity(produto).build();
    }

    @DELETE
    @Path("/{id}")
    public Response remover(@PathParam("id") Long id) {
        produtos.removeIf(p -> p.getId().equals(id));
        return Response.noContent().build();
    }
}
```

Pontos a explicar: `@Path` da classe mais `@Path` do método formam a URI; `@Produces` define o tipo de resposta; `Response` permite escolher o código HTTP (201, 204, 404).

### 15.2 Rest Client

Interface (padrão do site, com `@RegisterRestClient` e `baseUri`):

```java
package org.acme;

import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient(baseUri = "http://localhost:9080/books")
public interface IBookCatalog {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<Book> listBooks();

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    Book getBook(@PathParam("id") Long id);

    @PUT
    @Path("/{id}/loan")
    @Produces(MediaType.APPLICATION_JSON)
    Book markAsLoaned(@PathParam("id") Long id);
}
```

Uso no recurso:

```java
@Path("/loans")
public class LoanResource {

    @Inject
    @RestClient
    IBookCatalog catalog;

    @GET
    @Path("/books")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Book> availableBooks() {
        return catalog.listBooks();
    }
}
```

Para cada serviço remoto, defina a porta no `application.properties` (`quarkus.http.port=9081`) e rode os dois serviços ao mesmo tempo.

### 15.3 JWT: gerar token, proteger endpoint e ler claims

Configuração (`application.properties`):

```properties
smallrye.jwt.sign.key.location=privateKey.pem
mp.jwt.verify.issuer=http://localhost:8080
mp.jwt.verify.publickey.location=publicKey.pem
```

Serviço `users`, com login por `@FormParam` (padrão do BookStorePW2):

```java
package org.acme;

import java.util.Set;

import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/users")
public class UserWS {

    @POST
    @Path("/getJwt")
    @PermitAll
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public String generate(@FormParam("fullName") String fullName,
                           @FormParam("email") String email) {
        return Jwt.issuer("http://localhost:8080")
                .upn(email)
                .groups(Set.of("User"))
                .claim("full_name", fullName)
                .sign();
    }
}
```

Endpoint protegido, lendo o token:

```java
package org.acme;

import org.eclipse.microprofile.jwt.JsonWebToken;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/perfil")
public class PerfilResource {

    @Inject
    JsonWebToken token;

    @GET
    @RolesAllowed("User")
    @Produces(MediaType.TEXT_PLAIN)
    public String quemSouEu() {
        return "Olá, " + token.getClaim("full_name") + " (" + token.getName() + ")";
    }
}
```

Teste com `curl`:

```bash
TOKEN=$(curl -s -X POST -d "fullName=Ana Souza" -d "email=ana@email.com" http://localhost:8080/users/getJwt)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/perfil
```

Sem token o `/perfil` deve responder 401; com role errada, 403.

### 15.4 Configuração com `@ConfigProperty`

```java
@Path("/config")
public class ConfigResource {

    @Inject
    @ConfigProperty(name = "pw2.message", defaultValue = "sem mensagem")
    String message;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String mensagem() {
        return message;
    }
}
```

```properties
pw2.message=Olá da aula
%prod.pw2.message=Mensagem de produção
```

### 15.5 Tolerância a falhas

```java
@Path("/fault")
public class FaultResource {

    @GET
    @Path("/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    @Timeout(3000)
    @Retry(maxRetries = 3, delay = 500)
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5)
    @Fallback(fallbackMethod = "recover")
    public String getName(@PathParam("name") String name) {
        if (name.equalsIgnoreCase("error")) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return name;
    }

    public String recover(String name) {
        return "Resposta alternativa (fallback)";
    }
}
```

Regra: o método de fallback tem a mesma assinatura do método original.

### 15.6 Hibernate com Panache

```java
package org.acme;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class Usuario extends PanacheEntity {
    public String name;
}
```

```java
@Path("/usuarios")
public class UsuarioResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Usuario> listar() {
        return Usuario.listAll();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Usuario buscar(@PathParam("id") Long id) {
        return Usuario.findById(id);
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Usuario criar(Usuario usuario) {
        usuario.persist();
        return usuario;
    }
}
```

`@Transactional` é obrigatório em operações que gravam. Configure o banco no `application.properties` como na seção 11.

### 15.7 Health

```java
@Liveness
public class Live implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("Serviço vivo");
    }
}
```

### 15.8 Como adicionar novos exemplos

Quando o professor passar um exemplo novo, ou quando um exercício for resolvido e aprovado, acrescente-o aqui, já em `jakarta.*`, com uma linha de explicação. Esta seção é a fonte de padrões: se um exercício pedir algo que ela cobre, siga o modelo dela em vez de inventar outro estilo.
