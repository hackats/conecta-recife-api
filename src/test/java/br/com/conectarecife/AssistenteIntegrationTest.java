package br.com.conectarecife;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AssistenteIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test void respondeComServicosDoCatalogoENovidades() throws Exception {
        String corpo = "{\"mensagem\":\"quero procurar emprego\"}";
        var resposta = mvc.perform(post("/assistente/conversar").contentType("application/json").content(corpo))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mensagem").isNotEmpty())
                .andExpect(jsonPath("$.servicos[0].servicoId").isNumber()).andReturn().getResponse();
        JsonNode json = mapper.readTree(resposta.getContentAsString());
        assertThat(json.get("servicos").size()).isLessThanOrEqualTo(3);
        mvc.perform(post("/assistente/conversar").contentType("application/json")
                        .content("{\"mensagem\":\"quais sao as novidades?\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("voz")));
    }

    @Test void usaRecomendacoesSomenteParaCidadaoExistente() throws Exception {
        mvc.perform(post("/assistente/conversar").contentType("application/json")
                        .content("{\"mensagem\":\"me de sugestoes\",\"cidadaoId\":\"C007938\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.servicos[0].motivo").isNotEmpty());
        mvc.perform(post("/assistente/conversar").contentType("application/json")
                        .content("{\"mensagem\":\"oi\",\"cidadaoId\":\"C999999\"}"))
                .andExpect(status().isNotFound());
    }

    @Test void rejeitaMensagemAusenteOuMaiorQueOLimite() throws Exception {
        mvc.perform(post("/assistente/conversar").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/assistente/conversar").contentType("application/json")
                        .content("{\"mensagem\":\"" + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());
    }
}
