package br.com.conectarecife.data;

import br.com.conectarecife.model.Cidadao;
import br.com.conectarecife.model.Clique;
import br.com.conectarecife.model.PerfilCidadao;
import br.com.conectarecife.model.Servico;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class DadosStore {

    private Map<String, Cidadao> cidadaos = Map.of();
    private Map<Integer, Servico> servicos = Map.of();
    private Map<String, List<Clique>> cliquesPorCidadao = Map.of();

    private Map<Integer, Integer> popularidade = Map.of();
    private Map<String, Map<Integer, Integer>> popularidadePorFaixa = Map.of();

    private Map<String, Map<Integer, Integer>> popularidadePorPerfil = Map.of();

    public void initialize(Map<String, Cidadao> cidadaos,
                           Map<Integer, Servico> servicos,
                           List<Clique> cliques) {
        this.cidadaos = Map.copyOf(cidadaos);
        this.servicos = Map.copyOf(servicos);

        var agrupados = new java.util.HashMap<String, java.util.ArrayList<Clique>>();
        var totais = new HashMap<Integer, Integer>();
        var porFaixa = new HashMap<String, Map<Integer, Integer>>();
        var porPerfil = new HashMap<String, Map<Integer, Integer>>();
        var perfis = new HashMap<String, Set<String>>();
        for (Clique clique : cliques) {
            for (String publico : perfis.computeIfAbsent(clique.cidadaoId(),
                    id -> PerfilCidadao.publicosDoCidadao(cidadaos.get(id)))) {
                porPerfil.computeIfAbsent(publico, ignored -> new HashMap<>())
                        .merge(clique.servicoId(), 1, Integer::sum);
            }
            totais.merge(clique.servicoId(), 1, Integer::sum);
            String faixa = cidadaos.get(clique.cidadaoId()).faixaEtaria();
            if (faixa != null && !faixa.isBlank()) {
                porFaixa.computeIfAbsent(faixa, ignored -> new HashMap<>())
                        .merge(clique.servicoId(), 1, Integer::sum);
            }
            agrupados.computeIfAbsent(clique.cidadaoId(), ignored -> new java.util.ArrayList<>())
                    .add(clique);
        }

        var immutable = new java.util.HashMap<String, List<Clique>>();
        agrupados.forEach((id, lista) -> immutable.put(id, List.copyOf(lista)));
        this.cliquesPorCidadao = Map.copyOf(immutable);
        this.popularidade = Map.copyOf(totais);
        porFaixa.replaceAll((faixa, contagens) -> Map.copyOf(contagens));
        this.popularidadePorFaixa = Map.copyOf(porFaixa);
        porPerfil.replaceAll((publico, contagens) -> Map.copyOf(contagens));
        this.popularidadePorPerfil = Map.copyOf(porPerfil);
    }

    public Cidadao findCidadao(String id) {
        return cidadaos.get(id);
    }

    public Servico findServico(Integer id) {
        return servicos.get(id);
    }

    public List<Clique> findCliquesDoCidadao(String id) {
        return cliquesPorCidadao.getOrDefault(id, List.of());
    }

    public Collection<Servico> listarServicos() {
        return servicos.values();
    }

    public int contarCliques(Integer servicoId) {
        return popularidade.getOrDefault(servicoId, 0);
    }

    public Map<Integer, Integer> cliquesPorFaixa(String faixa) {
        return faixa == null ? Map.of() : popularidadePorFaixa.getOrDefault(faixa, Map.of());
    }

    public int contarCliquesPorPerfil(String publico, Integer servicoId) {
        return popularidadePorPerfil.getOrDefault(publico, Map.of()).getOrDefault(servicoId, 0);
    }

    public int totalCidadaos() { return cidadaos.size(); }
    public int totalServicos() { return servicos.size(); }
    public int totalCliques() {
        return cliquesPorCidadao.values().stream().mapToInt(List::size).sum();
    }
}
