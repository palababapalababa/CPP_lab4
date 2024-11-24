package org.vstar.lab4.ui;

// BatchInfo.java
import javafx.beans.property.*;
import org.vstar.lab4.data_consuming.BatchProcessor;

public class BatchInfo {
    private final IntegerProperty batchNumber;
    private final IntegerProperty threadNumber;
    private final StringProperty status;
    private final IntegerProperty currentRow;

    private final BatchProcessor processor;

    public BatchInfo(BatchProcessor processor) {
        this.processor = processor;
        this.batchNumber = new SimpleIntegerProperty(processor.getBatchNumber());
        this.threadNumber = new SimpleIntegerProperty(processor.getThreadNumber());
        this.status = processor.statusProperty();
        this.currentRow = processor.currentRowProperty();
    }

    public IntegerProperty batchNumberProperty() {
        return batchNumber;
    }

    public IntegerProperty threadNumberProperty() {
        return threadNumber;
    }

    public StringProperty statusProperty() {
        return status;
    }

    public IntegerProperty currentRowProperty() {
        return currentRow;
    }

    public BatchProcessor getProcessor() {
        return processor;
    }
}
