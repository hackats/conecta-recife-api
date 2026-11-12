package br.com.conectarecife.controller;

import br.com.conectarecife.service.AnonimoService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Recomendações sem login")
@RestController
public class AnonimoController {
    private final AnonimoService service;
    public AnonimoController(AnonimoService service) { this.service = service; }
    @io.swagger.v3.oas.annotations.Operation(summary = "Recomendar pela sessão",
            description = "acessados recebe até 50 IDs válidos separados por vírgula, em ordem de navegação, por exemplo 458,1359. Limpe o contexto em uma nova sessão. Sem perfil ou inferência de elegibilidade. transicoesDisponiveis=false indica ausência da base de visitas anônimas. Agregados semanais e diversidade limitam picos de procura.")
    @GetMapping("/anonimo/recomendacoes")
    public AnonimoService.Resposta recomendar(@RequestParam(defaultValue = "") List<Integer> acessados,
            jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return service.recomendar(acessados);
    }
}
