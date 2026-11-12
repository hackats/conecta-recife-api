package br.com.conectarecife;

import br.com.conectarecife.service.RecomendacoesCsvService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.cors.allowed-origins=https://front.exemplo.test")
@AutoConfigureMockMvc
class Etapa5IntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired RecomendacoesCsvService csv;

    @Test
    void endpointsRegistramEventosEMetricasEValidamEntrada() throws Exception {
        mvc.perform(post("/eventos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventoId\":\"api-1\",\"jornadaId\":\"j1\",\"cidadaoId\":\"C000001\","
                                + "\"servicoId\":1,\"etapa\":\"documentos\",\"tipo\":\"ABRIU\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dados.tipo").value("ABRIU"));
        mvc.perform(get("/metricas")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].abriu").value(1))
                .andExpect(jsonPath("$[0].taxaConclusao").value(0));
        mvc.perform(post("/eventos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/recomendacoes/validar")
                        .file(new MockMultipartFile("arquivo", "teste.csv", "text/csv", "invalido".getBytes())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valido").value(false));
    }

    @Test
    void corsLiberaSomenteOrigemConfigurada() throws Exception {
        mvc.perform(options("/eventos").header("Origin", "https://front.exemplo.test")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://front.exemplo.test"));
        mvc.perform(options("/eventos").header("Origin", "https://outra.exemplo.test")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    @EnabledIfSystemProperty(named = "exportar", matches = "true")
    void exportaEValidaTodosOsCidadaos() throws Exception {
        var resultado = csv.gerar(Path.of("target/recomendacoes.csv"));
        assertThat(resultado.valido()).isTrue();
        assertThat(resultado.cidadaos()).isEqualTo(89_046);
        assertThat(resultado.linhas()).isBetween(89_046L, 890_460L);
        System.out.println("EXPORTACAO_COMPLETA " + resultado);
    }
}
