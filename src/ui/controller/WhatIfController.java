package ui.controller;

import algorithm.RoutePlanner;
import algorithm.WhatIfSimulator;
import graph.Graph;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Location;
import model.TripPlan;
import model.WhatIfComparison;
import util.RoutePreference;
import util.UndoManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class WhatIfController {

    @FXML private ToggleGroup actionGroup;
    @FXML private RadioButton removeModeRadio;
    @FXML private RadioButton addModeRadio;
    @FXML private ComboBox<Location> locationComboBox;
    @FXML private Button removeButton;
    @FXML private Button addButton;
    @FXML private Button undoButton;
    @FXML private Button clearButton;
    @FXML private Label statusLabel;
    @FXML private TextArea oldPlanArea;
    @FXML private TextArea newPlanArea;
    @FXML private TextArea comparisonArea;
    @FXML private Label currentRouteLabel;

    private Stage dialogStage;
    private Graph graph;
    private RoutePlanner routePlanner;
    private UndoManager undoManager;
    private WhatIfSimulator simulator;
    private TripPlan basePlan;
    private TripPlan currentPlan;
    private RoutePreference preference;
    private int dailyMinutes;

    private Consumer<TripPlan> planConsumer = plan -> { };
    private Consumer<WhatIfComparison> comparisonConsumer = comparison -> { };

    public void bindContext(Stage dialogStage,
                            Graph graph,
                            RoutePlanner routePlanner,
                            UndoManager undoManager,
                            TripPlan currentPlan,
                            RoutePreference preference,
                            int dailyMinutes,
                            Consumer<TripPlan> planConsumer,
                            Consumer<WhatIfComparison> comparisonConsumer) {

        this.dialogStage = dialogStage;
        this.graph = graph;
        this.routePlanner = routePlanner;
        this.undoManager = undoManager;
        this.basePlan = currentPlan;
        this.currentPlan = currentPlan;
        this.preference = preference;
        this.dailyMinutes = dailyMinutes;
        this.planConsumer = planConsumer != null ? planConsumer : this.planConsumer;
        this.comparisonConsumer = comparisonConsumer != null ? comparisonConsumer : this.comparisonConsumer;
        this.simulator = new WhatIfSimulator(graph, routePlanner, undoManager, dailyMinutes);

        if (removeModeRadio != null) {
            removeModeRadio.setSelected(true);
        }

        refreshLocationChoices();
        refreshPlanAreas(null);
    }

    @FXML
    private void initialize() {
        removeModeRadio.setUserData("REMOVE");
        addModeRadio.setUserData("ADD");
        actionGroup.selectToggle(removeModeRadio);

        actionGroup.selectedToggleProperty().addListener((obs, oldValue, newValue) -> refreshLocationChoices());

        locationComboBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Location item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        locationComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Location item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Lokasyon seçin" : item.getName());
            }
        });

        statusLabel.setText("Hazır");
        oldPlanArea.setEditable(false);
        newPlanArea.setEditable(false);
        comparisonArea.setEditable(false);
    }

    @FXML
    private void handleRemoveLocation() {
        Location selected = locationComboBox.getValue();
        if (selected == null) {
            showStatus("Çıkarılacak bir lokasyon seçin.", true);
            return;
        }

        try {
            WhatIfComparison comparison = simulator.removeLocation(currentPlan, selected.getId(), preference);
            currentPlan = comparison.getNewPlan();
            planConsumer.accept(currentPlan);
            comparisonConsumer.accept(comparison);
            refreshPlanAreas(comparison);
            refreshLocationChoices();
            showStatus("Lokasyon çıkarıldı: " + selected.getName(), false);
        } catch (Exception ex) {
            showStatus(ex.getMessage(), true);
        }
    }

    @FXML
    private void handleAddLocation() {
        Location selected = locationComboBox.getValue();
        if (selected == null) {
            showStatus("Eklenecek bir lokasyon seçin.", true);
            return;
        }

        try {
            WhatIfComparison comparison = simulator.addLocation(currentPlan, selected, preference);
            currentPlan = comparison.getNewPlan();
            planConsumer.accept(currentPlan);
            comparisonConsumer.accept(comparison);
            refreshPlanAreas(comparison);
            refreshLocationChoices();
            showStatus("Lokasyon eklendi: " + selected.getName(), false);
        } catch (Exception ex) {
            showStatus(ex.getMessage(), true);
        }
    }

    @FXML
    private void handleUndo() {
        TripPlan undone = simulator.undo();
        if (undone == null) {
            showStatus("Geri alınacak işlem yok.", true);
            return;
        }

        currentPlan = undone;
        planConsumer.accept(currentPlan);
        comparisonConsumer.accept(null);
        refreshPlanAreas(null);
        refreshLocationChoices();
        showStatus("Son işlem geri alındı.", false);
    }

    @FXML
    private void handleClearSimulation() {
        undoManager.clear();
        currentPlan = basePlan;
        planConsumer.accept(currentPlan);
        comparisonConsumer.accept(null);
        refreshPlanAreas(null);
        refreshLocationChoices();
        showStatus("Simülasyon temizlendi.", false);
    }

    @FXML
    private void handleClose() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private void refreshLocationChoices() {
        if (graph == null || currentPlan == null || locationComboBox == null) {
            return;
        }

        boolean removeMode = actionGroup.getSelectedToggle() != null
                && "REMOVE".equals(actionGroup.getSelectedToggle().getUserData());

        List<Location> items = new ArrayList<>();
        List<Integer> currentRouteIds = currentPlan.getOrderedLocations().stream()
                .map(Location::getId)
                .collect(Collectors.toList());

        if (removeMode) {
            List<Location> ordered = currentPlan.getOrderedLocations();
            for (int i = 1; i < ordered.size(); i++) {
                items.add(ordered.get(i));
            }
        } else {
            for (Location location : graph.getAllLocations()) {
                if (!currentRouteIds.contains(location.getId())) {
                    items.add(location);
                }
            }
        }

        items.sort((a, b) -> Integer.compare(a.getId(), b.getId()));
        locationComboBox.setItems(FXCollections.observableArrayList(items));
        if (!items.isEmpty()) {
            locationComboBox.getSelectionModel().selectFirst();
        } else {
            locationComboBox.getSelectionModel().clearSelection();
        }
    }

    private void refreshPlanAreas(WhatIfComparison comparison) {
        if (currentPlan != null) {
            newPlanArea.setText(buildPlanText(currentPlan));
        } else {
            newPlanArea.setText("-");
        }

        if (comparison != null) {
            oldPlanArea.setText(buildPlanText(comparison.getOldPlan()));
            comparisonArea.setText(comparison.getComparisonText());
            currentRouteLabel.setText(comparison.getActionType() + " - " + comparison.getChangedLocationName());
        } else if (currentPlan != null) {
            oldPlanArea.setText(buildPlanText(currentPlan));
            comparisonArea.setText("What-If işlemi bekleniyor.");
            currentRouteLabel.setText("Güncel plan");
        }
    }

    private String buildPlanText(TripPlan plan) {
        if (plan == null) {
            return "-";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Rota: ").append(plan.getRouteSummary()).append("\n");
        sb.append("Mesafe: ").append(String.format("%.1f km", plan.getTotalDistance())).append("\n");
        sb.append("Süre: ").append(plan.getTotalTime()).append(" dk\n");
        sb.append("Maliyet: ").append(String.format("%.0f TL", plan.getTotalCost())).append("\n");
        sb.append("Aktarma: ").append(plan.getTotalTransfers()).append("\n");
        sb.append("Gün: ").append(plan.getEstimatedDays()).append("\n");
        sb.append("Zorluk: ").append(plan.getDifficultyLevel() != null ? plan.getDifficultyLevel() : "-").append("\n");
        return sb.toString();
    }

    private void showStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("status-ok", "status-error");
        statusLabel.getStyleClass().add(error ? "status-error" : "status-ok");
    }
}
