package br.com.conectarecife;

import br.com.conectarecife.data.DadosStore;
import br.com.conectarecife.model.*;
import br.com.conectarecife.service.*;
import br.com.conectarecife.service.EventosService.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

class Etapa5Test {
    @TempDir Path diretorio;
    private DadosStore dados() {
        var mapper = new ObjectMapper();
        var pessoa = mapper.convertValue(Map.of("cidadaoId", "a"), Cidadao.class);
        var servico = mapper.convertValue(Map.of("servicoId", 1, "nome", "Serviço",
                "temas", List.of(), "publicosAlvo", List.of()), Servico.class);
        var dados = new DadosStore();
        dados.initialize(Map.of("a", pessoa), Map.of(1, servico), List.of());
        return dados;
    }

    private Pedido evento(String id, String jornada, Tipo tipo) {
        return new Pedido(id, jornada, "a", 1, "solicitacao", tipo);
    }

    @Test
    void metricasContamJornadasComDenominadorEReenvioIdempotente() {
        var eventos = new EventosService(dados());
        assertThat(eventos.metricas()).isEmpty();
        Pedido abrir = evento("1", "j1", Tipo.ABRIU);
        var recebido = eventos.registrar(abrir);
        assertThat(eventos.registrar(abrir)).isEqualTo(recebido);
        eventos.registrar(evento("2", "j1", Tipo.INICIOU));
        eventos.registrar(evento("3", "j1", Tipo.CONCLUIU));
        eventos.registrar(evento("4", "j2", Tipo.ABRIU));
        eventos.registrar(evento("5", "j2", Tipo.ABANDONOU));
        eventos.registrar(evento("6", "j3", Tipo.ABRIU));
        var metrica = eventos.metricas().getFirst();
        assertThat(metrica.abriu()).isEqualTo(3);
        assertThat(metrica.iniciou()).isEqualTo(1);
        assertThat(metrica.concluiu()).isEqualTo(1);
        assertThat(metrica.abandonou()).isEqualTo(1);
        assertThat(metrica.emAndamento()).isEqualTo(1);
        assertThat(metrica.taxaConclusao()).isEqualTo(1.0 / 3);
        assertThat(metrica.taxaAbandono()).isEqualTo(1.0 / 3);
        assertThatThrownBy(() -> eventos.registrar(evento("1", "outra", Tipo.ABRIU)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> eventos.registrar(evento("7", "j1", Tipo.ABANDONOU)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> eventos.registrar(evento("8", "j4", Tipo.CONCLUIU)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void concorrenciaNaoDuplicaContagens() throws Exception {
        var eventos = new EventosService(dados());
        try (var executor = Executors.newFixedThreadPool(4)) {
            List<Callable<Evento>> chamadas = new ArrayList<>();
            for (int i = 0; i < 20; i++) chamadas.add(() -> eventos.registrar(evento("1", "j", Tipo.ABRIU)));
            for (var resultado : executor.invokeAll(chamadas)) resultado.get();
        }
        assertThat(eventos.metricas().getFirst().abriu()).isEqualTo(1);
    }

    @Test
    void geraValidaCsvERejeitaDadosInvalidos() throws Exception {
        var dados = dados();
        var csv = new RecomendacoesCsvService(dados, new Recomendador(dados));
        Path arquivo = diretorio.resolve("recomendacoes.csv");
        var validacao = csv.gerar(arquivo);
        assertThat(validacao.valido()).isTrue();
        assertThat(validacao.cidadaos()).isEqualTo(1);
        assertThat(validacao.linhas()).isEqualTo(1);
        assertThat(Files.readString(arquivo)).startsWith("cidadao_id,servico_id,posicao,motivo");
        String cabecalho = "cidadao_id,servico_id,posicao,motivo\n";
        for (String invalido : List.of(
                cabecalho, cabecalho + "a,999,1,motivo\n",
                cabecalho + "desconhecido,1,1,motivo\n",
                cabecalho + "a,1,1,\n",
                cabecalho + "a,1,2,motivo\n",
                cabecalho + "a,1,1,motivo\na,1,2,motivo\n",
                cabecalho + "a,1,1,motivo\n".repeat(11),
                cabecalho + "a,1,1,\"aspas abertas",
                "cabecalho incorreto\n")) {
            Files.writeString(arquivo, invalido);
            assertThat(csv.validar(arquivo).valido()).as(invalido).isFalse();
        }
        Files.write(arquivo, new byte[]{(byte) 0xc3, (byte) 0x28});
        assertThat(csv.validar(arquivo).valido()).isFalse();
        Files.writeString(arquivo, cabecalho + "a,1,1,\"Motivo com vírgula, acento e\nsegunda linha\"\n");
        assertThat(csv.validar(arquivo).valido()).isTrue();
    }

    @Test
    void falhaDeGeracaoPreservaArquivoAnterior() throws Exception {
        var dados = dados();
        dados.initialize(Map.of("a", dados.findCidadao("a")),
                Map.of(1, dados.findServico(1)), List.of(new Clique("a", "s", 1, null, null, 1)));
        var csv = new RecomendacoesCsvService(dados, new Recomendador(dados));
        Path arquivo = diretorio.resolve("recomendacoes.csv");
        Files.writeString(arquivo, "anterior");
        assertThatThrownBy(() -> csv.gerar(arquivo)).isInstanceOf(java.io.IOException.class);
        assertThat(Files.readString(arquivo)).isEqualTo("anterior");
        try (var arquivos = Files.list(diretorio)) { assertThat(arquivos.count()).isEqualTo(1); }
    }
}
