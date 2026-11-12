package br.com.conectarecife.service;

import br.com.conectarecife.data.DadosStore;
import br.com.conectarecife.model.Cidadao;
import br.com.conectarecife.model.Clique;
import br.com.conectarecife.model.PerfilCidadao;
import br.com.conectarecife.model.Servico;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class Recomendador {

    public record Recomendacao(Integer servicoId, String nome, String motivo) {}

    private record Candidato(Recomendacao recomendacao, double pontuacao) {}

    private final DadosStore store;

    public Recomendador(DadosStore store) {
        this.store = store;
    }

    public List<Recomendacao> recomendar(Cidadao cidadao) {
        Set<Integer> usados = store.findCliquesDoCidadao(cidadao.cidadaoId()).stream()
                .map(Clique::servicoId)
                .collect(Collectors.toSet());
        Set<String> temasVisitados = usados.stream()
                .flatMap(id -> store.findServico(id).temas().stream())
                .map(Recomendador::normalizar)
                .collect(Collectors.toSet());
        Set<String> perfil = PerfilCidadao.publicosDoCidadao(cidadao);
        Map<Integer, Integer> porFaixa = store.cliquesPorFaixa(cidadao.faixaEtaria());

        return store.listarServicos().stream()
                .filter(servico -> !usados.contains(servico.servicoId()))
                .map(servico -> classificar(servico, porFaixa, perfil, temasVisitados))
                .sorted(Comparator.comparingDouble(Candidato::pontuacao).reversed()
                        .thenComparing(candidato -> candidato.recomendacao().servicoId()))
                .limit(10)
                .map(Candidato::recomendacao)
                .toList();
    }

    private Candidato classificar(Servico servico, Map<Integer, Integer> porFaixa,
                                 Set<String> perfil, Set<String> temasVisitados) {
        int cliques = store.contarCliques(servico.servicoId());
        int cliquesFaixa = porFaixa.getOrDefault(servico.servicoId(), 0);
        int cliquesPerfil = perfil.stream()
                .mapToInt(publico -> store.contarCliquesPorPerfil(publico, servico.servicoId()))
                .max().orElse(0);
        boolean combinaPerfil = servico.publicosAlvo().stream()
                .map(Recomendador::normalizar)
                .anyMatch(perfil::contains);
        boolean combinaHistorico = servico.temas().stream()
                .map(Recomendador::normalizar)
                .anyMatch(temasVisitados::contains);

        double pontuacao = Math.log1p(cliques) + 2 * Math.log1p(cliquesFaixa)
                + Math.log1p(cliquesPerfil) + (combinaPerfil ? 5 : 0) + (combinaHistorico ? 4 : 0);
        List<String> motivos = new ArrayList<>();
        if (combinaPerfil) motivos.add("Compatível com seu perfil");
        if (combinaHistorico) motivos.add("Temas em comum com serviços que você acessou");
        if (cliquesFaixa > 0) motivos.add("Acessado por pessoas da sua faixa etária");
        if (cliquesPerfil > 0) motivos.add("Acessado por pessoas com perfil semelhante");
        if (cliques > 0) motivos.add("Acessado no catálogo");
        if (motivos.isEmpty()) motivos.add("Disponível no catálogo");

        return new Candidato(new Recomendacao(servico.servicoId(), servico.nome(),
                String.join(". ", motivos)), pontuacao);
    }

    private static String normalizar(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").strip().toLowerCase(Locale.ROOT);
    }
}
