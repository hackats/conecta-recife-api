package br.com.conectarecife.controller;

import br.com.conectarecife.data.DadosStore;
import br.com.conectarecife.model.Cidadao;
import br.com.conectarecife.model.Clique;
import br.com.conectarecife.service.Recomendador;
import br.com.conectarecife.service.Recomendador.Recomendacao;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/cidadaos")
public class CidadaoController {

    private final DadosStore store;

    private final Recomendador recomendador;

    public CidadaoController(DadosStore store, Recomendador recomendador) {
        this.store = store;
        this.recomendador = recomendador;
    }

    @GetMapping("/{id}")
    public Cidadao buscarPorId(@PathVariable String id) {
        Cidadao cidadao = store.findCidadao(id);
        if (cidadao == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Cidadão não encontrado: " + id);
        }
        return cidadao;
    }

    @GetMapping("/{id}/recomendacoes")
    public List<Recomendacao> listarRecomendacoes(@PathVariable String id) {
        return recomendador.recomendar(buscarPorId(id));
    }

    @GetMapping("/{id}/cliques")
    public List<Clique> listarCliques(@PathVariable String id) {
        if (store.findCidadao(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Cidadão não encontrado: " + id);
        }
        return store.findCliquesDoCidadao(id);
    }
}
