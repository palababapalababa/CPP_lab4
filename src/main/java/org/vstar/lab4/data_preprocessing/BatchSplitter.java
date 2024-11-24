package org.vstar.lab4.data_preprocessing;

import java.util.ArrayList;
import java.util.List;

public class BatchSplitter {
    public static List<List<String[]>> splitIntoBatches(List<String[]> data, int batchSize) {
        List<List<String[]>> batches = new ArrayList<>();
        int totalRows = data.size();

        for (int i = 0; i < totalRows; i += batchSize) {
            int end = Math.min(i + batchSize, totalRows);
            batches.add(new ArrayList<>(data.subList(i, end)));
        }
        return batches;
    }

    public static void main(String[] args) {
        // Для тесту
        List<String[]> data = CsvReader.readCsv(CsvReader.filePath);
        int batchSize = 10; // Розмір батчу
        List<List<String[]>> batches = splitIntoBatches(data, batchSize);

        System.out.println("Загальна кількість рядків: " + data.size());
        System.out.println("Кількість батчів: " + batches.size());
        System.out.println("Розмір першого батчу: " + batches.getFirst().size());
        System.out.println("Розмір останнього батчу: " + batches.getLast().size());
    }
}
