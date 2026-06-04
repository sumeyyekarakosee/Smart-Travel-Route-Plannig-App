package ui.view;

import graph.Graph;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.util.Duration;
import model.Location;
import model.RouteStep;
import model.TripPlan;
import model.WhatIfComparison;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Harita alanini locations.txt icindeki koordinatlari kullanarak cizer.
 * Noktalar Istanbul yerlesimine gore panoya dagitilir ve rota adimlari cizgiyle baglanir.
 */
public class MapRenderer {

    private static final Color START_COLOR = Color.web("#1e88e5");
    private static final Color SELECTED_COLOR = Color.web("#43a047");
    private static final Color NORMAL_COLOR = Color.web("#ffffff");
    private static final Color OUTLINE_COLOR = Color.web("#94a3b8");
    private static final Color LABEL_COLOR = Color.web("#0f172a");
    private static final Color OLD_ROUTE_COLOR = Color.web("#e53935");

    public void render(Pane mapPane,
                       Graph graph,
                       TripPlan plan,
                       WhatIfComparison comparison,
                       Set<Integer> selectedIds,
                       Integer startId) {

        if (mapPane == null || graph == null) {
            return;
        }

        mapPane.getChildren().clear();

        double width = resolveSize(mapPane.getWidth(), mapPane.getPrefWidth(), 900);
        double height = resolveSize(mapPane.getHeight(), mapPane.getPrefHeight(), 700);

        Group content = new Group();
        mapPane.getChildren().add(content);

        List<Location> locations = new ArrayList<>(graph.getAllLocations());
        locations.sort(Comparator.comparingInt(Location::getId));

        if (locations.isEmpty()) {
            return;
        }

        MapBounds bounds = MapBounds.from(locations);
        List<RenderItem> items = new ArrayList<>();
        for (Location location : locations) {
            items.add(new RenderItem(location, scale(location, bounds, width, height)));
        }

        TripPlan activePlan = plan != null ? plan : (comparison != null ? comparison.getNewPlan() : null);
        Set<Integer> routeIds = collectSelectedRouteIds(activePlan);

        if (comparison != null && comparison.getOldPlan() != null) {
            drawPlanLines(content, comparison.getOldPlan(), bounds, width, height, OLD_ROUTE_COLOR, true, 0.70);
        }

        if (plan != null) {
            drawPlanLines(content, plan, bounds, width, height, SELECTED_COLOR, false, 0.92);
        } else if (comparison != null && comparison.getNewPlan() != null) {
            drawPlanLines(content, comparison.getNewPlan(), bounds, width, height, SELECTED_COLOR, false, 0.92);
        }

        List<Rectangle2D> occupiedLabels = new ArrayList<>();
        for (RenderItem item : items) {
            boolean isRouteNode = routeIds.contains(item.location.getId());
            boolean isStartNode = startId != null && item.location.getId() == startId;
            boolean isSelectedNode = selectedIds != null && selectedIds.contains(item.location.getId());

            Circle circle = new Circle(item.point.getX(), item.point.getY(), 12);
            circle.setStroke(OUTLINE_COLOR);
            circle.setStrokeWidth(2);
            circle.setFill(determineFill(isStartNode, isRouteNode, isSelectedNode));

            Tooltip.install(circle, new Tooltip(buildTooltipText(item.location)));

            circle.setOnMouseEntered(event -> {
                circle.setStroke(Color.web("#1565c0"));
                circle.setStrokeWidth(3);
                ScaleTransition transition = new ScaleTransition(Duration.millis(120), circle);
                transition.setToX(1.15);
                transition.setToY(1.15);
                transition.play();
            });

            circle.setOnMouseExited(event -> {
                circle.setStroke(OUTLINE_COLOR);
                circle.setStrokeWidth(2);
                ScaleTransition transition = new ScaleTransition(Duration.millis(120), circle);
                transition.setToX(1.0);
                transition.setToY(1.0);
                transition.play();
            });

            Label label = createLabel(item.location.getName());
            LabelPlacement placement = placeLabel(item, label, width, height, occupiedLabels);
            label.setLayoutX(placement.x);
            label.setLayoutY(placement.y);
            occupiedLabels.add(placement.bounds);

            content.getChildren().addAll(circle, label);
        }

        content.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(220), content);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void drawPlanLines(Group content,
                               TripPlan plan,
                               MapBounds bounds,
                               double width,
                               double height,
                               Color color,
                               boolean dashed,
                               double opacity) {

        if (plan == null) {
            return;
        }

        List<RouteStep> steps = plan.getSteps();
        if (steps != null && !steps.isEmpty()) {
            for (RouteStep step : steps) {
                if (step == null || step.getFrom() == null || step.getTo() == null) {
                    continue;
                }
                drawLine(content, scale(step.getFrom(), bounds, width, height), scale(step.getTo(), bounds, width, height), color, dashed, opacity);
            }
            return;
        }

        List<Location> orderedLocations = plan.getOrderedLocations();
        if (orderedLocations == null || orderedLocations.size() < 2) {
            return;
        }

        for (int i = 0; i < orderedLocations.size() - 1; i++) {
            Location from = orderedLocations.get(i);
            Location to = orderedLocations.get(i + 1);
            if (from == null || to == null) {
                continue;
            }
            drawLine(content, scale(from, bounds, width, height), scale(to, bounds, width, height), color, dashed, opacity);
        }
    }

    private void drawLine(Group content,
                          Point2D from,
                          Point2D to,
                          Color color,
                          boolean dashed,
                          double opacity) {
        Line line = new Line(from.getX(), from.getY(), to.getX(), to.getY());
        line.setStroke(color);
        line.setStrokeWidth(3.0);
        line.setOpacity(opacity);
        if (dashed) {
            line.getStrokeDashArray().setAll(10.0, 8.0);
        }
        content.getChildren().add(line);
    }

    private Set<Integer> collectSelectedRouteIds(TripPlan plan, WhatIfComparison comparison) {
        Set<Integer> ids = new HashSet<>();
        if (plan != null) {
            addRouteIds(plan, ids);
        }
        if (comparison != null) {
            addRouteIds(comparison.getNewPlan(), ids);
            addRouteIds(comparison.getOldPlan(), ids);
        }
        return ids;
    }

    private void addRouteIds(TripPlan plan, Set<Integer> ids) {
        if (plan == null) {
            return;
        }

        if (plan.getSteps() != null && !plan.getSteps().isEmpty()) {
            for (RouteStep step : plan.getSteps()) {
                if (step == null) {
                    continue;
                }
                if (step.getFrom() != null) {
                    ids.add(step.getFrom().getId());
                }
                if (step.getTo() != null) {
                    ids.add(step.getTo().getId());
                }
            }
            return;
        }

        if (plan.getOrderedLocations() != null) {
            for (Location location : plan.getOrderedLocations()) {
                if (location != null) {
                    ids.add(location.getId());
                }
            }
        }
    }

    private Label createLabel(String name) {
        Label label = new Label(name);
        label.getStyleClass().add("map-label");
        label.setTextFill(LABEL_COLOR);
        label.setMouseTransparent(true);
        return label;
    }

    private LabelPlacement placeLabel(RenderItem item,
                                      Label label,
                                      double paneWidth,
                                      double paneHeight,
                                      List<Rectangle2D> occupiedLabels) {

        String text = item.location.getName();
        double labelWidth = estimateLabelWidth(text);
        double labelHeight = estimateLabelHeight(text, labelWidth);
        double gap = 14;
        double circleX = item.point.getX();
        double circleY = item.point.getY();

        List<Point2D> candidates = buildCandidates(circleX, circleY, labelWidth, labelHeight, gap, paneWidth, paneHeight);

        Point2D bestPoint = candidates.get(0);
        Rectangle2D bestBounds = rectangle(bestPoint.getX(), bestPoint.getY(), labelWidth, labelHeight);
        int bestScore = Integer.MAX_VALUE;

        for (Point2D candidate : candidates) {
            Rectangle2D bounds = rectangle(candidate.getX(), candidate.getY(), labelWidth, labelHeight);
            int score = score(bounds, occupiedLabels, paneWidth, paneHeight);
            if (score < bestScore) {
                bestScore = score;
                bestPoint = candidate;
                bestBounds = bounds;
                if (score == 0) {
                    break;
                }
            }
        }

        LabelPlacement placement = new LabelPlacement();
        placement.x = bestPoint.getX();
        placement.y = bestPoint.getY();
        placement.bounds = bestBounds;
        return placement;
    }

    private List<Point2D> buildCandidates(double circleX,
                                          double circleY,
                                          double labelWidth,
                                          double labelHeight,
                                          double gap,
                                          double paneWidth,
                                          double paneHeight) {

        double centerX = paneWidth / 2.0;
        double centerY = paneHeight / 2.0;
        boolean leftSide = circleX < centerX;
        boolean upperSide = circleY < centerY;

        List<Point2D> candidates = new ArrayList<>();

        if (leftSide) {
            candidates.add(candidate(circleX + gap, circleY - labelHeight / 2.0, labelWidth, labelHeight, paneWidth, paneHeight));
            candidates.add(candidate(circleX + gap, circleY + 14, labelWidth, labelHeight, paneWidth, paneHeight));
            candidates.add(candidate(circleX + gap, circleY - labelHeight - 14, labelWidth, labelHeight, paneWidth, paneHeight));
            candidates.add(candidate(circleX - labelWidth - gap, circleY - labelHeight / 2.0, labelWidth, labelHeight, paneWidth, paneHeight));
            candidates.add(candidate(circleX - labelWidth - gap, circleY + 14, labelWidth, labelHeight, paneWidth, paneHeight));
            candidates.add(candidate(circleX - labelWidth - gap, circleY - labelHeight - 14, labelWidth, labelHeight, paneWidth, paneHeight));
        } else {
            candidates.add(candidate(circleX - labelWidth - gap, circleY - labelHeight / 2.0, labelWidth, labelHeight, paneWidth, paneHeight));
            candidates.add(candidate(circleX - labelWidth - gap, circleY + 14, labelWidth, labelHeight, paneWidth, paneHeight));
            candidates.add(candidate(circleX - labelWidth - gap, circleY - labelHeight - 14, labelWidth, labelHeight, paneWidth, paneHeight));
            candidates.add(candidate(circleX + gap, circleY - labelHeight / 2.0, labelWidth, labelHeight, paneWidth, paneHeight));
            candidates.add(candidate(circleX + gap, circleY + 14, labelWidth, labelHeight, paneWidth, paneHeight));
            candidates.add(candidate(circleX + gap, circleY - labelHeight - 14, labelWidth, labelHeight, paneWidth, paneHeight));
        }

        if (upperSide) {
            candidates.add(candidate(circleX - labelWidth / 2.0, circleY + gap + 14, labelWidth, labelHeight, paneWidth, paneHeight));
        } else {
            candidates.add(candidate(circleX - labelWidth / 2.0, circleY - labelHeight - gap - 14, labelWidth, labelHeight, paneWidth, paneHeight));
        }

        candidates.add(candidate(circleX + 18, circleY - labelHeight / 2.0, labelWidth, labelHeight, paneWidth, paneHeight));
        candidates.add(candidate(circleX - labelWidth - 18, circleY - labelHeight / 2.0, labelWidth, labelHeight, paneWidth, paneHeight));

        return candidates;
    }

    private Point2D candidate(double x, double y, double labelWidth, double labelHeight, double paneWidth, double paneHeight) {
        double clampedX = clamp(x, 8, Math.max(8, paneWidth - labelWidth - 8));
        double clampedY = clamp(y, 8, Math.max(8, paneHeight - labelHeight - 8));
        return new Point2D(clampedX, clampedY);
    }

    private int score(Rectangle2D bounds, List<Rectangle2D> occupied, double paneWidth, double paneHeight) {
        int score = 0;

        if (bounds.getMinX() < 6 || bounds.getMinY() < 6
                || bounds.getMaxX() > paneWidth - 6
                || bounds.getMaxY() > paneHeight - 6) {
            score += 6;
        }

        for (Rectangle2D existing : occupied) {
            if (existing.intersects(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight())) {
                score += 12;
            }
        }

        return score;
    }

    private Rectangle2D rectangle(double x, double y, double width, double height) {
        return new Rectangle2D(x, y, width, height);
    }

    private Point2D scale(Location location, MapBounds bounds, double paneWidth, double paneHeight) {
        double usableWidth = Math.max(1, paneWidth - bounds.horizontalPadding * 2.0);
        double usableHeight = Math.max(1, paneHeight - bounds.verticalPadding * 2.0);
        double xRatio = bounds.xRange == 0 ? 0.5 : (location.getX() - bounds.minX) / bounds.xRange;
        double yRatio = bounds.yRange == 0 ? 0.5 : (location.getY() - bounds.minY) / bounds.yRange;

        double x = bounds.horizontalPadding + (xRatio * usableWidth);
        double y = bounds.verticalPadding + (yRatio * usableHeight);

        return new Point2D(clamp(x, 18, Math.max(18, paneWidth - 18)), clamp(y, 18, Math.max(18, paneHeight - 18)));
    }

    private String buildTooltipText(Location location) {
        String category = location.getId() <= 20 ? "Ilce / Semt" : "Tarihi / Turistik";
        return location.getName()
                + "\nKategori: " + category
                + "\nPuan: " + String.format("%.1f", location.getRating())
                + "\nZiyaret Suresi: " + location.getVisitTime() + " dk"
                + "\nGiris Ucreti: " + String.format("%.0f", location.getEntryFee()) + " TL";
    }

    private Color determineFill(boolean isStartNode, boolean isRouteNode, boolean isSelectedNode) {
        if (isStartNode) {
            return START_COLOR;
        }
        if (isRouteNode || isSelectedNode) {
            return SELECTED_COLOR;
        }
        return NORMAL_COLOR;
    }

    private double estimateLabelWidth(String text) {
        return Math.min(220, Math.max(72, text.length() * 6.2 + 14));
    }

    private double estimateLabelHeight(String text, double width) {
        if (text.length() > 28 && width > 160) {
            return 30;
        }
        return 18;
    }

    private double resolveSize(double actual, double fallback, double defaultValue) {
        if (actual > 0) {
            return actual;
        }
        if (fallback > 0) {
            return fallback;
        }
        return defaultValue;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private Set<Integer> collectSelectedRouteIds(TripPlan plan) {
        Set<Integer> ids = new LinkedHashSet<>();
        addRouteIds(plan, ids);
        return ids;
    }

    private static class RenderItem {
        private final Location location;
        private final Point2D point;

        private RenderItem(Location location, Point2D point) {
            this.location = location;
            this.point = point;
        }
    }

    private static class LabelPlacement {
        private double x;
        private double y;
        private Rectangle2D bounds;
    }

    private static class MapBounds {
        private final double minX;
        private final double maxX;
        private final double minY;
        private final double maxY;
        private final double xRange;
        private final double yRange;
        private final double horizontalPadding;
        private final double verticalPadding;

        private MapBounds(double minX, double maxX, double minY, double maxY) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.xRange = Math.max(1, maxX - minX);
            this.yRange = Math.max(1, maxY - minY);
            this.horizontalPadding = 34;
            this.verticalPadding = 30;
        }

        private static MapBounds from(List<Location> locations) {
            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;

            for (Location location : locations) {
                minX = Math.min(minX, location.getX());
                minY = Math.min(minY, location.getY());
                maxX = Math.max(maxX, location.getX());
                maxY = Math.max(maxY, location.getY());
            }

            if (minX == Double.MAX_VALUE || minY == Double.MAX_VALUE) {
                minX = minY = 0;
                maxX = maxY = 1;
            }

            return new MapBounds(minX, maxX, minY, maxY);
        }
    }
}
