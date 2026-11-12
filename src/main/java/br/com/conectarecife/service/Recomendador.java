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

    public record Pesos(double popularidade, double perfil, double transicao) {
        public Pesos {
            if (!Double.isFinite(popularidade) || !Double.isFinite(perfil)
                    || !Double.isFinite(transicao) || popularidade < 0 || perfil < 0
                    || transicao < 0 || popularidade + perfil + transicao == 0) {
                throw new IllegalArgumentException("Pesos devem ser finitos, não negativos e ter soma positiva");
            }
        }
    }

    public record Sinais(Recomendacao recomendacao, double popularidade,
                         double perfil, double transicao, List<String> motivosPerfil, List<String> motivosPopularidade) {
        public double pontuar(Pesos pesos) {
            return pesos.popularidade() * popularidade + pesos.perfil() * perfil
                    + pesos.transicao() * transicao;
        }
    }

    private final Map<Integer, Set<String>> temas = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Integer, Set<String>> publicos = new java.util.concurrent.ConcurrentHashMap<>();
    private Pesos pesos = new Pesos(1, 1, 0);

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarPesos(
            @org.springframework.beans.factory.annotation.Value("${app.recomendacao.a:1}") double a,
            @org.springframework.beans.factory.annotation.Value("${app.recomendacao.b:1}") double b,
            @org.springframework.beans.factory.annotation.Value("${app.recomendacao.c:0}") double c) {
        pesos = new Pesos(a, b, c);
    }

    private final DadosStore store;

    public Recomendador(DadosStore store) {
        this.store = store;
    }

    public List<Recomendacao> recomendar(Cidadao cidadao) {
        return ordenar(sinais(cidadao), pesos);
    }

    public static List<Recomendacao> ordenar(List<Sinais> sinais, Pesos pesos) {
        var ordem = Comparator.comparingDouble((Sinais sinal) -> sinal.pontuar(pesos)).reversed()
                .thenComparing(sinal -> sinal.recomendacao().servicoId());
        var melhores = new java.util.PriorityQueue<Sinais>(11, ordem.reversed());
        for (Sinais sinal : sinais) {
            melhores.add(sinal);
            if (melhores.size() > 10) melhores.poll();
        }
        return melhores.stream().sorted(ordem).map(sinal -> {
            Recomendacao recomendacao = sinal.recomendacao();
            List<String> motivos = new ArrayList<>();
            if (pesos.perfil() > 0) motivos.addAll(sinal.motivosPerfil());
            if (pesos.popularidade() > 0) motivos.addAll(sinal.motivosPopularidade());
            if (pesos.transicao() > 0 && sinal.transicao() > 0) {
                motivos.add("Outros acessos seguiram de serviços que você utilizou");
            }
            if (motivos.isEmpty()) motivos.add("Disponível no catálogo");
            return new Recomendacao(recomendacao.servicoId(), recomendacao.nome(), String.join(". ", motivos));
        }).toList();
    }

    public List<Sinais> sinais(Cidadao cidadao) {
        Set<Integer> usados = store.findCliquesDoCidadao(cidadao.cidadaoId()).stream()
                .map(Clique::servicoId)
                .collect(Collectors.toSet());
        Set<String> temasVisitados = usados.stream()
                .flatMap(id -> store.findServico(id).temas().stream())
                .map(Recomendador::normalizar)
                .collect(Collectors.toSet());
        Set<String> perfil = PerfilCidadao.publicosDoCidadao(cidadao);
        Map<Integer, Integer> porFaixa = store.cliquesPorFaixa(cidadao.faixaEtaria());

        Map<Integer, Integer> transicoes = new java.util.HashMap<>();
        for (Integer usado : usados) {
            store.transicoesDe(usado).forEach((destino, quantidade) ->
                    transicoes.merge(destino, quantidade, Integer::sum));
        }
        return store.listarServicos().stream()
                .filter(servico -> !usados.contains(servico.servicoId()))
                .map(servico -> classificar(servico, porFaixa, perfil, temasVisitados, transicoes))
                .toList();
    }

    private Sinais classificar(Servico servico, Map<Integer, Integer> porFaixa,
                                 Set<String> perfil, Set<String> temasVisitados, Map<Integer, Integer> transicoes) {
        int cliques = store.contarCliques(servico.servicoId());
        int cliquesFaixa = porFaixa.getOrDefault(servico.servicoId(), 0);
        int cliquesPerfil = perfil.stream()
                .mapToInt(publico -> store.contarCliquesPorPerfil(publico, servico.servicoId()))
                .max().orElse(0);
        boolean combinaPerfil = publicos.computeIfAbsent(servico.servicoId(), id -> normalizarLista(servico.publicosAlvo())).stream()
                .anyMatch(perfil::contains);
        boolean combinaHistorico = temas.computeIfAbsent(servico.servicoId(), id -> normalizarLista(servico.temas())).stream()
                .anyMatch(temasVisitados::contains);

        double popularidade = Math.log1p(cliques) + 2 * Math.log1p(cliquesFaixa)
                + Math.log1p(cliquesPerfil);
        List<String> motivos = new ArrayList<>();
        if (combinaPerfil) motivos.add("Compatível com seu perfil");
        if (combinaHistorico) motivos.add("Temas em comum com serviços que você acessou");
        List<String> motivosPerfil = List.copyOf(motivos);
        motivos.clear();
        if (cliquesFaixa > 0) motivos.add("Acessado por pessoas da sua faixa etária");
        if (cliquesPerfil > 0) motivos.add("Acessado por pessoas com perfil semelhante");
        if (cliques > 0) motivos.add("Acessado no catálogo");
        return new Sinais(new Recomendacao(servico.servicoId(), servico.nome(),
                String.join(". ", motivos)), popularidade,
                (combinaPerfil ? 5 : 0) + (combinaHistorico ? 4 : 0),
                Math.log1p(transicoes.getOrDefault(servico.servicoId(), 0)), motivosPerfil, List.copyOf(motivos));
    }

    private static Set<String> normalizarLista(List<String> valores) {
        return valores.stream().map(Recomendador::normalizar).collect(Collectors.toSet());
    }

    private static String normalizar(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").strip().toLowerCase(Locale.ROOT);
    }
}
