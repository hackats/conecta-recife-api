package br.com.conectarecife.loader;

import br.com.conectarecife.data.DadosStore;
import br.com.conectarecife.model.Clique;
import br.com.conectarecife.model.Cidadao;
import br.com.conectarecife.model.Servico;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Component
public class DadosInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosInitializer.class);

    private final CidadaoLoader cidadaoLoader;
    private final ServicoLoader servicoLoader;
    private final CliqueLoader cliqueLoader;
    private final DadosStore store;

    @Value("${app.data.directory:./data}")
    private String dataDirectory;

    @Value("${app.data.expected-cidadaos:89046}")
    private int expectedCidadaos;

    @Value("${app.data.expected-servicos:843}")
    private int expectedServicos;

    @Value("${app.data.expected-cliques:593056}")
    private int expectedCliques;

    public DadosInitializer(CidadaoLoader cidadaoLoader,
                            ServicoLoader servicoLoader,
                            CliqueLoader cliqueLoader,
                            DadosStore store) {
        this.cidadaoLoader = cidadaoLoader;
        this.servicoLoader = servicoLoader;
        this.cliqueLoader = cliqueLoader;
        this.store = store;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path dir = Path.of(dataDirectory);
        Path cidadaosPath = dir.resolve("cidadaos.csv");
        Path servicosPath = dir.resolve("servicos.csv");
        Path historicoPath = dir.resolve("historico.csv");

        requireFile(cidadaosPath);
        requireFile(servicosPath);
        requireFile(historicoPath);

        long inicio = System.nanoTime();

        long etapa = System.nanoTime();
        Map<String, Cidadao> cidadaos = cidadaoLoader.load(cidadaosPath);
        log.info("Carga cidadaos.csv: {} registros em {} ms",
                cidadaos.size(), elapsedMs(etapa));

        etapa = System.nanoTime();
        Map<Integer, Servico> servicos = servicoLoader.load(servicosPath);
        log.info("Carga servicos.csv: {} registros em {} ms",
                servicos.size(), elapsedMs(etapa));

        etapa = System.nanoTime();
        List<Clique> cliques = cliqueLoader.load(historicoPath);
        log.info("Carga historico.csv: {} registros em {} ms",
                cliques.size(), elapsedMs(etapa));

        checkTotal("cidadãos", cidadaos.size(), expectedCidadaos);
        checkTotal("serviços", servicos.size(), expectedServicos);
        checkTotal("cliques", cliques.size(), expectedCliques);

        // Além das contagens, valide as referências antes de publicar os índices.
        for (Clique clique : cliques) {
            if (!cidadaos.containsKey(clique.cidadaoId())) {
                throw new IllegalStateException("Clique com cidadão desconhecido: " + clique.cidadaoId());
            }
            if (!servicos.containsKey(clique.servicoId())) {
                throw new IllegalStateException("Clique com serviço desconhecido: " + clique.servicoId());
            }
        }
        log.info("Integridade referencial: todos os cliques apontam para cidadãos e serviços existentes");

        long indexStart = System.nanoTime();
        store.initialize(cidadaos, servicos, cliques);
        log.info("Indexação dos cliques por cidadão concluída em {} ms", elapsedMs(indexStart));

        log.info("DADOS CARREGADOS COM SUCESSO — cidadãos: {} (esperado {}), serviços: {} (esperado {}), cliques: {} (esperado {}). Tempo total: {} ms",
                store.totalCidadaos(), expectedCidadaos,
                store.totalServicos(), expectedServicos,
                store.totalCliques(), expectedCliques,
                elapsedMs(inicio));
    }

    private static void requireFile(Path path) {
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Arquivo CSV não encontrado: " + path.toAbsolutePath());
        }
    }

    private static void checkTotal(String label, int actual, int expected) {
        if (actual != expected) {
            throw new IllegalStateException("Total de " + label + " divergente. Esperado: "
                    + expected + ", encontrado: " + actual);
        }
    }

    private static long elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }
}
