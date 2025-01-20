package org.clientchat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Класс обрабатывает отправку и получение сообщений от клиента.
 *
 * <p>Переменные класса:</p>
 * <ul>
 *   <li>{@code logger} — Логгер для записи действий и ошибок.</li>
 *   <li>{@code connection} — Объект для взаимодействия с сервером через защищённое соединение.</li>
 * </ul>
 */
public class MessageHandler {
    private static final Logger logger = LogManager.getLogger(MessageHandler.class);
    private final ClientConnection connection;

    /**
     * Конструктор для создания обработчика сообщений.
     *
     * @param connection Соединение с сервером.
     */
    public MessageHandler(ClientConnection connection) {
        this.connection = connection;
    }

    /**
     * Отправляет сообщение на сервер.
     *
     * @param message Сообщение для отправки.
     */
    public void sendMessage(String message) {
        try {
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            connection.sendMessage(new String(messageBytes, StandardCharsets.UTF_8));
            logger.info("Сообщение отправлено: {}", message);
        } catch (IllegalArgumentException  e) {
            logger.error("Ошибка имени пользователя: {}", e.getMessage());
        } catch (IOException e) {
            logger.error("Ошибка при отправке сообщения: ", e);
        }
    }

    /**
     * Принимает сообщение от сервера.
     *
     * @return Сообщение от сервера.
     */
    public String receiveMessage() {
        try {
            String message = connection.receiveMessage();
            message = new String(message.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            logger.info("Сообщение получено: {}", message);
            return message;
        } catch (IOException e) {
            if ("Socket closed".equals(e.getMessage())) {
                logger.warn("Соединение закрыто, прием сообщений прекращен");
            } else {
                logger.error("Ошибка при получении сообщения: ", e);
            }
            return null;
        }
    }

    /**
     * Закрывает соединение с сервером.
     */
    public void closeConnection() {
        try {
            connection.close();
            logger.info("Соединение успешно закрыто!");
        } catch (Exception e) {
            logger.warn("Ошибка при закрытии соединения: ", e);
        }
    }
}