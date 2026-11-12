package br.com.conectarecife;

import br.com.conectarecife.data.DadosStore;
import br.com.conectarecife.model.Servico;
import br.com.conectarecife.service.AnonimoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class AnonimoTest {
    @TempDir Path dir;
    @Test void picoIsoladoEmDuasSemanasNaoPassaServicoEstavel() throws Exception {
        var mapper = new ObjectMapper();
        var bolsa = mapper.convertValue(Map.of("servicoId", 1, "itemNome", "Bolsa Família",
                "temas", List.of("Assistência")), Servico.class);
        var emprego = mapper.convertValue(Map.of("servicoId", 2, "itemNome", "Vagas de emprego",
                "temas", List.of("Trabalho")), Servico.class);
        var store = new DadosStore();
        store.initialize(Map.of(), Map.of(1, bolsa, 2, emprego), List.of());
        Files.writeString(dir.resolve("uso_semanal.csv"),
                "semana,servico_id,login,visitantes\n2026-01-05,1,nao,10\n2026-01-12,1,nao,999999\n2026-01-05,2,nao,100\n2026-01-12,2,nao,100\n");
        var service = new AnonimoService(store, dir.toString());
        assertThat(service.recomendar(List.of()).recomendacoes().getFirst().servicoId()).isEqualTo(2);
        assertThat(service.recomendar(List.of(), List.of(1)).recomendacoes())
                .extracting(r -> r.servicoId()).containsExactly(2);
    }

    @Test void contextoSuperaPicoEDiversificaSemInferirPerfil() throws Exception {
        var mapper = new ObjectMapper();
        Map<Integer, Servico> catalogo = new HashMap<>();
        for (int id = 1; id <= 15; id++) {
            catalogo.put(id, mapper.convertValue(Map.of("servicoId", id, "itemNome", "Serviço " + id,
                    "temas", List.of("Tema " + id / 3)), Servico.class));
        }
        var store = new DadosStore();
        store.initialize(Map.of(), catalogo, List.of());
        Files.writeString(dir.resolve("uso_semanal.csv"), "semana,servico_id,login,visitantes\n2026-01-05,15,nao,10\n2026-01-12,15,nao,10\n2026-01-19,15,nao,999999999\n");
        Files.writeString(dir.resolve("visitas_sem_login.csv"), "visitante_id,sessao_id,ordem_na_sessao,servico_id\nV1,s1,2,2\nV1,s1,1,1\nV2,s2,1,15\n");
        var service = new AnonimoService(store, dir.toString());
        var resultado = service.recomendar(List.of(1));
        assertThat(resultado.transicoesDisponiveis()).isTrue();
        assertThat(resultado.recomendacoes().getFirst().servicoId()).isEqualTo(2);
        assertThat(resultado.recomendacoes()).extracting(r -> r.servicoId()).doesNotHaveDuplicates().doesNotContain(1);
        assertThat(resultado.recomendacoes().size()).isLessThanOrEqualTo(10);
        assertThatThrownBy(() -> service.recomendar(List.of(999))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(service.recomendar(List.of(1), List.of(2)).recomendacoes())
                .extracting(r -> r.servicoId()).doesNotContain(1, 2);
        assertThat(service.recomendar(List.of(1)).recomendacoes().getFirst().servicoId()).isEqualTo(2);
        assertThatThrownBy(() -> service.recomendar(List.of(), List.of(999)))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        Files.delete(dir.resolve("visitas_sem_login.csv"));
        assertThat(new AnonimoService(store, dir.toString()).recomendar(List.of()).transicoesDisponiveis()).isFalse();
    }
}
