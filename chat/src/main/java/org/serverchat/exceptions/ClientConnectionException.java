package org.serverchat.exceptions;

/**
 * Исключение, возникающее при установке соединения с клиентом.
 * <p>
 * Это исключение используется для обработки ошибок, связанных с подключением клиента к серверу,
 * таких как ошибки сокета или проблемы с сетью.
 * </p>
 */
public class ClientConnectionException extends Exception {

    /**
     * Исключение, которое выбрасывается при ошибке подключения клиента.
     *
     * @param message Сообщение, описывающее ошибку подключения.
     */
    public ClientConnectionException(String message) {
        super(message);
    }

    /**
     * Создает новое исключение ClientConnectionException с указанным сообщением и причиной.
     *
     * @param message описание ошибки, объясняющее, что вызвало исключение.
     * @param cause   причина, по которой произошло исключение, может быть {@code null}.
     */
    public ClientConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}