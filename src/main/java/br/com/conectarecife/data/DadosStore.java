package br.com.conectarecife.data;

import br.com.conectarecife.model.Cidadao;
import br.com.conectarecife.model.Clique;
import br.com.conectarecife.model.Servico;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DadosStore {

    private Map<String, Cidadao> cidadaos = Map.of();
    private Map<Integer, Servico> servicos = Map.of();
    private Map<String, List<Clique>> cliquesPorCidadao = Map.of();

    public void initialize(Map<String, Cidadao> cidadaos,
                           Map<Integer, Servico> servicos,
                           List<Clique> cliques) {
        this.cidadaos = Map.copyOf(cidadaos);
        this.servicos = Map.copyOf(servicos);

        var agrupados = new java.util.HashMap<String, java.util.ArrayList<Clique>>();
        for (Clique clique : cliques) {
            agrupados.computeIfAbsent(clique.cidadaoId(), ignored -> new java.util.ArrayList<>())
                    .add(clique);
        }

        var immutable = new java.util.HashMap<String, List<Clique>>();
        agrupados.forEach((id, lista) -> immutable.put(id, List.copyOf(lista)));
        this.cliquesPorCidadao = Map.copyOf(immutable);
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

    public int totalCidadaos() { return cidadaos.size(); }
    public int totalServicos() { return servicos.size(); }
    public int totalCliques() {
        return cliquesPorCidadao.values().stream().mapToInt(List::size).sum();
    }
}
