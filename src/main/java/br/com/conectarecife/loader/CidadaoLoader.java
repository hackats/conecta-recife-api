package br.com.conectarecife.loader;

import br.com.conectarecife.model.Cidadao;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CidadaoLoader {

    public Map<String, Cidadao> load(Path file) throws IOException {
        Map<String, Cidadao> result = new LinkedHashMap<>();
        try (CSVParser parser = CsvSupport.parse(file)) {
            for (CSVRecord row : parser) {
                Cidadao cidadao = new Cidadao(
                        CsvSupport.text(row, "cidadao_id"),
                        CsvSupport.integer(row, "idade"),
                        CsvSupport.text(row, "faixa_etaria"),
                        CsvSupport.text(row, "sexo"),
                        CsvSupport.integer(row, "rpa"),
                        CsvSupport.bool(row, "empreendedor"),
                        CsvSupport.bool(row, "mei"),
                        CsvSupport.bool(row, "servidor_publico"),
                        CsvSupport.bool(row, "proprietario_imovel"),
                        CsvSupport.bool(row, "tutor_animal"),
                        CsvSupport.bool(row, "estudante"),
                        CsvSupport.bool(row, "pessoa_com_deficiencia"),
                        CsvSupport.bool(row, "cadastro_unico"),
                        CsvSupport.bool(row, "bolsa_familia"),
                        CsvSupport.bool(row, "usuario_compaz"),
                        CsvSupport.bool(row, "hipertensao"),
                        CsvSupport.bool(row, "diabetes"),
                        CsvSupport.bool(row, "saude_mental"),
                        CsvSupport.bool(row, "gestante"),
                        CsvSupport.bool(row, "microcredito"),
                        CsvSupport.bool(row, "aceita_notificacao"),
                        CsvSupport.text(row, "situacao_renda"),
                        CsvSupport.text(row, "criancas_na_familia")
                );
                if (result.putIfAbsent(cidadao.cidadaoId(), cidadao) != null) {
                    throw new IllegalStateException("cidadao_id duplicado: " + cidadao.cidadaoId());
                }
            }
        }
        return result;
    }
}
