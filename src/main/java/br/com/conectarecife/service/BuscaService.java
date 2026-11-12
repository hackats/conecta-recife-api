package br.com.conectarecife.service;

import br.com.conectarecife.data.DadosStore;
import br.com.conectarecife.model.Servico;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.text.Normalizer;
import java.util.*;

@Service
public class BuscaService {
    private final DadosStore store;
    private record Resultado(Servico servico, double pontos) {}
    private static final Set<String> IGNORAR = Set.of("a", "o", "as", "os", "de", "do", "da", "dos", "das", "e", "em", "um", "uma", "para", "por", "meu", "minha", "quero", "preciso", "como", "fazer");
    private static final Map<String, String> SINONIMOS = Map.ofEntries(
            Map.entry("emprego", "trabalho vagas"), Map.entry("trabalho", "emprego vagas"),
            Map.entry("medico", "consulta saude"), Map.entry("consulta", "medico agendar"),
            Map.entry("marcar", "agendar agendamento"), Map.entry("agendamento", "agendar marcar"),
            Map.entry("identidade", "rg identificacao"), Map.entry("rg", "identidade identificacao"),
            Map.entry("cadunico", "cadastro unico"), Map.entry("mei", "microempreendedor"));
    public BuscaService(DadosStore store) { this.store = store; }
    static String normalizar(String texto) {
        return Normalizer.normalize(Objects.toString(texto, ""), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").strip();
    }
    private static Set<String> palavras(String texto) {
        var resultado = new HashSet<>(Arrays.asList(normalizar(texto).split(" +")));
        resultado.removeAll(IGNORAR);
        resultado.remove("");
        return resultado;
    }
    public List<Servico> buscar(String consulta) {
        if (consulta.length() > 300) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use até 300 caracteres na busca");
        Set<String> termos = palavras(consulta);
        if (termos.isEmpty()) return List.of();
        return store.listarServicos().stream().map(s -> new Resultado(s, pontuar(s, termos)))
                .filter(r -> r.pontos() > 0)
                .sorted(Comparator.comparingDouble(Resultado::pontos).reversed().thenComparing(r -> r.servico().servicoId()))
                .limit(10).map(Resultado::servico).toList();
    }
    private double pontuar(Servico servico, Set<String> termos) {
        Set<String> titulo = palavras(servico.itemNome() + " " + Objects.toString(servico.nome(), ""));
        Set<String> outras = palavras(String.join(" ", servico.palavrasChave()) + " " + String.join(" ", servico.temas()));
        double pontos = 0;
        for (String termo : termos) {
            if (titulo.contains(termo)) { pontos += 4; continue; }
            if (outras.contains(termo)) { pontos += 2; continue; }
            Set<String> sinonimos = palavras(SINONIMOS.getOrDefault(termo, ""));
            if (!Collections.disjoint(titulo, sinonimos) || !Collections.disjoint(outras, sinonimos)) { pontos += 1; continue; }
            if (termo.length() >= 4 && titulo.stream().anyMatch(p -> distanciaUm(termo, p))) pontos += 0.5;
        }
        return pontos;
    }
    private static boolean distanciaUm(String a, String b) {
        if (Math.abs(a.length() - b.length()) > 1) return false;
        int i = 0, j = 0, erros = 0;
        while (i < a.length() && j < b.length()) {
            if (a.charAt(i) == b.charAt(j)) { i++; j++; continue; }
            if (++erros > 1) return false;
            if (a.length() >= b.length()) i++;
            if (b.length() >= a.length()) j++;
        }
        return erros + a.length() - i + b.length() - j <= 1;
    }
}
