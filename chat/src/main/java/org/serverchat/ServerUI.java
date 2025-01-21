package org.serverchat;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.Optional;

/**
 * Класс {@code ServerUI} отвечает за отображение пользовательского интерфейса сервера чата.
 * Он использует JavaFX для создания окна с информацией о сервере, а также запускает сервер в отдельном потоке.
 * <p>
 * Переменные:
 * <ul>
 *     <li>{@code server} — экземпляр класса {@code Server}, который управляет запуском и обслуживанием серверной части чата.</li>
 * </ul>
 */
public class ServerUI extends Application {

    private Server server;

    /**
     * Статический метод для запуска серверного интерфейса.
     * Вызывается для инициализации приложения.
     */
    public static void serverlaunch() {
        launch();
    }

    /**
     * Метод {@code start} инициализирует графический интерфейс приложения.
     * Создает окно с информацией о сервере и запускает сервер в отдельном потоке.
     *
     * @param primaryStage основное окно приложения.
     */
    @Override
    public void start(Stage primaryStage) {
        VBox layout = createLayout();
        setupScene(primaryStage, layout);

        // Запуск сервера в отдельном потоке
        startServerThread();
    }

    /**
     * Создает и настраивает layout для интерфейса сервера.
     *
     * @return объект VBox с элементами интерфейса.
     */
    private VBox createLayout() {
        VBox layout = new VBox(10);
        layout.getStyleClass().add("vbox-layout");

        String ip = "127.0.0.1";
        int port = 8040; // Порт сервера

        Label statusLabel = new Label("Сервер запущен!");
        statusLabel.getStyleClass().add("bold-label");

        Label ipLabel = new Label("IP: " + ip);

        Label portLabel = new Label("Port: " + port);

        layout.getChildren().addAll(statusLabel, ipLabel, portLabel);

        return layout;
    }

    /**
     * Настраивает сцену с элементами интерфейса и отображает её в указанном окне.
     *
     * @param primaryStage основное окно.
     * @param layout layout с элементами интерфейса.
     */
    private void setupScene(Stage primaryStage, VBox layout) {
        Scene scene = new Scene(layout, 200, 100);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/img/styles.css")).toExternalForm());

        Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/icon1.png")));
        primaryStage.getIcons().add(icon);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Сервер");
        primaryStage.setOnCloseRequest(event -> handleCloseRequest());
        primaryStage.show();
    }

    /**
     * Запускает сервер в отдельном потоке.
     * Этот метод создаёт новый поток и запускает сервер.
     */
    private void startServerThread() {
        new Thread(() -> {
            server = new Server();
            server.startServer();
        }).start();
    }

    /**
     * Обрабатывает закрытие окна и останавливает сервер.
     * При закрытии окна приложение корректно завершает работу.
     */
    private void handleCloseRequest() {
        Optional.ofNullable(server).ifPresent(Server::stopServer);
        Platform.exit(); // Закрытие приложения
    }
}