package br.com.conectarecife.controller;

import br.com.conectarecife.service.RecomendacoesCsvService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Exportação CSV")
@RestController
public class RecomendacoesCsvController {
    private final RecomendacoesCsvService csv;

    public RecomendacoesCsvController(RecomendacoesCsvService csv) {
        this.csv = csv;
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Gerar recomendações completas",
            description = "Gera e valida o arquivo UTF-8 para todos os cidadãos carregados, com até dez serviços distintos por cidadão. Operação custosa, adequada à preparação da entrega. Retorna download de recomendacoes.csv.")
    @GetMapping("/recomendacoes.csv")
    public ResponseEntity<InputStreamResource> gerar() throws IOException {
        Path arquivo = Files.createTempFile("recomendacoes-download-", ".csv");
        try {
            csv.gerar(arquivo);
            long tamanho = Files.size(arquivo);
            var stream = new FilterInputStream(Files.newInputStream(arquivo)) {
                @Override
                public void close() throws IOException {
                    try { super.close(); } finally { Files.deleteIfExists(arquivo); }
                }
            };
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recomendacoes.csv")
                    .contentLength(tamanho).body(new InputStreamResource(stream));
        } catch (IOException | RuntimeException erro) {
            Files.deleteIfExists(arquivo);
            throw erro;
        }
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Validar arquivo de recomendações",
            description = "Envie o arquivo CSV no campo multipart arquivo. Valida cobertura, posições, serviços e UTF-8. CSV inválido retorna HTTP 200 com valido=false e lista limitada de erros.")
    @PostMapping(value = "/recomendacoes/validar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RecomendacoesCsvService.Validacao validar(@RequestPart("arquivo") MultipartFile arquivo)
            throws IOException {
        Path temporario = Files.createTempFile("recomendacoes-validacao-", ".csv");
        try {
            arquivo.transferTo(temporario);
            return csv.validar(temporario);
        } finally {
            Files.deleteIfExists(temporario);
        }
    }
}
