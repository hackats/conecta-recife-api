package br.com.conectarecife.controller;

import br.com.conectarecife.data.DadosStore;
import br.com.conectarecife.model.Servico;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/servicos")
public class ServicoController {

    private final DadosStore store;

    public ServicoController(DadosStore store) {
        this.store = store;
    }

    @GetMapping("/{id}")
    public Servico buscarPorId(@PathVariable Integer id) {
        Servico servico = store.findServico(id);
        if (servico == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Serviço não encontrado: " + id);
        }
        return servico;
    }
}
