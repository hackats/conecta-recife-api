# Hacka/TS — Conecta Recife (Etapas 1, 2 e 3)

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

## 6. Recomendações

`GET /cidadaos/{id}/recomendacoes` devolve até dez objetos com `servicoId`, `nome` e `motivo`. Cidadãos desconhecidos recebem HTTP 404. Serviços já acessados pelo cidadão são excluídos.

A linha de base da etapa 2 soma o logaritmo de 1 mais os cliques gerais, duas vezes o mesmo cálculo para a faixa etária e o mesmo cálculo para o segmento de perfil com mais cliques naquele serviço. A correspondência entre marcador e público-alvo vale 5 pontos e um tema em comum com os serviços acessados vale 4 pontos. Empates são resolvidos pelo menor identificador. Sem cliques próprios, ainda entram os sinais de popularidade e perfil.


## Verificações incluídas

`Etapa1IntegrationTest` verifica as três contagens, os dois endpoints, os erros 404 e a listagem de cliques. Execute `mvn test` para validar tudo no ambiente com Maven disponível.

## 7. Transições e avaliação temporal

A etapa 3 usa a fórmula `a * popularidade + b * perfil + c * transicao`. Popularidade agrega os logaritmos dos cliques gerais e segmentados descritos acima. Perfil soma a compatibilidade com público-alvo e temas visitados. Transição é o logaritmo de 1 mais a soma das ocorrências X→Y, considerando os serviços X já acessados pelo cidadão.

As transições usam pares consecutivos na ordem da mesma sessão e do mesmo cidadão. Repetições X→X, sessões sem identificador, ordens ausentes e lacunas de ordem são ignoradas. O ranking mantém a exclusão dos serviços usados, o limite de dez e o desempate pelo identificador. Os motivos consideram apenas componentes com peso positivo.

O hit@10 é a fração dos cidadãos elegíveis com pelo menos um serviço futuro inédito entre as dez recomendações. Cada cidadão conta uma vez. Cidadãos com apenas repetições de serviços anteriores ao corte ficam fora do denominador. Cidadãos sem histórico anterior participam quando têm um alvo futuro. Todos os índices de popularidade, perfil e transições usam exclusivamente cliques anteriores ao corte. As recomendações permanecem fixas durante cada janela de avaliação.

Execute a avaliação com os CSVs locais na pasta data.

```powershell
mvn test -Dtest=AvaliacaoDadosTest -Davaliar=true
```

O comando produz `target/avaliacao-temporal.json`, contendo apenas métricas agregadas. Por padrão, os cortes são as datas nos percentis 60 e 80 dos cliques ordenados. A primeira janela escolhe os pesos em 16 combinações, mantendo a=1, b em {0, 0.5, 1, 2} e c em {0, 1, 2, 4}. Fixar a remove a redundância de multiplicar todos os pesos pelo mesmo fator. A janela final compara a linha de base (1, 1, 0) e os pesos escolhidos, após reconstruir os índices com os acessos anteriores ao segundo corte. Empates favorecem o primeiro candidato, começando pela linha de base.

Para reproduzir cortes específicos, acrescente `-DcorteValidacao=AAAA-MM-DD -DcorteTeste=AAAA-MM-DD`. Os cortes devem produzir janelas distintas e a validação precisa conter cidadãos elegíveis. A avaliação completa é opcional na suíte cotidiana devido ao custo de comparar o catálogo para cada cidadão.

Limitação da medição — perfis e catálogo são os snapshots fornecidos. Sem versões históricas desses arquivos, a separação temporal é garantida para os cliques. O hit@10 mede antecipação de acessos aos serviços, e a conclusão de solicitações será medida na etapa de eventos.

### Resultado da etapa 3

Validação de 17/08/2026 até 05/09/2026, com treino anterior a 17/08. Teste de 06/09/2026 até 27/09/2026, com treino anterior a 06/09. A API foi configurada com a=1, b=0.5 e c=4, escolhidos na validação. Os valores podem ser substituídos pelas propriedades `app.recomendacao.a`, `app.recomendacao.b` e `app.recomendacao.c`.

| Versão no teste final | Cidadãos elegíveis | Acertos | hit@10 |
| --- | --- | --- | --- |
| Linha de base (1, 1, 0) | 27.897 | 16.446 | 58,9526% |
| Pesos escolhidos (1, 0.5, 4) | 27.897 | 16.806 | 60,2430% |

O aumento observado foi de 1,2905 ponto percentual, com 360 cidadãos adicionais recebendo ao menos uma sugestão posteriormente acessada. O teste teve 40.762 cidadãos com acessos futuros, dos quais 12.865 ficaram fora do denominador por acessarem apenas serviços já usados. O relatório agregado da execução está em `docs/avaliacao-temporal.json`. Os CSVs originais permanecem locais.
