package org.vstar.lab4.ui;

import javafx.beans.property.*;

public class RunHistory {
    private final IntegerProperty runNumber;
    private final IntegerProperty threadCount;
    private final IntegerProperty batchSize;
    private final StringProperty speedMode;
    private final LongProperty executionTime; // У мілісекундах

    public RunHistory(int runNumber, int threadCount, int batchSize, String speedMode, long executionTime) {
        this.runNumber = new SimpleIntegerProperty(runNumber);
        this.threadCount = new SimpleIntegerProperty(threadCount);
        this.batchSize = new SimpleIntegerProperty(batchSize);
        this.speedMode = new SimpleStringProperty(speedMode);
        this.executionTime = new SimpleLongProperty(executionTime);
    }

    public IntegerProperty runNumberProperty() {
        return runNumber;
    }

    public IntegerProperty threadCountProperty() {
        return threadCount;
    }

    public IntegerProperty batchSizeProperty() {
        return batchSize;
    }

    public StringProperty speedModeProperty() {
        return speedMode;
    }

    public LongProperty executionTimeProperty() {
        return executionTime;
    }
}
