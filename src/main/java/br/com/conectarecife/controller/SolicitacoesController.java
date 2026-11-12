package br.com.conectarecife.controller;

import br.com.conectarecife.service.SolicitacoesService;
import br.com.conectarecife.service.SolicitacoesService.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Solicitações simuladas")
@RestController
@RequestMapping("/solicitacoes")
public class SolicitacoesController {
    private final SolicitacoesService solicitacoes;

    public SolicitacoesController(SolicitacoesService solicitacoes) {
        this.solicitacoes = solicitacoes;
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Criar solicitação simulada",
            description = "Gera protocolo e Location. Idempotency-Key é opcional, tem escopo por cidadão e deve ser reutilizada em tentativas do mesmo pedido. Reutilização para outro serviço recebe 409.")
    @PostMapping
    public ResponseEntity<Solicitacao> criar(@RequestBody Pedido pedido,
            @RequestHeader(value = "Idempotency-Key", required = false) String chave) {
        Solicitacao solicitacao = solicitacoes.criar(pedido, chave);
        return ResponseEntity.created(URI.create("/solicitacoes/" + solicitacao.protocolo())).body(solicitacao);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Consultar andamento simulado",
            description = "Recebida ao criar, em análise após 30 segundos e concluída após 60 segundos. Dados em memória, perdidos ao reiniciar.")
    @GetMapping("/{protocolo}")
    public Solicitacao consultar(@PathVariable String protocolo) {
        return solicitacoes.consultar(protocolo);
    }
}
