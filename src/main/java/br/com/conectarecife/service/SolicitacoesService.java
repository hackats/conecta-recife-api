package br.com.conectarecife.service;

import br.com.conectarecife.data.DadosStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SolicitacoesService {
    public record Pedido(String cidadaoId, Integer servicoId) {}
    public record Etapa(String status, Instant data) {}
    public record Solicitacao(String protocolo, String cidadaoId, Integer servicoId,
                              boolean simulada, String status, List<Etapa> linhaDoTempo) {}
    private record Registro(String protocolo, Pedido pedido, Instant criadoEm) {}
    private record Chave(String cidadaoId, String valor) {}

    private final DadosStore store;
    private final Clock clock;
    private final ConcurrentHashMap<String, Registro> registros = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Chave, Registro> repeticoes = new ConcurrentHashMap<>();

    @Autowired
    public SolicitacoesService(DadosStore store) {
        this(store, Clock.systemUTC());
    }

    public SolicitacoesService(DadosStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    public Solicitacao criar(Pedido pedido, String chave) {
        if (pedido.cidadaoId() == null || pedido.cidadaoId().isBlank() || pedido.servicoId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe cidadaoId e servicoId");
        }
        if (store.findCidadao(pedido.cidadaoId()) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Cidadão não encontrado: " + pedido.cidadaoId());
        }
        if (store.findServico(pedido.servicoId()) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Serviço não encontrado: " + pedido.servicoId());
        }
        if (chave == null) return representar(registrar(pedido));
        if (chave.isBlank() || chave.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Idempotency-Key deve conter de 1 a 200 caracteres");
        }
        Registro registro = repeticoes.compute(new Chave(pedido.cidadaoId(), chave), (id, anterior) -> {
            if (anterior != null && !anterior.pedido().equals(pedido)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Idempotency-Key já utilizada com outro serviço");
            }
            return anterior == null ? registrar(pedido) : anterior;
        });
        return representar(registro);
    }

    private Registro registrar(Pedido pedido) {
        Registro registro = new Registro(UUID.randomUUID().toString(), pedido, clock.instant());
        registros.put(registro.protocolo(), registro);
        return registro;
    }

    public Solicitacao consultar(String protocolo) {
        Registro registro = registros.get(protocolo);
        if (registro == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Protocolo não encontrado: " + protocolo);
        }
        return representar(registro);
    }

    private Solicitacao representar(Registro registro) {
        Instant agora = clock.instant();
        List<Etapa> etapas = List.of(
                new Etapa("RECEBIDA", registro.criadoEm()),
                new Etapa("EM_ANALISE", registro.criadoEm().plusSeconds(30)),
                new Etapa("CONCLUIDA", registro.criadoEm().plusSeconds(60)));
        List<Etapa> ocorridas = etapas.stream().filter(etapa -> !etapa.data().isAfter(agora)).toList();
        return new Solicitacao(registro.protocolo(), registro.pedido().cidadaoId(),
                registro.pedido().servicoId(), true,
                ocorridas.isEmpty() ? "RECEBIDA" : ocorridas.getLast().status(), ocorridas);
    }
}
