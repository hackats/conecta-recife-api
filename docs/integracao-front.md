# Handoff para o frontend

## Contrato e ambiente

Com a API rodando, usem o Swagger como fonte interativa de campos e respostas.

- Interface: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`

A URL local é `http://localhost:8080`. `localhost` é a máquina de quem abriu o navegador. Se a API estiver em outra máquina ou hospedagem, o frontend precisa usar uma URL HTTPS acessível.

Para desenvolvimento com o front em outra origem:

```bash
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000 mvn spring-boot:run
```

Troquem a origem pela URL real do frontend. Mais de uma origem pode ser separada por vírgula. Reiniciem a API após alterar a variável.

## Regras gerais

- Dados e IDs são simulados. Comecem a demo com `C007938` ou `C068579`.
- Não enviar CPF, nome, endereço ou documentos reais.
- Solicitações, preferências e eventos ficam em memória e somem se a API reiniciar.
- Solicitações retornam `simulada: true`. Protocolo e andamento servem à demonstração.
- Erros usam `application/problem+json`. Mostrem o campo `detail` ao usuário.
- Recomendação não confirma elegibilidade para um serviço.

## Fluxo do cidadão autenticado

1. Seleção de persona: `GET /cidadaos/{id}`.
2. Vitrine: `GET /cidadaos/{id}/recomendacoes`.
3. Card de serviço: `GET /servicos/{id}`.
4. Jornada: `GET /servicos/{id}/jornada`.
5. Checklist: `POST /servicos/{id}/jornada/progresso`.
6. Solicitação: `POST /solicitacoes`.
7. Acompanhamento: `GET /solicitacoes/{protocolo}`.

Na vitrine, mostrem `nome` e `motivo` de cada recomendação. O backend devolve até dez serviços distintos que ainda não foram acessados.

```json
{
  "servicoId": 942,
  "nome": "Agendar consultas e procedimentos na Minha Unidade de Saúde",
  "motivo": "Serviço relacionado ao seu perfil. Outros acessos seguiram de serviços que você utilizou"
}
```

## Busca

```text
GET /servicos/busca?q=quero%20marcar%20medico
```

Chamem após uma pausa curta de digitação, por exemplo 250 ms. A API normaliza acentos, usa sinônimos e aproxima pequenas diferenças de escrita. Retorna até dez serviços existentes. Uma consulta vazia ou sem resultado devolve `[]`.

Na tela, exibam “Não encontramos um serviço com esse termo. Tente outra busca.” quando a lista for vazia.

## Busca por voz e resposta falada

O recurso de voz reutiliza a busca existente. Não enviem áudio para a API e não criem um endpoint novo para isso.

1. O botão de microfone inicia o reconhecimento de fala no navegador.
2. Enquanto escuta, exibam um estado claro como “Ouvindo…” e um botão para cancelar.
3. Ao receber a transcrição, preencham o campo de busca para a pessoa revisar.
4. Chamem `GET /servicos/busca?q={transcrição codificada}` com o mesmo debounce da busca digitada.
5. Mostrem os resultados em texto sempre.
6. Disponibilizem um botão separado “Ouvir resposta” em cada resultado, ou na resposta selecionada, usando a síntese de fala do navegador.

O texto continua sendo a fonte principal da resposta. A fala é uma alternativa opcional. Não iniciem áudio automaticamente, pois isso pode surpreender a pessoa e conflitar com leitores de tela.

Para a leitura, usem uma frase curta, por exemplo: “Encontrei o serviço {nome}. {motivo}”. Não leiam URLs, identificadores internos, campos vazios ou todo o conteúdo do catálogo de uma vez.

Se o navegador não suportar reconhecimento de fala, mantenham a busca por texto normal e ocultem ou desabilitem o microfone com explicação. Se a permissão do microfone for negada, mostrem uma mensagem simples e deixem a pessoa digitar. A gravação deve durar somente enquanto o controle estiver ativo e não deve ser guardada.

O botão precisa ter rótulo acessível, por exemplo `aria-label="Buscar por voz"`. A resposta falada também precisa de botão de parar e controle de volume ou voz quando o navegador disponibilizar essas opções. Para português do Brasil, configurem `pt-BR` quando a API do navegador aceitar idioma.

## Navegação sem login

Não criem aba adicional. Usem o endpoint dentro da mesma navegação pelo catálogo.

```text
GET /anonimo/recomendacoes
GET /anonimo/recomendacoes?acessados=458,1359
```

Mantenham somente na sessão do navegador os IDs dos serviços abertos e enviem-nos em ordem, separados por vírgula. Limpem a lista em uma nova sessão. O limite é 50 IDs válidos.

A resposta contém `recomendacoes` e `transicoesDisponiveis`. Renderizem os cards como na vitrine autenticada. `transicoesDisponiveis=false` informa que o sinal de sequência não está disponível. A seleção continua usando catálogo, temas da sessão, uso semanal e diversidade. A resposta vem com `Cache-Control: no-store`, portanto não a reutilizem em outra sessão.

Não deduzam perfil, renda, direito a benefício ou elegibilidade. Um serviço pode reaparecer em uma nova sessão.

## Jornada e progresso

```text
GET /servicos/{id}/jornada
POST /servicos/{id}/jornada/progresso
Content-Type: application/json

[1, 2]
```

`/jornada` devolve `requisitos` e `passos` como listas. Renderizem cada item integralmente, com texto legível, contraste e controles de toque grandes.

O checklist recebe as posições marcadas, começando em 1. `[]` desmarca tudo. A resposta tem `checklist`, `concluidos`, `total`, `percentual`, `descricao` e `autodeclarado`.

Mostrem os checkboxes, “2 de 4 etapas marcadas por você” e uma barra de progresso com `percentual`. Atualizem o estado local após cada resposta. O progresso é autodeclarado e não confirma conclusão junto ao órgão. Não há pontos, medalhas ou recompensas.

## Solicitação e acompanhamento

```http
POST /solicitacoes
Content-Type: application/json
Idempotency-Key: uuid-gerado-pelo-front

{
  "cidadaoId": "C007938",
  "servicoId": 1
}
```

O retorno é `201`, inclui `protocolo` e o cabeçalho `Location`. Guardem o protocolo e consultem `GET /solicitacoes/{protocolo}`.

A linha do tempo é simulada: `RECEBIDA` ao criar, `EM_ANALISE` após 30 segundos e `CONCLUIDA` após 60 segundos. Usem `linhaDoTempo` como fonte da interface e indiquem que é uma simulação.

Gerem uma `Idempotency-Key` por clique de envio. Reutilizem-na somente para repetir o mesmo pedido após falha de rede. A mesma chave para outro serviço retorna 409.

## Consentimento

```text
GET /preferencias?cidadaoId=C007938
PUT /preferencias
```

```json
{
  "cidadaoId": "C007938",
  "consentimentoSaude": true,
  "aceitaNotificacao": false
}
```

`cidadaoId` e `consentimentoSaude` são obrigatórios. `aceitaNotificacao` é opcional. Depois de alterar o consentimento, carreguem as recomendações outra vez. Os motivos devem permanecer neutros e não citar diagnósticos.

## Eventos e métricas

```json
POST /eventos
{
  "eventoId": "uuid-unico-da-acao",
  "jornadaId": "uuid-da-tentativa",
  "cidadaoId": "C007938",
  "servicoId": 1,
  "etapa": "detalhes",
  "tipo": "ABRIU"
}
```

Para a mesma etapa, as sequências aceitas são:

```text
ABRIU -> INICIOU -> CONCLUIU
ABRIU -> ABANDONOU
INICIOU -> ABANDONOU
```

Usem um `jornadaId` por tentativa e outro para nova tentativa. Nomes de etapa devem ser estáveis, como `detalhes`, `documentos` e `solicitacao`. O mesmo `eventoId` com corpo idêntico é idempotente. Com dados diferentes, retorna 409.

Não bloqueiem a navegação se esse registro falhar. Tentem reenviar com o mesmo `eventoId`. `GET /metricas` devolve dados por `servicoId` e `etapa`. As taxas variam de 0 a 1. Para a tela, usem `taxaConclusao * 100` e `taxaAbandono * 100`.

## CSV administrativo

- `GET /recomendacoes.csv` gera e baixa o arquivo completo. Pode demorar e retorna um arquivo grande.
- `POST /recomendacoes/validar` recebe `multipart/form-data` no campo `arquivo`. Não definam manualmente `Content-Type`, pois o navegador cria o boundary.

O retorno da validação contém `valido`, `cidadaos`, `linhas`, `totalErros` e `erros`. CSV inválido retorna 200 com `valido: false`.

## Estados de tela

| Situação | Comportamento |
| --- | --- |
| Carregando | Skeleton ou texto de carregamento. |
| Busca vazia | Mensagem e nova tentativa. |
| 400 | Mostrar `detail`. |
| 404 | Voltar para seleção ou catálogo, mostrando `detail`. |
| 409 | Manter estado da tela, mostrar `detail` e não gerar chave nova automaticamente. |
| Falha de rede | Informar indisponibilidade e oferecer nova tentativa. |
| API reiniciada | Preferências, protocolos e métricas anteriores deixam de existir. Voltar ao fluxo inicial. |

## Conferência final do frontend

- Configurar uma variável `API_URL`.
- Configurar `APP_CORS_ALLOWED_ORIGINS` com a URL do front.
- Testar duas personas, uma pessoa sem histórico e uma busca sem resultado.
- Testar criação, repetição segura e consulta de protocolo.
- Testar consentimento ligado e desligado seguido de nova vitrine.
- Testar celular, teclado, foco visível, contraste e leitor de tela.
- Para a demo em máquinas diferentes, publicar API e front em URLs HTTPS acessíveis.
