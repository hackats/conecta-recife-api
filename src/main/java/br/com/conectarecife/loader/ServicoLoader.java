package br.com.conectarecife.loader;

import br.com.conectarecife.model.Servico;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ServicoLoader {

    public Map<Integer, Servico> load(Path file) throws IOException {
        Map<Integer, Servico> result = new LinkedHashMap<>();
        try (CSVParser parser = CsvSupport.parse(file)) {
            for (CSVRecord row : parser) {
                String ultimaAtualizacao = CsvSupport.text(row, "ultima_atualizacao");
                String notaMedia = CsvSupport.text(row, "nota_media");

                Servico servico = new Servico(
                        CsvSupport.integer(row, "servico_id"),
                        CsvSupport.text(row, "item_nome"),
                        CsvSupport.text(row, "nome"),
                        CsvSupport.text(row, "nome_resumido"),
                        CsvSupport.text(row, "orgao_nome"),
                        CsvSupport.text(row, "orgao_sigla"),
                        CsvSupport.list(row, "temas"),
                        CsvSupport.list(row, "grupos"),
                        CsvSupport.list(row, "publicos_alvo"),
                        CsvSupport.list(row, "palavras_chave"),
                        CsvSupport.text(row, "natureza"),
                        CsvSupport.text(row, "descricao"),
                        CsvSupport.text(row, "requisitos"),
                        CsvSupport.text(row, "passos"),
                        CsvSupport.text(row, "manifestacao"),
                        CsvSupport.text(row, "prioridade_atendimento"),
                        CsvSupport.bool(row, "gratuito"),
                        CsvSupport.text(row, "canal_web"),
                        CsvSupport.text(row, "canal_app"),
                        CsvSupport.text(row, "canal_chat"),
                        CsvSupport.integer(row, "selo_acesso"),
                        CsvSupport.text(row, "selo_acesso_descricao"),
                        CsvSupport.integer(row, "nivel_digital_atual"),
                        CsvSupport.integer(row, "nivel_digital_ideal"),
                        CsvSupport.text(row, "status_atual"),
                        CsvSupport.text(row, "status_ideal"),
                        ultimaAtualizacao == null ? null : LocalDate.parse(ultimaAtualizacao),
                        CsvSupport.longValue(row, "cliques_historico"),
                        CsvSupport.longValue(row, "pessoas_unicas_historico"),
                        CsvSupport.longValue(row, "aparelhos_sem_login_historico"),
                        notaMedia == null ? null : new BigDecimal(notaMedia),
                        CsvSupport.longValue(row, "avaliacoes_com_nota"),
                        CsvSupport.bool(row, "tem_historico")
                );
                if (result.putIfAbsent(servico.servicoId(), servico) != null) {
                    throw new IllegalStateException("servico_id duplicado: " + servico.servicoId());
                }
            }
        }
        return result;
    }
}
