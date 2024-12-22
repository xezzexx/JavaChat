package org.clientchat;

import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import java.io.InputStream;

/**
 * Класс для создания элементов пользовательского интерфейса.
 */
public class UIHandler {

    /**
     * Метод для создания области сообщений.
     * @return Объект TextArea для отображения сообщений.
     */
    public TextArea createMessagesArea() {
        TextArea messagesArea = new TextArea();
        messagesArea.setEditable(false);
        return messagesArea;
    }

    /**
     * Метод для создания поля ввода имени.
     * @return Поле ввода имени.
     */
    public TextField createNameField() {
        return new TextField();
    }

    /**
     * Метод для создания подсказки для ввода имени.
     * @return Подсказка для имени.
     */
    public Label createNamePrompt() {
        Label namePrompt = new Label("Введите ваше имя:");
        return namePrompt;
    }

    /**
     * Метод для создания поля ввода сообщения.
     * @return Поле для ввода сообщения.
     */
    public TextField createMessageField() {
        TextField messageField = new TextField();
        messageField.setPromptText("Введите сообщение...");
        return messageField;
    }

    /**
     * Метод для создания кнопки "Отправить".
     * @return Кнопка для отправки сообщений.
     */
    public Button createSendButton() {
        Button sendButton = new Button("Отправить");
        return sendButton;
    }

    /**
    * Метод для создания кнопки "Войти".
    */
    public Button createEnterButton(TextField nameField, Label namePrompt, TextArea messagesArea, Runnable onEnter) {
        Button enterButton = new Button("Войти");
        enterButton.setOnAction(e -> onEnter.run());
        return enterButton;
    }

    /**
    * Метод для загрузки иконки приложения.
    */
//    public ImageView loadAppIcon() {
//        // Загрузка изображения как ресурса из папки resources
//        InputStream iconStream = getClass().getResourceAsStream("/img/icon.png");
//        if (iconStream == null) {
//            throw new IllegalArgumentException("Иконка приложения не найдена!");
//        }
//        Image appIcon = new Image(iconStream);
//        return new ImageView(appIcon);
//    }


    /**
    * Метод для создания поля ввода IP.
     */
    public TextField createIPField() {
        TextField ipField = new TextField("127.0.0.1");
        return ipField;
    }

    /**
    * Метод для создания поля ввода порта.
     */
    public TextField createPortField() {
        TextField portField = new TextField("8030");
        return portField;
    }

    /**
     * Метод для создания основного интерфейса чата (размещение элементов на экране).
     */
    public VBox createLayout(TextArea messagesArea, TextField messageField, Button sendButton) {
        VBox layout = new VBox(10, messagesArea, messageField, sendButton);
        layout.setPadding(new javafx.geometry.Insets(10));
        return layout;
    }
}