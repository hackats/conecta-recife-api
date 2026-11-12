package br.com.conectarecife;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class Etapa4IntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    @Test
    void jornadaDisponivelEIdsInvalidos() throws Exception {
        mvc.perform(get("/servicos/1/jornada")).andExpect(status().isOk())
                .andExpect(jsonPath("$.servicoId").value(1))
                .andExpect(jsonPath("$.requisitos").isArray())
                .andExpect(jsonPath("$.passos").isArray());
        mvc.perform(get("/servicos/999999/jornada")).andExpect(status().isNotFound());
        mvc.perform(get("/servicos/abc/jornada")).andExpect(status().isBadRequest());
    }

    @Test
    void criaEConsultaSolicitacaoPeloLocation() throws Exception {
        var resposta = mvc.perform(post("/solicitacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cidadaoId\":\"C000001\",\"servicoId\":1}")
                        .header("Idempotency-Key", "integracao"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.simulada").value(true))
                .andExpect(jsonPath("$.status").value("RECEBIDA"))
                .andReturn().getResponse();
        mvc.perform(get(resposta.getHeader("Location"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.protocolo").value(
                        mapper.readTree(resposta.getContentAsString()).get("protocolo").asText()));
        mvc.perform(get("/solicitacoes/inexistente")).andExpect(status().isNotFound());
        mvc.perform(post("/solicitacoes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").exists());
        mvc.perform(post("/solicitacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cidadaoId\":\"C000001\",\"servicoId\":999999}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void alteraConsultaERevogaPreferencias() throws Exception {
        try {
            mvc.perform(put("/preferencias").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"cidadaoId\":\"C000001\",\"consentimentoSaude\":true}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.consentimentoSaude").value(true));
            mvc.perform(get("/preferencias").param("cidadaoId", "C000001"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.consentimentoSaude").value(true));
            mvc.perform(put("/preferencias").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"cidadaoId\":\"C000001\"}"))
                    .andExpect(status().isBadRequest());
            mvc.perform(put("/preferencias").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"cidadaoId\":\"inexistente\",\"consentimentoSaude\":true}"))
                    .andExpect(status().isNotFound());
        } finally {
            mvc.perform(put("/preferencias").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"cidadaoId\":\"C000001\",\"consentimentoSaude\":false}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.consentimentoSaude").value(false));
        }
    }
}
