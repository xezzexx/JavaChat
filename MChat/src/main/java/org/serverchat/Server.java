package org.serverchat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Класс, представляющий сервер для обработки подключения клиентов и их сообщений.
 * Сервер прослушивает определенный порт, принимает подключения клиентов и обрабатывает их сообщения.
 */
public class Server {

    private ServerSocket serverSocket;
    private final int PORT = 8030;
    private List<ClientHandler> synchronizedClients = Collections.synchronizedList(new ArrayList<>());
    private List<String> synchronizedClientNames = Collections.synchronizedList(new ArrayList<>());

    /**
     * Главный метод для запуска сервера.
     * Создает экземпляр сервера и запускает его.
     * @param args Аргументы командной строки (не используются).
     */
    public static void main(String[] args) {
        Server server = new Server();
        server.startServer();
    }

    /**
     * Запускает сервер, который прослушивает подключения от клиентов.
     * Ожидает подключения клиентов и передает их обработку в отдельные потоки.
     */
    public void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Сервер запущен на порту " + PORT);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Клиент подключен: " + clientSocket.getInetAddress());
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    new Thread(clientHandler).start();
                } catch (Exception e) {
                    System.err.println("Ошибка при подключении клиента: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (Exception e) {
                    System.err.println("Ошибка при закрытии сервера: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Класс для обработки сообщений клиента в отдельном потоке.
     */
    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String userName;

        /**
         * Конструктор для инициализации клиента с сокетом.
         *
         * @param socket Сокет клиента.
         */
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        /**
         * Обрабатывает подключение клиента, настройку потоков и коммуникацию с клиентом.
         */
        @Override
        public void run() {
            try {
                setupStreams();
                handleClientCommunication();
            } catch (Exception e) {
                System.err.println("Ошибка при общении с клиентом: " + e.getMessage());
            } finally {
                cleanupClient();
            }
        }

        /**
         * Настраивает потоки для общения с клиентом.
         * Читает имя клиента и добавляет его в список подключенных пользователей.
         */
        private void setupStreams() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                userName = in.readLine();
                addClient();
            } catch (Exception e) {
                System.err.println("Ошибка при настройке потоков клиента: " + e.getMessage());
                cleanupClient();
            }
        }


        /**
         * Обрабатывает сообщения от клиента.
         * Сообщения, содержащие "exit", приводят к завершению соединения с клиентом.
         */
        private void handleClientCommunication() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if ("exit".equalsIgnoreCase(message)) {
                        break;
                    }
                    sendToAllClients(userName + ": " + message, this);
                }
            } catch (Exception e) {
                System.err.println("Ошибка при обработке сообщений клиента: " + e.getMessage());
            }
        }

        /**
         * Выполняет очистку после завершения работы с клиентом.
         * Удаляет клиента из списка и закрывает его сокет.
         */
        private void cleanupClient() {
            try {
                removeClient();
                sendToAllClients(userName + " отключился.");
                sendUserCountToAllClients();
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (Exception e) {
                System.err.println("Ошибка при очистке клиента: " + e.getMessage());
            }
        }

        /**
         * Добавляет клиента в список подключенных пользователей.
         */
        private void addClient() {
            try {
                synchronized (synchronizedClientNames) {
                    synchronizedClientNames.add(userName);
                }
                synchronized (synchronizedClients) {
                    synchronizedClients.add(this);
                }
                sendToAllClients(userName + " подключился!");
                sendUserCountToAllClients();
            } catch (Exception e) {
                System.err.println("Ошибка при добавлении клиента: " + e.getMessage());
            }
        }

        /**
         * Удаляет клиента из списка подключенных пользователей.
         */
        private void removeClient() {
            try {
                synchronized (synchronizedClientNames) {
                    synchronizedClientNames.remove(userName);
                }
                synchronized (synchronizedClients) {
                    synchronizedClients.remove(this);
                }
            } catch (Exception e) {
                System.err.println("Ошибка при удалении клиента: " + e.getMessage());
            }
        }


        /**
         * Отправляет сообщение всем подключенным клиентам.
         * Этот метод вызывает {@link #sendToAllClients(String, ClientHandler)} без исключения клиента.
         * @param message Сообщение для отправки всем клиентам.
         */
        private void sendToAllClients(String message) {
            sendToAllClients(message, null);
        }

        /**
         * Отправляет сообщение всем подключенным клиентам.
         * @param message Сообщение для отправки.
         * @param excludedClient Клиент, которому не нужно отправлять сообщение.
         */
        private void sendToAllClients(String message, ClientHandler excludedClient) {
            synchronized (synchronizedClients) {
                for (ClientHandler client : synchronizedClients) {
                    try {
                        if (client != excludedClient) {
                            client.out.println(message);
                        }
                    } catch (Exception e) {
                        System.err.println("Ошибка при отправке сообщения клиенту: " + e.getMessage());
                    }
                }
            }
        }

        /**
         * Отправляет всем клиентам информацию о текущем количестве подключенных пользователей.
         */
        private void sendUserCountToAllClients() {
            synchronized (synchronizedClients) {
                try {
                    int userCount = synchronizedClients.size();
                    String userCountMessage = "Количество пользователей на сервере: " + userCount;
                    for (ClientHandler client : synchronizedClients) {
                        client.out.println(userCountMessage);
                    }
                    System.out.println(userCountMessage); // Лог в терминал
                } catch (Exception e) {
                    System.err.println("Ошибка при отправке количества пользователей: " + e.getMessage());
                }
            }
        }
    }
}