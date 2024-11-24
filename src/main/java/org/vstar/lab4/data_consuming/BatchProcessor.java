package org.vstar.lab4.data_consuming;

// BatchProcessor.java
import javafx.application.Platform;
import javafx.beans.property.*;

import java.util.List;

public class BatchProcessor implements Runnable {
    private final List<String[]> batchData;
    private final int batchNumber;
    private final int threadNumber;

    // Стан потоку
    private volatile boolean paused = false;
    private volatile boolean terminated = false;

    // Властивості для зв'язку з GUI
    private final StringProperty statusProperty = new SimpleStringProperty("Очікує");
    private final IntegerProperty currentRowProperty = new SimpleIntegerProperty(0);

    public BatchProcessor(List<String[]> batchData, int batchNumber, int threadNumber) {
        this.batchData = batchData;
        this.batchNumber = batchNumber;
        this.threadNumber = threadNumber;
    }

    @Override
    public void run() {
        updateStatus("Виконується");
        for (int i = 0; i < batchData.size(); i++) {
            if (terminated) {
                updateStatus("Завершено");
                break;
            }

            // Призупинення потоку
            synchronized (this) {
                while (paused) {
                    updateStatus("Призупинено");
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }

            // Обробка рядка
            String[] row = batchData.get(i);
            processRow(row);

            int currentRow = i + 1;
            updateCurrentRow(currentRow);
        }

        if (!terminated) {
            updateStatus("Завершено");
        }
    }

    private void processRow(String[] row) {
        // Симуляція обробки рядка (наприклад, реверс імені)
        String id = row[0];
        String firstname = new StringBuilder(row[1]).reverse().toString();
        String lastname = new StringBuilder(row[2]).reverse().toString();
        String email = row[3];

        // Можна додати збереження результатів або іншу обробку

        // Симуляція часу обробки (фіксований час)
        try {
            Thread.sleep(1); // 50 мс на рядок
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Методи для управління потоком
    public void pause() {
        paused = true;
    }

    public synchronized void resume() {
        paused = false;
        notify();
    }

    public void terminate() {
        terminated = true;
    }

    // Властивості для GUI
    public StringProperty statusProperty() {
        return statusProperty;
    }

    public IntegerProperty currentRowProperty() {
        return currentRowProperty;
    }

    public int getBatchNumber() {
        return batchNumber;
    }

    public int getThreadNumber() {
        return threadNumber;
    }

    // Допоміжні методи для оновлення GUI
    private void updateStatus(String status) {
        Platform.runLater(() -> statusProperty.set(status));
    }

    private void updateCurrentRow(int rowNumber) {
        Platform.runLater(() -> currentRowProperty.set(rowNumber));
    }
}
