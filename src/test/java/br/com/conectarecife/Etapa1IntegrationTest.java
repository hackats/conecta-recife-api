package br.com.conectarecife;

import br.com.conectarecife.data.DadosStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Etapa1IntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DadosStore store;

    @Test
    void deveCarregarTodosOsDados() {
        assertThat(store.totalCidadaos()).isEqualTo(89_046);
        assertThat(store.totalServicos()).isEqualTo(843);
        assertThat(store.totalCliques()).isEqualTo(593_056);
    }

    @Test
    void deveConsultarCidadao() throws Exception {
        mvc.perform(get("/cidadaos/C000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cidadaoId").value("C000001"))
                .andExpect(jsonPath("$.idade").value(35));
    }

    @Test
    void deveConsultarServico() throws Exception {
        mvc.perform(get("/servicos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicoId").value(1));
    }

    @Test
    void deveResponder404ParaCidadaoAusente() throws Exception {
        mvc.perform(get("/cidadaos/C999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Cidadão não encontrado: C999999"));
    }

    @Test
    void deveResponder404ParaServicoAusente() throws Exception {
        mvc.perform(get("/servicos/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Serviço não encontrado: 999999"));
    }

    @Test
    void deveConsultarCliques() throws Exception {
        mvc.perform(get("/cidadaos/C000001/cliques"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cidadaoId").value("C000001"));
    }
}
