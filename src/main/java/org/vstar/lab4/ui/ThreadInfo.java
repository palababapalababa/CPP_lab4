package org.vstar.lab4.ui;

import javafx.beans.property.*;
import org.vstar.lab4.data_consuming.ThreadProcessor;

/**
 * Клас, що представляє інформацію про потік для відображення в таблиці.
 */
public class ThreadInfo {
    private final IntegerProperty threadId;
    private final StringProperty status;
    private final IntegerProperty currentRowId;
    private final StringProperty currentBatchRange;
    private final IntegerProperty batchVowelCount;

    private final ThreadProcessor processor;

    public ThreadInfo(ThreadProcessor processor) {
        this.processor = processor;
        this.threadId = new SimpleIntegerProperty(processor.getThreadId());
        this.status = processor.statusProperty();
        this.currentRowId = processor.currentRowIdProperty();
        this.currentBatchRange = processor.currentBatchRangeProperty();
        this.batchVowelCount = processor.batchVowelCountProperty();
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

    public IntegerProperty batchVowelCountProperty() {
        return batchVowelCount;
    }

    public ThreadProcessor getProcessor() {
        return processor;
    }
}
