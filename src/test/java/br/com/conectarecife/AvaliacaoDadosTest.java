package br.com.conectarecife;

import br.com.conectarecife.evaluation.AvaliadorTemporal;
import br.com.conectarecife.loader.*;
import br.com.conectarecife.service.Recomendador.Pesos;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

class AvaliacaoDadosTest {
    @Test
    @EnabledIfSystemProperty(named = "avaliar", matches = "true")
    void avaliarDadosLocais() throws Exception {
        Path dados = Path.of(System.getProperty("dados", "data"));
        var cidadaos = new CidadaoLoader().load(dados.resolve("cidadaos.csv"));
        var servicos = new ServicoLoader().load(dados.resolve("servicos.csv"));
        var cliques = new CliqueLoader().load(dados.resolve("historico.csv"));
        var datas = cliques.stream().map(c -> c.data()).sorted().toList();
        LocalDate corteValidacao = LocalDate.parse(System.getProperty("corteValidacao",
                datas.get(datas.size() * 6 / 10).toString()));
        LocalDate corteTeste = LocalDate.parse(System.getProperty("corteTeste",
                datas.get(datas.size() * 8 / 10).toString()));
        LocalDate fim = datas.getLast().plusDays(1);
        var grade = new ArrayList<Pesos>();
        grade.add(new Pesos(1, 1, 0));
        for (double b : new double[]{0, 0.5, 1, 2}) {
            for (double c : new double[]{0, 1, 2, 4}) {
                var peso = new Pesos(1, b, c);
                if (!grade.contains(peso)) grade.add(peso);
            }
        }
        var avaliador = new AvaliadorTemporal();
        var validacao = avaliador.avaliar(cidadaos, servicos, cliques,
                corteValidacao, corteTeste, grade);
        var escolhido = AvaliadorTemporal.melhor(validacao).pesos();
        var teste = avaliador.avaliar(cidadaos, servicos, cliques,
                corteTeste, fim, List.of(new Pesos(1, 1, 0), escolhido));
        var relatorio = Map.of("validacao", validacao, "pesosEscolhidos", escolhido, "teste", teste);
        new ObjectMapper().writerWithDefaultPrettyPrinter()
                .writeValue(Path.of("target", "avaliacao-temporal.json").toFile(), relatorio);
        System.out.println("AVALIACAO pesos=" + escolhido + " teste=" + teste.resultados());
    }
}
