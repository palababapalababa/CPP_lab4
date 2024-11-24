package org.vstar.lab4.data_preprocessing;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class CsvReader {

    public static final String filePath = "C:\\пз\\пз 3 курс\\кпп\\лаб4\\MultiThreadMLModel\\src\\main\\resources\\RandomData.csv";

    public static List<String[]> readCsv(String filePath) {
        List<String[]> rows = new ArrayList<>();
        try (Reader reader = new FileReader(filePath)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .withDelimiter(';')
                    .withFirstRecordAsHeader() // Пропускаємо заголовок
                    .parse(reader);
            for (CSVRecord record : records) {
                // Читаємо дані по колонках
                String id = record.get("id");
                String firstname = record.get("firstname");
                String lastname = record.get("lastname");
                String email = record.get("email");
                rows.add(new String[]{id, firstname, lastname, email});
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rows;
    }

    public static void main(String[] args) {
        // Тест читання CSV
        List<String[]> data = readCsv(CsvReader.filePath);

        // Виводимо кількість рядків та перші 5 рядків для перевірки
        System.out.println("Прочитано " + data.size() + " рядків.");
        data.stream().limit(5).forEach(row -> System.out.println(String.join(", ", row)));
    }
}
