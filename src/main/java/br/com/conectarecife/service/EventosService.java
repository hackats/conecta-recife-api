package br.com.conectarecife.service;

import br.com.conectarecife.data.DadosStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
public class EventosService {
    public enum Tipo { ABRIU, INICIOU, CONCLUIU, ABANDONOU }
    public record Pedido(String eventoId, String jornadaId, String cidadaoId,
                         Integer servicoId, String etapa, Tipo tipo) {}
    public record Evento(Pedido dados, Instant registradoEm) {}
    public record Metrica(Integer servicoId, String etapa, long abriu, long iniciou,
                          long concluiu, long abandonou, long emAndamento,
                          double taxaConclusao, double taxaAbandono) {}
    private record Jornada(String jornadaId, String cidadaoId, Integer servicoId, String etapa) {}
    private record Estado(boolean iniciou, Tipo ultimo) {}
    private record Grupo(Integer servicoId, String etapa) {}

    private final DadosStore store;
    private final Map<String, Evento> eventos = new HashMap<>();
    private final Map<Jornada, Estado> jornadas = new HashMap<>();

    public EventosService(DadosStore store) {
        this.store = store;
    }

    public synchronized Evento registrar(Pedido pedido) {
        if (vazio(pedido.eventoId()) || vazio(pedido.jornadaId()) || vazio(pedido.cidadaoId())
                || vazio(pedido.etapa()) || pedido.servicoId() == null || pedido.tipo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe eventoId, jornadaId, cidadaoId, servicoId, etapa e tipo");
        }
        Evento existente = eventos.get(pedido.eventoId());
        if (existente != null) {
            if (!existente.dados().equals(pedido)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "eventoId já usado com outros dados");
            }
            return existente;
        }
        if (store.findCidadao(pedido.cidadaoId()) == null || store.findServico(pedido.servicoId()) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cidadão ou serviço não encontrado");
        }
        Jornada chave = new Jornada(pedido.jornadaId(), pedido.cidadaoId(), pedido.servicoId(), pedido.etapa());
        Estado anterior = jornadas.get(chave);
        if (!permitido(anterior, pedido.tipo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Sequência inválida: abra a etapa, inicie e conclua, ou registre abandono");
        }
        boolean iniciou = pedido.tipo() == Tipo.INICIOU || anterior != null && anterior.iniciou();
        Evento evento = new Evento(pedido, Instant.now());
        jornadas.put(chave, new Estado(iniciou, pedido.tipo()));
        eventos.put(pedido.eventoId(), evento);
        return evento;
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static boolean permitido(Estado anterior, Tipo tipo) {
        if (anterior == null) return tipo == Tipo.ABRIU;
        return switch (anterior.ultimo()) {
            case ABRIU -> tipo == Tipo.INICIOU || tipo == Tipo.ABANDONOU;
            case INICIOU -> tipo == Tipo.CONCLUIU || tipo == Tipo.ABANDONOU;
            case CONCLUIU, ABANDONOU -> false;
        };
    }

    public synchronized List<Metrica> metricas() {
        Map<Grupo, long[]> contagens = new HashMap<>();
        jornadas.forEach((jornada, estado) -> {
            long[] totais = contagens.computeIfAbsent(new Grupo(jornada.servicoId(), jornada.etapa()),
                    ignored -> new long[4]);
            totais[0]++;
            if (estado.iniciou()) totais[1]++;
            if (estado.ultimo() == Tipo.CONCLUIU) totais[2]++;
            if (estado.ultimo() == Tipo.ABANDONOU) totais[3]++;
        });
        return contagens.entrySet().stream().map(entrada -> {
            long[] total = entrada.getValue();
            return new Metrica(entrada.getKey().servicoId(), entrada.getKey().etapa(),
                    total[0], total[1], total[2], total[3], total[0] - total[2] - total[3],
                    (double) total[2] / total[0], (double) total[3] / total[0]);
        }).sorted(Comparator.comparing(Metrica::servicoId).thenComparing(Metrica::etapa)).toList();
    }
}
