package br.com.conectarecife;

import br.com.conectarecife.data.DadosStore;
import br.com.conectarecife.model.Cidadao;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class Etapa6IntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired DadosStore store;
    @Autowired ObjectMapper mapper;

    @Test
    void testaCemIdsAleatoriosEMedeLatencia() throws Exception {
        var pessoas = new ArrayList<>(store.listarCidadaos().stream()
                .sorted(Comparator.comparing(Cidadao::cidadaoId)).toList());
        Collections.shuffle(pessoas, new Random(2026));
        for (int i = 0; i < 10; i++) verificar(pessoas.get(i).cidadaoId());
        List<Double> tempos = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            long inicio = System.nanoTime();
            verificar(pessoas.get(i).cidadaoId());
            tempos.add((System.nanoTime() - inicio) / 1_000_000.0);
        }
        Collections.sort(tempos);
        var resultado = Map.of("amostras", 100, "semente", 2026,
                "medianaMs", tempos.get(49), "p95Ms", tempos.get(94), "maxMs", tempos.getLast(),
                "ambiente", "MockMvc local, sequencial, inclui verificações, após 10 aquecimentos");
        mapper.writerWithDefaultPrettyPrinter().writeValue(Path.of("target/desempenho-etapa6.json").toFile(), resultado);
        System.out.println("DESEMPENHO " + resultado);
    }

    private void verificar(String id) throws Exception {
        var resposta = mvc.perform(get("/cidadaos/{id}/recomendacoes", id))
                .andExpect(status().isOk()).andReturn().getResponse();
        var recomendacoes = mapper.readTree(resposta.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(recomendacoes.size()).isEqualTo(10);
        Set<Integer> usados = new HashSet<>();
        store.findCliquesDoCidadao(id).forEach(clique -> usados.add(clique.servicoId()));
        Set<Integer> distintos = new HashSet<>();
        for (var recomendacao : recomendacoes) {
            int servico = recomendacao.get("servicoId").asInt();
            assertThat(store.findServico(servico)).isNotNull();
            assertThat(usados).doesNotContain(servico);
            assertThat(distintos.add(servico)).isTrue();
            assertThat(recomendacao.get("motivo").asText()).isNotBlank();
        }
    }

    @Test
    void atendeSemHistoricoECriancasVazio() throws Exception {
        var semHistorico = store.listarCidadaos().stream()
                .filter(p -> store.findCliquesDoCidadao(p.cidadaoId()).isEmpty()).findFirst().orElseThrow();
        verificar(semHistorico.cidadaoId());
        var vazio = store.listarCidadaos().stream()
                .filter(p -> p.criancasNaFamilia() == null || p.criancasNaFamilia().isBlank())
                .findFirst().orElseThrow();
        verificar(vazio.cidadaoId());
    }

    @Test
    void entradasInvalidasRecebemErrosAmigaveis() throws Exception {
        mvc.perform(get("/cidadaos/inexistente/recomendacoes")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").exists());
        mvc.perform(post("/eventos").contentType("application/json").content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").exists());
        mvc.perform(get("/preferencias")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
    }
}
