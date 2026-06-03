package pgl.app.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import pgl.app.model.Hospital;
import pgl.app.model.MapManager;
import pgl.app.model.Point;
import pgl.app.model.Triangle;
import pgl.app.model.VictimIncident;

public class MapController {

    @FXML
    private Pane mapPane;

    private MapManager mapManager;

    /**
     * Méthode appelée automatiquement par JavaFX après le chargement du fichier FXML.
     */
    @FXML
    public void initialize() {
        System.out.println("MapController initialisé avec succès !");
    }

    public void setMapManager(MapManager mapManager) {
        this.mapManager = mapManager;
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
        drawHospitals();
        drawIncidents();
    }

    /**
     * Ajoute visuellement un hôpital sur la carte.
     */
    private void drawHospitals() {
        for (Point point : mapManager.getSites()) {
            Circle hospitalCircle = new Circle(point.getX(), point.getY(), 6);
            hospitalCircle.setFill(Color.DODGERBLUE);
            hospitalCircle.setStroke(Color.BLACK);
            mapPane.getChildren().add(hospitalCircle);
        }
    }

    /**
     * Ajoute visuellement les incidents sur la carte.
     */
    private void drawIncidents() {
        for (VictimIncident incident : mapManager.getIncidents()) {
            Circle incidentCircle = new Circle(incident.getX(), incident.getY(), 4);
            incidentCircle.setFill(Color.CRIMSON);
            incidentCircle.setStroke(Color.BLACK);
            mapPane.getChildren().add(incidentCircle);
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

            ab.setStroke(Color.GRAY);
            bc.setStroke(Color.GRAY);
            ca.setStroke(Color.GRAY);

            mapPane.getChildren().addAll(ab, bc, ca);
        }
    }

    /**
     * Clears all visual elements from the map.
     */
    public void clearMap() {
        mapPane.getChildren().clear();
    }
}