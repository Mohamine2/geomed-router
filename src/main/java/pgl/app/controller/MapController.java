package pgl.app.controller;

import javafx.fxml.FXML;

import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import pgl.app.model.Hospital;
import pgl.app.model.MapManager;
import pgl.app.model.Point;
import pgl.app.model.Triangle;
import pgl.app.model.VictimIncident;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.MouseButton;
import java.util.HashSet;
import java.util.Set;
import javafx.scene.shape.Polygon;
import pgl.app.model.VoronoiCell;
import java.util.List;
import javafx.scene.shape.Shape;
import javafx.scene.shape.Rectangle;
import pgl.app.model.RoadEdge;

/**
 * Controller class responsible for rendering the map visualization layer and managing its UI interactions.
 * <p>
 * This class handles the graphical canvas layers—rendering hospital nodes, victims' emergency incidents,
 * underlying road infrastructure (color-coded by active traffic load coefficients), Delaunay triangulation
 * meshes, and localized Voronoi cell territories.
 * </p>
 * <p>
 * It intercepts user interactions such as multi-tiered mouse wheel zooming, continuous click-and-drag panning,
 * interactive node selection with localized graph neighbor highlights, and a drag-and-drop mechanism
 * for nodes that automatically snaps to the nearest road network intersections upon release.
 * </p>
 *
 * @version 1.0
 */
public class MapController {

    /** The current scale metric zoom multiplier applied to the map panel content workspace. */
    private double zoomFactor = 1.0;

    /** Absolute structural X-coordinate of the cursor layout recorded during the initial pan click interaction sequence. */
    private double lastMouseX;

    /** Absolute structural Y-coordinate of the cursor layout recorded during the initial pan click interaction sequence. */
    private double lastMouseY;

    /** The incident instance selected on the map display pane layout by the user. */
    private VictimIncident selectedIncident;

    /** Shared registry containing specific identifying integer keys of hospital nodes highlighted on the view layout canvas. */
    private final Set<Integer> highlightedHospitalIds = new HashSet<>();

    /** The specific hospital center selected on the map viewport layout. */
    private Hospital selectedHospital;

    /** Structural control flag toggling whether Delaunay network edge wireframes are drawn onto the map workspace canvas. */
    private boolean showTriangles = true;

    /** Structural control flag toggling whether computed geometric Voronoi partition paths are drawn onto the map workspace canvas. */
    private boolean showVoronoi = true;

    /** Structural control flag toggling whether linear closest-destination lines connecting victims to hospitals are drawn. */
    private boolean showAssignments = true;

    /** Primary layout container surface where geometric vector nodes and shapes are dynamically rendered. */
    @FXML
    private Pane mapPane;

    /** The core orchestrator managing business logic states, routing graph computations, and layout coordinate parameters. */
    private MapManager mapManager;

    /** Linked controller handling complementary item information text changes inside the sidebar layout view. */
    private SidebarController sidebarController;

    /** Key identifying index belonging to the emergency hospital instance bound to the highlighted target incident. */
    private Integer selectedAssignedHospitalId;

    /**
     * Post-initialization constructor hook automatically executed by structural FXML loader components.
     * Hooks layout interaction boundaries to track and trigger zoom or displacement operations.
     */
    @FXML
    public void initialize() {
        System.out.println("MapController initialized successfully!");
        setupZoom();
        setupPan();
    }

    /**
     * Inverts the display visibility preference flag of target matching vectors linking incidents
     * to their resolved destinations and updates the viewport.
     */
    public void toggleAssignmentsVisibility() {
        showAssignments = !showAssignments;
        refreshMap();
    }

    /**
     * Links scrolling delta handlers to scale coordinates up or down inside the map workspace container panel bounds.
     * Constrains scale adjustments within structural safety limits ($0.5\times$ to $3.0\times$).
     */
    private void setupZoom() {
        mapPane.setPickOnBounds(true);

        mapPane.setOnScroll((ScrollEvent event) -> {
            double zoomDelta = 1.1;

            if (event.getDeltaY() > 0) {
                zoomFactor *= zoomDelta;
            } else if (event.getDeltaY() < 0) {
                zoomFactor /= zoomDelta;
            } else {
                return;
            }

            zoomFactor = Math.max(0.5, Math.min(zoomFactor, 3.0));

            mapPane.setScaleX(zoomFactor);
            mapPane.setScaleY(zoomFactor);

            event.consume();
        });
    }

    /**
     * Hooks drag action sequences onto primary canvas button clicks to execute translational panning adjustments.
     */
    private void setupPan() {
        mapPane.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                lastMouseX = event.getSceneX();
                lastMouseY = event.getSceneY();
            }
        });

        mapPane.setOnMouseDragged(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                double deltaX = event.getSceneX() - lastMouseX;
                double deltaY = event.getSceneY() - lastMouseY;

                mapPane.setTranslateX(
                        mapPane.getTranslateX() + deltaX
                );

                mapPane.setTranslateY(
                        mapPane.getTranslateY() + deltaY
                );

                lastMouseX = event.getSceneX();
                lastMouseY = event.getSceneY();

                event.consume();
            }
        });
    }

    /**
     * Marks a targeted emergency context node as selected, highlights the assigned destination hospital,
     * and highlights adjacent neighboring cells using topology lookup queries.
     * Reversing selection resets the canvas back to generic configurations.
     *
     * @param incident the target {@link VictimIncident} instance to highlight, or {@code null} to reset tracking state variables
     */
    private void selectIncident(VictimIncident incident) {
        if (selectedIncident != null
                && selectedIncident.getIncidentId().equals(incident.getIncidentId())) {
            selectedIncident = null;
            selectedAssignedHospitalId = null;
            highlightedHospitalIds.clear();

            if (sidebarController != null) {
                sidebarController.clearSelectionDetails();
            }

            refreshMap();
            return;
        }

        selectedIncident = incident;
        selectedHospital = null;
        highlightedHospitalIds.clear();
        selectedAssignedHospitalId = null;

        if (incident != null && incident.getClosestHospital() != null) {
            Hospital assignedHospital = (Hospital) incident.getClosestHospital();
            selectedAssignedHospitalId = assignedHospital.getId();
            highlightedHospitalIds.add(assignedHospital.getId());

            for (Hospital neighbor : mapManager.getVoronoiNeighbors(assignedHospital)) {
                highlightedHospitalIds.add(neighbor.getId());
            }
        }

        refreshMap();
    }

    /**
     * Iterates over incidents to draw allocation links. If an incident is selected,
     * computes and renders the true path along the road network using routing logic;
     * otherwise, falls back to an absolute direct line connection.
     */
    private void drawAssignments() {
        for (VictimIncident incident : mapManager.getIncidents()) {
            if (incident.getClosestHospital() != null) {
                boolean isSelected = selectedIncident != null
                        && selectedIncident.getIncidentId().equals(incident.getIncidentId());

                if (isSelected) {
                    List<Point> path = mapManager.computeRoadForIncident(incident);

                    if (path != null && path.size() > 1) {
                        for (int i = 0; i < path.size() - 1; i++) {
                            Point p1 = path.get(i);
                            Point p2 = path.get(i + 1);

                            Line routeLine = new Line(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                            routeLine.setStroke(Color.MAGENTA);
                            routeLine.setStrokeWidth(4.5);
                            routeLine.setOpacity(0.85);

                            mapPane.getChildren().add(routeLine);
                        }
                    } else {
                        drawStraightAssignmentLine(incident, true);
                    }
                }
                if (showAssignments) {
                    drawStraightAssignmentLine(incident, isSelected);
                }
            }
        }
    }

    /**
     * Renders a dashed straight-line vector overlay between an incident coordinate point
     * and its mapped destination hospital site.
     *
     * @param incident the source tracking element instance
     * @param isSelected configures specific style weights if this link is currently selected
     */
    private void drawStraightAssignmentLine(VictimIncident incident, boolean isSelected) {
        Line assignmentLine = new Line(
                incident.getX(), incident.getY(),
                incident.getClosestHospital().getX(), incident.getClosestHospital().getY()
        );

        assignmentLine.setStroke(isSelected ? Color.RED : Color.INDIANRED);
        assignmentLine.setStrokeWidth(isSelected ? 2.5 : 1.2);
        assignmentLine.setOpacity(isSelected ? 1.0 : 0.7);
        assignmentLine.getStrokeDashArray().addAll(6.0, 4.0);

        mapPane.getChildren().add(assignmentLine);
    }

    /**
     * Injects the application's global core business engine context resource references.
     *
     * @param mapManager the foundational model context container instance
     */
    public void setMapManager(MapManager mapManager) {
        this.mapManager = mapManager;
    }

    /**
     * Hooks complementary UI action layout components tracking sidebar configurations.
     *
     * @param sidebarController the target component layout companion instance
     */
    public void setSidebarController(SidebarController sidebarController) {
        this.sidebarController = sidebarController;
    }

    /**
     * Toggles whether Delaunay Triangulation grid lines are drawn on the panel,
     * then refreshes the canvas view.
     */
    public void toggleTrianglesVisibility() {
        showTriangles = !showTriangles;
        refreshMap();
    }

    /**
     * Toggles whether Voronoi boundary cell shapes are drawn on the panel,
     * then refreshes the canvas view.
     */
    public void toggleVoronoiVisibility() {
        showVoronoi = !showVoronoi;
        refreshMap();
    }

    /**
     * Wipes all child objects drawn inside the primary panel layout area and sequentially
     * triggers layered redraw passes for active data components.
     */
    public void refreshMap() {
        if (mapPane == null || mapManager == null) {
            return;
        }

        mapPane.getChildren().clear();

        drawRoads();

        if (showTriangles) {
            drawTriangles();
        }

        if (showVoronoi) {
            drawVoronoiCells();
            drawVoronoiVertices();
        }

        drawAssignments();
        drawHospitals();
        drawIncidents();
        drawLegend();
    }

    /**
     * Renders an explanatory color-coded map key legend panel onto the viewport area.
     */
    private void drawLegend() {
        double boxX = 20;
        double boxY = 500;
        double boxWidth = 220;
        double boxHeight = 130;

        Group legendGroup = new javafx.scene.Group();

        legendGroup.setMouseTransparent(true);

        Rectangle background = new Rectangle(boxX, boxY, boxWidth, boxHeight);
        background.setFill(Color.WHITE);
        background.setStroke(Color.GRAY);
        background.setArcWidth(10);
        background.setArcHeight(10);
        background.setOpacity(0.9);

        background.setMouseTransparent(true);

        Text title = new Text(boxX + 10, boxY + 20, "Legend");
        title.setFill(Color.BLACK);

        Circle hospitalCircle = new Circle(boxX + 15, boxY + 40, 5);
        hospitalCircle.setFill(Color.DODGERBLUE);
        hospitalCircle.setStroke(Color.BLACK);
        Text hospitalText = new Text(boxX + 30, boxY + 44, "Hospital");

        Circle incidentCircle = new Circle(boxX + 15, boxY + 60, 4);
        incidentCircle.setFill(Color.CRIMSON);
        incidentCircle.setStroke(Color.BLACK);
        Text incidentText = new Text(boxX + 30, boxY + 64, "Incident");

        Line delaunayLine = new Line(boxX + 8, boxY + 80, boxX + 22, boxY + 80);
        delaunayLine.setStroke(Color.GRAY);
        Text delaunayText = new Text(boxX + 30, boxY + 84, "Delaunay triangulation");

        Line assignLine = new Line(boxX + 8, boxY + 100, boxX + 22, boxY + 100);
        assignLine.setStroke(Color.INDIANRED);
        assignLine.setOpacity(0.7);
        assignLine.getStrokeDashArray().addAll(6.0, 4.0);
        Text assignText = new Text(boxX + 30, boxY + 104, "Incident assignment");

        Line roadLine = new Line(boxX + 8, boxY + 120, boxX + 22, boxY + 120);
        roadLine.setStroke(Color.DARKGRAY);
        roadLine.setStrokeWidth(3.0);
        Text roadText = new Text(boxX + 30, boxY + 124, "Road Network (Traffic)");

        legendGroup.getChildren().addAll(
                background,
                title,
                hospitalCircle, hospitalText,
                incidentCircle, incidentText,
                delaunayLine, delaunayText,
                assignLine, assignText,
                roadLine, roadText
        );

        mapPane.getChildren().add(legendGroup);
    }

    /**
     * Attaches mouse drag event filters onto a targeted hospital node layout circle.
     * Upon mouse release, it invokes graph lookups to automatically snap the node to the nearest
     * road infrastructure intersection point.
     *
     * @param hospital the data model reference to update
     * @param circle the visual shape component representing the hospital on the canvas
     * @param label descriptive structural text item grouped alongside the circle component
     */
    private void makeHospitalDraggable(Hospital hospital, Circle circle, Text label) {
        final double[] dragOffset = new double[2];

        circle.setOnMousePressed((MouseEvent event) -> {
            dragOffset[0] = hospital.getX() - event.getX();
            dragOffset[1] = hospital.getY() - event.getY();

            if (sidebarController != null) {
                sidebarController.showHospitalDetails(hospital);
            }

            selectedHospital = hospital;
            selectedIncident = null;
            selectedAssignedHospitalId = null;
            highlightedHospitalIds.clear();

            event.consume();
        });

        circle.setOnMouseDragged((MouseEvent event) -> {
            double newX = event.getX() + dragOffset[0];
            double newY = event.getY() + dragOffset[1];

            circle.setCenterX(newX);
            circle.setCenterY(newY);
            label.setX(newX + 8);
            label.setY(newY - 8);

            event.consume();
        });

        circle.setOnMouseReleased((MouseEvent event) -> {
            double finalX = circle.getCenterX();
            double finalY = circle.getCenterY();

            // --- Snap Logic via Closest Road Intersection Query ---
            Point snappedPoint = null;
            if (!mapManager.getRoadNetwork().getRoads().isEmpty()) {
                snappedPoint = mapManager.getRoadNetwork().findNearestIntersection(new Point(finalX, finalY));
            }

            if (snappedPoint != null) {
                hospital.setX(snappedPoint.getX());
                hospital.setY(snappedPoint.getY());

                circle.setCenterX(snappedPoint.getX());
                circle.setCenterY(snappedPoint.getY());
                label.setX(snappedPoint.getX() + 8);
                label.setY(snappedPoint.getY() - 8);
            } else {
                hospital.setX(finalX);
                hospital.setY(finalY);
            }

            mapManager.updateAll();
            refreshMap();

            event.consume();
        });
    }

    /**
     * Loops through available site instances to generate, style, and inject
     * node nodes representing active hospital hubs.
     */
    private void drawHospitals() {
        for (Hospital hospital : mapManager.getSites()) {

            boolean isHighlighted = highlightedHospitalIds.contains(hospital.getId());
            boolean isAssigned = selectedAssignedHospitalId != null
                    && selectedAssignedHospitalId.equals(hospital.getId());

            Circle hospitalCircle = new Circle(
                    hospital.getX(),
                    hospital.getY(),
                    isAssigned ? 9 : (isHighlighted ? 8 : 6)
            );

            if (isAssigned) {
                hospitalCircle.setFill(Color.ORANGE);
            } else if (isHighlighted) {
                hospitalCircle.setFill(Color.GOLD);
            } else {
                hospitalCircle.setFill(Color.DODGERBLUE);
            }

            hospitalCircle.setStroke(Color.BLACK);

            String labelText = "H" + hospital.getId();
            Text label = new Text(hospital.getX() + 8, hospital.getY() - 8, labelText);
            label.setFill(Color.BLACK);

            makeHospitalDraggable(hospital, hospitalCircle, label);

            hospitalCircle.setOnMouseEntered(event -> {
                if (!isAssigned && !isHighlighted) {
                    hospitalCircle.setRadius(8);
                    hospitalCircle.setFill(Color.DEEPSKYBLUE);
                }

                if (sidebarController != null) {
                    sidebarController.showHospitalDetails(hospital);
                }

                event.consume();
            });

            hospitalCircle.setOnMouseExited(event -> {
                if (!isAssigned && !isHighlighted) {
                    hospitalCircle.setRadius(6);
                    hospitalCircle.setFill(Color.DODGERBLUE);
                }
                event.consume();
            });

            mapPane.getChildren().addAll(hospitalCircle, label);
        }
    }

    /**
     * Attaches drag filtering actions onto an incident node shape.
     * If dragged, it snaps to the closest road network layout intersection; if clicked without movement,
     * it toggles the incident selection context.
     *
     * @param incident the data model item tracking entity reference
     * @param circle the vector circle layout shape representing the target incident
     * @param label label text displayed adjacent to the incident circle
     */
    private void makeIncidentDraggable(VictimIncident incident, Circle circle, Text label) {
        final double[] dragOffset = new double[2];
        final boolean[] wasDragged = {false};

        circle.setOnMousePressed((MouseEvent event) -> {
            dragOffset[0] = incident.getX() - event.getX();
            dragOffset[1] = incident.getY() - event.getY();
            wasDragged[0] = false;

            if (sidebarController != null) {
                sidebarController.showIncidentDetails(incident);
            }

            event.consume();
        });

        circle.setOnMouseDragged((MouseEvent event) -> {
            wasDragged[0] = true;

            double newX = event.getX() + dragOffset[0];
            double newY = event.getY() + dragOffset[1];

            circle.setCenterX(newX);
            circle.setCenterY(newY);
            label.setX(newX + 8);
            label.setY(newY - 8);

            event.consume();
        });

        circle.setOnMouseReleased((MouseEvent event) -> {
            if (wasDragged[0]) {
                double finalX = circle.getCenterX();
                double finalY = circle.getCenterY();

                Point snappedPoint = null;
                if (!mapManager.getRoadNetwork().getRoads().isEmpty()) {
                    snappedPoint = mapManager.getRoadNetwork().findNearestIntersection(new Point(finalX, finalY));
                }

                if (snappedPoint != null) {
                    incident.setX(snappedPoint.getX());
                    incident.setY(snappedPoint.getY());
                } else {
                    incident.setX(finalX);
                    incident.setY(finalY);
                }

                incident.setClosestHospital(null);

                mapManager.updateAll();
                refreshMap();
            } else {
                selectIncident(incident);
            }

            event.consume();
        });
    }

    /**
     * Iterates over incidents to draw and bind hover listeners onto tracking circles.
     */
    private void drawIncidents() {
        for (VictimIncident incident : mapManager.getIncidents()) {
            boolean isSelected = selectedIncident != null
                    && selectedIncident.getIncidentId().equals(incident.getIncidentId());

            Circle incidentCircle = new Circle(
                    incident.getX(),
                    incident.getY(),
                    isSelected ? 6 : 4
            );
            incidentCircle.setFill(isSelected ? Color.DARKRED : Color.CRIMSON);
            incidentCircle.setStroke(Color.BLACK);

            Text label = new Text(
                    incident.getX() + 8,
                    incident.getY() - 8,
                    incident.getIncidentId()
            );
            label.setFill(Color.BLACK);

            makeIncidentDraggable(incident, incidentCircle, label);

            incidentCircle.setOnMouseEntered(event -> {
                if (!isSelected) {
                    incidentCircle.setRadius(6);
                    incidentCircle.setFill(Color.ORANGERED);
                }

                if (sidebarController != null) {
                    sidebarController.showIncidentDetails(incident);
                }

                event.consume();
            });

            incidentCircle.setOnMouseExited(event -> {
                if (!isSelected) {
                    incidentCircle.setRadius(4);
                    incidentCircle.setFill(Color.CRIMSON);
                }
                event.consume();
            });

            mapPane.getChildren().addAll(incidentCircle, label);
        }
    }

    /**
     * Draws the geometric polygons representing the Voronoi cells on the map pane.
     * <p>
     * This method retrieves all computed Voronoi cells and clips them against a dynamic
     * bounding box using JavaFX {@link Shape#intersect(Shape, Shape)}.
     * This layout operation constrains unbounded, far-away outer cell vertices to the map's visible
     * area, ensuring a clean, professional map border alignment without overlapping sidebar controls.
     * </p>
     */
    private void drawVoronoiCells() {
        List<VoronoiCell> cells = mapManager.getVoronoiCells();

        double minX = 0, minY = 0, maxX = 750, maxY = 700;
        if (mapManager.getSites() != null && !mapManager.getSites().isEmpty()) {
            minX = mapManager.getSites().stream().mapToDouble(Point::getX).min().orElse(0);
            minY = mapManager.getSites().stream().mapToDouble(Point::getY).min().orElse(0);
            maxX = mapManager.getSites().stream().mapToDouble(Point::getX).max().orElse(750);
            maxY = mapManager.getSites().stream().mapToDouble(Point::getY).max().orElse(700);
        }

        double margin = 3000;
        Rectangle boundingBox = new Rectangle(
                minX - margin,
                minY - margin,
                (maxX - minX) + margin * 2,
                (maxY - minY) + margin * 2
        );

        for (VoronoiCell cell : cells) {
            List<Point> vertices = cell.getVertices();

            if (vertices.size() < 3) continue;

            double[] coords = new double[vertices.size() * 2];
            for (int i = 0; i < vertices.size(); i++) {
                coords[i * 2]     = vertices.get(i).getX();
                coords[i * 2 + 1] = vertices.get(i).getY();
            }

            Polygon polygon = new Polygon(coords);

            Shape clippedCell = Shape.intersect(polygon, boundingBox);

            clippedCell.setFill(Color.rgb(147, 112, 219, 0.05));
            clippedCell.setStroke(Color.MEDIUMPURPLE);
            clippedCell.setStrokeWidth(1.5);
            clippedCell.setOpacity(0.8);

            clippedCell.setMouseTransparent(true);

            mapPane.getChildren().add(clippedCell);
        }
    }

    /**
     * Renders small vertex indicators representing the intersection points of Voronoi cell shapes.
     * Clicking a vertex matches it to its corresponding Delaunay triangle circumcenter to update
     * structural stats on the sidebar UI.
     */
    private void drawVoronoiVertices() {
        List<VoronoiCell> cells = mapManager.getVoronoiCells();
        Set<Point> drawnVertices = new HashSet<>();

        for (VoronoiCell cell : cells) {
            for (Point vertex : cell.getVertices()) {

                if (vertex.getX() < 0 || vertex.getX() > 750 || vertex.getY() < 0 || vertex.getY() > 700) continue;
                if (!drawnVertices.add(vertex)) {
                    continue;
                }

                Circle vertexCircle = new Circle(vertex.getX(), vertex.getY(), 3);
                vertexCircle.setFill(Color.MEDIUMPURPLE);
                vertexCircle.setStroke(Color.INDIGO);
                vertexCircle.setStrokeWidth(0.8);
                vertexCircle.setOpacity(0.85);

                vertexCircle.setOnMouseClicked(event -> {
                    if (sidebarController != null && mapManager.getTriangles() != null) {
                        for (Triangle triangle : mapManager.getTriangles()) {
                            if (triangle.getCircumcenter() != null &&
                                    Math.abs(triangle.getCircumcenter().getX() - vertex.getX()) < 0.5 &&
                                    Math.abs(triangle.getCircumcenter().getY() - vertex.getY()) < 0.5) {

                                sidebarController.showTriangleDetails(triangle);
                                clearSelection();
                                refreshMap();
                                break;
                            }
                        }
                    }
                    event.consume();
                });

                mapPane.getChildren().add(vertexCircle);
            }
        }
    }

    /**
     * Renders the triangulation meshes linking hospital nodes into adjacent planar Delaunay triangles.
     * <p>
     * This method constructs an interactive JavaFX {@link Polygon} for each geometric triangle mesh element.
     * By applying a near-transparent background fill, it ensures that the entire interior surface area
     * of the triangle intercepts mouse click sequences, providing a smooth user selection experience.
     * </p>
     */
    private void drawTriangles() {
        for (Triangle triangle : mapManager.getTriangles()) {
            Point a = triangle.getA();
            Point b = triangle.getB();
            Point c = triangle.getC();

            // 1. Create a JavaFX Polygon representing the geometric triangle
            Polygon fxTriangle = new Polygon();
            fxTriangle.getPoints().addAll(
                    a.getX(), a.getY(),
                    b.getX(), b.getY(),
                    c.getX(), c.getY()
            );

            // 2. Configure visual styling with a standard border stroke
            fxTriangle.setStroke(Color.GRAY);
            fxTriangle.setStrokeWidth(1.0);

            // 3. Apply a near-transparent background fill (2% opacity)
            // This remains invisible to the eye but enables the cursor to capture clicks anywhere inside
            fxTriangle.setFill(Color.rgb(0, 150, 255, 0.02));

            // 4. Attach mouse click event handlers to trigger sidebar updates
            fxTriangle.setOnMouseClicked(event -> {
                if (sidebarController != null) {
                    sidebarController.showTriangleDetails(triangle);
                }
                clearSelection();

                // Optional: Provide a slight visual highlight upon selection
                fxTriangle.setFill(Color.rgb(0, 150, 255, 0.2));

                event.consume(); // Prevent the click event from bubbling up to the map background
            });

            // 5. Inject the rendered shape vector into the main canvas layout layer
            mapPane.getChildren().add(fxTriangle);
        }
    }

    /**
     * Renders the road grid paths across active coordinates.
     * The method applies color-coding variations based on the current edge's traffic factor coefficient:
     * <ul>
     * <li>$\text{Traffic} \ge 2.0$: Dark Red (Severe traffic jam)</li>
     * <li>$1.2 < \text{Traffic} < 2.0$: Dark Orange (Heavy traffic)</li>
     * <li>$\text{Traffic} \le 1.2$: Dark Gray (Fluid traffic flow)</li>
     * </ul>
     */
    private void drawRoads() {
        if (mapManager == null || mapManager.getRoadNetwork() == null) {
            return;
        }

        for (RoadEdge edge : mapManager.getRoadNetwork().getRoads()) {
            Line roadLine = new Line(
                    edge.getStart().getX(), edge.getStart().getY(),
                    edge.getEnd().getX(), edge.getEnd().getY()
            );

            roadLine.setStrokeWidth(3.5);
            roadLine.setOpacity(0.8);

            double traffic = edge.getTrafficFactor();
            if (traffic >= 2.0) {
                roadLine.setStroke(Color.FIREBRICK);
            } else if (traffic > 1.2) {
                roadLine.setStroke(Color.DARKORANGE);
            } else {
                roadLine.setStroke(Color.DARKGRAY);
            }

            roadLine.setOnMouseClicked(event -> {
                System.out.println("Selected road edge context: " + edge);
                event.consume();
            });

            mapPane.getChildren().add(roadLine);
        }
    }

    /**
     * Deletes the active selected element (either a Hospital node or VictimIncident instance)
     * from both the model data layer and map display layout before forcing a viewport refresh.
     */
    public void deleteSelectedElement() {
        if (mapManager == null) {
            return;
        }

        if (selectedHospital != null) {
            mapManager.removeHospital(selectedHospital);
        } else if (selectedIncident != null) {
            mapManager.removeIncident(selectedIncident);
        } else {
            return;
        }

        clearSelection();

        if (sidebarController != null) {
            sidebarController.clearSelectionDetails();
        }

        refreshMap();
    }

    /**
     * Resets active selection states and clear highlight collections tracking entities.
     */
    public void clearSelection() {
        selectedIncident = null;
        selectedHospital = null;
        selectedAssignedHospitalId = null;
        highlightedHospitalIds.clear();
    }

    /**
     * Wipes all nodes and geometric shapes from the parent display container pane.
     */
    public void clearMap() {
        mapPane.getChildren().clear();
    }
}