package org.clientchat;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Главный класс приложения клиента чата.
 * Реализует интерфейс пользователя и логику подключения к серверу.
 */
public class ClientApp extends Application {

    private ClientConnection connection;
    private MessageHandler messageHandler;
    private Stage primaryStage;
    private final UIHandler uiHandler = new UIHandler();
    private Label userCountLabel;
    private TextArea messagesArea;

    /**
     * Метод для запуска приложения, точка входа в программу.
     */
    public static void applaunch() {
        launch();
    }

    /**
     * Инициализация главного окна и отображение окна входа.
     * @param primaryStage Главная сцена приложения.
     */
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        showLoginWindow();
    }

    /**
     * Отображает окно входа, где пользователь вводит имя и параметры подключения.
     */
    private void showLoginWindow() {
        Label namePrompt = uiHandler.createNamePrompt();
        TextField nameField = uiHandler.createNameField();
        TextField ipField = uiHandler.createIPField();
        TextField portField = uiHandler.createPortField();

        Button enterButton = uiHandler.createEnterButton(nameField, namePrompt, null, () -> {
            try {
                handleLogin(nameField, ipField, portField);
            } catch (Exception e) {
                showErrorAlert("Ошибка входа", e.getMessage());
            }
        });

        VBox loginLayout = new VBox(10, namePrompt, nameField, ipField, portField, enterButton);
        loginLayout.setPadding(new javafx.geometry.Insets(20));
        Scene loginScene = new Scene(loginLayout, 300, 200);

        primaryStage.setTitle("Chat Login");
        primaryStage.setScene(loginScene);
        primaryStage.show();
    }

    /**
     * Обрабатывает процесс входа пользователя, проверяя корректность введенных данных.
     * Выполняет подключение к серверу с использованием введенных имени, IP и порта.
     * @param nameField Поле ввода имени пользователя.
     * @param ipField Поле ввода IP-адреса сервера.
     * @param portField Поле ввода порта сервера.
     * @throws IllegalArgumentException Если имя пользователя пустое или порт не является числом.
     */
    private void handleLogin(TextField nameField, TextField ipField, TextField portField) {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Имя пользователя обязательно для входа.");
        }

        String ip = ipField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Порт должен быть числом.");
        }

        connectToServer(name, ip, port);
    }

    /**
     * Подключает клиента к серверу с указанным именем, IP и портом.
     * @param name Имя пользователя.
     * @param ip Адрес сервера.
     * @param port Порт сервера.
     */
    private void connectToServer(String name, String ip, int port) {
        try {
            connection = new ClientConnection(ip, port);

            if (!connection.isConnected()) {
                throw new IOException("Не удалось подключиться к серверу.");
            }

            messageHandler = new MessageHandler(connection);
            messageHandler.sendMessage(name); // Отправляем имя пользователя

            showChatWindow();
            listenForMessages();
        } catch (Exception e) {
            showErrorAlert("Ошибка подключения", e.getMessage());
        }
    }

    /**
     * Отображает основное окно чата.
     */
    private void showChatWindow() {
        messagesArea = uiHandler.createMessagesArea();
        TextField messageField = uiHandler.createMessageField();
        Button sendButton = uiHandler.createSendButton();

        userCountLabel = new Label("Количество пользователей на сервере: 0");

        sendButton.setOnAction(e -> {
            try {
                handleSendMessage(messageField);
            } catch (Exception ex) {
                appendMessageToChat(messagesArea, "Ошибка отправки сообщения: " + ex.getMessage());
            }
        });

        VBox chatLayout = uiHandler.createLayout(messagesArea, messageField, sendButton);
        chatLayout.getChildren().add(0, userCountLabel);

        primaryStage.setOnCloseRequest(e -> handleExit());

        Scene chatScene = new Scene(chatLayout, 400, 300);
        primaryStage.setScene(chatScene);
    }

    /**
     * Обрабатывает отправку сообщения. Проверяет, что сообщение не пустое и что клиент подключен к серверу.
     * Если все проверки пройдены, отправляет сообщение и добавляет его в чат.
     * Если подключение отсутствует, выводит соответствующее сообщение в чат.
     * @param messageField Поле ввода сообщения.
     * @throws IllegalArgumentException Если сообщение пустое.
     */
    private void handleSendMessage(TextField messageField) {
        String message = messageField.getText();
        if (message.isEmpty()) {
            throw new IllegalArgumentException("Сообщение не может быть пустым.");
        }
        if (connection != null && connection.isConnected()) {
            messageHandler.sendMessage(message);
            appendMessageToChat(messagesArea, "Вы: " + message);
            messageField.clear();
        } else {
            appendMessageToChat(messagesArea, "Вы не подключены к серверу.");
        }
    }

    /**
     * Обрабатывает завершение работы приложения. Закрывает соединение с сервером (если оно установлено)
     * и завершает выполнение программы.
     */
    private void handleExit() {
        if (messageHandler != null) {
            messageHandler.closeConnection();
        }
        Platform.exit();
    }

    /**
     * Добавляет новое сообщение в область чата.
     * @param messagesArea Область сообщений.
     * @param message Сообщение для добавления.
     */
    private void appendMessageToChat(TextArea messagesArea, String message) {
        messagesArea.appendText(message + "\n");
    }

    /**
     * Запускает поток для прослушивания сообщений от сервера.
     */
    private void listenForMessages() {
        Thread messageListener = new Thread(() -> {
            try {
                String message;
                while ((message = messageHandler.receiveMessage()) != null) {
                    handleIncomingMessage(message);
                }
            } catch (Exception e) {
                Platform.runLater(() -> appendMessageToChat(messagesArea, "Ошибка при получении сообщения: " + e.getMessage()));
            }
        });
        messageListener.setDaemon(true);
        messageListener.start();
    }

    /**
     * Обрабатывает входящее сообщение от сервера. Если сообщение содержит информацию о количестве пользователей на сервере,
     * обновляется метка с количеством пользователей. В противном случае сообщение добавляется в чат.
     * @param message Сообщение, полученное от сервера.
     */
    private void handleIncomingMessage(String message) {
        if (message.startsWith("Количество пользователей на сервере: ")) {
            String userCount = message.split(":")[1].trim();
            Platform.runLater(() -> userCountLabel.setText("Количество пользователей на сервере: " + userCount));
        } else {
            Platform.runLater(() -> appendMessageToChat(messagesArea, message));
        }
    }

    /**
     * Отображает окно с ошибкой.
     * @param title Заголовок окна.
     * @param content Текст ошибки.
     */
    private void showErrorAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}