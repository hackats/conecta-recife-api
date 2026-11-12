package br.com.conectarecife.loader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class CsvSupport {

    private CsvSupport() {}

    static CSVParser parse(Path path) throws IOException {
        Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
        return CSVFormat.RFC4180.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .build()
                .parse(reader);
    }

    static String text(CSVRecord row, String column) {
        String value = row.get(column);
        return value == null || value.isBlank() ? null : value;
    }

    static Integer integer(CSVRecord row, String column) {
        String value = text(row, column);
        return value == null ? null : Integer.valueOf(value);
    }

    static Long longValue(CSVRecord row, String column) {
        String value = text(row, column);
        return value == null ? null : Long.valueOf(value);
    }

    static Boolean bool(CSVRecord row, String column) {
        String value = text(row, column);
        return value == null ? null : Boolean.valueOf(value);
    }

    static List<String> list(CSVRecord row, String column) {
        String value = text(row, column);
        if (value == null) return List.of();
        return java.util.Arrays.stream(value.split("\\|"))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }
}
