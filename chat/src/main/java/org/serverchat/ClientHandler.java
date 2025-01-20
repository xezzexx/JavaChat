package org.serverchat;

import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Класс, обрабатывающий клиента на сервере.
 * Он управляет подключениями, регистрацией имен пользователей, отправкой сообщений и выполнением команд администратора.
 *
 * <p>Переменные класса:</p>
 * <ul>
 *   <li>{@code clientSocket} — SSL-сокет, связанный с клиентом.</li>
 *   <li>{@code outputWriter} — Поток для отправки данных клиенту.</li>
 *   <li>{@code inputReader} — Поток для чтения данных от клиента.</li>
 *   <li>{@code username} — Имя пользователя клиента.</li>
 *   <li>{@code isClosed} — Флаг для отслеживания закрытия соединения.</li>
 *   <li>{@code SUPERADMIN_NAME} — Имя супер-администратора.</li>
 *   <li>{@code superAdminConnected} — Флаг, указывающий на подключение супер-администратора.</li>
 *   <li>{@code connectedClients} — Список всех подключенных клиентов.</li>
 * </ul>
 */
public class ClientHandler implements Runnable {

    public SSLSocket clientSocket;
    public PrintWriter outputWriter;
    public BufferedReader inputReader;
    public String username;
    public volatile boolean isClosed = false;

    private static final String SUPERADMIN_NAME = "Админ1270018040";
    public static boolean superAdminConnected = false; // Флаг подключения администратора
    private static final List<ClientHandler> connectedClients = Collections.synchronizedList(new ArrayList<>());

    /**
     * Конструктор для создания обработчика клиента.
     *
     * @param clientSocket Сокет клиента для обмена данными.
     */
    public ClientHandler(SSLSocket clientSocket) {
        this.clientSocket = clientSocket;
    }

    /**
     * Основной метод для обработки подключений клиента.
     * Выполняет установку потоков, регистрацию пользователя, и обработку сообщений от клиента.
     */
    @Override
    public void run() {
        try {
            setupClientStreams();
            verifyAndRegisterUsername();
            processClientMessages();
        } catch (IOException e) {
            if (!isClosed) {
                System.err.println("Error processing client: " + e.getMessage());
            }
        } finally {
            cleanupClientResources();
        }
    }

    /**
     * Устанавливает потоки для обмена данными с клиентом.
     *
     * @throws IOException если возникла ошибка при установке потоков.
     */
    public void setupClientStreams() throws IOException {
        inputReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        outputWriter = new PrintWriter(clientSocket.getOutputStream(), true);
    }

    /**
     * Проверяет имя пользователя и регистрирует его.
     *
     * @throws IOException если возникла ошибка при чтении данных.
     */
    public void verifyAndRegisterUsername() throws IOException {
        username = inputReader.readLine(); // Читаем имя пользователя

        if (username == null) {
            throw new IOException("Username not provided");
        }

        synchronized (connectedClients) {
            // Проверяем, занято ли имя
            if (isUsernameTaken(username)) {
                outputWriter.println("/error Имя пользователя '" + username + "' уже занято!");
                return; // Завершаем метод, но не закрываем соединение
            }

            if (SUPERADMIN_NAME.equalsIgnoreCase(username)) {
                handleSuperAdminConnection();
            } else {
                outputWriter.println("/role Пользователь");
            }

            // Добавляем клиента в список подключенных
            connectedClients.add(this);
        }

        // Уведомляем других о подключении
        broadcastMessage(username + " подключился!");
        updateClientCount();
        printConnectedClients();
    }

    /**
     * Обрабатывает подключение суперадмина.
     */
    private void handleSuperAdminConnection() {
        if (superAdminConnected) {
            outputWriter.println("/error Администратор уже подключен");
            return;
        }
        superAdminConnected = true;
        outputWriter.println("/role Админ1270018040");
    }

    /**
     * Проверяет, занято ли имя пользователя.
     * Использование функционала StreamAPI.
     *
     * @param username Имя пользователя.
     * @return true, если имя занято; иначе false.
     */
    private boolean isUsernameTaken(String username) {
        synchronized (connectedClients) {
            return connectedClients.stream().anyMatch(client -> client.username.equalsIgnoreCase(username));
        }
    }

    /**
     * Обрабатывает сообщения от клиента.
     *
     * @throws IOException если возникла ошибка при чтении данных.
     */
    private void processClientMessages() throws IOException {
        String message;
        while ((message = inputReader.readLine()) != null) {
            if (!isClosed) {
                if ("exit".equalsIgnoreCase(message)) {
                    cleanupClientResources();
                    break;
                } else if (message.startsWith("/kick ")) {
                    if (SUPERADMIN_NAME.equalsIgnoreCase(username)) {
                        handleKickCommand(message.substring(6).trim());
                    } else {
                        outputWriter.println("/error Команда доступна только Администратору");
                    }
                } else if ("/list".equalsIgnoreCase(message)) {
                    if (!Server.isServerRunning()) {
                        outputWriter.println("/error Сервер завершает работу, список пользователей недоступен");
                    } else {
                        synchronized (connectedClients) {
                            outputWriter.println("/users " + String.join(",", getConnectedUsernames()));
                        }
                    }
                } else {
                    if (!isClosed) {
                        broadcastMessage(username + ": " + message, this);
                    }
                }
            } else {
                break;
            }
        }
    }

    /**
     * Обрабатывает команду /kick для отключения пользователя.
     * Использование функционала StreamAPI.
     *
     * @param targetUsername имя пользователя, которого нужно отключить
     */
    private void handleKickCommand(String targetUsername) {
        if (username.equalsIgnoreCase(targetUsername)) {
            outputWriter.println("Нельзя заблокировать самого себя!");
            return;
        }

        synchronized (connectedClients) {
            ClientHandler targetClient = connectedClients.stream()
                    .filter(client -> client.username.equalsIgnoreCase(targetUsername))
                    .findFirst()
                    .orElse(null);

            if (targetClient != null) {
                targetClient.sendBanNotification();
                targetClient.cleanupClientResources(false);
                broadcastMessage("Пользователь " + targetUsername + " был отключён Администратором");
                updateClientCount();
                printConnectedClients();
            } else {
                outputWriter.println("/error Пользователь " + targetUsername + " не найден!");
            }
        }
    }

    /**
     * Отправляет уведомления о бане.
     */
    private void sendBanNotification() {
        String message = "/banned " + "Вы были отключены Администратором!";
        outputWriter.println(message);
    }

    /**
     * Очистка ресурсов клиента, таких как потоки и сокет.
     */
    public void cleanupClientResources() {
        cleanupClientResources(true);
    }

    /**
     * Очистка ресурсов клиента с возможностью оповещения других клиентов.
     *
     * @param notifyBroadcast Флаг, указывающий, нужно ли оповещать остальных.
     */
    public void cleanupClientResources(boolean notifyBroadcast) {
        if (isClosed) return;
        if (username == null) return;

        try {
            synchronized (connectedClients) {
                connectedClients.remove(this);
                if (SUPERADMIN_NAME.equalsIgnoreCase(username)) {
                    superAdminConnected = false;
                }
            }
            if (notifyBroadcast) {
                updateClientCount();
                broadcastMessage(username + " отключился!");
                printConnectedClients();
            }
            isClosed = true;
        } catch (Exception e) {
            System.err.println("Error while cleaning client: " + e.getMessage());
        } finally {
            try {
                if (inputReader != null) {
                    inputReader.close();
                }
                if (outputWriter != null) {
                    outputWriter.close();
                }
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                if (!"Socket closed".equals(e.getMessage())) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Получение списка всех подключенных клиентов.
     *
     * @return Список всех подключенных клиентов.
     */
    public static List<ClientHandler> getConnectedClients() {
        return connectedClients;
    }

    /**
     * Отправка сообщения всем клиентам.
     *
     * @param message Сообщение для отправки.
     */
    public void broadcastMessage(String message) {
        if (!isClosed) {
            broadcastMessage(message, null);
        }
    }

    /**
     * Отправка сообщения всем клиентам, кроме указанного.
     * Использование функционала StreamAPI.
     *
     * @param message Сообщение для отправки.
     * @param excludedClient Клиент, которому не нужно отправлять сообщение.
     */
    private void broadcastMessage(String message, ClientHandler excludedClient) {
        synchronized (connectedClients) {
            connectedClients.stream()
                    .filter(client -> client != excludedClient && !client.isClosed )
                    .forEach(client -> client.outputWriter.println(message));
        }
    }

    /**
     * Отправка сообщения текущему клиенту.
     *
     * @param message Сообщение для отправки.
     */
    public void sendMessage(String message) {
        if (clientSocket.isClosed() || outputWriter == null) {
            return; // Поток закрыт
        }
        outputWriter.println(message);
    }

    /**
     * Обновление информации о количестве пользователей на сервере.
     */
    private void updateClientCount() {
        String userCountMessage = "Количество пользователей на сервере: " + connectedClients.size();
        broadcastMessage(userCountMessage);
        System.out.println(userCountMessage);
    }

    /**
     * Печать списка подключенных пользователей в консоль.
     */
    private void printConnectedClients() {
        synchronized (connectedClients) {
            System.out.println("List of connected users: " + String.join(", ", getConnectedUsernames()));
        }
    }

    /**
     * Получение списка имен всех подключенных пользователей.
     * Использование функционала StreamAPI.
     *
     * @return Список имен пользователей.
     */
    private List<String> getConnectedUsernames() {
        synchronized (connectedClients) {
            return connectedClients.stream()
                    .map(client -> client.username)
                    .toList();
        }
    }
}