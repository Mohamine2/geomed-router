module pgl.app {
    // Required to use JavaFX
    requires javafx.controls;
    requires javafx.fxml;

    // Allow JavaFx to read the controllers (FXML)
    opens pgl.app.controller to javafx.fxml;

    exports pgl.app;
}