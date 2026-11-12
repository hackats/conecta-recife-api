package br.com.conectarecife.controller;

import br.com.conectarecife.service.SolicitacoesService;
import br.com.conectarecife.service.SolicitacoesService.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/solicitacoes")
public class SolicitacoesController {
    private final SolicitacoesService solicitacoes;

    public SolicitacoesController(SolicitacoesService solicitacoes) {
        this.solicitacoes = solicitacoes;
    }

    @PostMapping
    public ResponseEntity<Solicitacao> criar(@RequestBody Pedido pedido,
            @RequestHeader(value = "Idempotency-Key", required = false) String chave) {
        Solicitacao solicitacao = solicitacoes.criar(pedido, chave);
        return ResponseEntity.created(URI.create("/solicitacoes/" + solicitacao.protocolo())).body(solicitacao);
    }

    @GetMapping("/{protocolo}")
    public Solicitacao consultar(@PathVariable String protocolo) {
        return solicitacoes.consultar(protocolo);
    }
}
