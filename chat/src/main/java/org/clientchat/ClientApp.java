package org.clientchat;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * Главный класс приложения клиента чата.
 * Реализует интерфейс пользователя и логику подключения к серверу.
 *
 * <p>Переменные класса:</p>
 * <ul>
 *   <li>{@code userListSheduler} — Планировщик для обновления списка пользователей.</li>
 *   <li>{@code connection} — Соединение с сервером.</li>
 *   <li>{@code messageHandler} — Обработчик сообщений от сервера.</li>
 *   <li>{@code primaryStage} — Главная сцена приложения.</li>
 *   <li>{@code uiHandler} — Обработчик пользовательского интерфейса.</li>
 *   <li>{@code userCountLabel} — Метка для отображения количества подключённых пользователей.</li>
 *   <li>{@code messagesArea} — Область для отображения сообщений чата.</li>
 *   <li>{@code userList} — Список подключённых пользователей.</li>
 *   <li>{@code isAdminWindowOpen} — Флаг для отслеживания открытия окна администратора.</li>
 *   <li>{@code isWindowClosed} — Флаг для отслеживания закрытия окна.</li>
 *   <li>{@code isApplicationRunning} — Флаг для отслеживания состояния приложения.</li>
 * </ul>
 */
public class ClientApp extends Application {

    private ScheduledExecutorService userListSheduler;
    private ClientConnection connection;
    private MessageHandler messageHandler;
    private Stage primaryStage;
    private final UIHandler uiHandler = new UIHandler();
    private Label userCountLabel;
    private TextArea messagesArea;
    private final ListView<String> userList = new ListView<>();
    private boolean isAdminWindowOpen = false;
    private volatile boolean isWindowClosed = false;
    private volatile boolean isApplicationRunning = true;

    /**
     * Метод для запуска приложения, точка входа в программу.
     */
    public static void applaunch() {
        launch();
    }

    /**
     * Инициализация главного окна и отображение окна входа.
     *
     * @param primaryStage Главная сцена приложения.
     */
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Обработчик закрытия окна
        primaryStage.setOnCloseRequest(event -> {
            isWindowClosed = true;
            handleExit();
        });
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

        ImageView gifImageView = createGifImageView("/img/loading2.gif");

        VBox loginLayout = new VBox(10, namePrompt, nameField, ipField, portField, enterButton);
        loginLayout.setPadding(new javafx.geometry.Insets(20));

        StackPane rootLayout = new StackPane();
        rootLayout.getChildren().addAll(loginLayout, gifImageView);
        StackPane.setAlignment(gifImageView, javafx.geometry.Pos.BOTTOM_RIGHT);

        Scene loginScene = new Scene(rootLayout, 300, 250);
        loginScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/img/styles.css")).toExternalForm());

        primaryStage.getIcons().add(uiHandler.loadAppIcon().getImage());
        primaryStage.setTitle("Регистрация");
        primaryStage.setScene(loginScene);
        primaryStage.show();
    }

    /**
     * Обрабатывает процесс входа пользователя, проверяя корректность введенных данных.
     * Выполняет подключение к серверу с использованием введенных имени, IP и порта.
     * Использование функционала StreamAPI.
     *
     * @param nameField Поле ввода имени пользователя.
     * @param ipField   Поле ввода IP-адреса сервера.
     * @param portField Поле ввода порта сервера.
     * @throws IllegalArgumentException Если имя пользователя пустое или порт не является числом.
     */
    private void handleLogin(TextField nameField, TextField ipField, TextField portField) {
        String name = nameField.getText().trim();
        List<String> invalidNames = Arrays.asList("admin", "root", "админ", "администратор");
        boolean isValid = invalidNames.stream().noneMatch(name::equalsIgnoreCase);

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Имя пользователя обязательно для входа!");
        } else if (!isValid) {
            throw new IllegalArgumentException("Имя пользователя недопустимо!");
        }

        String ip = ipField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Порт должен быть числом!");
        }
        connectToServer(name, ip, port);
    }

    /**
     * Подключает клиента к серверу с указанным именем, IP и портом.
     *
     * @param name Имя пользователя.
     * @param ip   Адрес сервера.
     * @param port Порт сервера.
     */
    private void connectToServer(String name, String ip, int port) {
        try {
            connection = new ClientConnection(ip, port);
            if (!connection.isConnected()) {
                throw new IOException("Не удалось подключиться к серверу!");
            }

            messageHandler = new MessageHandler(connection);
            messageHandler.sendMessage(name); // Отправляем имя пользователя

            showChatWindow("Админ1270018040".equalsIgnoreCase(name)); // Открываем окно чата с кнопкой администратора
            listenForMessages();
        } catch (Exception e) {
            showErrorAlert("Ошибка подключения", e.getMessage());
        }
    }

    /**
     * Отображает основное окно чата.
     *
     * @param isAdmin Флаг, указывающий, является ли пользователь администратором.
     */
    private void showChatWindow(boolean isAdmin) {
        messagesArea = uiHandler.createMessagesArea();
        TextField messageField = uiHandler.createMessageField();
        Button sendButton = uiHandler.createSendButton();

        userCountLabel = new Label("Количество пользователей на сервере: 0");

        sendButton.setOnAction(event -> {
            try {
                handleSendMessage(messageField);
            } catch (Exception ex) {
                appendMessageToChat(messagesArea, "Ошибка отправки сообщения: " + ex.getMessage());
            }
        });

        Button adminButton = isAdmin ? uiHandler.createAdminButton() : null;

        VBox chatLayout = uiHandler.createChatLayout(userCountLabel, messagesArea, messageField, sendButton, adminButton);

        if (adminButton != null) {
            adminButton.setOnAction(event -> showAdminWindow());
        }

        primaryStage.setOnCloseRequest(event -> handleExit());

        ImageView gifImageView = createGifImageView("/img/loading.gif");

        StackPane rootLayout = new StackPane();
        rootLayout.getChildren().addAll(chatLayout, gifImageView);
        StackPane.setAlignment(gifImageView, javafx.geometry.Pos.BOTTOM_RIGHT);

        Scene chatScene = new Scene(rootLayout, 550, 400);
        chatScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/img/styles.css")).toExternalForm());
        primaryStage.getIcons().add(uiHandler.loadAppIcon().getImage());
        primaryStage.setTitle("Чат");
        primaryStage.setScene(chatScene);
    }

    /**
     * Создает и возвращает объект ImageView, отображающий GIF-изображение.
     *
     * @param gifPath Путь к файлу GIF-изображения, который будет загружен.
     * @return Возвращает ImageView с загруженным изображением.
     */
    private ImageView createGifImageView(String gifPath) {
        Image gifImage = new Image(Objects.requireNonNull(getClass().getResource(gifPath)).toExternalForm());
        ImageView gifImageView = new ImageView(gifImage);
        gifImageView.setFitHeight(69); // Установите нужный размер
        gifImageView.setFitWidth(69);
        return gifImageView;
    }

    /**
     * Отображает окно управления для администратора в виде нового окна.
     */
    private void showAdminWindow() {
        Stage adminStage = new Stage();
        Button disconnectButton = new Button("Отключить");
        Button closeAdminButton = new Button("Закрыть");

        // Устанавливаем флаг, указывающий на открытие окна администратора
        isAdminWindowOpen = true;

        disconnectButton.setOnAction(event -> {
            String selectedUser = userList.getSelectionModel().getSelectedItem();
            if (selectedUser != null) {
                messageHandler.sendMessage("/kick " + selectedUser); // Команда отключения
            } else {
                appendMessageToChat(messagesArea, "Пожалуйста, выберите пользователя.");
            }
        });

        closeAdminButton.setOnAction(event -> {
            isAdminWindowOpen = false; // Устанавливаем флаг, указывающий, что окно закрыто
            adminStage.close();
            stopAdminWindowProcesses(); // Останавливаем процессы, связанные с окном администратора
        });

        // Проверяем, если окно администратора открыто, запускаем планировщик
        if (isAdminWindowOpen) {
            userListSheduler = Executors.newSingleThreadScheduledExecutor();
            userListSheduler.scheduleAtFixedRate(this::updateUserList, 0, 3, TimeUnit.SECONDS);
        }

        adminStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResource("/img/icon3.png")).toExternalForm()));

        HBox buttonLayout = new HBox(10, disconnectButton, closeAdminButton);
        buttonLayout.setPadding(new javafx.geometry.Insets(10));

        VBox adminLayout = new VBox(10, new Label("Список пользователей:"), userList, buttonLayout);
        adminLayout.setPadding(new javafx.geometry.Insets(20));

        Scene adminScene = new Scene(adminLayout, 400, 400);
        adminScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/img/styles.css")).toExternalForm());

        adminStage.setScene(adminScene);
        adminStage.setTitle("Окно администратора");

        // Закрытие окна администратора при закрытии главного окна
        adminStage.setOnCloseRequest(event -> {
            isAdminWindowOpen = false;
            stopAdminWindowProcesses();
        });

        adminStage.setOnCloseRequest(event -> isAdminWindowOpen = false);
        adminStage.show();
    }

    /**
     * Останавливает все процессы, связанные с окном администратора.
     * Этот метод завершает работу планировщика, который обновляет список пользователей.
     * После завершения процесса выводится сообщение в консоль об успешном завершении.
     */
    private void stopAdminWindowProcesses() {
        if (userListSheduler != null) {
            userListSheduler.shutdownNow();
        }
        System.out.println("Admin window process terminated successfilly!");
    }

    /**
     * Обновляет список пользователей в окне администратора.
     */
    private void updateUserList() {
        if (!isAdminWindowOpen || !isApplicationRunning || connection == null || !connection.isConnected()) {
            return; // Не обновляем список, если приложение завершено
        }

        String selectedUser = userList.getSelectionModel().getSelectedItem();

        Thread userListUpdater = new Thread(() -> {
            try {
                String message;
                messageHandler.sendMessage("/list");
                while ((message = messageHandler.receiveMessage()) != null) {
                    if (message.startsWith("/users ")) {
                        String userData = message.substring(7);
                        List<String> users = Arrays.asList(userData.split(","));
                        Platform.runLater(() -> {
                            userList.getItems().clear();
                            userList.getItems().addAll(users);

                            if (selectedUser != null && users.contains(selectedUser)) {
                                userList.getSelectionModel().select(selectedUser);
                            }
                        });
                    } else {
                        handleIncomingMessage(message);
                    }
                }
            } catch (Exception e) {
                if (isApplicationRunning) {
                    Platform.runLater(() -> {
                        showErrorAlert("Ошибка", "Ошибка обновления списка пользователей!");
                        handleExit();
                    });
                }
            }
        });
        userListUpdater.setDaemon(true);
        userListUpdater.start();
    }

    /**
     * Обрабатывает отправку сообщения. Проверяет, что сообщение не пустое и что клиент подключен к серверу.
     * Если все проверки пройдены, отправляет сообщение и добавляет его в чат.
     * Если подключение отсутствует, выводит соответствующее сообщение в чат.
     *
     * @param messageField Поле ввода сообщения.
     * @throws IllegalArgumentException Если сообщение пустое.
     */
    private void handleSendMessage(TextField messageField) {
        String message = messageField.getText();
        if (message.isEmpty()) {
            throw new IllegalArgumentException("Сообщение не может быть пустым!");
        }
        if (connection != null && connection.isConnected()) {
            messageHandler.sendMessage(message);
            appendMessageToChat(messagesArea, "Вы: " + message);
            messageField.clear();
        } else {
            appendMessageToChat(messagesArea, "Вы не подключены к серверу");
        }
    }

    /**
     * Закрывает соединение с сервером и завершает работу приложения.
     */
    private void handleExit() {
        try {
            isApplicationRunning = false;
            isWindowClosed = true;

            if (userListSheduler != null) {
                userListSheduler.shutdownNow(); // Останавливаем планировщик
            }
            if (messageHandler != null) {
                messageHandler.closeConnection(); // Закрытие соединения
            }
            if (connection != null && connection.isConnected()) {
                connection.close(); // Закрываем сокет
            }

            System.out.println("The connection was closed correctly");
        } catch (Exception e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
        Platform.exit();
        System.exit(0);
    }

    /**
     * Добавляет новое сообщение в область чата.
     *
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
                while (!(isWindowClosed)) {
                    assert messageHandler != null;
                    message = messageHandler.receiveMessage();
                    if (message != null) {
                        if (message.startsWith("/role ")) {
                            String role = message.split(" ")[1].trim();
                            Platform.runLater(() -> showChatWindow("Админ1270018040".equalsIgnoreCase(role)));
                        } else if (message.startsWith("/error ")) {
                            String errorMessage = message.substring(7);
                            if ("Разрыв соединения!".equals(errorMessage)) {
                                Platform.runLater(() -> showErrorAlert("Ошибка", errorMessage));
                                break;
                            } else if (errorMessage.startsWith("Имя пользователя")) {
                                Platform.runLater(() -> showErrorAlert("Ошибка имени", errorMessage));
                                break;
                            } else {
                                Platform.runLater(() -> appendMessageToChat(messagesArea, "Ошибка: " + errorMessage));
                            }
                        } else if (message.startsWith("/banned ")) {
                            String reason = message.substring(8);
                            Platform.runLater(() -> {
                                showErrorAlert("Отключение", reason);
                                messageHandler.closeConnection();
                            });
                            break;
                        } else {
                            handleIncomingMessage(message);
                        }
                    }
                }
            } catch (Exception e) {
                if (!isWindowClosed && isApplicationRunning) {
                    Platform.runLater(() -> {
                        showErrorAlert("Разрыв соединения!", "Соединение с сервером потеряно!");
                        handleExit();
                    });
                } else if (!"Socket closed".equals(e.getMessage())) {
                    Platform.runLater(() -> appendMessageToChat(messagesArea, "Ошибка при получении сообщения: " + e.getMessage()));
                }
            }
        });
        messageListener.setDaemon(true);
        messageListener.start();
    }

    /**
     * Обрабатывает входящее сообщение от сервера. Если сообщение содержит информацию о количестве пользователей на сервере,
     * обновляется метка с количеством пользователей. В противном случае сообщение добавляется в чат.
     *
     * @param message Сообщение, полученное от сервера.
     */
    private void handleIncomingMessage(String message) {
        // Фильтруем сообщения, чтобы исключить команды /role и /users
        if (message.startsWith("/role ") || message.startsWith("/users ")) {
            return;
        }

        if (message.startsWith("Количество пользователей на сервере: ")) {
            String userCount = message.split(":")[1].trim();
            Platform.runLater(() -> userCountLabel.setText("Количество пользователей на сервере: " + userCount));
        } else {
            Platform.runLater(() -> appendMessageToChat(messagesArea, message));
        }
    }

    /**
     * Отображает окно с ошибкой.
     *
     * @param title   Заголовок окна.
     * @param content Текст ошибки.
     */
    private void showErrorAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);

            ImageView errorIcon = new ImageView(Objects.requireNonNull(getClass().getResource("/img/error_icon.png")).toExternalForm());
            errorIcon.setFitHeight(50);
            errorIcon.setFitWidth(50);

            alert.setGraphic(errorIcon);
            alert.getDialogPane().getStylesheets().add(Objects.requireNonNull(getClass().getResource("/img/styles.css")).toExternalForm());

            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            Image windowIcon = new Image(Objects.requireNonNull(getClass().getResource("/img/icon2.png")).toExternalForm());
            stage.getIcons().add(windowIcon);

            alert.getDialogPane().getStyleClass().add("alert");

            if ("Отключение".equals(title) || "Ошибка имени".equals(title) || "Ошибка".equals(title) && "Разрыв соединения!".equals(content)) {
                alert.setOnHidden(event -> handleExit());
            }
            alert.showAndWait();
        });
    }
}