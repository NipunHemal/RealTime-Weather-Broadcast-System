module TempSocket {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens lk.ijse to javafx.fxml;
    exports lk.ijse;
}