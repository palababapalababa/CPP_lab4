package org.vstar.lab4.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.vstar.lab4.data_consuming.ThreadProcessor;
import org.vstar.lab4.data_preprocessing.BatchSplitter;
import org.vstar.lab4.data_preprocessing.CsvReader;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class MTGuiApplication extends Application {
    private TextField threadCountField;
    private TextField batchSizeField;
    private Button startButton;
    private static TextArea logArea = new TextArea();

    private ExecutorService executorService;
    private ObservableList<ThreadInfo> threadInfos = FXCollections.observableArrayList();
    private ObservableList<RunHistory> runHistories = FXCollections.observableArrayList();

    private boolean isDarkTheme = false;

    // Глобальна властивість режиму швидкості
    private IntegerProperty globalSleepTimeProperty = new SimpleIntegerProperty(20); // За замовчуванням середній режим

    // Для керування режимом швидкості
    private String[] speedModes = {"Швидкий", "Середній", "Повільний"};
    private ConcurrentHashMap<String, Integer> speedModeMap = new ConcurrentHashMap<>();
    private String currentSpeedMode = "Середній";

    // Черга батчів для обробки
    private BlockingQueue<List<String[]>> batchQueue;

    // Позиція для перетягування вікна
    private double xOffset = 0;
    private double yOffset = 0;

    // Глобальна модель
    private LongProperty globalModelVowelCount = new SimpleLongProperty(0);

    public static void appendLog(String message) {
        logArea.appendText(message + "\n");
    }

    @Override
    public void start(Stage primaryStage) {
        // Ініціалізуємо швидкості
        speedModeMap.put("Швидкий", 1);
        speedModeMap.put("Середній", 20);
        speedModeMap.put("Повільний", 100);

        primaryStage.setTitle("Багатопотокова обробка даних");

        // Видаляємо стандартну заголовкову панель
        primaryStage.initStyle(StageStyle.UNDECORATED);

        TabPane tabPane = new TabPane();

        Tab mainTab = new Tab("Головна");
        mainTab.setContent(createMainTabContent(primaryStage));
        mainTab.setClosable(false);

        Tab historyTab = new Tab("Історія запусків");
        historyTab.setContent(createHistoryTabContent());
        historyTab.setClosable(false);

        Tab logTab = new Tab("Логи");
        logTab.setContent(createLogTabContent());
        logTab.setClosable(false);

        tabPane.getTabs().addAll(mainTab, historyTab, logTab);

        // Створюємо власну заголовкову панель
        BorderPane root = new BorderPane();
        root.setTop(createCustomTitleBar(primaryStage));
        root.setCenter(tabPane);

        // Створюємо сцену та застосовуємо стилі
        Scene scene = new Scene(root, 1000, 700); // Збільшуємо розмір вікна
        scene.getStylesheets().add("style.css");

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private HBox createCustomTitleBar(Stage stage) {
        HBox titleBar = new HBox();
        titleBar.setPadding(new Insets(5));
        titleBar.setStyle("-fx-background-color: #2d2d2d; -fx-border-color: #444444; -fx-border-width: 0 0 1 0;");

        // Іконка програми
        Label icon = new Label("\uD83C\uDF10"); // Unicode-символ для іконки
        icon.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-padding: 0 10 0 0;");

        // Назва програми
        Label title = new Label("Багатопотокова обробка даних");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Кнопка мінімізації
        Button minimizeButton = new Button("-");
        minimizeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 30; -fx-pref-height: 30;");
        minimizeButton.setOnAction(e -> stage.setIconified(true));
        minimizeButton.setOnMouseEntered(e -> minimizeButton.setStyle("-fx-background-color: #666666; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 30; -fx-pref-height: 30;"));
        minimizeButton.setOnMouseExited(e -> minimizeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 30; -fx-pref-height: 30;"));

        // Кнопка закриття
        Button closeButton = new Button("X");
        closeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 30; -fx-pref-height: 30;");
        closeButton.setOnAction(e -> stage.close());
        closeButton.setOnMouseEntered(e -> closeButton.setStyle("-fx-background-color: #ff5555; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 30; -fx-pref-height: 30;"));
        closeButton.setOnMouseExited(e -> closeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 30; -fx-pref-height: 30;"));

        titleBar.getChildren().addAll(icon, title, spacer, minimizeButton, closeButton);

        // Додаємо можливість перетягувати вікно
        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        titleBar.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        return titleBar;
    }

    private VBox createMainTabContent(Stage primaryStage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        // Контролери
        HBox controls = new HBox(10);

        threadCountField = new TextField("4");
        threadCountField.setPrefWidth(60);
        batchSizeField = new TextField("1000");
        batchSizeField.setPrefWidth(80);
        startButton = new Button("Почати обробку");

        // ChoiceBox для режиму швидкості
        ChoiceBox<String> speedChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList(speedModes));
        speedChoiceBox.setValue(currentSpeedMode);
        speedChoiceBox.setOnAction(e -> {
            currentSpeedMode = speedChoiceBox.getValue();
            int sleepTime = speedModeMap.get(currentSpeedMode);
            globalSleepTimeProperty.set(sleepTime);
        });

        controls.getChildren().addAll(
                new Label("Кількість потоків:"), threadCountField,
                new Label("Розмір батчу:"), batchSizeField,
                new Label("Режим швидкості:"), speedChoiceBox,
                startButton
        );

        // Стилізація меню вибору швидкості
        speedChoiceBox.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: #000000;");

        // Кнопка перемикання теми
        Button themeButton = new Button("Перемкнути тему");
        themeButton.setOnAction(e -> toggleTheme(root.getScene()));

        // Таблиця
        TableView<ThreadInfo> table = createTableView();
        VBox.setVgrow(table, Priority.ALWAYS);

        // Відображення глобальної моделі
        Label globalModelLabel = new Label();
        globalModelLabel.textProperty().bind(globalModelVowelCount.asString("Поточне значення моделі (кількість голосних): %d"));

        root.getChildren().addAll(controls, themeButton, globalModelLabel, table);

        startButton.setOnAction(e -> startProcessing());

        return root;
    }

    private VBox createHistoryTabContent() {
        // Кнопка перемикання теми
        Button themeButton = new Button("Перемкнути тему");
        themeButton.setOnAction(e -> toggleTheme(themeButton.getScene()));

        TableView<RunHistory> historyTable = new TableView<>();
        historyTable.setItems(runHistories);

        TableColumn<RunHistory, Number> runNumberCol = new TableColumn<>("Запуск №");
        runNumberCol.setCellValueFactory(new PropertyValueFactory<>("runNumber"));
        runNumberCol.prefWidthProperty().bind(historyTable.widthProperty().multiply(0.15));

        TableColumn<RunHistory, Number> threadCountCol = new TableColumn<>("Кількість потоків");
        threadCountCol.setCellValueFactory(new PropertyValueFactory<>("threadCount"));
        threadCountCol.prefWidthProperty().bind(historyTable.widthProperty().multiply(0.2));

        TableColumn<RunHistory, Number> batchSizeCol = new TableColumn<>("Розмір батчу");
        batchSizeCol.setCellValueFactory(new PropertyValueFactory<>("batchSize"));
        batchSizeCol.prefWidthProperty().bind(historyTable.widthProperty().multiply(0.2));

        TableColumn<RunHistory, String> speedModeCol = new TableColumn<>("Режим швидкості");
        speedModeCol.setCellValueFactory(new PropertyValueFactory<>("speedMode"));
        speedModeCol.prefWidthProperty().bind(historyTable.widthProperty().multiply(0.2));

        TableColumn<RunHistory, Number> executionTimeCol = new TableColumn<>("Час виконання (мс)");
        executionTimeCol.setCellValueFactory(new PropertyValueFactory<>("executionTime"));
        executionTimeCol.prefWidthProperty().bind(historyTable.widthProperty().multiply(0.25));

        historyTable.getColumns().addAll(runNumberCol, threadCountCol, batchSizeCol, speedModeCol, executionTimeCol);

        VBox vbox = new VBox(10, themeButton, historyTable);
        vbox.setPadding(new Insets(10));
        return vbox;
    }

    private VBox createLogTabContent() {
        // Кнопка перемикання теми
        Button themeButton = new Button("Перемкнути тему");
        themeButton.setOnAction(e -> toggleTheme(themeButton.getScene()));

        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefHeight(600);
        VBox vbox = new VBox(10, themeButton, logArea);
        vbox.setPadding(new Insets(10));
        VBox.setVgrow(logArea, Priority.ALWAYS);
        return vbox;
    }

    private TableView<ThreadInfo> createTableView() {
        TableView<ThreadInfo> table = new TableView<>();
        table.setItems(threadInfos);

        TableColumn<ThreadInfo, Number> threadIdCol = new TableColumn<>("Потік №");
        threadIdCol.setCellValueFactory(new PropertyValueFactory<>("threadId"));
        threadIdCol.prefWidthProperty().bind(table.widthProperty().multiply(0.08));

        TableColumn<ThreadInfo, String> statusCol = new TableColumn<>("Стан");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.prefWidthProperty().bind(table.widthProperty().multiply(0.12));

        TableColumn<ThreadInfo, Number> currentRowIdCol = new TableColumn<>("Поточний ID рядка");
        currentRowIdCol.setCellValueFactory(new PropertyValueFactory<>("currentRowId"));
        currentRowIdCol.prefWidthProperty().bind(table.widthProperty().multiply(0.15));

        TableColumn<ThreadInfo, String> batchRangeCol = new TableColumn<>("Діапазон рядків батчу");
        batchRangeCol.setCellValueFactory(new PropertyValueFactory<>("currentBatchRange"));
        batchRangeCol.prefWidthProperty().bind(table.widthProperty().multiply(0.2));

        TableColumn<ThreadInfo, Number> batchVowelCountCol = new TableColumn<>("Кількість голосних (батч)");
        batchVowelCountCol.setCellValueFactory(new PropertyValueFactory<>("batchVowelCount"));
        batchVowelCountCol.prefWidthProperty().bind(table.widthProperty().multiply(0.15));

        TableColumn<ThreadInfo, Void> actionCol = new TableColumn<>("Дії");
        actionCol.prefWidthProperty().bind(table.widthProperty().multiply(0.3));
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button pauseButton = new Button("Пауза");
            private final Button resumeButton = new Button("Відновити");
            private final Button terminateButton = new Button("Завершити");
            private final HBox pane = new HBox(5, pauseButton, resumeButton, terminateButton);

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

                // Стилізація кнопок
                pauseButton.setStyle("-fx-background-color: #ffa500; -fx-text-fill: white;");
                resumeButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                terminateButton.setStyle("-fx-background-color: #ff0000; -fx-text-fill: white;");
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

        table.getColumns().addAll(threadIdCol, statusCol, currentRowIdCol, batchRangeCol, batchVowelCountCol, actionCol);

        return table;
    }

    private void startProcessing() {
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
        globalModelVowelCount.set(0);

        Task<Void> processingTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Завантажуємо дані
                List<String[]> data = CsvReader.readCsv(CsvReader.filePath);
                if (data.isEmpty()) {
                    Platform.runLater(() -> {
                        showAlert("Помилка", "Дані не знайдено у CSV-файлі.");
                        startButton.setDisable(false);
                    });
                    return null;
                }

                // Розбиваємо дані на батчі
                List<List<String[]>> batches = BatchSplitter.splitIntoBatches(data, batchSize);
                batchQueue = new LinkedBlockingQueue<>(batches);

                // Створюємо пул потоків
                executorService = Executors.newFixedThreadPool(threadCount);

                long startTime = System.currentTimeMillis();

                // Запускаємо потоки
                List<Future<Integer>> futures = new ArrayList<>();

                // Define a Consumer to handle batch results
                Consumer<Integer> batchResultCallback = count -> {
                    Platform.runLater(() -> {
                        globalModelVowelCount.set(globalModelVowelCount.get() + count);
                    });
                };

                for (int i = 0; i < threadCount; i++) {
                    ThreadProcessor processor = new ThreadProcessor(i + 1, batchQueue, globalSleepTimeProperty, batchResultCallback);

                    ThreadInfo threadInfo = new ThreadInfo(processor);
                    Platform.runLater(() -> threadInfos.add(threadInfo));

                    Future<Integer> future = executorService.submit(processor);
                    futures.add(future);
                }

                // Агрегація результатів
                for (Future<Integer> future : futures) {
                    try {
                        int result = future.get();
                        // The global count is already updated via the callback
                        // But if needed, we can add here as well
                        // Platform.runLater(() -> globalModelVowelCount.set(globalModelVowelCount.get() + result));
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }

                long endTime = System.currentTimeMillis();
                long executionTime = endTime - startTime;

                // Додаємо запис до історії запусків
                int runNumber = runHistories.size() + 1;
                RunHistory history = new RunHistory(runNumber, threadCount, batchSize, currentSpeedMode, executionTime);
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
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
