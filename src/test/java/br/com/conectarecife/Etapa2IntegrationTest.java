package br.com.conectarecife;

import br.com.conectarecife.data.DadosStore;
import br.com.conectarecife.model.Cidadao;
import br.com.conectarecife.model.Clique;
import br.com.conectarecife.service.Recomendador;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Etapa2IntegrationTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private DadosStore store;

    @Test
    void deveUsarPopularidadeSegmentadaParaCidadaoSemCliques() {
        var solicitante = estudante("solicitante");
        var outro = estudante("outro");
        var candidatos = store.listarServicos().stream()
                .filter(servico -> servico.publicosAlvo().stream()
                        .noneMatch("estudante"::equalsIgnoreCase))
                .sorted(java.util.Comparator.comparing(servico -> servico.servicoId()))
                .limit(2)
                .toList();
        var primeiro = candidatos.getFirst();
        var popular = candidatos.getLast();
        var catalogo = Map.of(primeiro.servicoId(), primeiro, popular.servicoId(), popular);
        var dados = new DadosStore();
        dados.initialize(Map.of("solicitante", solicitante, "outro", outro), catalogo,
                List.of(new Clique("outro", "sessao", 1, null, null, popular.servicoId())));

        var recomendacoes = new Recomendador(dados).recomendar(solicitante);

        assertThat(recomendacoes).hasSize(2);
        assertThat(recomendacoes.getFirst().servicoId()).isEqualTo(popular.servicoId());
        assertThat(recomendacoes.getFirst().motivo()).contains(
                "Acessado por pessoas da sua faixa etária",
                "Acessado por pessoas com perfil semelhante");
    }

    private static Cidadao estudante(String id) {
        return new Cidadao(id, 21, "18-24", null, null,
                null, null, null, null, null, true, null, null, null,
                null, null, null, null, null, null, null, null, null);
    }


    @Test
    void deveRecomendarDezServicosAindaNaoAcessados() throws Exception {
        var usados = store.findCliquesDoCidadao("C000001").stream()
                .map(clique -> clique.servicoId())
                .toList();

        mvc.perform(get("/cidadaos/C000001/recomendacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[*].servicoId").value(
                        everyItem(not(
                                in(usados)))))
                .andExpect(jsonPath("$[*].motivo").value(
                        everyItem(not(
                                emptyString()))));
    }

    @Test
    void deveResponder404AoRecomendarParaCidadaoAusente() throws Exception {
        mvc.perform(get("/cidadaos/C999999/recomendacoes"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Cidadão não encontrado: C999999"));
    }
}
