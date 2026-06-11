package pgl.app.controller;

import javafx.fxml.FXML;
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


public class MapController {
	
	private double zoomFactor = 1.0;
	
	private double lastMouseX;
	
	private double lastMouseY;
	
	private VictimIncident selectedIncident;
	
	private final Set<Integer> highlightedHospitalIds = new HashSet<>();
	
	private final Set<String> highlightedIncidentIds = new HashSet<>();
	
	private Hospital selectedHospital;
	
	private Triangle selectedTriangle;
	
    @FXML
    private Pane mapPane;

    private MapManager mapManager;
    
    private SidebarController sidebarController;
    
    private Integer selectedAssignedHospitalId;

    /**
     * Méthode appelée automatiquement par JavaFX après le chargement du fichier FXML.
     */
    @FXML
    public void initialize() {
        System.out.println("MapController initialisé avec succès !");
        setupZoom();
        setupPan();

    }
    
    private void setupZoom() {
        mapPane.setOnScroll((ScrollEvent event) -> {
            double zoomDelta = 1.1;

            if (event.getDeltaY() > 0) {
                zoomFactor *= zoomDelta;
            } else {
                zoomFactor /= zoomDelta;
            }

            zoomFactor = Math.max(0.5, Math.min(zoomFactor, 3.0));

            mapPane.setScaleX(zoomFactor);
            mapPane.setScaleY(zoomFactor);

            event.consume();
        });
    }
    
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

                mapPane.setTranslateX(mapPane.getTranslateX() + deltaX);
                mapPane.setTranslateY(mapPane.getTranslateY() + deltaY);

                lastMouseX = event.getSceneX();
                lastMouseY = event.getSceneY();

                event.consume();
            }
        });
    }
    
    public void selectTriangle(Triangle triangle) {
        selectedTriangle = triangle;
        selectedHospital = null;
        selectedIncident = null;
        selectedAssignedHospitalId = null;
        highlightedHospitalIds.clear();
        highlightedIncidentIds.clear();
        refreshMap();
    }
    
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
        selectedTriangle = null;
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
    
    private void drawAssignments() {
        for (VictimIncident incident : mapManager.getIncidents()) {
            if (incident.getClosestHospital() != null) {
                boolean isSelected = selectedIncident != null
                        && selectedIncident.getIncidentId().equals(incident.getIncidentId());

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
        }
    }
    
    private void drawLegend() {
        double boxX = 20;
        double boxY = 520;
        double boxWidth = 220;
        double boxHeight = 110;

        javafx.scene.shape.Rectangle background = new javafx.scene.shape.Rectangle(boxX, boxY, boxWidth, boxHeight);
        background.setFill(Color.WHITE);
        background.setStroke(Color.GRAY);
        background.setArcWidth(10);
        background.setArcHeight(10);
        background.setOpacity(0.9);

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

        mapPane.getChildren().addAll(
                background,
                title,
                hospitalCircle, hospitalText,
                incidentCircle, incidentText,
                delaunayLine, delaunayText,
                assignLine, assignText
        );
    }

    public void setMapManager(MapManager mapManager) {
        this.mapManager = mapManager;
    }
    
    public void setSidebarController(SidebarController sidebarController) {
    	this.sidebarController = sidebarController;
    }

    /**
     * Redessine entièrement la carte à partir des données du MapManager.
     */
    public void refreshMap() {
        if (mapPane == null || mapManager == null) {
            return;
        }

        mapPane.getChildren().clear();
        drawTriangles();
        drawVoronoiCells();
        drawVoronoiVertices();
        drawAssignments();
        drawHospitals();
        drawIncidents();
        drawLegend();
    }
    
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
            selectedTriangle = null;
            selectedAssignedHospitalId = null;
            highlightedIncidentIds.clear();
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

            hospital.setX(finalX);
            hospital.setY(finalY);

            mapManager.updateAll();
            refreshMap();

            event.consume();
        });
    }

    /**
     * Ajoute visuellement les hôpitaux sur la carte.
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

                incident.setX(finalX);
                incident.setY(finalY);

                mapManager.updateAll();
                refreshMap();
            } else {
                selectIncident(incident);
            }

            event.consume();
        });
    }

    /**
     * Ajoute visuellement les incidents sur la carte.
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
    /** dessine les polygones des cellules de voronoi **/
    private void drawVoronoiCells() {
        List<VoronoiCell> cells = mapManager.getVoronoiCells();

        for (VoronoiCell cell : cells) {
            List<Point> vertices = cell.getVertices();

            // Skip cell if any vertex is outside the map bounds
            boolean allInBounds = vertices.stream()
                    .allMatch(p -> p.getX() >= 0 && p.getX() <= 750
                            && p.getY() >= 0 && p.getY() <= 700);
            if (!allInBounds) continue;

            double[] coords = new double[vertices.size() * 2];
            for (int i = 0; i < vertices.size(); i++) {
                coords[i * 2]     = vertices.get(i).getX();
                coords[i * 2 + 1] = vertices.get(i).getY();
            }

            Polygon polygon = new Polygon(coords);
            polygon.setFill(Color.TRANSPARENT);
            polygon.setStroke(Color.MEDIUMPURPLE);
            polygon.setStrokeWidth(1.0);
            polygon.setOpacity(0.6);

            mapPane.getChildren().add(polygon);
        }
    }
    private void drawVoronoiVertices() {
        List<VoronoiCell> cells = mapManager.getVoronoiCells();
        Set<Point> drawnVertices = new HashSet<>();

        for (VoronoiCell cell : cells) {
            for (Point vertex : cell.getVertices()) {




                    if (vertex.getX() < 0 || vertex.getX() > 750 || vertex.getY() < 0 || vertex.getY() > 700) continue ;
                if (!drawnVertices.add(vertex)) {
                    continue;
                }

                Circle vertexCircle = new Circle(vertex.getX(), vertex.getY(), 3);
                vertexCircle.setFill(Color.MEDIUMPURPLE);
                vertexCircle.setStroke(Color.INDIGO);
                vertexCircle.setStrokeWidth(0.8);
                vertexCircle.setOpacity(0.85);

                mapPane.getChildren().add(vertexCircle);
            }
        }
    }
    /**
     * Dessine la triangulation de Delaunay.
     */
    private void drawTriangles() {
        for (Triangle triangle : mapManager.getTriangles()) {
            Point a = triangle.getA();
            Point b = triangle.getB();
            Point c = triangle.getC();

            Line ab = new Line(a.getX(), a.getY(), b.getX(), b.getY());
            Line bc = new Line(b.getX(), b.getY(), c.getX(), c.getY());
            Line ca = new Line(c.getX(), c.getY(), a.getX(), a.getY());
            
            ab.setOnMouseClicked(event -> {
                if (sidebarController != null) {
                    sidebarController.showTriangleDetails(triangle);
                }
                selectTriangle(triangle);
                event.consume();
            });

            bc.setOnMouseClicked(event -> {
                if (sidebarController != null) {
                    sidebarController.showTriangleDetails(triangle);
                }
                selectTriangle(triangle);
                event.consume();
            });

            ca.setOnMouseClicked(event -> {
                if (sidebarController != null) {
                    sidebarController.showTriangleDetails(triangle);
                }
                selectTriangle(triangle);
                event.consume();
            });

            ab.setStroke(Color.GRAY);
            bc.setStroke(Color.GRAY);
            ca.setStroke(Color.GRAY);

            mapPane.getChildren().addAll(ab, bc, ca);
        }
    }
    
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
    
    public void clearSelection() {
    	selectedIncident = null;
    	selectedHospital = null;
    	selectedTriangle = null;
    	selectedAssignedHospitalId = null;
    	highlightedHospitalIds.clear();
    	highlightedIncidentIds.clear();
    }

    /**
     * Clears all visual elements from the map.
     */
    public void clearMap() {
        mapPane.getChildren().clear();
    }
}