# Melhorias da mentoria

## Recomendações sem login

`GET /anonimo/recomendacoes?acessados=458,1359` recebe até 50 IDs válidos, na ordem em que foram acessados na sessão atual. O cliente deve limpar o contexto ao iniciar uma nova sessão. Os serviços podem reaparecer em sessões futuras conforme o contexto. A elegibilidade é conferida pelas regras do serviço. A resposta contém `transicoesDisponiveis` e até dez recomendações distintas do catálogo, excluindo serviços já acessados.

As transições vêm de pares consecutivos de `visitas_sem_login.csv`, agrupados por visitante e sessão e ordenados por `ordem_na_sessao`. A última escolha da sessão fornece o sinal de transição. Os temas acessados fornecem contexto explícito. A popularidade usa o logaritmo da mediana inferior de visitantes das semanas observadas com `login=nao`, com contribuição limitada a um ponto. Semanas ausentes permanecem sem observação. A seleção limita a dois serviços por tema principal, podendo retornar menos de dez.

Os agregados são carregados uma vez na inicialização. O cache em memória reduz leituras. As respostas individuais usam `Cache-Control: no-store` para evitar reutilização por caches HTTP entre sessões. Os pesos, a mediana e a diversidade determinam o ranking. Nenhum perfil ou elegibilidade é inferido de visitantes. Sem a base de visitas, `transicoesDisponiveis=false` deixa explícito o uso apenas do catálogo, temas da sessão e agregados semanais disponíveis.

O teste sintético verifica ordem de sessão, prioridade do contexto diante de pico de popularidade, exclusão e IDs inválidos. A carga e qualidade com as 3.563.242 visitas oficiais permanecem pendentes do arquivo. O carregador agrupa sessões em memória, portanto o consumo de memória também precisa ser medido com essa base.

## Busca

`GET /servicos/busca?q=quero%20marcar%20medico` retorna até dez objetos de serviços existentes. Normalização de acentos, palavras-chave, sinônimos explícitos e distância de uma edição para palavras de quatro ou mais caracteres compõem o ranking. Consultas vazias ou sem correspondência retornam lista vazia. O limite é 300 caracteres. Não há dependência de IA generativa.

## Checklist

`POST /servicos/{id}/jornada/progresso` recebe um array das posições concluídas, começando em 1, como `[1,2]`. `[]` desmarca todas. A resposta contém checklist, total, quantidade marcada, percentual e descrição textual. A operação é determinística e aceita repetição. Posições repetidas ou inexistentes recebem 400.

O progresso é autodeclarado, com `autodeclarado=true`. A confirmação oficial depende do órgão. O cliente mantém a lista de posições por jornada, sem compartilhar progresso entre pessoas. Serviços sem passos informados têm total zero e orientação para consultar o órgão.

Na integração visual, use checkboxes com rótulos completos, foco visível e navegação por teclado. Apresente a descrição textual junto ao progresso, com valores acessíveis de mínimo 0, máximo 100 e valor atual. Este repositório contém a API. A integração visual no Kodiak depende do frontend.

A revisão adicional testa um pico isolado em uma amostra de duas semanas, o retorno de serviços em uma nova sessão e a independência entre sessões.

## Escopo confirmado pela equipe

A equipe confirmou que a base da entrega é a planilha disponível no Drive. O CSV de visitas anônimas citado no tutorial é opcional e não constitui requisito pendente da entrega. O carregador mantém suporte a esse arquivo, caso ele venha a existir. A resposta informa a disponibilidade das transições. O endpoint sem login é consumido pelo fluxo existente do app, sem criação de aba ou modo adicional.

Os contratos interativos de todos os endpoints estão em `/swagger-ui/index.html`, com a especificação em `/v3/api-docs`.
