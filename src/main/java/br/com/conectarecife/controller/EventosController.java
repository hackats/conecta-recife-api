package br.com.conectarecife.controller;

import br.com.conectarecife.service.EventosService;
import br.com.conectarecife.service.EventosService.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class EventosController {
    private final EventosService eventos;

    public EventosController(EventosService eventos) {
        this.eventos = eventos;
    }

    @PostMapping("/eventos")
    public Evento registrar(@RequestBody Pedido pedido) {
        return eventos.registrar(pedido);
    }

    @GetMapping("/metricas")
    public List<Metrica> metricas() {
        return eventos.metricas();
    }
}
