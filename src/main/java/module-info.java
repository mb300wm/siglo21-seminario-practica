module com.comp.spiapp {
    requires java.sql;
    requires mysql.connector.j;
    requires password4j;
    requires javafx.controls;
    requires javafx.fxml;

    opens com.comp.spiapp to javafx.fxml;
    opens com.comp.spiapp.controller to javafx.fxml;
    exports com.comp.spiapp;
}
