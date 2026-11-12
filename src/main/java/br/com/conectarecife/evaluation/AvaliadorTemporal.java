package br.com.conectarecife.evaluation;

import br.com.conectarecife.data.DadosStore;
import br.com.conectarecife.model.*;
import br.com.conectarecife.service.Recomendador;
import br.com.conectarecife.service.Recomendador.Pesos;

import java.time.LocalDate;
import java.util.*;

public final class AvaliadorTemporal {
    public record Resultado(Pesos pesos, int elegiveis, int acertos, double hit10) {}
    public record Relatorio(String corte, String fimExclusivo, int cliquesTreino,
                            int cidadaosComAcessosFuturos, List<Resultado> resultados) {}

    public Relatorio avaliar(Map<String, Cidadao> cidadaos, Map<Integer, Servico> servicos,
                             List<Clique> cliques, LocalDate corte, LocalDate fimExclusivo,
                             List<Pesos> pesos) {
        if (!fimExclusivo.isAfter(corte) || pesos.isEmpty()) {
            throw new IllegalArgumentException("Informe uma janela posterior ao corte e pesos");
        }
        List<Clique> treino = cliques.stream().filter(c -> c.data().isBefore(corte)).toList();
        var store = new DadosStore();
        store.initialize(cidadaos, servicos, treino);
        var recomendador = new Recomendador(store);
        var futuros = new TreeMap<String, Set<Integer>>();
        for (Clique clique : cliques) {
            if (!clique.data().isBefore(corte) && clique.data().isBefore(fimExclusivo)) {
                futuros.computeIfAbsent(clique.cidadaoId(), ignored -> new HashSet<>())
                        .add(clique.servicoId());
            }
        }
        int elegiveis = 0;
        int[] acertos = new int[pesos.size()];
        for (var entrada : futuros.entrySet()) {
            Set<Integer> alvos = new HashSet<>(entrada.getValue());
            for (Clique anterior : store.findCliquesDoCidadao(entrada.getKey())) {
                alvos.remove(anterior.servicoId());
            }
            if (alvos.isEmpty()) continue;
            elegiveis++;
            var sinais = recomendador.sinais(cidadaos.get(entrada.getKey()));
            for (int i = 0; i < pesos.size(); i++) {
                boolean acertou = Recomendador.ordenar(sinais, pesos.get(i)).stream()
                        .anyMatch(r -> alvos.contains(r.servicoId()));
                if (acertou) acertos[i]++;
            }
        }
        List<Resultado> resultados = new ArrayList<>();
        for (int i = 0; i < pesos.size(); i++) {
            resultados.add(new Resultado(pesos.get(i), elegiveis, acertos[i],
                    elegiveis == 0 ? 0 : (double) acertos[i] / elegiveis));
        }
        return new Relatorio(corte.toString(), fimExclusivo.toString(), treino.size(),
                futuros.size(), List.copyOf(resultados));
    }

    public static Resultado melhor(Relatorio relatorio) {
        if (relatorio.resultados().isEmpty() || relatorio.resultados().getFirst().elegiveis() == 0) {
            throw new IllegalArgumentException("Sem cidadãos elegíveis para ajustar pesos");
        }
        // Empate favorece o primeiro candidato, começando pela linha de base.
        return relatorio.resultados().stream()
                .max(Comparator.comparingDouble(Resultado::hit10)).orElseThrow();
    }
}
