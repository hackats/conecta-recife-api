# Execução e verificação da demo

A execução local no computador da equipe é a opção preparada para demonstrar a API, utilizando o equipamento existente. Ela evita contratar hospedagem para a BBTS. A publicação em infraestrutura externa permanece pendente de definição de ambiente e acesso.

## Antes da apresentação

1. Mantenha os três CSVs originais na pasta data.
2. Gere o JAR com mvn package. A primeira execução precisa de internet para baixar dependências.
3. Execute .\scripts\iniciar-demo.ps1 em um terminal na raiz do projeto. O script aceita -Porta 8081 se necessário.
4. Aguarde o log DADOS CARREGADOS COM SUCESSO antes de abrir a demo.
5. Consulte http://localhost:8080/cidadaos/C000001/recomendacoes e confirme dez recomendações.

O JAR empacotado foi iniciado em 127.0.0.1:18080 e respondeu via HTTP real com dez recomendações. A carga registrou os totais de 89.046 cidadãos, 843 serviços e 593.056 cliques. O processo de teste foi encerrado depois da verificação.

Com o JAR e os CSVs presentes, a API local pode ser iniciada sem baixar dependências. Para o plano B, mantenha o JAR pronto, os dados locais e um cliente HTTP com as chamadas do documento integracao-front.md. Inicie a API antes do pitch.

## Comunicação com o Kodiak

O front precisa alcançar a URL base da API. localhost aponta para o computador que executa a chamada. Uma chamada executada nos servidores do Kodiak exige um endereço acessível por esses servidores. Uma página HTTPS também pode exigir uma API HTTPS conforme as restrições do navegador.

Quando a forma de execução do Kodiak e sua origem forem conhecidas, configure APP_CORS_ALLOWED_ORIGINS e valide a comunicação no ambiente final. O front e essa verificação ponta a ponta ainda estão pendentes.

## Resultado dos testes da etapa 6

Foram executados 30 testes com sucesso. A avaliação hit@10 completa e a exportação completa são opcionais e ficaram desativadas nesta rodada; suas execuções anteriores estão documentadas nas etapas 3 e 5.

O teste de recomendações seleciona cem cidadãos com semente 2026, valida dez resultados distintos, serviços existentes, motivos preenchidos e exclusão dos serviços usados. Também exercita cidadãos sem histórico, criancas_na_familia vazio, IDs inexistentes, JSON malformado e parâmetros ausentes.

Após dez aquecimentos, a mediana foi 6,6653 ms, o p95 20,1995 ms e o máximo 66,9635 ms. A medição usa MockMvc local, consultas sequenciais e inclui as verificações. Ela não representa latência de rede nem teste de carga concorrente. O relatório agregado está em desempenho-etapa6.json.

## Pendências explícitas

- As oito personas oficiais do guia ainda precisam de IDs ou descrições. O link do Drive não ficou acessível pela ferramenta durante esta etapa. Os testes com essas personas não foram realizados.
- O front Kodiak ainda está em construção.
- A hospedagem externa e sua URL HTTPS não foram configuradas. A execução local foi empacotada e verificada.
- Preferências, solicitações e eventos ficam em memória e são perdidos ao reiniciar. A API permanece sem autenticação para os IDs simulados.

A branch da etapa 6 parte da etapa 5. O pull request deve ser revisado após ou junto da etapa 5, preservando essa dependência.
