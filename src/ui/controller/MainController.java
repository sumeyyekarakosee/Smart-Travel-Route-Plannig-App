package ui.controller;

import algorithm.RoutePlanner;
import algorithm.TripScheduler;
import data.RouteSaver;
import graph.Graph;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.DayPlan;
import model.Location;
import model.RouteStep;
import model.TripPlan;
import model.WhatIfComparison;
import ui.service.TravelDataService;
import ui.view.MapRenderer;
import util.RoutePreference;
import util.UndoManager;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MainController {

    @FXML private BorderPane rootPane;
    @FXML private ComboBox<Location> startLocationCombo;
    @FXML private TextField searchField;
    @FXML private ListView<Location> districtListView;
    @FXML private ListView<Location> historicalListView;
    @FXML private ToggleGroup routePreferenceGroup;
    @FXML private RadioButton shortestDistanceRadio;
    @FXML private RadioButton lowestCostRadio;
    @FXML private RadioButton shortestTimeRadio;
    @FXML private RadioButton leastTransferRadio;
    @FXML private RadioButton balancedRadio;
    @FXML private Spinner<Integer> dailyHoursSpinner;
    @FXML private TextField budgetField;
    @FXML private TextField routeNameField;
    @FXML private Button generateRouteButton;
    @FXML private Button clearButton;
    @FXML private Button saveButton;
    @FXML private Button whatIfButton;
    @FXML private Pane mapPane;

    @FXML private Label totalDistanceLabel;
    @FXML private Label totalTimeLabel;
    @FXML private Label totalCostLabel;
    @FXML private Label totalTransfersLabel;
    @FXML private Label estimatedDaysLabel;
    @FXML private Label difficultyLabel;
    @FXML private Label budgetStatusLabel;
    @FXML private Label selectedCountLabel;
    @FXML private Label routeSummaryLabel;
    @FXML private Label statusLabel;
    @FXML private ListView<RouteStep> routeStepsListView;
    @FXML private TableView<TripPlan> alternativesTableView;
    @FXML private TableColumn<TripPlan, String> alternativeTypeColumn;
    @FXML private TableColumn<TripPlan, Number> alternativeDistanceColumn;
    @FXML private TableColumn<TripPlan, Number> alternativeTimeColumn;
    @FXML private TableColumn<TripPlan, Number> alternativeCostColumn;
    @FXML private TableColumn<TripPlan, Number> alternativeTransferColumn;
    @FXML private Accordion dayPlanAccordion;
    @FXML private TextArea routeDetailsArea;

    private final TravelDataService dataService = new TravelDataService();
    private final MapRenderer mapRenderer = new MapRenderer();
    private final UndoManager undoManager = new UndoManager();
    private final TripScheduler tripScheduler = new TripScheduler();

    private Graph graph;
    private RoutePlanner routePlanner;
    private Stage hostStage;

    private final ObservableList<Location> districtItems = FXCollections.observableArrayList();
    private final ObservableList<Location> historicalItems = FXCollections.observableArrayList();
    private final FilteredList<Location> districtFiltered = new FilteredList<>(districtItems, item -> true);
    private final FilteredList<Location> historicalFiltered = new FilteredList<>(historicalItems, item -> true);

    private final Set<Integer> selectedLocationIds = new LinkedHashSet<>();

    private TripPlan currentPlan;
    private List<DayPlan> currentDayPlans = new ArrayList<>();
    private List<TripPlan> currentAlternatives = new ArrayList<>();
    private WhatIfComparison currentComparison;
    private RoutePreference currentPreference = RoutePreference.SHORTEST_DISTANCE;

    private int currentDailyHours = 6;
    private double currentBudget = Double.MAX_VALUE;

    public void setHostStage(Stage hostStage) {
        this.hostStage = hostStage;
    }

    @FXML
    private void initialize() {
        graph = dataService.getGraph();
        routePlanner = new RoutePlanner(graph);

        districtItems.setAll(dataService.getDistricts());
        historicalItems.setAll(dataService.getHistoricalPlaces());

        startLocationCombo.setItems(FXCollections.observableArrayList(dataService.getDistricts()));
        startLocationCombo.getSelectionModel().selectFirst();
        startLocationCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && selectedLocationIds.remove(newValue.getId())) {
                refreshLocationViews();
            }
            renderMap();
        });

        searchField.textProperty().addListener((obs, oldValue, newValue) -> updateFilters(newValue));
        updateFilters("");

        setupLocationCellFactories();
        setupRouteTable();
        setupDurationInput();
        setupRoutePreference();
        setupRouteDetailArea();
        setupDefaultState();
    }

    private void setupDefaultState() {
        routeNameField.setText("İstanbul Gezi Rotası");
        budgetField.setPromptText("Örn: 2500");
        selectedCountLabel.setText("0 nokta seçildi");
        statusLabel.setText("Hazır");
        budgetStatusLabel.setText("Bütçe girilmedi");
        routeSummaryLabel.setText("Henüz rota oluşturulmadı");
        routeStepsListView.setPlaceholder(new Label("Rota adımları burada görünecek."));
        alternativesTableView.setPlaceholder(new Label("Alternatif rotalar için rota oluşturun."));
        dayPlanAccordion.setExpandedPane(null);
        renderMap();
    }

    private void setupLocationCellFactories() {
        districtListView.setItems(districtFiltered);
        historicalListView.setItems(historicalFiltered);

        districtListView.setCellFactory(list -> new SelectableLocationCell());
        historicalListView.setCellFactory(list -> new SelectableLocationCell());

        districtListView.setPlaceholder(new Label("İlçe / semt bulunamadı."));
        historicalListView.setPlaceholder(new Label("Tarihi / turistik yer bulunamadı."));
    }

    private void setupRouteTable() {
        alternativeTypeColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getPreference().getDisplayName()));
        alternativeDistanceColumn.setCellValueFactory(cell ->
                new SimpleDoubleProperty(cell.getValue().getTotalDistance()));
        alternativeTimeColumn.setCellValueFactory(cell ->
                new SimpleIntegerProperty(cell.getValue().getTotalTime()));
        alternativeCostColumn.setCellValueFactory(cell ->
                new SimpleDoubleProperty(cell.getValue().getTotalCost()));
        alternativeTransferColumn.setCellValueFactory(cell ->
                new SimpleIntegerProperty(cell.getValue().getTotalTransfers()));

        alternativeDistanceColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.1f km", item.doubleValue()));
            }
        });
        alternativeTimeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.intValue() + " dk");
            }
        });
        alternativeCostColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.0f TL", item.doubleValue()));
            }
        });
        alternativeTransferColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.intValue() + "");
            }
        });

        routeStepsListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(RouteStep item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        });
    }

    private void setupDurationInput() {
        SpinnerValueFactory.IntegerSpinnerValueFactory factory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 16, 6);
        dailyHoursSpinner.setValueFactory(factory);
        dailyHoursSpinner.setEditable(true);
    }

    private void setupRoutePreference() {
        shortestDistanceRadio.setUserData(RoutePreference.SHORTEST_DISTANCE);
        lowestCostRadio.setUserData(RoutePreference.LOWEST_COST);
        shortestTimeRadio.setUserData(RoutePreference.SHORTEST_TIME);
        leastTransferRadio.setUserData(RoutePreference.LEAST_TRANSFER);
        balancedRadio.setUserData(RoutePreference.BALANCED);
        routePreferenceGroup.selectToggle(shortestDistanceRadio);
    }

    private void setupRouteDetailArea() {
        routeDetailsArea.setEditable(false);
        routeDetailsArea.setWrapText(true);
        routeDetailsArea.setText("Rota oluşturulduğunda özet, bütçe durumu ve plan açıklaması burada görünecek.");
    }

    private void updateFilters(String query) {
        String normalized = Optional.ofNullable(query).orElse("")
                .trim()
                .toLowerCase(new Locale("tr", "TR"));

        districtFiltered.setPredicate(location -> matchesQuery(location, normalized));
        historicalFiltered.setPredicate(location -> matchesQuery(location, normalized));
    }

    private boolean matchesQuery(Location location, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String name = location.getName() == null ? "" : location.getName().toLowerCase(new Locale("tr", "TR"));
        return name.contains(query);
    }

    @FXML
    private void handleGenerateRoute() {
        Location startLocation = startLocationCombo.getValue();
        if (startLocation == null) {
            showStatus("Başlangıç noktası seçin.", true);
            return;
        }

        if (selectedLocationIds.isEmpty()) {
            showStatus("En az bir gezi noktası seçin.", true);
            return;
        }

        int dailyHours = getDailyHours();
        if (dailyHours <= 0) {
            showStatus("Günlük süre 1 saatten büyük olmalı.", true);
            return;
        }

        try {
            currentBudget = parseBudget();
        } catch (NumberFormatException ex) {
            showStatus("Bütçe sayısal olmalı. Örn: 2500", true);
            return;
        }
        if (currentBudget < 0) {
            showStatus("Bütçe geçersiz. Pozitif bir sayı girin.", true);
            return;
        }

        currentPreference = getSelectedPreference();
        currentDailyHours = dailyHours;

        List<Integer> selectedIds = selectedLocationIds.stream()
                .filter(id -> id != startLocation.getId())
                .collect(Collectors.toList());

        if (selectedIds.isEmpty()) {
            showStatus("Başlangıç noktası dışında en az bir lokasyon seçin.", true);
            return;
        }

        currentPlan = routePlanner.buildTripPlan(
                startLocation.getId(),
                selectedIds,
                currentPreference,
                graph);

        currentPlan.calculateDifficulty();
        currentPlan.calculateEstimatedDays(currentDailyHours);

        currentDayPlans = tripScheduler.createDayPlans(currentPlan, currentDailyHours);
        currentAlternatives = routePlanner.buildAllAlternatives(
                startLocation.getId(),
                selectedIds,
                currentDailyHours);

        currentComparison = null;
        undoManager.clear();
        updateRouteView();

        if (currentPlan.getTotalCost() > currentBudget) {
            budgetStatusLabel.setText(String.format(
                    "Bütçe aşıldı: %.0f TL fazla",
                    currentPlan.getTotalCost() - currentBudget));
            budgetStatusLabel.getStyleClass().remove("success-text");
            if (!budgetStatusLabel.getStyleClass().contains("danger-text")) {
                budgetStatusLabel.getStyleClass().add("danger-text");
            }
            showStatus("Rota oluşturuldu fakat bütçe aşıldı.", true);
        } else {
            budgetStatusLabel.setText(String.format(
                    "Bütçe uygun: %.0f TL kaldı",
                    currentBudget - currentPlan.getTotalCost()));
            budgetStatusLabel.getStyleClass().remove("danger-text");
            if (!budgetStatusLabel.getStyleClass().contains("success-text")) {
                budgetStatusLabel.getStyleClass().add("success-text");
            }
            showStatus("Rota oluşturuldu.", false);
        }
    }

    @FXML
    private void handleClear() {
        selectedLocationIds.clear();
        currentPlan = null;
        currentComparison = null;
        currentDayPlans = new ArrayList<>();
        currentAlternatives = new ArrayList<>();
        undoManager.clear();

        routeStepsListView.getItems().clear();
        alternativesTableView.getItems().clear();
        dayPlanAccordion.getPanes().clear();
        routeDetailsArea.setText("Rota oluşturulduğunda özet, bütçe durumu ve plan açıklaması burada görünecek.");
        routeSummaryLabel.setText("Henüz rota oluşturulmadı");
        budgetStatusLabel.setText("Bütçe girilmedi");
        budgetStatusLabel.getStyleClass().removeAll("success-text", "danger-text");
        selectedCountLabel.setText("0 nokta seçildi");
        clearSummaryLabels();
        refreshLocationViews();
        renderMap();
        showStatus("Seçimler temizlendi.", false);
    }

    @FXML
    private void handleSaveRoute() {
        if (currentPlan == null) {
            showStatus("Kaydedilecek rota yok.", true);
            return;
        }

        String routeName = routeNameField.getText();
        if (routeName == null || routeName.trim().isEmpty()) {
            routeName = "İstanbul Gezi Rotası";
        }

        RouteSaver saver = new RouteSaver("resources/saved_routes.txt");

        try {
            saver.saveTripPlan(routeName, currentPlan, currentDayPlans);
            if (currentComparison != null) {
                saver.saveWhatIfComparison(routeName + " - What-If", currentComparison);
            }
            showStatus("Rota kaydedildi.", false);
        } catch (IOException ex) {
            showStatus("Rota kaydedilemedi: " + ex.getMessage(), true);
        }
    }

    @FXML
    private void handleOpenWhatIf() {
        if (currentPlan == null) {
            showStatus("Önce bir rota oluşturun.", true);
            return;
        }

        if (hostStage == null) {
            showStatus("Pencere bağlanamadı.", true);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader();
            URL fxmlUrl = Path.of("resources", "fxml", "whatif-view.fxml").toUri().toURL();
            loader.setLocation(fxmlUrl);
            Stage dialog = new Stage();
            dialog.initOwner(hostStage);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("What-If Simülasyonu");
            dialog.setMinWidth(1100);
            dialog.setMinHeight(720);

            Scene scene = new Scene(loader.load(), 1100, 720);
            scene.getStylesheets().add(Path.of("resources", "styles", "app.css").toUri().toString());
            dialog.setScene(scene);

            WhatIfController controller = loader.getController();
            controller.bindContext(
                    dialog,
                    graph,
                    routePlanner,
                    undoManager,
                    currentPlan,
                    currentPreference,
                    currentDailyHours,
                    this::applyWhatIfPlan,
                    this::applyWhatIfComparison);

            dialog.showAndWait();
        } catch (Exception ex) {
            showStatus("What-If penceresi açılamadı: " + ex.getMessage(), true);
        }
    }

    private void applyWhatIfPlan(TripPlan plan) {
        currentPlan = plan;
        if (plan != null) {
            currentPreference = plan.getPreference();
            currentDailyHours = Math.max(currentDailyHours, 1);
            currentDayPlans = tripScheduler.createDayPlans(plan, currentDailyHours);
            syncSelectedIdsWithPlan(plan);
            rebuildAlternativesFromCurrentPlan();
            routeDetailsArea.setText(buildRouteDetailsText(plan, currentComparison));
        } else {
            currentDayPlans = new ArrayList<>();
        }
        updateRouteView();
    }

    private void applyWhatIfComparison(WhatIfComparison comparison) {
        currentComparison = comparison;
        if (comparison != null) {
            currentPlan = comparison.getNewPlan();
            currentDayPlans = tripScheduler.createDayPlans(currentPlan, currentDailyHours);
            syncSelectedIdsWithPlan(currentPlan);
            rebuildAlternativesFromCurrentPlan();
        }
        updateRouteView();
    }

    private void updateRouteView() {
        if (currentPlan == null) {
            clearSummaryLabels();
            routeStepsListView.getItems().clear();
            alternativesTableView.getItems().clear();
            dayPlanAccordion.getPanes().clear();
            routeSummaryLabel.setText("Henüz rota oluşturulmadı");
            renderMap();
            return;
        }

        totalDistanceLabel.setText(String.format("%.1f km", currentPlan.getTotalDistance()));
        totalTimeLabel.setText(currentPlan.getTotalTime() + " dk");
        totalCostLabel.setText(String.format("%.0f TL", currentPlan.getTotalCost()));
        totalTransfersLabel.setText(String.valueOf(currentPlan.getTotalTransfers()));
        estimatedDaysLabel.setText(String.valueOf(currentPlan.getEstimatedDays()));
        difficultyLabel.setText(currentPlan.getDifficultyLevel() != null ? currentPlan.getDifficultyLevel() : "-");
        routeSummaryLabel.setText(currentPlan.getRouteSummary());
        selectedCountLabel.setText(selectedLocationIds.size() + " nokta seçildi");

        routeStepsListView.setItems(FXCollections.observableArrayList(currentPlan.getSteps()));
        alternativesTableView.setItems(FXCollections.observableArrayList(currentAlternatives));
        buildDayPlanAccordion();
        routeDetailsArea.setText(buildRouteDetailsText(currentPlan, currentComparison));
        renderMap();
    }

    private void buildDayPlanAccordion() {
        dayPlanAccordion.getPanes().clear();

        for (DayPlan dayPlan : currentDayPlans) {
            TextArea textArea = new TextArea(dayPlan.toString() + "\n\n" + dayPlan.getStepSummary());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setPrefRowCount(6);

            TitledPane pane = new TitledPane(dayPlan.getDayNumber() + ". Gün", textArea);
            dayPlanAccordion.getPanes().add(pane);
        }
    }

    private String buildRouteDetailsText(TripPlan plan, WhatIfComparison comparison) {
        StringBuilder sb = new StringBuilder();
        sb.append("Rota Özeti: ").append(plan.getRouteSummary()).append("\n");
        sb.append("Tercih: ").append(plan.getPreference().getDisplayName()).append("\n");
        sb.append("Toplam Mesafe: ").append(String.format("%.1f km", plan.getTotalDistance())).append("\n");
        sb.append("Toplam Süre: ").append(plan.getTotalTime()).append(" dk\n");
        sb.append("Toplam Maliyet: ").append(String.format("%.0f TL", plan.getTotalCost())).append("\n");
        sb.append("Toplam Aktarma: ").append(plan.getTotalTransfers()).append("\n");
        sb.append("Tahmini Gün: ").append(plan.getEstimatedDays()).append("\n");
        sb.append("Zorluk Seviyesi: ").append(plan.getDifficultyLevel() != null ? plan.getDifficultyLevel() : "-").append("\n");
        sb.append("Kullanılan Ulaşım Türleri: ").append(plan.getUsedTransports()).append("\n");

        if (currentBudget != Double.MAX_VALUE) {
            sb.append("\nBütçe: ").append(String.format("%.0f TL", currentBudget)).append("\n");
            sb.append("Bütçe Durumu: ");
            if (plan.getTotalCost() > currentBudget) {
                sb.append("Aşıldı (").append(String.format("%.0f TL", plan.getTotalCost() - currentBudget)).append(" fazla)");
            } else {
                sb.append("Uygun (").append(String.format("%.0f TL", currentBudget - plan.getTotalCost())).append(" kaldı)");
            }
            sb.append("\n");
        }

        if (comparison != null) {
            sb.append("\n").append(comparison.getComparisonText());
        }

        return sb.toString();
    }

    private void renderMap() {
        Set<Integer> selected = new LinkedHashSet<>(selectedLocationIds);
        Integer startId = Optional.ofNullable(startLocationCombo.getValue()).map(Location::getId).orElse(null);
        mapRenderer.render(mapPane, graph, currentPlan, currentComparison, selected, startId);
    }

    private void refreshLocationViews() {
        districtListView.refresh();
        historicalListView.refresh();
        selectedCountLabel.setText(selectedLocationIds.size() + " nokta seçildi");
    }

    private void syncSelectedIdsWithPlan(TripPlan plan) {
        selectedLocationIds.clear();
        if (plan == null || plan.getOrderedLocations() == null) {
            refreshLocationViews();
            return;
        }

        for (int i = 1; i < plan.getOrderedLocations().size(); i++) {
            selectedLocationIds.add(plan.getOrderedLocations().get(i).getId());
        }

        refreshLocationViews();
    }

    private void rebuildAlternativesFromCurrentPlan() {
        if (currentPlan == null || currentPlan.getOrderedLocations().isEmpty()) {
            currentAlternatives = new ArrayList<>();
            return;
        }

        int startId = currentPlan.getOrderedLocations().get(0).getId();
        List<Integer> selectedIds = new ArrayList<>();
        for (int i = 1; i < currentPlan.getOrderedLocations().size(); i++) {
            selectedIds.add(currentPlan.getOrderedLocations().get(i).getId());
        }

        currentAlternatives = routePlanner.buildAllAlternatives(startId, selectedIds, currentDailyHours);
    }

    private void clearSummaryLabels() {
        totalDistanceLabel.setText("-");
        totalTimeLabel.setText("-");
        totalCostLabel.setText("-");
        totalTransfersLabel.setText("-");
        estimatedDaysLabel.setText("-");
        difficultyLabel.setText("-");
    }

    private RoutePreference getSelectedPreference() {
        Toggle selected = routePreferenceGroup.getSelectedToggle();
        if (selected == null || selected.getUserData() == null) {
            return RoutePreference.SHORTEST_DISTANCE;
        }

        return (RoutePreference) selected.getUserData();
    }

    private int getDailyHours() {
        Integer hours = dailyHoursSpinner.getValue();
        if (hours == null) {
            return 0;
        }

        return hours;
    }

    private double parseBudget() {
        String text = budgetField.getText();
        if (text == null || text.trim().isEmpty()) {
            return Double.MAX_VALUE;
        }

        return Double.parseDouble(text.trim());
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("status-ok", "status-error");
        statusLabel.getStyleClass().add(isError ? "status-error" : "status-ok");
    }

    private class SelectableLocationCell extends ListCell<Location> {
        private final CheckBox checkBox = new CheckBox();
        private final Label label = new Label();
        private final HBox box = createCellNode();

        private HBox createCellNode() {
            checkBox.setFocusTraversable(false);
            label.getStyleClass().add("location-cell-label");
            HBox container = new HBox(10, checkBox, label);
            container.getStyleClass().add("location-cell");
            container.setStyle("-fx-background-color: transparent;");
            return container;
        }

        @Override
        protected void updateItem(Location item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            String category = item.getId() <= 20 ? "İlçe / Semt" : "Tarihi / Turistik";
            label.setText(item.getName() + "  •  " + category);
            checkBox.setSelected(selectedLocationIds.contains(item.getId()));
            checkBox.setOnAction(event -> {
                if (checkBox.isSelected()) {
                    selectedLocationIds.add(item.getId());
                } else {
                    selectedLocationIds.remove(item.getId());
                }
                selectedCountLabel.setText(selectedLocationIds.size() + " nokta seçildi");
                renderMap();
            });

            if (startLocationCombo.getValue() != null && startLocationCombo.getValue().getId() == item.getId()) {
                label.setText(label.getText() + "  •  başlangıç");
            }

            setGraphic(box);
        }
    }
}
