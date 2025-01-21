package org.serverchat;

import org.serverchat.exceptions.*;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс {@code Server} предназначен для запуска SSL-сервера чата.
 * Он принимает подключения клиентов и обрабатывает их.
 * Сервер работает на порту 8040 и IP-адресе 127.0.0.1.
 * <p>
 * Переменные:
 * <ul>
 *     <li>{@code serverSocket} — сокет для SSL-соединений, который используется для прослушивания входящих соединений.</li>
 *     <li>{@code PORT} — порт, на котором работает сервер (по умолчанию 8040).</li>
 *     <li>{@code IP} — IP-адрес, на котором работает сервер (по умолчанию 127.0.0.1).</li>
 *     <li>{@code isServerRunning} — флаг, указывающий на состояние сервера. Если сервер работает, значение {@code true}; если остановлен, значение {@code false}.</li>
 * </ul>
 */
public class Server {

    public SSLServerSocket serverSocket;
    private static final int PORT = 8040; // Порт сервера
    private static final String IP = "127.0.0.1"; // IP-адрес сервера
    private static boolean isServerRunning = false; // Флаг для проверки состояния сервера

    /**
     * Точка входа в приложение для запуска сервера.
     *
     * @param args Аргументы командной строки.
     */
    public static void main(String[] args) {
        Server server = new Server();
        server.startServer();
    }

    /**
     * Метод для запуска сервера.
     * Настроить SSL, создать сокет и начать принимать подключения от клиентов.
     * Также регистрирует хук для остановки сервера при завершении работы программы.
     */
    public void startServer() {
        try {
            isServerRunning = true;
            SSLContext sslContext = setupSSLContext();
            serverSocket = createServerSocket(sslContext);

            System.out.println("The server is running IP: " + IP + ", and port: " + PORT);

            Runtime.getRuntime().addShutdownHook(new Thread(this::stopServer));

            while (isServerRunning) {
                acceptClientConnection();
            }
        } catch (IOException | SSLConfigurationException | KeyStoreException | ClientConnectionException e) {
            handleServerError(e);
        }
    }

    /**
     * Метод для настройки SSL-контекста для сервера.
     *
     * @return SSLContext настроенный для сервера.
     * @throws SSLConfigurationException При ошибке настройки SSL.
     * @throws KeyStoreException При ошибке загрузки ключевого хранилища.
     */
    public SSLContext setupSSLContext() throws SSLConfigurationException, KeyStoreException {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
            KeyStore keyStore = KeyStore.getInstance("JKS");

            try (InputStream keyStoreInput = Server.class.getResourceAsStream("/serverkeystore.jks")) {
                if (keyStoreInput == null) {
                    throw new KeyStoreException("Failed to find keystore!");
                }
                keyStore.load(keyStoreInput, "xezzexserverkey".toCharArray());
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, "xezzexserverkey".toCharArray());
            sslContext.init(kmf.getKeyManagers(), null, null);

            return sslContext;

        } catch (Exception e) {
            throw new SSLConfigurationException("SSL setup error", e);
        }
    }

    /**
     * Создание SSL-сокета для сервера.
     *
     * @param sslContext SSL-контекст.
     * @return SSL-серверный сокет.
     * @throws IOException При ошибке создания сокета.
     */
    public SSLServerSocket createServerSocket(SSLContext sslContext) throws IOException {
        SSLServerSocketFactory socketFactory = sslContext.getServerSocketFactory();
        return (SSLServerSocket) socketFactory.createServerSocket(PORT, 50, InetAddress.getByName(IP));
    }

    /**
     * Ожидает подключения клиента и передает его в обработчик.
     *
     * @throws ClientConnectionException При ошибке подключения клиента.
     */
    public void acceptClientConnection() throws ClientConnectionException {
        if (!isServerRunning) {
            throw new ClientConnectionException("The server has stopped, connection is not possible");
        }

        try {
            // Проверяем, что серверный сокет не закрыт перед ожиданием подключения
            if (serverSocket != null && !serverSocket.isClosed()) {
                SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                System.out.println("The client is connected: " + clientSocket.getInetAddress());
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            if (isServerRunning) {
                throw new ClientConnectionException("Error connecting client", e);
            }
        }
    }

    /**
     * Останавливает сервер и разрывает все активные соединения с клиентами.
     * Использование функционала StreamAPI.
     */
    public void stopServer() {
        try {
            isServerRunning = false;
            // Создаем копию списка подключенных клиентов для безопасного обхода
            List<ClientHandler> clientsCopy;

            synchronized (ClientHandler.getConnectedClients()) {
                clientsCopy = new ArrayList<>(ClientHandler.getConnectedClients());
            }

            for (ClientHandler client : clientsCopy) {
                if (!client.clientSocket.isClosed()) {
                    client.sendMessage("/error Разрыв соединения!");
                    client.cleanupClientResources(false); // Закрываем соединения
                }
            }

            // Закрываем серверный сокет
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Server stopped!");
            }
        } catch (IOException e) {
            System.err.println("Error while stopping server: " + e.getMessage());
        }
    }

    /**
     * Проверяет, запущен ли сервер в текущий момент.
     *
     * @return {@code true}, если сервер работает; {@code false}, если сервер остановлен.
     */
    public static boolean isServerRunning() {
        return isServerRunning;
    }

    /**
     * Обработка ошибок при запуске сервера.
     *
     * @param e Исключение, которое произошло.
     */
    private void handleServerError(Exception e) {
        if (e instanceof java.net.BindException) {
            System.err.println("Error: Port " + PORT + " already in use");
        } else {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }
}