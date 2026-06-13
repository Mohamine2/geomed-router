module pgl.app {
    // Required to use JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires org.json;

    // Allow JavaFx to read the controllers (FXML)
    opens pgl.app.controller to javafx.fxml;

    exports pgl.app;
    exports pgl.app.model;
    exports pgl.app.algo;
    exports pgl.app.io;
    exports pgl.app.security;
    exports pgl.app.explainability;
}