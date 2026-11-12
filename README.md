# Hacka/TS — Conecta Recife (Etapas 1 a 5)

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

## 8. Jornada, solicitações e consentimento

`GET /servicos/{id}/jornada` retorna `servicoId`, `nome`, `requisitos` e `passos`. Os dois últimos campos são listas derivadas do catálogo, com HTML removido, marcadores de lista limpos e substituições simples de vocabulário. Condições e prazos são preservados. Quando o campo está vazio, a resposta orienta consultar o órgão responsável. IDs desconhecidos recebem 404.

`POST /solicitacoes` recebe o corpo abaixo e responde 201, com protocolo e cabeçalho `Location`.

```json
{"cidadaoId":"C000001","servicoId":1}
```

`GET /solicitacoes/{protocolo}` consulta o andamento. A resposta inclui `simulada: true` e a linha do tempo das etapas já ocorridas. A simulação começa em RECEBIDA, passa a EM_ANALISE após 30 segundos e a CONCLUIDA após 60 segundos. As datas são instantes UTC. O protocolo pertence à simulação local.

O cabeçalho opcional `Idempotency-Key` permite reenviar o pedido e recuperar o mesmo protocolo, inclusive em chamadas simultâneas. A chave tem escopo por cidadão. Reutilizar a chave para outro serviço recebe 409. Sem chave, cada POST cria uma solicitação. Campos obrigatórios ausentes recebem 400; cidadão, serviço ou protocolo desconhecido recebe 404.

`GET /preferencias?cidadaoId=C000001` consulta as preferências. `PUT /preferencias` altera o consentimento.

```json
{
  "cidadaoId": "C000001",
  "consentimentoSaude": true,
  "aceitaNotificacao": false
}
```

`cidadaoId` e `consentimentoSaude` são obrigatórios no PUT. `aceitaNotificacao` é opcional e preserva o valor atual quando omitido. O consentimento de saúde começa desligado para todos. A preferência de notificações começa no valor do CSV e pode ser alterada de forma independente.

Com consentimento, os marcadores diabetes, hipertensão, saúde mental, gestação e deficiência podem acrescentar 5 pontos ao componente de perfil quando o nome, público-alvo ou palavras-chave do serviço correspondem ao marcador. A regra usa termos explícitos do catálogo. O motivo desse sinal é sempre “Serviço de saúde para o seu acompanhamento”. A revogação vale para a próxima chamada de recomendação. Gestação e deficiência foram retiradas dos segmentos gerais para respeitar essa opção. O histórico de cliques e a popularidade continuam sendo sinais próprios do recomendador.

Solicitações, chaves de repetição e preferências ficam em memória e são perdidas ao reiniciar a API. Os endpoints usam os IDs simulados e ainda não possuem autenticação de cidadãos. Essa entrega serve ao protótipo local do hackathon.

Os pesos da etapa 3 foram mantidos. O relatório temporal e os percentuais da seção 7 documentam aquela versão, anterior ao controle de consentimento. A etapa 4 valida o comportamento do consentimento com testes determinísticos.

`Etapa4Test` cobre consentimento e revogação, motivos neutros, concorrência, idempotência, andamento simulado e tratamento da jornada. `Etapa4IntegrationTest` exercita os endpoints e os erros de entrada. Execute `mvn test` para a suíte cotidiana.

## 9. Eventos, métricas e exportação

A etapa 5 implementa POST /eventos, GET /metricas, GET /recomendacoes.csv e POST /recomendacoes/validar. Os contratos, exemplos e configuração de CORS para o Kodiak estão em [Integração do front](docs/integracao-front.md).

Para gerar e validar o arquivo completo localmente durante os testes, execute o comando abaixo.

```powershell
mvn test -Dexportar=true
```

O resultado fica em target/recomendacoes.csv. A execução da etapa 5 gerou 890.460 recomendações para os 89.046 cidadãos, com zero erros de validação. A suíte teve 28 testes aprovados e uma avaliação temporal opcional desativada. O CSV original e o CSV gerado ficam fora do Git.

A integração HTTP está preparada e testada no backend. O front Kodiak e sua URL ainda estão pendentes, portanto o teste ponta a ponta será realizado quando a interface estiver disponível.

## 10. Verificação e demo

A etapa 6 acrescenta testes para cem IDs aleatórios, cidadão sem histórico, campo de crianças vazio e erros de entrada. O roteiro de execução local, a medição de desempenho e as pendências das personas oficiais estão em [Demo](docs/demo.md).

O JAR foi empacotado e validado por uma chamada HTTP real. Execute scripts/iniciar-demo.ps1 para iniciar a API com os CSVs locais.

## Melhorias da mentoria e validação final

A integração das etapas 5 e 6 e as melhorias de sessão anônima, busca e checklist estão na main. Consulte [contratos da mentoria](docs/mentoria.md), [validação local da etapa 7](docs/validacao-etapa7.md) e [roteiro de apresentação e plano B](docs/pitch-etapa7.md). O relatório da etapa 7 registra os resultados atuais e as pendências de visitas anônimas, frontend e publicação.

## Swagger e OpenAPI

Com a aplicação em execução, abra `http://localhost:8080/swagger-ui/index.html`. Os contratos também estão em `http://localhost:8080/v3/api-docs` (JSON) e `/v3/api-docs.yaml` (YAML). Ajuste a porta se iniciar com `--server.port`.

Expanda uma operação, clique em **Try it out**, preencha os parâmetros ou use o exemplo do corpo e clique em **Execute**. Para começar, consulte `/cidadaos/{id}` com `C007938` e `/servicos/{id}` com `1`. Solicitações, consentimento e eventos executados pelo Swagger alteram o estado em memória da API. Para validar um CSV, selecione o arquivo no campo `arquivo` do endpoint `/recomendacoes/validar`. A geração completa em `/recomendacoes.csv` pode demorar e produzir um arquivo grande.

A documentação usa springdoc-openapi 2.10.0, da linha compatível com Spring Boot 3.5, conforme a [matriz oficial](https://springdoc.org/v2/). Os esquemas são gerados a partir dos tipos Java e os testes conferem a cobertura dos endpoints, exemplos, respostas e carregamento da interface.

A entrega usa os arquivos efetivamente disponíveis no Drive. A recomendação sem login serve ao fluxo existente do aplicativo, sem exigir uma aba adicional. O arquivo de visitas anônimas citado no tutorial é uma entrada opcional do carregador. Sua ausência não bloqueia a aplicação nem a entrega baseada em catálogo, contexto da sessão e uso semanal. `transicoesDisponiveis=false` informa a disponibilidade desse sinal específico.
