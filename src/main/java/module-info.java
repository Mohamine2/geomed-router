module geomed.app {
    // Required to use JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires org.json;

    // Allow JavaFx to read the controllers (FXML)
    opens geomed.app.controller to javafx.fxml;

    exports geomed.app;
    exports geomed.app.model;
    exports geomed.app.algo;
    exports geomed.app.io;
    exports geomed.app.security;
    exports geomed.app.explainability;
}