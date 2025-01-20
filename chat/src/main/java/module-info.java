module org.mainchar {
    exports org.serverchat;
    exports org.clientchat;
    exports org.serverchat.exceptions;
    exports org.mainchat;

    requires java.base;
    requires java.logging;
    requires org.apache.logging.log4j;
    requires javafx.controls;
    requires javafx.fxml;

    opens org.serverchat to javafx.fxml;
    opens org.clientchat to javafx.fxml;

    uses org.mainchat.LauncherServer;
    uses org.mainchat.LauncherApp;
}