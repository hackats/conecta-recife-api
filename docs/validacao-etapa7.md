# Validação local de 12/11/2026

Código validado em `551f5a2`, após integração por fast-forward da etapa 5 (`2bd0943`) e etapa 6 (`70edba8`). O PR 1 consta como merged no GitHub. Os commits de mentoria são `faf6946`, `c602e9e` e `551f5a2`.

## Dados e testes

Os CSVs locais foram convertidos das abas da planilha oficial `dados_em_planilha.xlsx` da pasta fornecida pela organização. A carga confirmou 89.046 cidadãos, 843 serviços e 593.056 cliques com integridade referencial. A aba uso_semanal contém 11.587 registros. Os arquivos permanecem fora do Git.

`mvn package -Dexportar=true` terminou com 44 testes, zero falhas, zero erros e um teste desativado. O teste desativado é a avaliação temporal hit@10 opcional. Foram verificadas as oito personas oficiais, cem IDs aleatórios, ausência de histórico, erros de entrada, consentimento, eventos, solicitações, busca, recomendações anônimas e checklist.

O gerador e validador confirmaram `recomendacoes.csv` com 89.046 cidadãos, 890.460 linhas e zero erros. Uma conferência independente em Python verificou UTF-8 estrito, cabeçalho, cobertura exata dos cidadãos, posições consecutivas, até dez serviços distintos e existentes por cidadão.

A medição sequencial MockMvc com cem amostras, semente 2026 e dez aquecimentos registrou mediana de 1,585348 ms, p95 de 2,702197 ms e máximo de 21,842153 ms. Inclui verificações dos resultados. Não foi realizado teste de carga concorrente ou medição de rede nesta etapa.

O JAR foi iniciado em 127.0.0.1:18080 e foram conferidos por HTTP os endpoints de recomendações autenticadas, busca, recomendações anônimas e progresso. A API respondeu com sucesso.

## Limites do congelamento

A versão do backend está empacotada e testada. A entrega completa continua com pendências verificáveis.

- `visitas_sem_login.csv` não foi encontrado na pasta compartilhada nem na busca do Drive. As transições foram testadas com dados sintéticos. A validação de qualidade, carga e memória com as 3.563.242 visitas oficiais depende desse arquivo. A API informa `transicoesDisponiveis=false` enquanto ele estiver ausente.
- A interface Kodiak está fora deste repositório. A validação visual, acessibilidade no celular e integração ponta a ponta dependem do frontend.
- A publicação externa permanece pendente de ambiente e credenciais. A execução HTTP verificada foi local.
- Os números de hit@10 existentes no README são de execução anterior. Esta etapa não realizou nova medição dessa métrica.

O roteiro e o plano B estão em `pitch-etapa7.md`. Os contratos das melhorias estão em `mentoria.md`.
