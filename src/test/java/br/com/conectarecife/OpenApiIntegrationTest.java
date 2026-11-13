package br.com.conectarecife;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test void documentaTodosOsEndpointsComContratosEExemplos() throws Exception {
        var response = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse();
        var api = mapper.readTree(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        var paths = api.get("paths");
        int operacoes = 0;
        for (var path : paths) {
            for (var operation : path) {
                assertThat(operation.path("summary").asText()).isNotBlank();
                operacoes++;
            }
        }
        assertThat(operacoes).isEqualTo(17);
        assertThat(paths.get("/solicitacoes").get("post").get("responses").has("201")).isTrue();
        assertThat(paths.get("/recomendacoes.csv").get("get").get("responses").get("200")
                .get("content").has("text/csv")).isTrue();
        var pedido = paths.get("/solicitacoes").get("post").get("requestBody").get("content").get("application/json");
        assertThat(pedido.get("example").get("cidadaoId").asText()).isEqualTo("C007938");
        String referencia = pedido.get("schema").get("$ref").asText();
        var schema = api.get("components").get("schemas").get(referencia.substring(referencia.lastIndexOf('/') + 1));
        assertThat(schema.get("properties").has("eventoId")).isFalse();
        assertThat(schema.get("properties").has("cidadaoId")).isTrue();
        assertThat(paths.get("/recomendacoes/validar").get("post").get("requestBody").get("content")
                .has("multipart/form-data")).isTrue();
        assertThat(paths.get("/assistente/conversar").get("post").get("requestBody").get("content")
                .get("application/json").get("example").get("mensagem").asText()).isEqualTo("Quero marcar uma consulta");
    }

    @Test void disponibilizaInterfaceEConfiguracao() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/v3/api-docs"));
    }
}
