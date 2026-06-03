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
import pgl.app.model.Site;

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
    
    private void drawAssignments() {
        for (VictimIncident incident : mapManager.getIncidents()) {
            if (incident.getClosestSite() != null) {
                Line assignmentLine = new Line(
                        incident.getX(), incident.getY(),
                        incident.getClosestSite().getX(), incident.getClosestSite().getY()
                );

                assignmentLine.setStroke(Color.INDIANRED);
                assignmentLine.setOpacity(0.7);
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

    /**
     * Redessine entièrement la carte à partir des données du MapManager.
     */
    public void refreshMap() {
        if (mapPane == null || mapManager == null) {
            return;
        }

        mapPane.getChildren().clear();
        drawTriangles();
        drawAssignments();
        drawHospitals();
        drawIncidents();
        drawLegend();
    }

    /**
     * Ajoute visuellement les hôpitaux sur la carte.
     */
    private void drawHospitals() {
        for (Site site : mapManager.getSites()) {
            Circle hospitalCircle = new Circle(site.getX(), site.getY(), 6);
            hospitalCircle.setFill(Color.DODGERBLUE);
            hospitalCircle.setStroke(Color.BLACK);

            String labelText = "H" + ((Hospital) site).getId();
            Text label = new Text(site.getX() + 8, site.getY() - 8, labelText);
            label.setFill(Color.BLACK);

            mapPane.getChildren().addAll(hospitalCircle, label);
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

            Text label = new Text(
                    incident.getX() + 8,
                    incident.getY() - 8,
                    incident.getIncidentId()
            );
            label.setFill(Color.BLACK);

            mapPane.getChildren().addAll(incidentCircle, label);
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