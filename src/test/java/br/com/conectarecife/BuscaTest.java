package br.com.conectarecife;
import br.com.conectarecife.data.DadosStore;
import br.com.conectarecife.model.Servico;
import br.com.conectarecife.service.BuscaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class BuscaTest {
    @Test void encontraAcentosSinonimosErrosESemResultadosInventados() {
        var mapper = new ObjectMapper();
        var s = mapper.convertValue(Map.of("servicoId", 1, "itemNome", "Agendar consultas médicas", "temas", List.of("Saúde"), "palavrasChave", List.of("consulta", "medico")), Servico.class);
        var store = new DadosStore(); store.initialize(Map.of(), Map.of(1,s), List.of());
        var busca = new BuscaService(store);
        for (String q : List.of("saude", "quero marcar medico", "agendr")) assertThat(busca.buscar(q)).containsExactly(s);
        assertThat(busca.buscar("astronauta")).isEmpty();
        assertThat(busca.buscar(" ")).isEmpty();
        assertThatThrownBy(() -> busca.buscar("a".repeat(301))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
