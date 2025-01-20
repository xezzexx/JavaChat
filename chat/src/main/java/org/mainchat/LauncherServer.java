package org.mainchat;

import org.serverchat.ServerUI;

/**
 * Класс LauncherServer является точкой входа для запуска сервера чата.
 * Этот класс отвечает за запуск пользовательского интерфейса сервера и его работу.
 * При запуске вызывается метод {@link ServerUI#serverlaunch()} для инициализации и старта сервера.
 */
public class LauncherServer {

    /**
     * Точка входа в приложение для запуска сервера.
     * Этот метод запускает серверный интерфейс, используя статический метод
     * {@link ServerUI#serverlaunch()} из класса {@link ServerUI}.
     *
     * @param args Аргументы командной строки (не используются в данном классе).
     * @see ServerUI#serverlaunch()
     */
    public static void main(String[] args) {
        ServerUI.serverlaunch();
    }
}