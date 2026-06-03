package pgl.app.controller;


import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class MapController {

    @FXML

    private Canvas  canvas;

    private GraphicsContext gc;


    public void initialize() {
        gc = canvas.getGraphicsContext2D();
        drawDemo();
    }

    // Dessine quelques points de démonstration
    private void drawDemo() {
        gc.setFill(Color.LIGHTBLUE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Quelques sites de démo
        drawSite(100, 150, "S1");
        drawSite(300, 80, "S2");
        drawSite(500, 200, "S3");
        drawSite(250, 350, "S4");

        // Un user point de démo
        drawUserPoint(400, 300);
    }

    public void drawSite(double x, double y, String label) {
        gc.setFill(Color.DARKBLUE);
        gc.fillOval(x - 6, y - 6, 12, 12);
        gc.setFill(Color.BLACK);
        gc.fillText(label, x + 8, y - 4);
    }

    public void drawUserPoint(double x, double y) {
        gc.setFill(Color.RED);
        gc.fillOval(x - 5, y - 5, 10, 10);
        gc.setFill(Color.BLACK);
        gc.fillText("U", x + 7, y - 3);
    }

    public void clearMap() {
        gc.setFill(Color.LIGHTBLUE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    public void addPointOnMap(double x, double y) {
        drawUserPoint(x, y);
    }
}
