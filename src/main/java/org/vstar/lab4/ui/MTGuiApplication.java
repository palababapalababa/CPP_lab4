package org.vstar.lab4.ui;

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
import org.vstar.lab4.data_consuming.ThreadProcessor;
import org.vstar.lab4.data_preprocessing.BatchSplitter;
import org.vstar.lab4.data_preprocessing.CsvReader;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MTGuiApplication extends Application {
    private TextField threadCountField;
    private TextField batchSizeField;
    private Button startButton;
    private static TextArea logArea = new TextArea();

    private ExecutorService executorService;
    private List<ThreadProcessor> processors = new ArrayList<>();
    private ObservableList<ThreadInfo> threadInfos = FXCollections.observableArrayList();
    private ObservableList<RunHistory> runHistories = FXCollections.observableArrayList();

    private boolean isDarkTheme = false;

    public static void appendLog(String message) {
        logArea.appendText(message + "\n");
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Багатопотокова обробка даних");

        TabPane tabPane = new TabPane();

        Tab mainTab = new Tab("Головна");
        mainTab.setContent(createMainTabContent());
        mainTab.setClosable(false);

        Tab historyTab = new Tab("Історія запусків");
        historyTab.setContent(createHistoryTabContent());
        historyTab.setClosable(false);

        Tab logTab = new Tab("Логи");
        logTab.setContent(createLogTabContent());
        logTab.setClosable(false);

        tabPane.getTabs().addAll(mainTab, historyTab, logTab);

        Scene scene = new Scene(tabPane, 800, 600);
        scene.getStylesheets().add("style.css");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createMainTabContent() {
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

        Button themeButton = new Button("Перемкнути тему");
        themeButton.setOnAction(e -> toggleTheme(root.getScene()));

        TableView<ThreadInfo> table = createTableView();

        root.getChildren().addAll(controls, themeButton, table);

        startButton.setOnAction(e -> startProcessing(table));

        return root;
    }

    private VBox createHistoryTabContent() {
        TableView<RunHistory> historyTable = new TableView<>();
        historyTable.setItems(runHistories);

        TableColumn<RunHistory, Number> runNumberCol = new TableColumn<>("Запуск №");
        runNumberCol.setCellValueFactory(data -> data.getValue().runNumberProperty());

        TableColumn<RunHistory, Number> threadCountCol = new TableColumn<>("Кількість потоків");
        threadCountCol.setCellValueFactory(data -> data.getValue().threadCountProperty());

        TableColumn<RunHistory, Number> batchSizeCol = new TableColumn<>("Розмір батчу");
        batchSizeCol.setCellValueFactory(data -> data.getValue().batchSizeProperty());

        TableColumn<RunHistory, String> speedModeCol = new TableColumn<>("Режим швидкості");
        speedModeCol.setCellValueFactory(data -> data.getValue().speedModeProperty());

        TableColumn<RunHistory, Number> executionTimeCol = new TableColumn<>("Час виконання (мс)");
        executionTimeCol.setCellValueFactory(data -> data.getValue().executionTimeProperty());

        historyTable.getColumns().addAll(runNumberCol, threadCountCol, batchSizeCol, speedModeCol, executionTimeCol);

        VBox vbox = new VBox(historyTable);
        return vbox;
    }

    private VBox createLogTabContent() {
        logArea.setEditable(false);
        VBox vbox = new VBox(logArea);
        return vbox;
    }

    private TableView<ThreadInfo> createTableView() {
        TableView<ThreadInfo> table = new TableView<>();
        table.setItems(threadInfos);

        TableColumn<ThreadInfo, Number> threadIdCol = new TableColumn<>("Потік №");
        threadIdCol.setCellValueFactory(data -> data.getValue().threadIdProperty());

        TableColumn<ThreadInfo, String> statusCol = new TableColumn<>("Стан");
        statusCol.setCellValueFactory(data -> data.getValue().statusProperty());

        TableColumn<ThreadInfo, Number> currentRowIdCol = new TableColumn<>("Поточний ID рядка");
        currentRowIdCol.setCellValueFactory(data -> data.getValue().currentRowIdProperty());

        TableColumn<ThreadInfo, String> idRangeCol = new TableColumn<>("Діапазон ID рядків");
        idRangeCol.setCellValueFactory(data -> data.getValue().idRangeProperty());

        TableColumn<ThreadInfo, String> speedModeCol = new TableColumn<>("Режим швидкості");
        speedModeCol.setCellValueFactory(data -> data.getValue().speedModeProperty());

        TableColumn<ThreadInfo, Void> actionCol = new TableColumn<>("Дії");
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button pauseButton = new Button("Пауза");
            private final Button resumeButton = new Button("Відновити");
            private final Button terminateButton = new Button("Завершити");
            private final ChoiceBox<String> speedChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList("Швидкий", "Середній", "Повільний"));
            private final HBox pane = new HBox(5, pauseButton, resumeButton, terminateButton, speedChoiceBox);

            {
                pauseButton.setOnAction(e -> {
                    ThreadInfo threadInfo = getTableView().getItems().get(getIndex());
                    threadInfo.getProcessor().pause();
                });

                resumeButton.setOnAction(e -> {
                    ThreadInfo threadInfo = getTableView().getItems().get(getIndex());
                    threadInfo.getProcessor().resume();
                });

                terminateButton.setOnAction(e -> {
                    ThreadInfo threadInfo = getTableView().getItems().get(getIndex());
                    threadInfo.getProcessor().terminate();
                });

                speedChoiceBox.setOnAction(e -> {
                    String selectedMode = speedChoiceBox.getValue();
                    ThreadInfo threadInfo = getTableView().getItems().get(getIndex());
                    threadInfo.getProcessor().setSpeedMode(selectedMode);
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

        table.getColumns().addAll(threadIdCol, statusCol, currentRowIdCol, idRangeCol, speedModeCol, actionCol);
        return table;
    }

    private void startProcessing(TableView<ThreadInfo> table) {
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
        threadInfos.clear();
        processors.clear();

        Task<Void> processingTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Завантаження даних
                List<String[]> data = CsvReader.readCsv(CsvReader.filePath);

                // Розбиваємо дані на батчі
                List<List<String[]>> batches = BatchSplitter.splitIntoBatches(data, batchSize);

                // Створюємо пул потоків
                executorService = Executors.newFixedThreadPool(threadCount);

                int startId = 1;

                long startTime = System.currentTimeMillis();

                for (int i = 0; i < threadCount; i++) {
                    List<String[]> batchData = batches.get(i % batches.size());
                    int endId = startId + batchData.size() - 1;
                    ThreadProcessor processor = new ThreadProcessor(batchData, i + 1, startId, endId);
                    processors.add(processor);

                    ThreadInfo threadInfo = new ThreadInfo(processor);
                    Platform.runLater(() -> threadInfos.add(threadInfo));

                    executorService.submit(processor);

                    startId = endId + 1;
                }

                // Чекаємо завершення всіх потоків
                executorService.shutdown();
                while (!executorService.isTerminated()) {
                    Thread.sleep(100);
                }

                long endTime = System.currentTimeMillis();
                long executionTime = endTime - startTime;

                // Визначаємо поточний режим швидкості (якщо режими змінювалися, можна відобразити перший)
                String speedMode = processors.get(0).speedModeProperty().get();

                // Додаємо запис до історії запусків
                int runNumber = runHistories.size() + 1;
                RunHistory history = new RunHistory(runNumber, threadCount, batchSize, speedMode, executionTime);
                Platform.runLater(() -> runHistories.add(history));

                Platform.runLater(() -> startButton.setDisable(false));

                return null;
            }
        };

        new Thread(processingTask).start();
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
            alert.setTitle(title);
            alert.showAndWait();
        });
    }

    private void toggleTheme(Scene scene) {
        if (isDarkTheme) {
            scene.getStylesheets().remove("dark-theme.css");
            scene.getStylesheets().add("style.css");
            isDarkTheme = false;
        } else {
            scene.getStylesheets().remove("style.css");
            scene.getStylesheets().add("dark-theme.css");
            isDarkTheme = true;
        }
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
