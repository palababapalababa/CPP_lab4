package org.vstar.lab4.data_consuming;

import javafx.application.Platform;
import javafx.beans.property.*;
import org.vstar.lab4.ui.MTGuiApplication;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

public class ThreadProcessor implements Callable<Integer> {
    private final int threadId;
    private final BlockingQueue<List<String[]>> batchQueue;

    private volatile boolean paused = false;
    private volatile boolean terminated = false;

    private final StringProperty statusProperty = new SimpleStringProperty("Очікує");
    private final IntegerProperty currentRowIdProperty = new SimpleIntegerProperty(0);
    private final StringProperty currentBatchRangeProperty = new SimpleStringProperty("");
    private final IntegerProperty batchVowelCountProperty = new SimpleIntegerProperty(0);

    private final IntegerProperty globalSleepTimeProperty;

    // Поля для діапазону рядків поточного батчу
    private int currentBatchStartId;
    private int currentBatchEndId;

    public ThreadProcessor(int threadId, BlockingQueue<List<String[]>> batchQueue, IntegerProperty globalSleepTimeProperty) {
        this.threadId = threadId;
        this.batchQueue = batchQueue;
        this.globalSleepTimeProperty = globalSleepTimeProperty;
    }

    @Override
    public Integer call() {
        updateStatus("Виконується");
        int totalVowelCount = 0;
        try {
            while (!terminated) {
                synchronized (this) {
                    while (paused && !terminated) {
                        updateStatus("Призупинено");
                        wait();
                    }
                    if (terminated) {
                        break;
                    }
                }

                List<String[]> batch = batchQueue.poll();
                if (batch == null) {
                    // Немає більше батчів для обробки
                    break;
                }

                // Встановлюємо діапазон рядків батчу
                currentBatchStartId = Integer.parseInt(batch.get(0)[0]);
                currentBatchEndId = Integer.parseInt(batch.get(batch.size() - 1)[0]);
                updateCurrentBatchRange(currentBatchStartId + " - " + currentBatchEndId);

                // Обробка батчу
                int batchVowelCount = processBatch(batch);
                totalVowelCount += batchVowelCount;

                // Оновлюємо кількість голосних для поточного батчу
                updateBatchVowelCount(batchVowelCount);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        updateStatus("Завершено");

        // Повертаємо загальну кількість голосних, підрахованих потоком
        return totalVowelCount;
    }

    private int processBatch(List<String[]> batch) {
        int sleepTime = globalSleepTimeProperty.get();
        int batchVowelCount = 0;
        for (String[] row : batch) {
            if (terminated) {
                break;
            }

            synchronized (this) {
                while (paused && !terminated) {
                    updateStatus("Призупинено");
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                if (terminated) {
                    break;
                }
            }

            // Обробка рядка
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            int rowId = Integer.parseInt(row[0]);
            updateCurrentRowId(rowId);

            // Підраховуємо кількість голосних у імені та прізвищі
            int vowelCount = countVowels(row[1]) + countVowels(row[2]);
            batchVowelCount += vowelCount;

            // Перевіряємо, чи змінився sleepTime
            sleepTime = globalSleepTimeProperty.get();
        }

        // Логування завершення батчу з діапазоном рядків
        log("Потік " + threadId + " обробив батч з рядками " + currentBatchStartId + " - " + currentBatchEndId + ". Кількість голосних: " + batchVowelCount);

        return batchVowelCount;
    }

    private int countVowels(String text) {
        int count = 0;
        text = text.toLowerCase();
        for (char c : text.toCharArray()) {
            if ("aeiouyаеєиіїоуюя".indexOf(c) != -1) {
                count++;
            }
        }
        return count;
    }

    private void log(String message) {
        Platform.runLater(() -> {
            MTGuiApplication.appendLog(message);
        });
    }

    public void pause() {
        paused = true;
        log("Потік " + threadId + " призупинено.");
    }

    public synchronized void resume() {
        paused = false;
        notify();
        log("Потік " + threadId + " відновлено.");
    }

    public void terminate() {
        terminated = true;
        synchronized (this) {
            notify();
        }
        log("Потік " + threadId + " завершено.");
    }

    // Властивості для GUI
    public StringProperty statusProperty() {
        return statusProperty;
    }

    public IntegerProperty currentRowIdProperty() {
        return currentRowIdProperty;
    }

    public StringProperty currentBatchRangeProperty() {
        return currentBatchRangeProperty;
    }

    public IntegerProperty batchVowelCountProperty() {
        return batchVowelCountProperty;
    }

    public int getThreadId() {
        return threadId;
    }

    private void updateStatus(String status) {
        Platform.runLater(() -> statusProperty.set(status));
    }

    private void updateCurrentRowId(int rowId) {
        Platform.runLater(() -> currentRowIdProperty.set(rowId));
    }

    private void updateCurrentBatchRange(String range) {
        Platform.runLater(() -> currentBatchRangeProperty.set(range));
    }

    private void updateBatchVowelCount(int count) {
        Platform.runLater(() -> batchVowelCountProperty.set(count));
    }
}
