# Roteiro de apresentação

## Abertura — 20 segundos

“Encontrar um serviço público é só o começo. A pessoa também precisa entender os requisitos, saber o próximo passo e acompanhar o pedido. Nossa proposta organiza essa jornada no Conecta Recife.”

## Demonstração — 80 segundos

Escolha duas personas oficiais, como C007938 e C068579. Mostre as recomendações e seus motivos. Compare os resultados e consulte uma jornada. Na integração com a interface, marque uma etapa do checklist e mostre o texto do progresso.

Busque “quero marcar medico”. Explique que a busca consulta o catálogo existente. Abra a vitrine anônima e passe serviços acessados na sessão. Mostre o indicador de disponibilidade das transições. Na ausência do arquivo oficial, informe essa limitação.

Crie uma solicitação com dados simulados, mostre o protocolo e consulte o andamento. Explique que o andamento é simulado. Mostre a preferência de consentimento e sua revogação.

## Decisões e medição — 40 segundos

“O contexto da sessão ajuda quem chega sem login. A diversidade e a mediana semanal reduzem a influência de picos isolados. A busca aceita linguagem cotidiana. O checklist deixa visível o que a pessoa marcou como concluído.”

Apresente cobertura e consistência do CSV, latência medida localmente e instrumentação de abertura, início, conclusão e abandono. Use os números de `validacao-etapa7.md`. O hit@10 histórico do README corresponde à execução anterior documentada, sem nova medição nesta etapa.

## Encerramento — 20 segundos

“A próxima validação é com pessoas usando a interface no celular, medindo conclusão, abandono por etapa e satisfação. A integração real com os órgãos permitirá confirmar a conclusão do serviço.”

## Plano B e publicação

Mantenha Java 21, JAR empacotado, dados locais e chamadas HTTP salvas no computador da apresentação. Execute `java -jar target/conecta-recife-api-0.0.1-SNAPSHOT.jar --server.address=127.0.0.1`. Os dados precisam estar na pasta `data` relativa ao diretório de execução. O JAR pronto funciona sem Maven ou downloads de dependências.

Se o frontend estiver indisponível, demonstre as chamadas da API com um cliente HTTP e explique o limite dessa demonstração. Prepare capturas reais após ensaiar. A demonstração móvel depende da interface.

Para publicação externa, defina a hospedagem, carregue os dados por canal privado, configure a origem exata do frontend em `APP_CORS_ALLOWED_ORIGINS`, use HTTPS e valide as chamadas no ambiente final. Os endpoints do protótipo usam IDs simulados sem autenticação. Solicitações, preferências e eventos ficam em memória. Apague os dados ao encerrar o evento conforme o tutorial.
