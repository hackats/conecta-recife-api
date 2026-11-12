package br.com.conectarecife.controller;

import br.com.conectarecife.service.EventosService;
import br.com.conectarecife.service.EventosService.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Eventos e métricas")
@RestController
public class EventosController {
    private final EventosService eventos;

    public EventosController(EventosService eventos) {
        this.eventos = eventos;
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Registrar evento",
            description = "Tipos ABRIU, INICIOU, CONCLUIU e ABANDONOU. Inicie com ABRIU, depois INICIOU e CONCLUIU, ou ABANDONOU após abertura/início. eventoId permite repetição idempotente. Dados em memória.")
    @PostMapping("/eventos")
    public Evento registrar(@RequestBody Pedido pedido) {
        return eventos.registrar(pedido);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Consultar métricas",
            description = "Contagens e taxas de conclusão e abandono agrupadas por serviço e etapa. O cliente deve enviar os eventos. Taxas variam de 0 a 1.")
    @GetMapping("/metricas")
    public List<Metrica> metricas() {
        return eventos.metricas();
    }
}
