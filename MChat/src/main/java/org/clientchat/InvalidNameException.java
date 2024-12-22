package org.clientchat;

/**
 * Исключение, которое выбрасывается, когда имя пользователя недействительно.
 */
public class InvalidNameException extends Exception {
    /**
     * Конструктор для создания исключения с заданным сообщением.
     * @param message Сообщение об ошибке.
     */
    public InvalidNameException(String message) {
        super(message);
    }
}