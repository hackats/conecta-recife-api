package br.com.conectarecife.loader;

import br.com.conectarecife.model.Clique;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class CliqueLoader {

    public List<Clique> load(Path file) throws IOException {
        List<Clique> result = new ArrayList<>(593_056);
        try (CSVParser parser = CsvSupport.parse(file)) {
            for (CSVRecord row : parser) {
                Clique clique = new Clique(
                        CsvSupport.text(row, "cidadao_id"),
                        CsvSupport.text(row, "sessao_id"),
                        CsvSupport.integer(row, "ordem_na_sessao"),
                        LocalDate.parse(CsvSupport.text(row, "data")),
                        LocalTime.parse(CsvSupport.text(row, "hora")),
                        CsvSupport.integer(row, "servico_id")
                );
                result.add(clique);
            }
        }
        return List.copyOf(result);
    }
}
