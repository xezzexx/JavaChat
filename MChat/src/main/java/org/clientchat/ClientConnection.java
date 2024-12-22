package org.clientchat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Класс для установления соединения с сервером и отправки/приема сообщений.
 */
public class ClientConnection {
    private static final Logger logger = LogManager.getLogger(ClientConnection.class);
    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    /**
     * Конструктор для подключения к серверу по указанному адресу и порту.
     * @param host Адрес сервера.
     * @param port Порт сервера.
     */
    public ClientConnection(String host, int port) {
        this.host = host;
        this.port = port;
        try {
            connect();
        } catch (IOException e) {
            logger.error("Ошибка при подключении к серверу: ", e);
        }
    }

    /**
     * Устанавливает соединение с сервером.
     * @throws IOException В случае ошибки при подключении.
     */
    private void connect() throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        logger.info("Подключено к серверу {}:{}", host, port);
    }

    /**
     * Отправляет сообщение на сервер.
     * @param message Сообщение для отправки.
     * @throws IOException В случае ошибки при отправке.
     * @throws InvalidNameException Если имя пользователя пустое.
     */
    public void sendMessage(String message) throws IOException, InvalidNameException {
        if (message == null || message.trim().isEmpty()) {
            throw new InvalidNameException("Имя пользователя не может быть пустым.");
        }
        writer.println(message);
    }

    /**
     * Принимает сообщение от сервера.
     * @return Сообщение от сервера.
     * @throws IOException В случае ошибки при получении.
     */
    public String receiveMessage() throws IOException {
        return reader.readLine();
    }

    /**
     * Проверяет, подключен ли клиент к серверу.
     * @return true, если подключение активно.
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    /**
     * Закрывает соединение с сервером.
     */
    public void close() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            logger.error("Ошибка при закрытии соединения: ", e);
        }
    }
}