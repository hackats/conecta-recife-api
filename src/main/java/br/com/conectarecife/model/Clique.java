package br.com.conectarecife.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record Clique(
        String cidadaoId,
        String sessaoId,
        Integer ordemNaSessao,
        LocalDate data,
        LocalTime hora,
        Integer servicoId
) {}
