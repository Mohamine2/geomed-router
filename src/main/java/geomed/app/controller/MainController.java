package geomed.app.controller;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import geomed.app.model.MapManager;

/**
 * Root structural layout controller coordinating the integration lifecycle of the application.
 * <p>
 * This class serves as the master dashboard coordinator. It dynamically bootstraps independent FXML layout
 * components (the interactive graphics map canvas and the contextual management sidebar) from localized package
 * paths and binds them securely within its parent structural layout view wrappers.
 * </p>
 * <p>
 * Following a modular decoupling architecture pattern, it instantiates the primary shared operational data domain model
 * context ({@link MapManager}) and injects cross-references between the instantiated peer controllers, ensuring
 * seamless bidirectional event updates (e.g., matching clicked map node geometry data directly to text updates on the sidebar UI).
 * </p>
 *
 * @version 1.0
 */
public class MainController {

    /** Structural pane wrapper dedicated to containing the standalone interactive geometric map canvas view layout. */
    @FXML
    private AnchorPane mapContainer;

    /** Structural pane wrapper dedicated to containing the control statistics sidebar menu view layout. */
    @FXML
    private AnchorPane sidebarContainer;

    /** The injected instance of the graphical viewport layout controller. */
    private MapController mapController;

    /** The injected instance of the control metadata and metrics panel controller. */
    private SidebarController sidebarController;

    /**
     * Post-initialization constructor lifecycle hook executed automatically by JavaFX loading routines.
     * <p>
     * Triggers sub-module file streaming loads and configures cross-injections linking the underlying business model data,
     * the canvas display layout rendering layers, and the accompanying control sidebar menu interfaces.
     * </p>
     */
    @FXML
    public void initialize() {
        System.out.println("MainController initialized successfully!");

        try {
            loadSubViews();

            // Instantiate the main foundational business layer data coordinator context
            MapManager mapManager = new MapManager();

            // Establish direct object dependencies across layout component architecture modules
            mapController.setMapManager(mapManager);
            sidebarController.setMapManager(mapManager);
            sidebarController.setMapController(mapController);
            mapController.setSidebarController(sidebarController);

        } catch (IOException e) {
            System.err.println("Critical error occurred during JavaFX sub-view loading sequences.");
            e.printStackTrace();
        }
    }

    /**
     * Loads secondary nested layout structures into their specific designated AnchorPane surface contexts.
     * <p>
     * Resolves layout streaming inputs using {@link FXMLLoader}:
     * <ul>
     * <li>Streams {@code /geomed/app/fxml/map.fxml} into the designated {@link #mapContainer}.</li>
     * <li>Streams {@code /geomed/app/fxml/sidebar.fxml} into the designated {@link #sidebarContainer}.</li>
     * </ul>
     * Automatically applies anchor alignment constraints ($0.0\text{ px}$) on the sub-modules to enforce clean scaling behavior
     * across the window workspace.
     * </p>
     * * @throws IOException if an error occurs while locating, reading, or opening target FXML file resource streams
     */
    private void loadSubViews() throws IOException {
        // 1. Resolve, stream parse, and inject the interactive graphical map canvas view component
        FXMLLoader mapLoader = new FXMLLoader(getClass().getResource("/geomed/app/fxml/map.fxml"));
        AnchorPane mapView = mapLoader.load();
        mapController = mapLoader.getController();

        mapContainer.getChildren().setAll(mapView);
        AnchorPane.setTopAnchor(mapView, 0.0);
        AnchorPane.setRightAnchor(mapView, 0.0);
        AnchorPane.setBottomAnchor(mapView, 0.0);
        AnchorPane.setLeftAnchor(mapView, 0.0);

        // 2. Resolve, stream parse, and inject the contextual statistics controller sidebar view component
        FXMLLoader sidebarLoader = new FXMLLoader(getClass().getResource("/geomed/app/fxml/sidebar.fxml"));
        AnchorPane sidebarView = sidebarLoader.load();
        sidebarController = sidebarLoader.getController();

        sidebarContainer.getChildren().setAll(sidebarView);
        AnchorPane.setTopAnchor(sidebarView, 0.0);
        AnchorPane.setRightAnchor(sidebarView, 0.0);
        AnchorPane.setBottomAnchor(sidebarView, 0.0);
        AnchorPane.setLeftAnchor(sidebarView, 0.0);
    }
}