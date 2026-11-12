package br.com.conectarecife;

import br.com.conectarecife.data.DadosStore;
import br.com.conectarecife.service.JornadaServico;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MentoriaIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired DadosStore store;
    @Test void endpointsDaBuscaESessaoRespondem() throws Exception {
        mvc.perform(get("/servicos/busca").param("q", "quero emprego")).andExpect(status().isOk()).andExpect(jsonPath("$[0].servicoId").isNumber());
        mvc.perform(get("/anonimo/recomendacoes")).andExpect(status().isOk()).andExpect(jsonPath("$.recomendacoes[0].servicoId").isNumber());
        mvc.perform(get("/anonimo/recomendacoes").param("acessados", "999999")).andExpect(status().isBadRequest());
    }
    @Test void progressoAceitaRepeticaoDesmarcacaoERejeitaPosicoesInvalidas() throws Exception {
        var s = store.listarServicos().stream().filter(v -> v.passos() != null && !v.passos().isBlank()).findFirst().orElseThrow();
        String url = "/servicos/" + s.servicoId() + "/jornada/progresso";
        int total = JornadaServico.criar(s).passos().size();
        for (int tentativa = 0; tentativa < 2; tentativa++) {
            mvc.perform(post(url).contentType("application/json").content("[1]"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.concluidos").value(1))
                    .andExpect(jsonPath("$.total").value(total)).andExpect(jsonPath("$.autodeclarado").value(true));
        }
        mvc.perform(post(url).contentType("application/json").content("[]")).andExpect(status().isOk()).andExpect(jsonPath("$.percentual").value(0));
        for (String corpo : new String[]{"[0]", "[1,1]", "[null]", "[999999]"})
            mvc.perform(post(url).contentType("application/json").content(corpo)).andExpect(status().isBadRequest());
        mvc.perform(post("/servicos/999999/jornada/progresso").contentType("application/json").content("[]")).andExpect(status().isNotFound());
    }
}
