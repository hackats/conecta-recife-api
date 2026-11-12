# Integração do front Kodiak

O front chama a API por HTTP. Use a URL base do ambiente onde a API estiver rodando. A URL local padrão é http://localhost:8080. O navegador precisa alcançar a API; localhost corresponde ao computador de quem abriu o front.

Quando houver uma origem publicada do Kodiak, configure APP_CORS_ALLOWED_ORIGINS com a origem exata, incluindo protocolo e porta quando houver. Separe múltiplas origens por vírgula. Reinicie a API após alterar a variável. A configuração padrão permite chamadas de mesma origem. A configuração CORS foi testada com preflight OPTIONS.

## Fluxo das telas

| Ação | Método e caminho |
| --- | --- |
| Consultar cidadão | GET /cidadaos/{id} |
| Mostrar recomendações | GET /cidadaos/{id}/recomendacoes |
| Abrir jornada | GET /servicos/{id}/jornada |
| Consultar preferências | GET /preferencias?cidadaoId={id} |
| Salvar consentimento | PUT /preferencias |
| Criar protocolo simulado | POST /solicitacoes |
| Acompanhar protocolo | GET /solicitacoes/{protocolo} |
| Registrar interação | POST /eventos |
| Consultar métricas | GET /metricas |
| Baixar CSV completo | GET /recomendacoes.csv |
| Validar CSV | POST /recomendacoes/validar |

Envie Content-Type application/json nos POST e PUT com JSON. Para validar um CSV, use multipart/form-data com o campo arquivo. Deixe o cliente HTTP gerar o boundary do multipart.

## Eventos e métricas

Exemplo de corpo para POST /eventos.

```json
{
  "eventoId": "uuid-unico-do-evento",
  "jornadaId": "uuid-da-tentativa",
  "cidadaoId": "C000001",
  "servicoId": 1,
  "etapa": "documentos",
  "tipo": "ABRIU"
}
```

Crie eventoId uma vez por ação e reutilize esse valor e o mesmo corpo quando repetir uma chamada que falhou. A API devolve o evento original em reenvios idênticos. O mesmo eventoId com dados diferentes recebe 409.

Mantenha jornadaId durante uma tentativa. A sequência por cidadão, serviço, jornada e etapa é ABRIU → INICIOU → CONCLUIU. ABANDONOU pode ocorrer depois de ABRIU ou INICIOU. Uma nova tentativa precisa de outro jornadaId. Use nomes estáveis de etapa nas telas, como detalhes, documentos e solicitacao. Cada etapa começa com ABRIU. A sequência inválida recebe 409.

O retorno contém dados e registradoEm. GET /metricas devolve uma lista agrupada por serviço e etapa. abriu e iniciou contam jornadas distintas. concluiu e abandonou contam os estados terminais. emAndamento conta as jornadas abertas sem estado terminal. taxaConclusao = concluiu / abriu; taxaAbandono = abandonou / abriu. As taxas variam de 0 a 1. A lista fica vazia enquanto não houver jornadas.

Abandono precisa ser registrado explicitamente pelo front. Fechar o navegador sem enviar o evento mantém a jornada em andamento. O estado CONCLUIDA do protocolo simulado e o evento CONCLUIU são fluxos independentes; o front registra o evento ao observar a conclusão da ação. Eventos e métricas ficam em memória e reiniciam com a API.

## CSV de recomendações

O download gera um arquivo UTF-8 no formato RFC 4180, com cabeçalho cidadao_id,posicao,servico_id,motivo. Há uma linha por recomendação, ordenada por cidadão e posição. O arquivo completo pode levar alguns minutos para ser gerado. Configure o timeout do cliente conforme essa operação.

O validador verifica UTF-8, cabeçalho, quatro colunas, cidadãos e serviços existentes, cobertura de todos os cidadãos carregados, no máximo dez recomendações por pessoa, serviços distintos, posições consecutivas de 1 a 10; o motivo pode ficar vazio, conforme o guia. O retorno inclui valido, cidadaos, linhas, totalErros e até vinte mensagens de erro. Um arquivo inválido retorna 200 com valido=false. O limite de upload é 300 MB.

A geração usa o recomendador e as preferências atuais, escreve em arquivo temporário e valida antes de disponibilizar o resultado. Uma falha preserva o arquivo anterior. O arquivo gerado permanece local e está excluído do Git.

## Situação da integração

Os endpoints, os contratos HTTP e o CORS foram testados na API. O front no Kodiak ainda está em construção. A URL do front e o teste ponta a ponta permanecem pendentes.

A API continua como protótipo em memória com IDs simulados e sem autenticação de cidadãos. As solicitações são simuladas. As respostas de erro de negócio usam status 400, 404 ou 409 e incluem detail para apresentação ao usuário.
