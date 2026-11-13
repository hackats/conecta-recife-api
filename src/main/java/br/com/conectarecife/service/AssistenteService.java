package br.com.conectarecife.service;

import br.com.conectarecife.data.DadosStore;
import br.com.conectarecife.model.Cidadao;
import br.com.conectarecife.model.Servico;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class AssistenteService {
    public record Pergunta(String mensagem, String cidadaoId) {}
    public record ServicoSugerido(Integer servicoId, String nome, String motivo) {}
    public record Acao(String tipo, String rotulo, String caminho) {}
    public record Resposta(String mensagem, List<ServicoSugerido> servicos,
                           List<Acao> acoes, List<String> avisos) {}

    private final DadosStore store;
    private final BuscaService busca;
    private final Recomendador recomendador;

    public AssistenteService(DadosStore store, BuscaService busca, Recomendador recomendador) {
        this.store = store;
        this.busca = busca;
        this.recomendador = recomendador;
    }

    public Resposta conversar(Pergunta pergunta) {
        if (pergunta == null || pergunta.mensagem() == null || pergunta.mensagem().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escreva uma pergunta para o assistente");
        }
        if (pergunta.mensagem().length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use até 500 caracteres por mensagem");
        }

        Cidadao cidadao = buscarCidadaoOpcional(pergunta.cidadaoId());
        String texto = BuscaService.normalizar(pergunta.mensagem());

        if (contem(texto, "novidade", "novo", "atualizacao")) {
            return resposta("No Conecta Recife, você pode buscar serviços por texto ou voz, "
                            + "ver sugestões com explicação, acompanhar uma jornada e consultar um protocolo.",
                    List.of(), List.of(new Acao("BUSCAR", "Buscar um serviço", "/servicos/busca")), List.of());
        }
        if (contem(texto, "protocolo", "acompanhar", "andamento", "solicitacao")) {
            return resposta("Para acompanhar uma solicitação, informe ou abra o protocolo recebido ao enviar o pedido. "
                            + "Nesta demonstração, o andamento é simulado.",
                    List.of(), List.of(new Acao("ACOMPANHAR", "Consultar protocolo", "/solicitacoes/{protocolo}")),
                    List.of("A linha do tempo desta versão é simulada."));
        }
        if (contem(texto, "privacidade", "consentimento", "dados de saude", "saude")) {
            return resposta("Você controla o uso de sinais de saúde nas sugestões. "
                            + "Ao desligar o consentimento, eles deixam de influenciar a próxima recomendação.",
                    List.of(), List.of(new Acao("PREFERENCIAS", "Abrir preferências", "/preferencias")), List.of());
        }
        if (cidadao != null && contem(texto, "recomende", "recomendacao", "sugestao", "sugerir")) {
            List<ServicoSugerido> sugestoes = recomendador.recomendar(cidadao).stream().limit(3)
                    .map(item -> new ServicoSugerido(item.servicoId(), item.nome(), item.motivo())).toList();
            return resposta("Encontrei sugestões com base na sua navegação e nas preferências autorizadas.",
                    sugestoes, List.of(new Acao("VITRINE", "Ver todas as sugestões",
                            "/cidadaos/" + cidadao.cidadaoId() + "/recomendacoes")), List.of());
        }

        List<ServicoSugerido> encontrados = busca.buscar(pergunta.mensagem()).stream().limit(3)
                .map(servico -> new ServicoSugerido(servico.servicoId(), nome(servico),
                        "Serviço encontrado no catálogo"))
                .toList();
        if (!encontrados.isEmpty()) {
            return resposta("Encontrei estes serviços no catálogo. Escolha um para ver requisitos e passos.", encontrados,
                    List.of(new Acao("BUSCAR", "Ver resultados da busca", "/servicos/busca")), List.of());
        }
        return resposta("Posso ajudar a encontrar serviços, explicar a jornada, mostrar novidades, "
                        + "acompanhar um protocolo ou abrir suas preferências. Tente dizer o que você precisa resolver.",
                List.of(), List.of(new Acao("BUSCAR", "Buscar serviços", "/servicos/busca")), List.of());
    }

    private Cidadao buscarCidadaoOpcional(String cidadaoId) {
        if (cidadaoId == null || cidadaoId.isBlank()) return null;
        Cidadao cidadao = store.findCidadao(cidadaoId);
        if (cidadao == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cidadão não encontrado: " + cidadaoId);
        }
        return cidadao;
    }

    private static Resposta resposta(String mensagem, List<ServicoSugerido> servicos,
                                     List<Acao> acoes, List<String> avisos) {
        return new Resposta(mensagem, servicos, acoes, avisos);
    }

    private static String nome(Servico servico) {
        return servico.itemNome() == null || servico.itemNome().isBlank() ? servico.nome() : servico.itemNome();
    }

    private static boolean contem(String texto, String... termos) {
        for (String termo : termos) {
            if (texto.toLowerCase(Locale.ROOT).contains(termo)) return true;
        }
        return false;
    }
}
