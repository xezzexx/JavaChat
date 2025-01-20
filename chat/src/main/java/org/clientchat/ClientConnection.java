package org.clientchat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Класс для установления защищенного соединения с сервером (через SSL) и отправки/приема сообщений.
 *
 * <p>Переменные класса:</p>
 * <ul>
 *   <li>{@code logger} — Логгер для записи действий и ошибок.</li>
 *   <li>{@code host} — Адрес сервера, к которому подключается клиент.</li>
 *   <li>{@code port} — Порт сервера для подключения.</li>
 *   <li>{@code sslSocket} — SSL-сокет для соединения с сервером.</li>
 *   <li>{@code reader} — Поток для чтения данных от сервера.</li>
 *   <li>{@code writer} — Поток для отправки данных на сервер.</li>
 * </ul>
 */
public class ClientConnection {
    private static final Logger logger = LogManager.getLogger(ClientConnection.class);
    private final String host;
    private final int port;
    private SSLSocket sslSocket;
    private BufferedReader reader;
    private PrintWriter writer;

    /**
     * Конструктор для подключения к серверу по указанному адресу и порту через SSL.
     *
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
     * Устанавливает защищенное соединение с сервером.
     *
     * @throws IOException В случае ошибки при подключении.
     */
    private void connect() throws IOException, NullPointerException {
        try (InputStream trustStoreInput = getClass().getClassLoader().getResourceAsStream("serverkeystore.jks")) {
            if (trustStoreInput == null) {
                throw new IOException("Не удалось найти serverkeystore.jks в ресурсах.");
            }

            // Создаём временный файл для trustStore
            Path tempTrustStore = Files.createTempFile("serverkeystore", ".jks");
            Files.copy(trustStoreInput, tempTrustStore, StandardCopyOption.REPLACE_EXISTING);

            // Устанавливаем свойства для хранилища доверенных сертификатов
            System.setProperty("javax.net.ssl.trustStore", tempTrustStore.toAbsolutePath().toString());
            System.setProperty("javax.net.ssl.trustStorePassword", "xezzexserverkey");

            // Создаём SSL-сокет
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            sslSocket = (SSLSocket) factory.createSocket(host, port);

            // Создаём потоки ввода и вывода для общения через защищённое соединение
            reader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(new OutputStreamWriter(sslSocket.getOutputStream(), StandardCharsets.UTF_8), true);

             logger.info("Установлено защищенное соединение с сервером {}:{}", host, port);
        } catch (java.net.UnknownHostException e) {
            logger.error("Ошибка: указанный хост недоступен или не существует ({}). Проверьте IP-адрес и попробуйте снова.", host);
        } catch (javax.net.ssl.SSLException e) {
            logger.error("Ошибка SSL при установлении соединения. Проверьте настройки сертификатов.", e);
        } catch (IOException e) {
            logger.error("Ошибка при установлении соединения: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Неизвестная ошибка: {}", e.getMessage());
        }
    }

    /**
     * Отправляет сообщение на сервер.
     *
     * @param message Сообщение для отправки.
     * @throws IOException В случае ошибки при отправке.
     */
    public void sendMessage(String message) throws IOException {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Сообщение не может быть пустым!");
        }
        writer.println(message);
    }

    /**
     * Принимает сообщение от сервера.
     *
     * @return Сообщение от сервера.
     * @throws IOException В случае ошибки при получении.
     */
    public String receiveMessage() throws IOException {
        try {
            return reader.readLine();
        } catch (IOException e) {
                logger.warn("Соединение закрыто");
            }
        return null;
    }

    /**
     * Проверяет, подключен ли клиент к серверу.
     *
     * @return true, если подключение активно.
     */
    public boolean isConnected() throws NullPointerException {
        return sslSocket != null && sslSocket.isConnected();
    }

    /**
     * Закрывает соединение с сервером.
     */
    public void close() {
        try {
            if (sslSocket != null) {
                sslSocket.close();
            }
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            logger.error("Ошибка при закрытии соединения: ", e);
        }
    }
}