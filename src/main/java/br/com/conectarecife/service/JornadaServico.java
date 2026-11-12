package br.com.conectarecife.service;

import br.com.conectarecife.model.Servico;
import org.springframework.web.util.HtmlUtils;
import java.util.Arrays;
import java.util.List;

public final class JornadaServico {
    public record Jornada(Integer servicoId, String nome, List<String> requisitos, List<String> passos) {}

    private JornadaServico() {}

    public static Jornada criar(Servico servico) {
        return new Jornada(servico.servicoId(), servico.nome(),
                simplificar(servico.requisitos(), "Consulte o órgão responsável sobre os documentos necessários."),
                simplificar(servico.passos(), "Consulte o órgão responsável sobre como solicitar este serviço."));
    }

    private static List<String> simplificar(String texto, String alternativa) {
        if (texto == null || texto.isBlank()) return List.of(alternativa);
        String limpo = HtmlUtils.htmlUnescape(texto)
                .replaceAll("(?is)<(script|style)\\b[^>]*>.*?</\\1>", "")
                .replaceAll("(?i)<(?:br\\s*/?|/p|/div|/li)>", "\n")
                .replaceAll("<[^>]+>", "")
                .replaceAll("(?i)\\bda tramitação\\b", "do andamento")
                .replaceAll("(?i)\\bna tramitação\\b", "no andamento")
                .replaceAll("(?i)\\ba tramitação\\b", "o andamento")
                .replaceAll("(?i)\\btramitação\\b", "andamento")
                .replaceAll("(?i)\\brequerer\\b", "solicitar");
        List<String> linhas = Arrays.stream(limpo.split("\\R"))
                .map(linha -> linha.replaceAll("^[\\s•*-]+", "").replaceAll("[\\t ]+", " ").strip())
                .filter(linha -> !linha.isBlank())
                .toList();
        return linhas.isEmpty() ? List.of(alternativa) : linhas;
    }
}
