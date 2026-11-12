package br.com.conectarecife;

import br.com.conectarecife.data.DadosStore;
import br.com.conectarecife.evaluation.AvaliadorTemporal;
import br.com.conectarecife.model.*;
import br.com.conectarecife.service.Recomendador;
import br.com.conectarecife.service.Recomendador.Pesos;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

class Etapa3Test {
    private final ObjectMapper mapper = new ObjectMapper();

    private Cidadao cidadao(String id) {
        return mapper.convertValue(Map.of("cidadaoId", id, "faixaEtaria", "18-24"), Cidadao.class);
    }

    private Map<Integer, Servico> catalogo() {
        Map<Integer, Servico> resultado = new HashMap<>();
        for (int i = 1; i <= 12; i++) {
            resultado.put(i, mapper.convertValue(Map.of("servicoId", i, "nome", "Serviço " + i,
                    "temas", List.of(), "publicosAlvo", List.of()), Servico.class));
        }
        return resultado;
    }

    private Clique clique(String pessoa, String sessao, int ordem, int servico, String data) {
        return new Clique(pessoa, sessao, ordem, LocalDate.parse(data), LocalTime.NOON, servico);
    }

    @Test
    void transicoesRespeitamPessoaSessaoOrdemELacunas() {
        var dados = new DadosStore();
        dados.initialize(Map.of("a", cidadao("a"), "b", cidadao("b")), catalogo(), List.of(
                clique("a", "s", 2, 12, "2026-01-01"),
                clique("a", "s", 1, 1, "2026-01-01"),
                clique("b", "s", 3, 5, "2026-01-01"),
                clique("a", "outra", 3, 6, "2026-01-01"),
                clique("a", "s", 4, 7, "2026-01-01")));
        assertThat(dados.transicoesDe(1)).containsExactly(entry(12, 1));
        assertThat(dados.transicoesDe(12)).isEmpty();
        dados.initialize(Map.of("a", cidadao("a")), catalogo(), List.of());
        assertThat(dados.transicoesDe(1)).isEmpty();
    }

    @Test
    void sinalDeTransicaoAlteraRankingEMotivoSemRecomendarUsados() {
        var dados = new DadosStore();
        var pessoa = cidadao("a");
        dados.initialize(Map.of("a", pessoa, "b", cidadao("b")), catalogo(), List.of(
                clique("a", "a", 1, 1, "2026-01-01"),
                clique("b", "b", 1, 1, "2026-01-01"),
                clique("b", "b", 2, 12, "2026-01-01")));
        var recomendador = new Recomendador(dados);
        var sinais = recomendador.sinais(pessoa);
        var resultado = Recomendador.ordenar(sinais, new Pesos(0, 0, 1));
        assertThat(resultado).hasSize(10);
        assertThat(resultado.getFirst().servicoId()).isEqualTo(12);
        assertThat(resultado.getFirst().motivo()).contains("Outros acessos seguiram");
        assertThat(resultado).extracting(r -> r.servicoId()).doesNotContain(1).doesNotHaveDuplicates();
        assertThat(Recomendador.ordenar(sinais, new Pesos(0, 1, 0)).getFirst().servicoId())
                .isEqualTo(2);
    }

    @Test
    void corteExcluiCliquesFuturosEIncluiCidadaoSemHistorico() {
        var pessoas = Map.of("a", cidadao("a"));
        var cliques = List.of(clique("a", "s", 1, 12, "2026-01-02"));
        var resultado = new AvaliadorTemporal().avaliar(pessoas, catalogo(), cliques,
                LocalDate.parse("2026-01-02"), LocalDate.parse("2026-01-03"),
                List.of(new Pesos(1, 1, 0)));
        assertThat(resultado.cliquesTreino()).isZero();
        assertThat(resultado.resultados().getFirst().elegiveis()).isEqualTo(1);
        // Sem vazamento, todos empatam e o serviço 12 fica fora do top 10.
        assertThat(resultado.resultados().getFirst().hit10()).isZero();
    }

    @Test
    void hitContaCidadaosUmaVezEExcluiQuemSoRepeteServicos() {
        var pessoas = Map.of("a", cidadao("a"), "b", cidadao("b"));
        var cliques = List.of(
                clique("a", "s", 1, 1, "2026-01-01"),
                clique("a", "s2", 1, 1, "2026-01-02"),
                clique("b", "s3", 1, 2, "2026-01-02"),
                clique("b", "s3", 2, 2, "2026-01-02"));
        var resultado = new AvaliadorTemporal().avaliar(pessoas, catalogo(), cliques,
                LocalDate.parse("2026-01-02"), LocalDate.parse("2026-01-03"),
                List.of(new Pesos(1, 1, 0)));
        assertThat(resultado.cidadaosComAcessosFuturos()).isEqualTo(2);
        assertThat(resultado.resultados().getFirst().elegiveis()).isEqualTo(1);
        assertThat(resultado.resultados().getFirst().hit10()).isEqualTo(1);
    }

    @Test
    void rejeitaPesosInvalidosEJanelaInvalida() {
        assertThatThrownBy(() -> new Pesos(0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Pesos(1, -1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Pesos(1, Double.NaN, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AvaliadorTemporal().avaliar(Map.of(), Map.of(), List.of(),
                LocalDate.now(), LocalDate.now(), List.of(new Pesos(1, 1, 0))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
