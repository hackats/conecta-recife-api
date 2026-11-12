package br.com.conectarecife.service;

import br.com.conectarecife.data.DadosStore;
import br.com.conectarecife.model.Servico;
import org.apache.commons.csv.CSVFormat;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class AnonimoService {
    public record Resposta(boolean transicoesDisponiveis, List<Recomendador.Recomendacao> recomendacoes) {}
    private record Sessao(String visitante, String sessao) {}
    private final DadosStore store;
    private final Map<Integer, Map<Integer, Integer>> transicoes = new HashMap<>();
    private final Map<Integer, Double> popularidade = new HashMap<>();
    private final boolean disponivel;

    public AnonimoService(DadosStore store, @Value("${app.data.directory:./data}") String diretorio) throws java.io.IOException {
        this.store = store;
        Path semanal = Path.of(diretorio, "uso_semanal.csv");
        if (Files.isRegularFile(semanal)) {
            Map<Integer, List<Long>> semanas = new HashMap<>();
            try (var reader = Files.newBufferedReader(semanal, StandardCharsets.UTF_8);
                 var csv = CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).get().parse(reader)) {
                for (var row : csv) {
                    if (row.get("login").equals("nao")) {
                        semanas.computeIfAbsent(Integer.valueOf(row.get("servico_id")), k -> new ArrayList<>())
                                .add(Long.valueOf(row.get("visitantes")));
                    }
                }
            }
            semanas.forEach((id, valores) -> {
                Collections.sort(valores);
                // Mediana das semanas observadas limita a influência de mutirões isolados.
                popularidade.put(id, Math.log1p(valores.get(valores.size() / 2)));
            });
        }
        Path visitas = Path.of(diretorio, "visitas_sem_login.csv");
        disponivel = Files.isRegularFile(visitas);
        if (disponivel) {
            Map<Sessao, TreeMap<Integer, Integer>> sessoes = new HashMap<>();
            try (var reader = Files.newBufferedReader(visitas, StandardCharsets.UTF_8);
                 var csv = CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).get().parse(reader)) {
                for (var row : csv) {
                    Sessao chave = new Sessao(row.get("visitante_id"), row.get("sessao_id"));
                    int ordem = Integer.parseInt(row.get("ordem_na_sessao"));
                    int servico = Integer.parseInt(row.get("servico_id"));
                    if (chave.visitante().isBlank() || chave.sessao().isBlank() || ordem < 1)
                        throw new java.io.IOException("Visita sem identificador ou ordem válida");
                    if (sessoes.computeIfAbsent(chave, k -> new TreeMap<>()).putIfAbsent(ordem, servico) != null)
                        throw new java.io.IOException("Ordem duplicada na sessão anônima");
                }
            }
            for (var sessao : sessoes.values()) {
                for (var clique : sessao.entrySet()) {
                    Integer proximo = sessao.get(clique.getKey() + 1);
                    if (proximo != null && !proximo.equals(clique.getValue()))
                        transicoes.computeIfAbsent(clique.getValue(), k -> new HashMap<>()).merge(proximo, 1, Integer::sum);
                }
            }
        }
    }

    public Resposta recomendar(List<Integer> acessados) {
        if (acessados.size() > 50 || acessados.stream().anyMatch(id -> id == null || store.findServico(id) == null))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe até 50 serviços válidos da sessão atual");
        Set<Integer> usados = new HashSet<>(acessados);
        Map<Integer, Integer> destinos = acessados.isEmpty() ? Map.of()
                : transicoes.getOrDefault(acessados.getLast(), Map.of());
        double total = destinos.values().stream().mapToDouble(Integer::doubleValue).sum();
        Set<String> temas = new HashSet<>();
        acessados.forEach(id -> temas.addAll(store.findServico(id).temas()));
        var candidatos = store.listarServicos().stream().filter(s -> !usados.contains(s.servicoId()))
                .sorted(Comparator.comparingDouble((Servico s) ->
                        4 * destinos.getOrDefault(s.servicoId(), 0) / Math.max(1, total)
                        + (Collections.disjoint(s.temas(), temas) ? 0 : 2)
                        + Math.min(1, popularidade.getOrDefault(s.servicoId(), 0.0) / 10)).reversed()
                        .thenComparing(Servico::servicoId)).toList();
        Map<String, Integer> porTema = new HashMap<>();
        List<Recomendador.Recomendacao> resultado = new ArrayList<>();
        for (Servico servico : candidatos) {
            String tema = servico.temas().isEmpty() ? "Outros" : servico.temas().getFirst();
            if (porTema.getOrDefault(tema, 0) >= 2) continue;
            porTema.merge(tema, 1, Integer::sum);
            String motivo = destinos.containsKey(servico.servicoId()) ? "Próximo serviço frequente em visitas semelhantes"
                    : Collections.disjoint(servico.temas(), temas) ? "Opção do catálogo para explorar"
                    : "Relacionado aos serviços desta sessão";
            resultado.add(new Recomendador.Recomendacao(servico.servicoId(), servico.itemNome(), motivo));
            if (resultado.size() == 10) break;
        }
        return new Resposta(disponivel, List.copyOf(resultado));
    }
}
