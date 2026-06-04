package ui.view;

import graph.Graph;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import model.Location;
import model.RouteStep;
import model.TripPlan;
import model.WhatIfComparison;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * OpenStreetMap raster tiles rendered directly inside a JavaFX Pane.
 *
 * This version supports:
 * - pan by dragging
 * - zoom in/out via mouse wheel or buttons
 * - marker positions that recalculate for every zoom level
 */
public class MapRenderer {

    private static final int TILE_SIZE = 256;
    private static final int DEFAULT_ZOOM = 11;
    private static final int MIN_ZOOM = 10;
    private static final int MAX_ZOOM = 13;

    private static final Color START_COLOR = Color.web("#1e88e5");
    private static final Color SELECTED_COLOR = Color.web("#43a047");
    private static final Color NORMAL_COLOR = Color.web("#ffffff");
    private static final Color OUTLINE_COLOR = Color.web("#94a3b8");
    private static final Color LABEL_COLOR = Color.web("#0f172a");
    private static final Color OLD_ROUTE_COLOR = Color.web("#e53935");
    private static final Color TILE_BACKGROUND = Color.web("#dfeff8");
    private static final String TRANSPARENT_PIXEL =
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAASsJTYQAAAAASUVORK5CYII=";
    private static final Path LOCAL_TILE_ROOT = Path.of("resources", "map-tiles");

    private Pane mapPane;
    private final Pane tileLayer = new Pane();
    private final Pane routeLayer = new Pane();
    private final Pane markerLayer = new Pane();
    private final VBox controlsBox = new VBox(8);
    private final Button zoomInButton = new Button("+");
    private final Button zoomOutButton = new Button("−");
    private final Label zoomLabel = new Label();

    private boolean initialized;
    private boolean userAdjustedView;
    private int currentZoom = DEFAULT_ZOOM;
    private double centerLat = 41.0082;
    private double centerLon = 28.9784;
    private double dragStartX;
    private double dragStartY;
    private double dragStartLat;
    private double dragStartLon;

    private Graph currentGraph;
    private TripPlan currentPlan;
    private WhatIfComparison currentComparison;
    private Set<Integer> currentSelectedIds = Set.of();
    private Integer currentStartId;

    public void render(Pane mapPane,
                       Graph graph,
                       TripPlan plan,
                       WhatIfComparison comparison,
                       Set<Integer> selectedIds,
                       Integer startId) {

        if (mapPane == null || graph == null) {
            return;
        }

        ensureInitialized(mapPane);

        currentGraph = graph;
        currentPlan = plan;
        currentComparison = comparison;
        currentSelectedIds = selectedIds != null ? new LinkedHashSet<>(selectedIds) : Set.of();
        currentStartId = startId;

        if (!userAdjustedView) {
            Point2D preferredCenter = computePreferredCenter(graph, plan, comparison, startId);
            if (preferredCenter != null) {
                centerLat = preferredCenter.getX();
                centerLon = preferredCenter.getY();
            }
        }

        redraw();
    }

    private void ensureInitialized(Pane pane) {
        if (initialized && pane == mapPane) {
            return;
        }

        mapPane = pane;
        mapPane.getChildren().clear();
        mapPane.setStyle("-fx-background-color: #dfeff8;");

        tileLayer.setPickOnBounds(false);
        routeLayer.setPickOnBounds(false);
        markerLayer.setPickOnBounds(false);
        tileLayer.setManaged(false);
        routeLayer.setManaged(false);
        markerLayer.setManaged(false);

        setupZoomControls();

        mapPane.getChildren().addAll(tileLayer, routeLayer, markerLayer, controlsBox);

        mapPane.widthProperty().addListener((obs, oldValue, newValue) -> redraw());
        mapPane.heightProperty().addListener((obs, oldValue, newValue) -> redraw());

        mapPane.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            dragStartX = event.getX();
            dragStartY = event.getY();
            dragStartLat = centerLat;
            dragStartLon = centerLon;
            mapPane.setCursor(javafx.scene.Cursor.CLOSED_HAND);
        });

        mapPane.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!event.isPrimaryButtonDown()) {
                return;
            }

            double dx = event.getX() - dragStartX;
            double dy = event.getY() - dragStartY;
            Point2D startPixel = latLonToWorldPixels(dragStartLat, dragStartLon, currentZoom);
            Point2D newCenterPixel = new Point2D(startPixel.getX() - dx, startPixel.getY() - dy);
            Point2D newCenter = worldPixelsToLatLon(newCenterPixel.getX(), newCenterPixel.getY(), currentZoom);
            centerLat = newCenter.getX();
            centerLon = newCenter.getY();
            userAdjustedView = true;
            redraw();
        });

        mapPane.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> mapPane.setCursor(javafx.scene.Cursor.DEFAULT));
        mapPane.addEventHandler(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() == 0) {
                return;
            }
            zoomBy(event.getDeltaY() > 0 ? 1 : -1, event.getX(), event.getY());
            event.consume();
        });

        initialized = true;
        redraw();
    }

    private void setupZoomControls() {
        zoomInButton.setPrefWidth(34);
        zoomOutButton.setPrefWidth(34);
        zoomInButton.setPrefHeight(34);
        zoomOutButton.setPrefHeight(34);
        zoomLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #0f2f57;");
        zoomLabel.setPadding(new Insets(2, 4, 0, 4));

        String buttonStyle = """
                -fx-background-radius: 10;
                -fx-background-color: rgba(255,255,255,0.95);
                -fx-border-color: rgba(15,23,42,0.18);
                -fx-border-radius: 10;
                -fx-font-size: 18px;
                -fx-font-weight: 700;
                -fx-text-fill: #12395f;
                -fx-cursor: hand;
                -fx-effect: dropshadow(gaussian, rgba(15,23,42,0.14), 10, 0.18, 0, 2);
                """;
        zoomInButton.setStyle(buttonStyle);
        zoomOutButton.setStyle(buttonStyle);

        zoomInButton.setOnAction(event -> zoomBy(1, mapPane.getWidth() / 2.0, mapPane.getHeight() / 2.0));
        zoomOutButton.setOnAction(event -> zoomBy(-1, mapPane.getWidth() / 2.0, mapPane.getHeight() / 2.0));

        controlsBox.getChildren().setAll(zoomInButton, zoomOutButton, zoomLabel);
        controlsBox.setPadding(new Insets(14));
        controlsBox.setStyle("""
                -fx-background-color: rgba(255,255,255,0.90);
                -fx-background-radius: 16;
                -fx-border-radius: 16;
                -fx-border-color: rgba(15,23,42,0.10);
                -fx-effect: dropshadow(gaussian, rgba(15,23,42,0.12), 14, 0.18, 0, 3);
                """);
        controlsBox.setLayoutX(16);
        controlsBox.setLayoutY(16);
        controlsBox.setMinWidth(58);
        controlsBox.setMaxWidth(58);
        controlsBox.setMouseTransparent(false);
    }

    private void redraw() {
        if (!initialized || mapPane == null) {
            return;
        }

        double width = resolveSize(mapPane.getWidth(), mapPane.getPrefWidth(), 900);
        double height = resolveSize(mapPane.getHeight(), mapPane.getPrefHeight(), 700);
        if (width <= 0 || height <= 0) {
            return;
        }

        mapPane.setClip(new Rectangle(width, height));
        tileLayer.setPrefSize(width, height);
        routeLayer.setPrefSize(width, height);
        markerLayer.setPrefSize(width, height);
        controlsBox.setLayoutX(16);
        controlsBox.setLayoutY(16);
        updateZoomLabel();

        redrawBackground(width, height);
        drawTiles(width, height);

        routeLayer.getChildren().clear();
        markerLayer.getChildren().clear();

        if (currentGraph == null) {
            return;
        }

        List<Location> locations = new ArrayList<>(currentGraph.getAllLocations());
        locations.sort(Comparator.comparingInt(Location::getId));
        if (locations.isEmpty()) {
            return;
        }

        TripPlan activePlan = currentPlan != null ? currentPlan : currentComparison != null ? currentComparison.getNewPlan() : null;
        Set<Integer> routeIds = collectSelectedRouteIds(activePlan, currentComparison);

        if (currentComparison != null && currentComparison.getOldPlan() != null) {
            drawPlanLines(routeLayer, currentComparison.getOldPlan(), width, height, OLD_ROUTE_COLOR, true, 0.72);
        }

        if (currentPlan != null) {
            drawPlanLines(routeLayer, currentPlan, width, height, SELECTED_COLOR, false, 0.92);
        } else if (currentComparison != null && currentComparison.getNewPlan() != null) {
            drawPlanLines(routeLayer, currentComparison.getNewPlan(), width, height, SELECTED_COLOR, false, 0.92);
        }

        List<Rectangle2D> occupiedLabels = new ArrayList<>();
        for (Location location : locations) {
            Point2D screen = toScreen(location, width, height);
            boolean isRouteNode = routeIds.contains(location.getId());
            boolean isStartNode = currentStartId != null && location.getId() == currentStartId;
            boolean isSelectedNode = currentSelectedIds != null && currentSelectedIds.contains(location.getId());

            double circleRadius = circleRadiusForZoom(currentZoom);
            Circle circle = new Circle(screen.getX(), screen.getY(), circleRadius);
            circle.setStroke(OUTLINE_COLOR);
            circle.setStrokeWidth(2);
            circle.setFill(determineFill(isStartNode, isRouteNode, isSelectedNode));

            Tooltip.install(circle, new Tooltip(buildTooltipText(location)));

            circle.setOnMouseEntered(event -> {
                circle.setStroke(Color.web("#1565c0"));
                circle.setStrokeWidth(3);
                ScaleTransition transition = new ScaleTransition(Duration.millis(120), circle);
                transition.setToX(1.14);
                transition.setToY(1.14);
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

            Label label = createLabel(location.getName());
            LabelPlacement placement = placeLabel(screen, label, width, height, occupiedLabels);
            label.setLayoutX(placement.x);
            label.setLayoutY(placement.y);
            occupiedLabels.add(placement.bounds);

            markerLayer.getChildren().addAll(circle, label);
        }

        markerLayer.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(180), markerLayer);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void redrawBackground(double width, double height) {
        tileLayer.getChildren().clear();
        Rectangle background = new Rectangle(width, height);
        background.setFill(TILE_BACKGROUND);
        tileLayer.getChildren().add(background);
    }

    private void drawTiles(double width, double height) {
        Point2D centerPixels = latLonToWorldPixels(centerLat, centerLon, currentZoom);
        double halfWidth = width / 2.0;
        double halfHeight = height / 2.0;

        int tilesPerAxis = 1 << currentZoom;
        int minTileX = Math.max(0, (int) Math.floor((centerPixels.getX() - halfWidth) / TILE_SIZE) - 1);
        int maxTileX = Math.min(tilesPerAxis - 1, (int) Math.ceil((centerPixels.getX() + halfWidth) / TILE_SIZE) + 1);
        int minTileY = Math.max(0, (int) Math.floor((centerPixels.getY() - halfHeight) / TILE_SIZE) - 1);
        int maxTileY = Math.min(tilesPerAxis - 1, (int) Math.ceil((centerPixels.getY() + halfHeight) / TILE_SIZE) + 1);

        for (int x = minTileX; x <= maxTileX; x++) {
            for (int y = minTileY; y <= maxTileY; y++) {
                ImageView tileView = new ImageView(new Image(resolveTileSource(currentZoom, x, y), true));
                tileView.setFitWidth(TILE_SIZE);
                tileView.setFitHeight(TILE_SIZE);
                tileView.setPreserveRatio(false);
                tileView.setSmooth(true);
                tileView.setCache(true);

                double worldX = x * TILE_SIZE;
                double worldY = y * TILE_SIZE;
                tileView.setLayoutX(worldX - centerPixels.getX() + halfWidth);
                tileView.setLayoutY(worldY - centerPixels.getY() + halfHeight);
                tileLayer.getChildren().add(tileView);
            }
        }
    }

    private String resolveTileSource(int zoom, int x, int y) {
        Path localTile = LOCAL_TILE_ROOT.resolve(String.valueOf(zoom)).resolve(String.valueOf(x)).resolve(y + ".png");
        if (Files.exists(localTile)) {
            return localTile.toUri().toString();
        }
        return TRANSPARENT_PIXEL;
    }

    private void zoomBy(int delta, double anchorX, double anchorY) {
        int targetZoom = clamp(currentZoom + delta, MIN_ZOOM, MAX_ZOOM);
        if (targetZoom == currentZoom) {
            return;
        }

        double width = resolveSize(mapPane.getWidth(), mapPane.getPrefWidth(), 900);
        double height = resolveSize(mapPane.getHeight(), mapPane.getPrefHeight(), 700);
        Point2D anchorLatLon = screenToLatLon(anchorX, anchorY, width, height, centerLat, centerLon, currentZoom);
        currentZoom = targetZoom;

        Point2D anchorPixelAtNewZoom = latLonToWorldPixels(anchorLatLon.getX(), anchorLatLon.getY(), currentZoom);
        Point2D desiredCenterPixel = new Point2D(
                anchorPixelAtNewZoom.getX() - (anchorX - width / 2.0),
                anchorPixelAtNewZoom.getY() - (anchorY - height / 2.0)
        );
        Point2D newCenter = worldPixelsToLatLon(desiredCenterPixel.getX(), desiredCenterPixel.getY(), currentZoom);
        centerLat = newCenter.getX();
        centerLon = newCenter.getY();
        userAdjustedView = true;
        redraw();
    }

    private void updateZoomLabel() {
        zoomLabel.setText("Zoom " + currentZoom);
    }

    private void drawPlanLines(Pane content,
                               TripPlan plan,
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
                drawLine(content, toScreen(step.getFrom(), width, height), toScreen(step.getTo(), width, height), color, dashed, opacity);
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
            drawLine(content, toScreen(from, width, height), toScreen(to, width, height), color, dashed, opacity);
        }
    }

    private void drawLine(Pane content,
                          Point2D from,
                          Point2D to,
                          Color color,
                          boolean dashed,
                          double opacity) {
        Line line = new Line(from.getX(), from.getY(), to.getX(), to.getY());
        line.setStroke(color);
        line.setStrokeWidth(4.0);
        line.setOpacity(opacity);
        if (dashed) {
            line.getStrokeDashArray().setAll(12.0, 8.0);
        }
        content.getChildren().add(line);
    }

    private Label createLabel(String name) {
        Label label = new Label(name);
        label.getStyleClass().add("map-label");
        label.setTextFill(LABEL_COLOR);
        label.setMouseTransparent(true);
        return label;
    }

    private LabelPlacement placeLabel(Point2D point,
                                      Label label,
                                      double paneWidth,
                                      double paneHeight,
                                      List<Rectangle2D> occupiedLabels) {

        String text = label.getText();
        double labelWidth = estimateLabelWidth(text);
        double labelHeight = estimateLabelHeight(text, labelWidth);
        double gap = 12;
        double circleX = point.getX();
        double circleY = point.getY();

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

        List<Point2D> candidates = new ArrayList<>();
        candidates.add(candidate(circleX + gap, circleY - labelHeight / 2.0, labelWidth, labelHeight, paneWidth, paneHeight));
        candidates.add(candidate(circleX - labelWidth - gap, circleY - labelHeight / 2.0, labelWidth, labelHeight, paneWidth, paneHeight));
        candidates.add(candidate(circleX - labelWidth / 2.0, circleY - labelHeight - gap, labelWidth, labelHeight, paneWidth, paneHeight));
        candidates.add(candidate(circleX - labelWidth / 2.0, circleY + gap, labelWidth, labelHeight, paneWidth, paneHeight));
        candidates.add(candidate(circleX + gap, circleY - labelHeight - 16, labelWidth, labelHeight, paneWidth, paneHeight));
        candidates.add(candidate(circleX - labelWidth - gap, circleY - labelHeight - 16, labelWidth, labelHeight, paneWidth, paneHeight));
        return candidates;
    }

    private Point2D candidate(double x, double y, double labelWidth, double labelHeight, double paneWidth, double paneHeight) {
        double clampedX = clampDouble(x, 8, Math.max(8, paneWidth - labelWidth - 8));
        double clampedY = clampDouble(y, 8, Math.max(8, paneHeight - labelHeight - 8));
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

    private Point2D toScreen(Location location, double paneWidth, double paneHeight) {
        Point2D pixel = latLonToWorldPixels(location.getLatitude(), location.getLongitude(), currentZoom);
        Point2D centerPixels = latLonToWorldPixels(centerLat, centerLon, currentZoom);
        double x = paneWidth / 2.0 + (pixel.getX() - centerPixels.getX());
        double y = paneHeight / 2.0 + (pixel.getY() - centerPixels.getY());
        return new Point2D(x, y);
    }

    private Point2D screenToLatLon(double screenX,
                                   double screenY,
                                   double paneWidth,
                                   double paneHeight,
                                   double centerLat,
                                   double centerLon,
                                   int zoom) {
        Point2D centerPixels = latLonToWorldPixels(centerLat, centerLon, zoom);
        Point2D anchorPixels = new Point2D(
                centerPixels.getX() + (screenX - paneWidth / 2.0),
                centerPixels.getY() + (screenY - paneHeight / 2.0)
        );
        return worldPixelsToLatLon(anchorPixels.getX(), anchorPixels.getY(), zoom);
    }

    private Point2D computePreferredCenter(Graph graph,
                                           TripPlan plan,
                                           WhatIfComparison comparison,
                                           Integer startId) {
        List<Location> candidates = new ArrayList<>();

        if (plan != null && plan.getOrderedLocations() != null && !plan.getOrderedLocations().isEmpty()) {
            candidates.addAll(plan.getOrderedLocations());
        } else if (comparison != null && comparison.getNewPlan() != null && comparison.getNewPlan().getOrderedLocations() != null) {
            candidates.addAll(comparison.getNewPlan().getOrderedLocations());
        } else if (startId != null && graph.getLocation(startId) != null) {
            candidates.add(graph.getLocation(startId));
        } else {
            candidates.addAll(graph.getAllLocations());
        }

        if (candidates.isEmpty()) {
            return new Point2D(centerLat, centerLon);
        }

        double latSum = 0;
        double lonSum = 0;
        int count = 0;
        for (Location location : candidates) {
            if (location == null) {
                continue;
            }
            latSum += location.getLatitude();
            lonSum += location.getLongitude();
            count++;
        }

        if (count == 0) {
            return new Point2D(centerLat, centerLon);
        }

        return new Point2D(latSum / count, lonSum / count);
    }

    private Set<Integer> collectSelectedRouteIds(TripPlan plan, WhatIfComparison comparison) {
        Set<Integer> ids = new HashSet<>();
        addRouteIds(plan, ids);
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

    private Point2D latLonToWorldPixels(double lat, double lon, int zoom) {
        double sinLat = Math.sin(Math.toRadians(lat));
        double worldSize = TILE_SIZE * Math.pow(2, zoom);
        double x = (lon + 180.0) / 360.0 * worldSize;
        double y = (0.5 - Math.log((1 + sinLat) / (1 - sinLat)) / (4 * Math.PI)) * worldSize;
        return new Point2D(x, y);
    }

    private Point2D worldPixelsToLatLon(double x, double y, int zoom) {
        double worldSize = TILE_SIZE * Math.pow(2, zoom);
        double lon = x / worldSize * 360.0 - 180.0;
        double n = Math.PI - 2.0 * Math.PI * y / worldSize;
        double lat = Math.toDegrees(Math.atan(Math.sinh(n)));
        return new Point2D(lat, lon);
    }

    private double circleRadiusForZoom(int zoom) {
        return clampDouble(7 + (zoom - MIN_ZOOM) * 1.5, 7, 12);
    }

    private String buildTooltipText(Location location) {
        String category = location.getId() <= 20 ? "İlçe / Semt" : "Tarihi / Turistik";
        return location.getName()
                + "\nKategori: " + category
                + "\nPuan: " + String.format("%.1f", location.getRating())
                + "\nZiyaret Süresi: " + location.getVisitTime() + " dk"
                + "\nGiriş Ücreti: " + String.format("%.0f", location.getEntryFee()) + " TL";
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

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class LabelPlacement {
        private double x;
        private double y;
        private Rectangle2D bounds;
    }
}
