package org.serverchat.exceptions;

/**
 * Исключение, возникающее при ошибках работы с ключевым хранилищем.
 * <p>
 * Это исключение используется для индикации проблем, связанных с загрузкой или использованием
 * ключевого хранилища, таких как его отсутствие, повреждение или неправильный пароль.
 * </p>
 */
public class KeyStoreException extends Exception {

    /**
     * Создает новое исключение KeyStoreException с указанным сообщением.
     *
     * @param message описание ошибки, объясняющее, что вызвало исключение.
     */
    public KeyStoreException(String message) {
        super(message);
    }
}