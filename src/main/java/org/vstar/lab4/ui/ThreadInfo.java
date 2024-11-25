package org.vstar.lab4.ui;

import javafx.beans.property.*;
import org.vstar.lab4.data_consuming.ThreadProcessor;

public class ThreadInfo {
    private final IntegerProperty threadId;
    private final StringProperty status;
    private final IntegerProperty currentRowId;
    private final StringProperty currentBatchRange;

    private final ThreadProcessor processor;

    public ThreadInfo(ThreadProcessor processor) {
        this.processor = processor;
        this.threadId = new SimpleIntegerProperty(processor.getThreadId());
        this.status = processor.statusProperty();
        this.currentRowId = processor.currentRowIdProperty();
        this.currentBatchRange = processor.currentBatchRangeProperty();
    }

    public IntegerProperty threadIdProperty() {
        return threadId;
    }

    public StringProperty statusProperty() {
        return status;
    }

    public IntegerProperty currentRowIdProperty() {
        return currentRowId;
    }

    public StringProperty currentBatchRangeProperty() {
        return currentBatchRange;
    }

    public ThreadProcessor getProcessor() {
        return processor;
    }
}
