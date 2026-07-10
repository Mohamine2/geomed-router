package geomed.app.algo;

import geomed.app.model.Point;
import java.util.List;

/**
 * Encapsulates the result of a routing operation, combining the geometric path
 * and its pre-calculated total operational cost.
 *
 * @param path      the ordered sequence of coordinates representing the computed route
 * @param totalCost the cumulative distance weight or operational cost of the path
 */
public record RoutingResult(List<Point> path, double totalCost) {}