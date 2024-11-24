package org.vstar.lab4.ui;

// ThreadInfo.java
import javafx.beans.property.*;
import org.vstar.lab4.data_consuming.ThreadProcessor;

public class ThreadInfo {
    private final IntegerProperty threadId;
    private final StringProperty status;
    private final IntegerProperty currentRowId;
    private final StringProperty speedMode;
    private final StringProperty idRange;

    private final ThreadProcessor processor;

    public ThreadInfo(ThreadProcessor processor) {
        this.processor = processor;
        this.threadId = new SimpleIntegerProperty(processor.getThreadId());
        this.status = processor.statusProperty();
        this.currentRowId = processor.currentRowIdProperty();
        this.speedMode = processor.speedModeProperty();
        this.idRange = new SimpleStringProperty(processor.getStartId() + " - " + processor.getEndId());
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

    public StringProperty speedModeProperty() {
        return speedMode;
    }

    public StringProperty idRangeProperty() {
        return idRange;
    }

    public ThreadProcessor getProcessor() {
        return processor;
    }
}
