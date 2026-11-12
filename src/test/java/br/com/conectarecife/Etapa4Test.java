package br.com.conectarecife;

import br.com.conectarecife.data.DadosStore;
import br.com.conectarecife.model.*;
import br.com.conectarecife.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

class Etapa4Test {
    private final ObjectMapper mapper = new ObjectMapper();

    private Servico servico(int id, String nome) {
        return mapper.convertValue(Map.of("servicoId", id, "nome", nome, "temas", List.of(),
                "publicosAlvo", List.of(), "palavrasChave", List.of()), Servico.class);
    }

    private DadosStore dados() {
        var pessoa = mapper.convertValue(Map.of("cidadaoId", "a", "diabetes", true,
                "gestante", true, "pessoaComDeficiencia", true, "aceitaNotificacao", true), Cidadao.class);
        var dados = new DadosStore();
        dados.initialize(Map.of("a", pessoa), Map.of(1, servico(1, "Atendimento geral"),
                2, servico(2, "Acompanhamento de diabetes")), List.of());
        return dados;
    }

    @Test
    void consentimentoExplicitoAtivaSaudeERevogacaoRestauraRanking() {
        var dados = dados();
        var preferencias = new PreferenciasService(dados);
        var recomendador = new Recomendador(dados, preferencias);
        var pessoa = dados.findCidadao("a");
        var antes = recomendador.recomendar(pessoa);
        assertThat(preferencias.consultar("a").consentimentoSaude()).isFalse();
        assertThat(PerfilCidadao.publicosDoCidadao(pessoa)).doesNotContain(
                "gestante", "pessoa com deficiencia - pcd");
        assertThat(antes.getFirst().servicoId()).isEqualTo(1);

        preferencias.alterar(new PreferenciasService.Alteracao("a", true, null));
        var comConsentimento = recomendador.recomendar(pessoa);
        assertThat(comConsentimento.getFirst().servicoId()).isEqualTo(2);
        assertThat(comConsentimento.getFirst().motivo()).contains("Serviço de saúde para o seu acompanhamento")
                .doesNotContainIgnoringCase("diabetes");
        assertThat(preferencias.consultar("a").aceitaNotificacao()).isTrue();

        preferencias.alterar(new PreferenciasService.Alteracao("a", false, false));
        assertThat(recomendador.recomendar(pessoa)).isEqualTo(antes);
        assertThat(preferencias.consultar("a").aceitaNotificacao()).isFalse();
        assertThatThrownBy(() -> preferencias.alterar(
                new PreferenciasService.Alteracao("a", null, true)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void geraProtocoloETimelineSimuladaComRelogioControlado() {
        var instante = Instant.parse("2026-01-01T00:00:00Z");
        var relogio = org.mockito.Mockito.mock(Clock.class);
        org.mockito.Mockito.when(relogio.instant()).thenReturn(instante);
        var service = new SolicitacoesService(dados(), relogio);
        var criada = service.criar(new SolicitacoesService.Pedido("a", 1), null);
        assertThat(criada.simulada()).isTrue();
        assertThat(criada.status()).isEqualTo("RECEBIDA");
        assertThat(criada.linhaDoTempo()).hasSize(1);

        org.mockito.Mockito.when(relogio.instant()).thenReturn(instante.plusSeconds(30));
        assertThat(service.consultar(criada.protocolo()).status()).isEqualTo("EM_ANALISE");
        org.mockito.Mockito.when(relogio.instant()).thenReturn(instante.plusSeconds(60));
        var concluida = service.consultar(criada.protocolo());
        assertThat(concluida.status()).isEqualTo("CONCLUIDA");
        assertThat(concluida.linhaDoTempo()).hasSize(3);
        assertThat(service.consultar(criada.protocolo())).isEqualTo(concluida);
    }

    @Test
    void repeticoesConcorrentesGeramUmProtocoloEConflitoNaoAlteraPedido() throws Exception {
        var service = new SolicitacoesService(dados(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        try (var executor = Executors.newFixedThreadPool(4)) {
            List<Callable<String>> tarefas = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                tarefas.add(() -> service.criar(new SolicitacoesService.Pedido("a", 1), "tentativa").protocolo());
            }
            var resultados = executor.invokeAll(tarefas);
            Set<String> protocolos = new HashSet<>();
            for (var resultado : resultados) protocolos.add(resultado.get());
            assertThat(protocolos).hasSize(1);
            assertThatThrownBy(() -> service.criar(new SolicitacoesService.Pedido("a", 2), "tentativa"))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            erro -> assertThat(erro.getStatusCode().value()).isEqualTo(409));
            assertThat(service.consultar(protocolos.iterator().next()).servicoId()).isEqualTo(1);
        }
    }

    @Test
    void jornadaPreservaCondicoesRemoveHtmlETrataCamposVazios() {
        var servico = mapper.convertValue(Map.of("servicoId", 1, "nome", "Teste",
                "requisitos", "<p>Documento com validade de 60 dias &amp; comprovante.</p>",
                "passos", " - Acompanhe a tramitação.\n - É necessário requerer autorização."), Servico.class);
        var jornada = JornadaServico.criar(servico);
        assertThat(jornada.requisitos()).containsExactly("Documento com validade de 60 dias & comprovante.");
        assertThat(jornada.passos()).containsExactly(
                "Acompanhe o andamento.", "É necessário solicitar autorização.");
        assertThat(JornadaServico.criar(servico(2, "Vazio")).requisitos().getFirst())
                .contains("Consulte o órgão");
    }
}
