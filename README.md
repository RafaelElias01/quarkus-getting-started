# quarkus-getting-started

Repositório: https://github.com/RafaelElias01/quarkus-getting-started

## Exercícios de Health Checks

Pasta do código: [src/main/java/org/acme/](src/main/java/org/acme/)

| # | Exercício | Onde está | O que faz |
|---|---|---|---|
| 1 | Liveness simples | [Live.java](src/main/java/org/acme/Live.java) | Sempre responde `UP` com a mensagem `alive`. Teste em `/q/health/live` |
| 2 | Simulando falha | [ReadyDown.java](src/main/java/org/acme/ReadyDown.java) | Sempre responde `DOWN`. Por isso `/q/health/ready` retorna 503 |
| 3 | Informação extra | [Ready.java](src/main/java/org/acme/Ready.java) | Responde `UP` com `version: 1.0.0`. Aparece em `/q/health` |
| 4 | Interface web | Não tem código | Rode em modo dev e abra `/q/health-ui/`. Os checks `UP` e `DOWN` aparecem na tela |
| 5 | Depende de outro serviço | [ReadyCatalog.java](src/main/java/org/acme/ReadyCatalog.java) | Chama o serviço de usuários (`IUserClient`). `UP` se responder, `DOWN` se falhar |

A extensão de health está no [pom.xml](pom.xml): `quarkus-smallrye-health`.

## Como testar

Rodar em modo desenvolvimento:

```shell script
./mvnw compile quarkus:dev
```

Endereços:
- Liveness: http://localhost:8080/q/health/live
- Readiness: http://localhost:8080/q/health/ready
- Todos os checks: http://localhost:8080/q/health
- Interface web (só em modo dev): http://localhost:8080/q/health-ui/

## Respostas de cada exercício

Exercício 1 (`/q/health/live`, HTTP 200):

```json
{"status":"UP","checks":[{"name":"alive","status":"UP"}]}
```

Exercício 2 (`/q/health/ready`, HTTP 503):

```json
{"status":"DOWN","checks":[{"name":"ready","status":"UP","data":{"version":"1.0.0"}},{"name":"ready-down","status":"DOWN"},{"name":"catalog-service","status":"UP"}]}
```

Exercícios 3 e 4 (`/q/health`, HTTP 503, com todos os checks):

```json
{"status":"DOWN","checks":[{"name":"alive","status":"UP"},{"name":"ready","status":"UP","data":{"version":"1.0.0"}},{"name":"ready-down","status":"DOWN"},{"name":"catalog-service","status":"UP"}]}
```

Exercício 5 com o serviço fora do ar (`/q/health/ready`, HTTP 503):

```json
{"status":"DOWN","checks":[{"name":"catalog-service","status":"DOWN"},{"name":"ready","status":"UP","data":{"version":"1.0.0"}},{"name":"ready-down","status":"DOWN"}]}
```

Exercício 5 com o serviço no ar: o `catalog-service` aparece `UP` no mesmo endpoint, como no exemplo acima.

## Testar o exercício 5 com o serviço fora do ar

```powershell
.\mvnw quarkus:dev "-Dorg.acme.IUserClient/mp-rest/url=http://localhost:9999"
```

Abra http://localhost:8080/q/health/ready e veja a linha `catalog-service`: ela deve estar `DOWN`.
