package br.com.conectarecife.service;

import br.com.conectarecife.data.DadosStore;
import br.com.conectarecife.model.Cidadao;
import org.apache.commons.csv.*;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@Service
public class RecomendacoesCsvService {
    public record Validacao(boolean valido, int cidadaos, long linhas, long totalErros, List<String> erros) {}
    private static final List<String> CABECALHO = List.of("cidadao_id", "posicao", "servico_id", "motivo");
    private static final class Pessoa {
        int linhas;
        final Set<Integer> servicos = new HashSet<>();
        final Set<Integer> posicoes = new HashSet<>();
    }
    private static final class Erros {
        long total;
        final List<String> mensagens = new ArrayList<>();
        void adicionar(String mensagem) {
            total++;
            if (mensagens.size() < 20) mensagens.add(mensagem);
        }
    }

    private final DadosStore store;
    private final Recomendador recomendador;

    public RecomendacoesCsvService(DadosStore store, Recomendador recomendador) {
        this.store = store;
        this.recomendador = recomendador;
    }

    public Validacao gerar(Path destino) throws IOException {
        Path absoluto = destino.toAbsolutePath();
        Files.createDirectories(absoluto.getParent());
        Path temporario = Files.createTempFile(absoluto.getParent(), "recomendacoes-", ".tmp");
        try {
            var formato = CSVFormat.RFC4180.builder().setHeader(CABECALHO.toArray(String[]::new)).get();
            try (var writer = Files.newBufferedWriter(temporario, StandardCharsets.UTF_8);
                 var csv = new CSVPrinter(writer, formato)) {
                var cidadaos = store.listarCidadaos().stream()
                        .sorted(Comparator.comparing(Cidadao::cidadaoId)).toList();
                for (Cidadao cidadao : cidadaos) {
                    int posicao = 0;
                    var recomendacoes = recomendador.recomendar(cidadao);
                    if (recomendacoes.isEmpty()) {
                        throw new IOException("Cidadão sem serviços elegíveis; exportação completa indisponível");
                    }
                    for (var recomendacao : recomendacoes) {
                        csv.printRecord(cidadao.cidadaoId(), ++posicao,
                                recomendacao.servicoId(), recomendacao.motivo());
                    }
                }
            }
            Validacao validacao = validar(temporario);
            if (!validacao.valido()) throw new IOException("CSV gerado inválido: " + validacao.erros());
            try {
                Files.move(temporario, absoluto, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException erro) {
                Files.move(temporario, absoluto, StandardCopyOption.REPLACE_EXISTING);
            }
            return validacao;
        } finally {
            Files.deleteIfExists(temporario);
        }
    }

    public Validacao validar(Path arquivo) throws IOException {
        var pessoas = new HashMap<String, Pessoa>();
        var erros = new Erros();
        long linhas = 0;
        var formato = CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(false).get();
        try (var reader = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8);
             var parser = formato.parse(reader)) {
            if (!parser.getHeaderNames().equals(CABECALHO)) {
                erros.adicionar("Cabeçalho esperado: " + String.join(",", CABECALHO));
            } else {
                for (CSVRecord registro : parser) {
                    linhas++;
                    validarLinha(registro, pessoas, erros);
                }
            }
        } catch (UncheckedIOException | IllegalArgumentException erro) {
            erros.adicionar("CSV malformado ou codificação UTF-8 inválida");
        } catch (java.nio.charset.CharacterCodingException erro) {
            erros.adicionar("Codificação UTF-8 inválida");
        }
        for (Pessoa pessoa : pessoas.values()) {
            if (pessoa.posicoes.size() != pessoa.linhas
                    || !pessoa.posicoes.containsAll(java.util.stream.IntStream.rangeClosed(1, pessoa.linhas)
                            .boxed().toList())) {
                erros.adicionar("Posições devem ser consecutivas por cidadão, começando em 1");
            }
        }
        if (pessoas.size() != store.totalCidadaos()) {
            erros.adicionar("Cobertura incompleta: esperado " + store.totalCidadaos()
                    + " cidadãos, encontrado " + pessoas.size());
        }
        return new Validacao(erros.total == 0, pessoas.size(), linhas, erros.total, List.copyOf(erros.mensagens));
    }

    private void validarLinha(CSVRecord registro, Map<String, Pessoa> pessoas, Erros erros) {
        if (registro.size() != CABECALHO.size()) {
            erros.adicionar("Quantidade de colunas inválida na linha " + registro.getRecordNumber());
            return;
        }
        String cidadao = registro.get("cidadao_id");
        if (store.findCidadao(cidadao) == null) {
            erros.adicionar("Cidadão inexistente na linha " + registro.getRecordNumber());
            return;
        }
        Pessoa pessoa = pessoas.computeIfAbsent(cidadao, ignored -> new Pessoa());
        pessoa.linhas++;
        if (pessoa.linhas > 10) erros.adicionar("Mais de dez recomendações para um cidadão");
        try {
            int servico = Integer.parseInt(registro.get("servico_id"));
            int posicao = Integer.parseInt(registro.get("posicao"));
            if (store.findServico(servico) == null) erros.adicionar("Serviço inexistente");
            if (!pessoa.servicos.add(servico)) erros.adicionar("Serviço duplicado para um cidadão");
            if (posicao < 1 || posicao > 10 || !pessoa.posicoes.add(posicao)) {
                erros.adicionar("Posição inválida ou duplicada");
            }
        } catch (NumberFormatException erro) {
            erros.adicionar("servico_id e posicao devem ser inteiros");
        }

    }
}
