package br.com.conectarecife.controller;

import br.com.conectarecife.service.AssistenteService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Assistente")
@RestController
public class AssistenteController {
    private final AssistenteService assistente;

    public AssistenteController(AssistenteService assistente) {
        this.assistente = assistente;
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Conversar com o assistente",
            description = "Interpreta dúvidas em linguagem natural com base no catálogo, jornadas, preferências e sugestões existentes. Não decide elegibilidade e não cria respostas sobre regras que não estejam no catálogo.")
    @PostMapping("/assistente/conversar")
    public AssistenteService.Resposta conversar(@RequestBody AssistenteService.Pergunta pergunta) {
        return assistente.conversar(pergunta);
    }
}
