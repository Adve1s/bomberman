module com.bomberman.bomberman {
    requires javafx.controls;
    requires javafx.graphics;

    // JavaFX needs reflective access to the Application subclass
    opens com.bomberman.bomberman.client.app to javafx.graphics;

    // Export packages so they're accessible across the module
    exports com.bomberman.bomberman.client.app;
    exports com.bomberman.bomberman.client.rendering;
    exports com.bomberman.bomberman.client.input;
    exports com.bomberman.bomberman.local;
    exports com.bomberman.bomberman.shared.model;
    exports com.bomberman.bomberman.shared.entity;
    exports com.bomberman.bomberman.shared.util;
}