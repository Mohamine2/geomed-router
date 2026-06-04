package pgl.app.algo;

import pgl.app.model.Point;
import java.util.List;

/**
 * Encapsulates the result of a routing operation, combining the geometric path
 * and its pre-calculated total operational cost.
 */
public record RoutingResult(List<Point> path, double totalCost) {}