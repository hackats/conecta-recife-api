# Hacka/TS — Conecta Recife (Etapa 1)

Backend **Java 21 + Spring Boot 3.5.7**, Maven, com três `record` (`Cidadao`, `Servico`, `Clique`), leitura robusta dos CSVs e consultas REST.

## 1. Onde extrair

Extraia **o conteúdo** deste ZIP dentro da pasta `Downloads/conecta-recife` (ou trabalhe numa pasta nova para não sobrescrever código existente). A estrutura deve ficar assim:

```text
conecta-recife/
├── pom.xml
├── data/
│   ├── cidadaos.csv
│   ├── servicos.csv
│   └── historico.csv
├── src/main/java/br/com/conectarecife/
│   ├── ConectaRecifeApplication.java
│   ├── model/               # records Java
│   ├── loader/              # leitura CSV + startup + logs
│   ├── data/                # índices em memória
│   ├── controller/          # endpoints REST
│   └── exception/           # erros HTTP legíveis
└── src/test/java/br/com/conectarecife/
```

Os CSVs incluídos são os dados simulados fornecidos para o desafio. Não publique os CSVs em repositório público e remova-os ao final do evento, conforme orientações do guia.

## 2. Requisitos

- JDK 21 (ou superior compatível)
- Maven 3.9+
- Conexão de internet **na primeira execução** para o Maven baixar as dependências

No terminal aberto **na raiz do projeto**, execute:

```bash
mvn test
mvn spring-boot:run
```

Em seguida, abra as URLs (em outra aba/terminal):

```text
http://localhost:8080/cidadaos/C000001
http://localhost:8080/servicos/1
http://localhost:8080/cidadaos/C000001/cliques
```

Para gerar o `.jar`:

```bash
mvn clean package
java -jar target/conecta-recife-api-0.0.1-SNAPSHOT.jar
```

## 3. O que acontece quando a aplicação inicia

1. Lê `data/cidadaos.csv`, `data/servicos.csv` e `data/historico.csv` com Apache Commons CSV (UTF-8, aspas, vírgulas e campos multilinha).
2. Converte os registros em objetos `record` Java.
3. Indexa os cidadãos em `Map<String, Cidadao>`, os serviços em `Map<Integer, Servico>` e os cliques por cidadão.
4. Confere **89.046 cidadãos**, **843 serviços** e **593.056 cliques**; recusa inicializar se um total estiver incorreto.
5. Confere que todos os cliques apontam para um cidadão e serviço existente.
6. Registra no log a duração de cada carga, da indexação e o tempo total.

Os dados ficam em memória durante a execução (sem PostgreSQL nesta etapa).

## 4. Endpoints disponíveis

| Método | Rota | Comportamento |
| --- | --- | --- |
| GET | `/cidadaos/{id}` | Retorna o cidadão simulado em JSON |
| GET | `/servicos/{id}` | Retorna o serviço em JSON |
| GET | `/cidadaos/{id}/cliques` | Retorna os cliques do cidadão (extra útil para a próxima etapa) |

Exemplos de erros:

- `GET /cidadaos/C999999` → HTTP 404, detalhe `Cidadão não encontrado: C999999`.
- `GET /servicos/999999` → HTTP 404, detalhe `Serviço não encontrado: 999999`.
- `GET /servicos/abc` → HTTP 400, identificador inválido.

## 5. Configurações

Em `src/main/resources/application.properties`:

```properties
spring.application.name=conecta-recife-api
app.data.directory=./data
app.data.expected-cidadaos=89046
app.data.expected-servicos=843
app.data.expected-cliques=593056
server.port=8080
```

**Importante:** inicie o Maven sempre na pasta que contém o `pom.xml` e a pasta `data`. No IntelliJ, configure o *Working directory* do Run/Debug para a raiz do projeto se necessário.

## 6. Próxima etapa

O recomendador usará os `Map`s existentes, os cliques por cidadão e as listas `temas` e `publicosAlvo` para construir o top 10 personalizado. Não há recomendador ainda: esta entrega é somente a **etapa 1**.

## Verificações incluídas

`Etapa1IntegrationTest` verifica as três contagens, os dois endpoints, os erros 404 e a listagem de cliques. Execute `mvn test` para validar tudo no ambiente com Maven disponível.
