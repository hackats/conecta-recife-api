package br.com.conectarecife.controller;

import br.com.conectarecife.service.AnonimoService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class AnonimoController {
    private final AnonimoService service;
    public AnonimoController(AnonimoService service) { this.service = service; }
    @GetMapping("/anonimo/recomendacoes")
    public AnonimoService.Resposta recomendar(@RequestParam(defaultValue = "") List<Integer> acessados,
            @RequestParam(defaultValue = "") List<Integer> ignorados,
            jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return service.recomendar(acessados, ignorados);
    }
}
