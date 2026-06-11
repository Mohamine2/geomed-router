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
import javafx.scene.shape.Shape;
import javafx.scene.shape.Rectangle;
import pgl.app.model.RoadEdge;


public class MapController {
	
	private double zoomFactor = 1.0;
	
	private double lastMouseX;
	
	private double lastMouseY;
	
	private VictimIncident selectedIncident;
	
	private final Set<Integer> highlightedHospitalIds = new HashSet<>();
	
	private final Set<String> highlightedIncidentIds = new HashSet<>();
	
	private Hospital selectedHospital;
	
	private Triangle selectedTriangle;

    private boolean showTriangles = true;
    private boolean showVoronoi = true;
	
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
        mapPane.setPickOnBounds(true);

        mapPane.setOnScroll((ScrollEvent event) -> {
            double zoomDelta = 1.1;

            // Correction : On cible explicitement les scrolls positifs et négatifs
            if (event.getDeltaY() > 0) {
                zoomFactor *= zoomDelta;
            } else if (event.getDeltaY() < 0) {
                zoomFactor /= zoomDelta;
            } else {
                // Si le deltaY est 0 (ex: inertie trackpad), on ne fait rien
                return;
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

    public void setMapManager(MapManager mapManager) {
        this.mapManager = mapManager;
    }
    
    public void setSidebarController(SidebarController sidebarController) {
    	this.sidebarController = sidebarController;
    }

    public void toggleTrianglesVisibility() {
        showTriangles = !showTriangles;
        refreshMap();
    }

    public void toggleVoronoiVisibility() {
        showVoronoi = !showVoronoi;
        refreshMap();
    }

    /**
     * Redessine entièrement la carte à partir des données du MapManager.
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
        }

        drawAssignments();
        drawHospitals();
        drawIncidents();
        drawLegend();
    }

    private void drawLegend() {
        double boxX = 20;
        double boxY = 500;
        double boxWidth = 220;
        double boxHeight = 130;

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

        // <-- NOUVELLE LIGNE POUR LA LÉGENDE DES ROUTES
        Line roadLine = new Line(boxX + 8, boxY + 120, boxX + 22, boxY + 120);
        roadLine.setStroke(Color.DARKGRAY);
        roadLine.setStrokeWidth(3.0);
        Text roadText = new Text(boxX + 30, boxY + 124, "Road Network (Traffic)");

        mapPane.getChildren().addAll(
                background,
                title,
                hospitalCircle, hospitalText,
                incidentCircle, incidentText,
                delaunayLine, delaunayText,
                assignLine, assignText,
                roadLine, roadText
        );
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

            // --- NOUVEAU : Logique de Snap pour l'Hôpital ---
            Point snappedPoint = null;
            if (!mapManager.getRoadNetwork().getRoads().isEmpty()) {
                snappedPoint = mapManager.getRoadNetwork().findNearestIntersection(new Point(finalX, finalY));
            }

            if (snappedPoint != null) {
                // Application au Modèle
                hospital.setX(snappedPoint.getX());
                hospital.setY(snappedPoint.getY());

                // Alignement direct de la Vue pour éviter un saut visuel
                circle.setCenterX(snappedPoint.getX());
                circle.setCenterY(snappedPoint.getY());
                label.setX(snappedPoint.getX() + 8);
                label.setY(snappedPoint.getY() - 8);
            } else {
                // Cas par défaut si aucune route n'existe
                hospital.setX(finalX);
                hospital.setY(finalY);
            }

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

    /**
     * Draws the geometric polygons representing the Voronoi cells on the map pane.
     * <p>
     * This method retrieves all computed Voronoi cells and clips them against a dynamic
     * bounding box using JavaFX {@link Shape#intersect(Shape, Shape)}.
     * This layout operation constrains unbounded, far-away outer cell vertices to the map's visible
     * area, ensuring a clean, professional map border alignment without overlapping the sidebar controls.
     * </p>
     */
    private void drawVoronoiCells() {
        List<VoronoiCell> cells = mapManager.getVoronoiCells();

        // 1. Calculer dynamiquement les limites de la carte
        double minX = 0, minY = 0, maxX = 750, maxY = 700;
        if (mapManager.getSites() != null && !mapManager.getSites().isEmpty()) {
            minX = mapManager.getSites().stream().mapToDouble(h -> h.getX()).min().orElse(0);
            minY = mapManager.getSites().stream().mapToDouble(h -> h.getY()).min().orElse(0);
            maxX = mapManager.getSites().stream().mapToDouble(h -> h.getX()).max().orElse(750);
            maxY = mapManager.getSites().stream().mapToDouble(h -> h.getY()).max().orElse(700);
        }

        // 2. Définir une très grande marge pour que la découpe soit repoussée loin hors champ
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

            // 3. Geometric intersection avec la boîte dynamique
            Shape clippedCell = Shape.intersect(polygon, boundingBox);

            // 4. Apply styles and visual properties on the clipped shape
            clippedCell.setFill(Color.rgb(147, 112, 219, 0.05));
            clippedCell.setStroke(Color.MEDIUMPURPLE);
            clippedCell.setStrokeWidth(1.5);
            clippedCell.setOpacity(0.8);

            mapPane.getChildren().add(clippedCell);
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

    /**
     * Dessine les routes du réseau (RoadNetwork).
     * La couleur de la route change en fonction de son facteur de trafic (Traffic Factor).
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

            // Épaisseur de la route
            roadLine.setStrokeWidth(3.5);
            roadLine.setOpacity(0.8);

            // Code couleur dynamique basé sur le trafic
            double traffic = edge.getTrafficFactor();
            if (traffic >= 2.0) {
                roadLine.setStroke(Color.FIREBRICK); // Rouge sombre (Bouchon sévère)
            } else if (traffic > 1.2) {
                roadLine.setStroke(Color.DARKORANGE); // Orange (Trafic ralenti)
            } else {
                roadLine.setStroke(Color.DARKGRAY); // Gris (Route fluide)
            }

            // On ajoute un événement au clic (optionnel) pour voir les détails de la route dans le futur
            roadLine.setOnMouseClicked(event -> {
                System.out.println("Route sélectionnée : " + edge.toString());
                event.consume();
            });

            mapPane.getChildren().add(roadLine);
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