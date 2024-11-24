package org.vstar.lab4.data_consuming;

// ThreadProcessor.java
import javafx.application.Platform;
import javafx.beans.property.*;

import java.util.List;

public class ThreadProcessor implements Runnable {
    private final List<String[]> data;
    private final int threadId;
    private final int startId;
    private final int endId;

    private volatile boolean paused = false;
    private volatile boolean terminated = false;

    private final StringProperty statusProperty = new SimpleStringProperty("Очікує");
    private final IntegerProperty currentRowIdProperty = new SimpleIntegerProperty(0);
    private final StringProperty speedModeProperty = new SimpleStringProperty("Середній");

    private volatile int sleepTime = 20; // Початково середній режим (20 мс)

    public ThreadProcessor(List<String[]> data, int threadId, int startId, int endId) {
        this.data = data;
        this.threadId = threadId;
        this.startId = startId;
        this.endId = endId;
    }

    @Override
    public void run() {
        updateStatus("Виконується");
        for (int i = 0; i < data.size(); i++) {
            if (terminated) {
                updateStatus("Завершено");
                break;
            }

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

            String[] row = data.get(i);
            processRow(row);

            int currentRowId = startId + i;
            updateCurrentRowId(currentRowId);
        }

        if (!terminated) {
            updateStatus("Завершено");
        }
    }

    private void processRow(String[] row) {
        // Обробка рядка (можна додати реальну логіку)
        try {
            Thread.sleep(sleepTime); // Час обробки залежить від обраного режиму
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

    public void setSpeedMode(String mode) {
        switch (mode) {
            case "Швидкий":
                sleepTime = 1;
                break;
            case "Середній":
                sleepTime = 20;
                break;
            case "Повільний":
                sleepTime = 100;
                break;
        }
        Platform.runLater(() -> speedModeProperty.set(mode));
    }

    // Властивості для GUI
    public StringProperty statusProperty() {
        return statusProperty;
    }

    public IntegerProperty currentRowIdProperty() {
        return currentRowIdProperty;
    }

    public StringProperty speedModeProperty() {
        return speedModeProperty;
    }

    public int getThreadId() {
        return threadId;
    }

    public int getStartId() {
        return startId;
    }

    public int getEndId() {
        return endId;
    }

    // Допоміжні методи для оновлення GUI
    private void updateStatus(String status) {
        Platform.runLater(() -> statusProperty.set(status));
    }

    private void updateCurrentRowId(int rowId) {
        Platform.runLater(() -> currentRowIdProperty.set(rowId));
    }
}
