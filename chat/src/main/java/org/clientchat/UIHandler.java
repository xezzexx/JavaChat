package org.clientchat;

import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.io.InputStream;

/**
 * Класс для создания элементов пользовательского интерфейса.
 */
public class UIHandler {

    /**
     * Метод для создания области сообщений.
     *
     * @return Объект TextArea для отображения сообщений.
     */
    public TextArea createMessagesArea() {
        TextArea messagesArea = new TextArea();
        messagesArea.setEditable(false);
        messagesArea.setPrefWidth(500);
        messagesArea.setPrefHeight(400);
        return messagesArea;
    }

    /**
     * Метод для создания поля ввода имени.
     *
     * @return Поле ввода имени.
     */
    public TextField createNameField() {
        return new TextField();
    }

    /**
     * Метод для создания подсказки для ввода имени.
     *
     * @return Подсказка для имени.
     */
    public Label createNamePrompt() {
        return new Label("Введите ваше имя:");
    }

    /**
     * Метод для создания поля ввода сообщения.
     *
     * @return Поле для ввода сообщения.
     */
    public TextField createMessageField() {
        TextField messageField = new TextField();
        messageField.setPromptText("Введите сообщение...");
        return messageField;
    }

    /**
     * Метод для создания кнопки "Отправить".
     *
     * @return Кнопка для отправки сообщений.
     */
    public Button createSendButton() {
        return new Button("Отправить");
    }

    /**
     * Метод для создания кнопки "Войти".
     *
     * @param nameField поле ввода имени пользователя.
     * @param namePrompt подсказка для имени пользователя.
     * @param messagesArea область сообщений.
     * @param onEnter действие, выполняемое при нажатии кнопки.
     * @return кнопка "Войти".
     */
    public Button createEnterButton(TextField nameField, Label namePrompt, TextArea messagesArea, Runnable onEnter) {
        Button enterButton = new Button("Войти");
        enterButton.setOnAction(event -> onEnter.run());
        return enterButton;
    }

    /**
     * Метод для загрузки иконки приложения.
     *
     * @return элемент {@link ImageView} с иконкой приложения.
     * @throws IllegalArgumentException если иконка приложения не найдена.
     */
    public ImageView loadAppIcon() {
        InputStream iconStream = getClass().getResourceAsStream("/img/icon.png");
        if (iconStream == null) {
            throw new IllegalArgumentException("Application icon not found!");
        }
        Image appIcon = new Image(iconStream);
        return new ImageView(appIcon);
    }

    /**
     * Метод для создания поля ввода IP-адреса.
     *
     * @return текстовое поле для ввода IP-адреса с предварительно установленным значением.
     */
    public TextField createIPField() {
        return new TextField("127.0.0.1");
    }

    /**
     * Метод для создания поля ввода порта.
     *
     * @return текстовое поле для ввода порта с предварительно установленным значением.
     */
    public TextField createPortField() {
        return new TextField("8040");
    }

    /**
     * Метод для создания кнопки "Открыть окно администратора".
     *
     * @return Кнопка для открытия окна администратора.
     */
    public Button createAdminButton() {
        Button adminButton = new Button("Администратор");
        adminButton.setPrefWidth(200);
        return adminButton;
    }

    /**
     * Метод для создания основного интерфейса чата (размещение элементов на экране).
     * Здесь добавляется метка с количеством пользователей сверху.
     *
     * @param userCountLabel метка для отображения количества пользователей.
     * @param messagesArea область для отображения сообщений.
     * @param messageField поле ввода сообщения.
     * @param sendButton кнопка для отправки сообщений.
     * @param adminButton кнопка администратора.
     * @return компоновка элементов чата в виде {@link VBox}.
     */
    public VBox createChatLayout(Label userCountLabel, TextArea messagesArea, TextField messageField, Button sendButton, Button adminButton) {
        VBox layout = new VBox(12, userCountLabel, messagesArea, messageField);
        HBox buttonLayout = createButtonLayout(sendButton, adminButton);
        layout.getChildren().add(buttonLayout);
        layout.setPadding(new javafx.geometry.Insets(20));
        return layout;
    }

    /**
     * Метод для создания разметки кнопок (расположение кнопок "Отправить" и "Администратор").
     *
     * @param sendButton кнопка для отправки сообщений.
     * @param adminButton кнопка администратора (может быть {@code null}).
     * @return горизонтальная компоновка кнопок в виде {@link HBox}.
     */
    public HBox createButtonLayout(Button sendButton, Button adminButton) {
        HBox buttonLayout = new HBox(10, sendButton);
        if (adminButton != null) {
            buttonLayout.getChildren().add(adminButton); // Добавляем кнопку администратора, если она существует
        }
        return buttonLayout;
    }
}