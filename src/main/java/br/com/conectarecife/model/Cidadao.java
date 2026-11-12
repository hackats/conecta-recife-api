package br.com.conectarecife.model;

public record Cidadao(
        String cidadaoId,
        Integer idade,
        String faixaEtaria,
        String sexo,
        Integer rpa,
        Boolean empreendedor,
        Boolean mei,
        Boolean servidorPublico,
        Boolean proprietarioImovel,
        Boolean tutorAnimal,
        Boolean estudante,
        Boolean pessoaComDeficiencia,
        Boolean cadastroUnico,
        Boolean bolsaFamilia,
        Boolean usuarioCompaz,
        Boolean hipertensao,
        Boolean diabetes,
        Boolean saudeMental,
        Boolean gestante,
        Boolean microcredito,
        Boolean aceitaNotificacao,
        String situacaoRenda,
        String criancasNaFamilia
) {}
