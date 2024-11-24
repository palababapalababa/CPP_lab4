package org.vstar.lab4.ui;

// MainApp.java
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import org.vstar.lab4.data_consuming.BatchProcessor;
import org.vstar.lab4.data_preprocessing.BatchSplitter;
import org.vstar.lab4.data_preprocessing.CsvReader;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MTGuiApplication extends Application {
    private TextField threadCountField;
    private TextField batchSizeField;
    private Button startButton;

    private ExecutorService executorService;
    private List<BatchProcessor> processors = new ArrayList<>();
    private ObservableList<BatchInfo> batchInfos = FXCollections.observableArrayList();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Багатопотокова обробка даних");

        // Створюємо інтерфейс
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        HBox controls = new HBox(10);
        threadCountField = new TextField("4");
        batchSizeField = new TextField("1000");
        startButton = new Button("Почати обробку");
        controls.getChildren().addAll(
                new Label("Кількість потоків:"), threadCountField,
                new Label("Розмір батчу:"), batchSizeField,
                startButton
        );

        TableView<BatchInfo> table = createTableView();

        root.getChildren().addAll(controls, table);

        startButton.setOnAction(e -> startProcessing(table));

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private TableView<BatchInfo> createTableView() {
        TableView<BatchInfo> table = new TableView<>();
        table.setItems(batchInfos);

        TableColumn<BatchInfo, Number> batchNumberCol = new TableColumn<>("Батч №");
        batchNumberCol.setCellValueFactory(data -> data.getValue().batchNumberProperty());

        TableColumn<BatchInfo, Number> threadNumberCol = new TableColumn<>("Потік №");
        threadNumberCol.setCellValueFactory(data -> data.getValue().threadNumberProperty());

        TableColumn<BatchInfo, String> statusCol = new TableColumn<>("Стан");
        statusCol.setCellValueFactory(data -> data.getValue().statusProperty());

        TableColumn<BatchInfo, Number> currentRowCol = new TableColumn<>("Поточний рядок");
        currentRowCol.setCellValueFactory(data -> data.getValue().currentRowProperty());

        TableColumn<BatchInfo, Void> actionCol = new TableColumn<>("Дії");
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button pauseButton = new Button("Пауза");
            private final Button resumeButton = new Button("Відновити");
            private final Button terminateButton = new Button("Завершити");
            private final HBox pane = new HBox(5, pauseButton, resumeButton, terminateButton);

            {
                pauseButton.setOnAction(e -> {
                    BatchInfo batchInfo = getTableView().getItems().get(getIndex());
                    batchInfo.getProcessor().pause();
                });

                resumeButton.setOnAction(e -> {
                    BatchInfo batchInfo = getTableView().getItems().get(getIndex());
                    batchInfo.getProcessor().resume();
                });

                terminateButton.setOnAction(e -> {
                    BatchInfo batchInfo = getTableView().getItems().get(getIndex());
                    batchInfo.getProcessor().terminate();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });

        table.getColumns().addAll(batchNumberCol, threadNumberCol, statusCol, currentRowCol, actionCol);
        return table;
    }

    private void startProcessing(TableView<BatchInfo> table) {
        int threadCount;
        int batchSize;

        try {
            threadCount = Integer.parseInt(threadCountField.getText());
            batchSize = Integer.parseInt(batchSizeField.getText());
        } catch (NumberFormatException e) {
            showAlert("Помилка", "Введіть коректні числа для кількості потоків та розміру батчу.");
            return;
        }

        startButton.setDisable(true);
        batchInfos.clear();
        processors.clear();

        // Читаємо дані
        Task<List<String[]>> loadDataTask = new Task<>() {
            @Override
            protected List<String[]> call() throws Exception {
                return CsvReader.readCsv(CsvReader.filePath);
            }
        };

        loadDataTask.setOnSucceeded(e -> {
            List<String[]> data = loadDataTask.getValue();

            // Розбиваємо на батчі
            List<List<String[]>> batches = BatchSplitter.splitIntoBatches(data, batchSize);

            // Створюємо пул потоків
            executorService = Executors.newFixedThreadPool(threadCount);

            int threadNumber = 1;

            for (int i = 0; i < batches.size(); i++) {
                List<String[]> batch = batches.get(i);
                BatchProcessor processor = new BatchProcessor(batch, i + 1, threadNumber);
                processors.add(processor);

                BatchInfo batchInfo = new BatchInfo(processor);
                batchInfos.add(batchInfo);

                executorService.submit(processor);

                threadNumber = (threadNumber % threadCount) + 1;
            }

            startButton.setDisable(false);
        });

        loadDataTask.setOnFailed(e -> {
            showAlert("Помилка", "Не вдалося завантажити дані.");
            startButton.setDisable(false);
        });

        new Thread(loadDataTask).start();
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
            alert.setTitle(title);
            alert.showAndWait();
        });
    }

    @Override
    public void stop() throws Exception {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
