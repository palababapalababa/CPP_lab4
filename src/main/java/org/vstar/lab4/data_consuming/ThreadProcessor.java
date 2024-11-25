package org.vstar.lab4.data_consuming;

import javafx.application.Platform;
import javafx.beans.property.*;
import org.vstar.lab4.ui.MTGuiApplication;

import java.util.List;
import java.util.concurrent.BlockingQueue;

public class ThreadProcessor implements Runnable {
    private final int threadId;
    private final BlockingQueue<List<String[]>> batchQueue;

    private volatile boolean paused = false;
    private volatile boolean terminated = false;

    private final StringProperty statusProperty = new SimpleStringProperty("Очікує");
    private final IntegerProperty currentRowIdProperty = new SimpleIntegerProperty(0);
    private final StringProperty currentBatchRangeProperty = new SimpleStringProperty("");

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
    public void run() {
        updateStatus("Виконується");
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
                processBatch(batch);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        updateStatus("Завершено");
    }

    private void processBatch(List<String[]> batch) {
        int sleepTime = globalSleepTimeProperty.get();
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

            // Перевіряємо, чи змінився sleepTime
            sleepTime = globalSleepTimeProperty.get();
        }

        // Логування завершення батчу з діапазоном рядків
        log("Потік " + threadId + " обробив батч з рядками " + currentBatchStartId + " - " + currentBatchEndId);
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
}
