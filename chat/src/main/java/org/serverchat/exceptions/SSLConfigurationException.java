package org.serverchat.exceptions;

/**
 * Исключение, возникающее при ошибках настройки SSL-контекста.
 * <p>
 * Это исключение используется для индикации проблем, связанных с настройкой SSL-контекста,
 * таких как ошибки загрузки ключевого хранилища, создания сокета или инициализации SSL.
 * </p>
 */
public class SSLConfigurationException extends Exception {

    /**
     * Создает новое исключение SSLConfigurationException с указанным сообщением и причиной.
     *
     * @param message описание ошибки, объясняющее, что вызвало исключение.
     * @param cause   причина возникновения исключения (может быть {@code null}).
     */
    public SSLConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}