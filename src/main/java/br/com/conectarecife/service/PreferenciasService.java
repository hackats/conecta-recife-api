package br.com.conectarecife.service;

import br.com.conectarecife.data.DadosStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class PreferenciasService {
    public record Preferencias(String cidadaoId, boolean consentimentoSaude, boolean aceitaNotificacao) {}
    public record Alteracao(String cidadaoId, Boolean consentimentoSaude, Boolean aceitaNotificacao) {}

    private final DadosStore store;
    private final ConcurrentHashMap<String, Preferencias> preferencias = new ConcurrentHashMap<>();

    public PreferenciasService(DadosStore store) {
        this.store = store;
    }

    public Preferencias consultar(String id) {
        var cidadao = store.findCidadao(id);
        if (cidadao == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cidadão não encontrado: " + id);
        }
        return preferencias.getOrDefault(id,
                new Preferencias(id, false, Boolean.TRUE.equals(cidadao.aceitaNotificacao())));
    }

    public Preferencias alterar(Alteracao alteracao) {
        if (alteracao.cidadaoId() == null || alteracao.cidadaoId().isBlank()
                || alteracao.consentimentoSaude() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe cidadaoId e consentimentoSaude");
        }
        Preferencias padrao = consultar(alteracao.cidadaoId());
        return preferencias.compute(alteracao.cidadaoId(), (id, anterior) -> {
            Preferencias atual = anterior == null ? padrao : anterior;
            boolean notificacoes = alteracao.aceitaNotificacao() == null
                    ? atual.aceitaNotificacao() : alteracao.aceitaNotificacao();
            return new Preferencias(id, alteracao.consentimentoSaude(), notificacoes);
        });
    }
}
