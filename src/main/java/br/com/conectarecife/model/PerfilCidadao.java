package br.com.conectarecife.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PerfilCidadao {

    private PerfilCidadao() {}

    public static Set<String> publicosDoCidadao(Cidadao cidadao) {
        Set<String> perfil = new HashSet<>();
        adicionar(perfil, cidadao.empreendedor(), "empreendedor");
        adicionar(perfil, cidadao.mei(), "microempreendedor", "empreendedor");
        adicionar(perfil, cidadao.servidorPublico(), "servidor publico municipal");
        adicionar(perfil, cidadao.proprietarioImovel(), "proprietario de imoveis");
        adicionar(perfil, cidadao.tutorAnimal(), "tutor de animal");
        adicionar(perfil, cidadao.estudante(), "estudante");


        return perfil;
    }

    private static void adicionar(Set<String> perfil, Boolean marcador, String... publicos) {
        if (Boolean.TRUE.equals(marcador)) {
            perfil.addAll(List.of(publicos));
        }
    }

}
