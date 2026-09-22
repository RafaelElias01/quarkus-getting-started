# Como rodar o projeto nesta rede (proxy corporativo)

## Bugs corrigidos no REST Client

1. **`@RegisterRestClient` na classe errada** — estava em `UserResource` (deveria
   estar só na interface `IUserClient`). Causava falha no registro do bean.
2. **Métodos do Resource sem `public`** — `listLocalUsers()` (UserResource) e
   `getLocalPost()` (PostResource) eram package-private. O Quarkus REST ignora
   silenciosamente métodos não-públicos em classes de recurso, então os
   endpoints `/local-users` e `/local-posts/{id}` davam 404 mesmo com tudo
   aparentemente certo. Corrigido adicionando `public` nos dois.
3. **Proxy corporativo obrigatório para sair para a internet** — esta rede
   exige o proxy `proxymwg.ses.reders:3128` autenticado. O `curl` já usa isso
   via `~/.curlrc`, mas o Quarkus não lê essa configuração sozinho. Foi
   adicionado em `application.properties`:

   ```properties
   quarkus.rest-client."org.acme.IUserClient".proxy-address=proxymwg.ses.reders:3128
   quarkus.rest-client."org.acme.IUserClient".proxy-user=${PROXY_USER:}
   quarkus.rest-client."org.acme.IUserClient".proxy-password=${PROXY_PASSWORD:}
   quarkus.rest-client."org.acme.IPostClient".proxy-address=proxymwg.ses.reders:3128
   quarkus.rest-client."org.acme.IPostClient".proxy-user=${PROXY_USER:}
   quarkus.rest-client."org.acme.IPostClient".proxy-password=${PROXY_PASSWORD:}
   ```

   A senha **não fica no arquivo** — vem das variáveis de ambiente
   `PROXY_USER` e `PROXY_PASSWORD`, que precisam existir antes de subir o
   `quarkus:dev`.

## Como rodar (PowerShell)

Você **não precisa digitar a senha de novo** — o Windows já tem
`$env:HTTPS_PROXY` configurado com usuário e senha embutidos
(`http://usuario:senha@proxymwg.ses.reders:3128`). Basta extrair as duas
partes dessa variável e repassar como `PROXY_USER`/`PROXY_PASSWORD` antes de
subir o `quarkus:dev`:

```powershell
$uri = [uri]$env:HTTPS_PROXY
$env:PROXY_USER = $uri.UserInfo.Split(':')[0]
$env:PROXY_PASSWORD = [uri]::UnescapeDataString($uri.UserInfo.Split(':')[1])
& "C:\Users\rafael-ioppi\apache-maven-3.9.9\bin\mvn.cmd" quarkus:dev
```

**Não use `.\mvnw.cmd`** (o Maven Wrapper) — ele tenta baixar sua própria
cópia do Maven pela internet usando o downloader nativo do .NET
(`WebClient.DownloadFile`), que **não lê** `$env:HTTPS_PROXY`/`HTTP_PROXY`
(só o Invoke-WebRequest e ferramentas .NET mais novas leem isso
automaticamente). Resultado: erro 407 (Autenticação de Proxy Obrigatória) ao
tentar baixar o Maven, mesmo com o proxy configurado corretamente para tudo
mais. Como você já tem o Maven instalado em
`C:\Users\rafael-ioppi\apache-maven-3.9.9`, é mais simples chamá-lo
diretamente, pulando o wrapper.

Isso precisa ser feito de novo toda vez que abrir um terminal novo (a
variável `PROXY_USER`/`PROXY_PASSWORD` não persiste entre sessões — mas como
ela é extraída do `HTTPS_PROXY` que o Windows já define, você não precisa
saber ou digitar a senha manualmente).

## Aviso de segurança

O `HTTP_PROXY`/`HTTPS_PROXY` do sistema (visível com `$env:HTTPS_PROXY` ou
`env | grep -i proxy` no Git Bash) contém a senha do usuário de domínio em
**texto puro**. Isso é um risco: qualquer processo ou script no computador
pode ler essa variável. Se a rede/proxy suportar autenticação integrada do
Windows (NTLM/Kerberos), vale trocar para esse método em vez de embutir a
senha em variável de ambiente.
