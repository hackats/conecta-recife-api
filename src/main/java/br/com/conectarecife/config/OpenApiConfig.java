package br.com.conectarecife.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Map;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI documentacao() {
        return new OpenAPI().info(new Info().title("Conecta Recife — Hacka/TS")
                .version("1.0.0")
                .description("Protótipo com dados simulados. Use os IDs do catálogo e dos cidadãos carregados. "
                        + "Solicitações e andamento são simulados. Preferências, eventos e solicitações ficam em memória. "
                        + "Os endpoints de cidadãos usam IDs simulados sem autenticação. "
                        + "Recomendações sem login atendem à navegação existente do app, sem aba ou modo adicional. "
                        + "A busca, a jornada e o progresso também podem ser usados antes do login."));
    }

    @Bean
    public OpenApiCustomizer exemplosEContratos() {
        return api -> {
            api.getPaths().forEach((path, item) -> item.readOperations().forEach(operation -> {
                if (!path.equals("/metricas") && !path.equals("/recomendacoes.csv")) {
                    operation.getResponses().addApiResponse("400", erro("Entrada inválida"));
                }
                if (path.contains("{id}") || path.startsWith("/solicitacoes")
                        || path.equals("/preferencias") || path.equals("/eventos")) {
                    operation.getResponses().addApiResponse("404", erro("Cidadão, serviço ou protocolo inexistente"));
                }
            }));
            var solicitacao = api.getPaths().get("/solicitacoes").getPost();
            var sucesso = solicitacao.getResponses().remove("200");
            if (sucesso != null) solicitacao.getResponses().addApiResponse("201", sucesso.description("Protocolo criado; Location indica a consulta"));
            solicitacao.getResponses().addApiResponse("409", erro("Chave de idempotência reutilizada para outro serviço"));
            api.getPaths().get("/eventos").getPost().getResponses()
                    .addApiResponse("409", erro("Evento divergente ou sequência de etapas inválida"));
            exemplo(api, "/solicitacoes", Map.of("cidadaoId", "C007938", "servicoId", 1));
            exemplo(api, "/eventos", Map.of("eventoId", "demo-abertura-1", "jornadaId", "demo-jornada-1",
                    "cidadaoId", "C007938", "servicoId", 1, "etapa", "pedido", "tipo", "ABRIU"));
            exemplo(api, "/servicos/{id}/jornada/progresso", java.util.List.of());
            api.getPaths().get("/preferencias").getPut().getRequestBody().getContent()
                    .values().forEach(media -> media.setExample(Map.of("cidadaoId", "C007938", "consentimentoSaude", false)));
            api.getPaths().get("/recomendacoes.csv").getGet().getResponses().put("200",
                    new ApiResponse().description("Arquivo completo UTF-8")
                            .content(new Content().addMediaType("text/csv", new MediaType()
                                    .schema(new StringSchema().format("binary")))));
        };
    }

    private static void exemplo(OpenAPI api, String path, Object exemplo) {
        api.getPaths().get(path).getPost().getRequestBody().getContent()
                .values().forEach(media -> media.setExample(exemplo));
    }

    private static ApiResponse erro(String descricao) {
        return new ApiResponse().description(descricao).content(new Content().addMediaType("application/problem+json",
                new MediaType().schema(new ObjectSchema()
                        .addProperty("type", new StringSchema()).addProperty("title", new StringSchema())
                        .addProperty("status", new IntegerSchema()).addProperty("detail", new StringSchema())
                        .addProperty("path", new StringSchema()))));
    }
}
