package algorithm;

import graph.Graph;
import model.Location;
import model.RouteEdge;
import model.RouteStep;
import model.TripPlan;
import model.WhatIfComparison;
import util.RoutePreference;
import util.UndoManager;

import java.util.ArrayList;
import java.util.List;

/*
  WhatIfSimulator sınıfı, kullanıcı oluşturduğu rota üzerinde
  geçici değişiklikler yapmak istediğinde çalışır.

  Desteklenen işlemler:
  - Rotadan lokasyon çıkarma
  - Rotaya yeni lokasyon ekleme
  - Eski rota ile yeni rotayı karşılaştırma
  - Undo Stack ile geri alma

  Bu sınıf orijinal graph yapısını bozmaz.
  Lokasyon çıkarma işleminde Graph.copyGraph() ile geçici graf kopyası oluşturulur.
 */
public class WhatIfSimulator {

    private Graph originalGraph;
    private RoutePlanner routePlanner;
    private UndoManager undoManager;
    private int dailyMinutes;

    public WhatIfSimulator(Graph originalGraph,
                           RoutePlanner routePlanner,
                           UndoManager undoManager,
                           int dailyMinutes) {

        this.originalGraph = originalGraph;
        this.routePlanner = routePlanner;
        this.undoManager = undoManager;
        this.dailyMinutes = dailyMinutes;
    }

    public WhatIfComparison removeLocation(TripPlan currentPlan,
                                           int removeLocationId,
                                           RoutePreference preference) {

        if (currentPlan == null) {
            throw new IllegalArgumentException("Mevcut rota boş olamaz.");
        }

        List<Location> oldOrderedLocations = currentPlan.getOrderedLocations();

        if (oldOrderedLocations == null || oldOrderedLocations.size() <= 1) {
            throw new IllegalArgumentException("Rotadan çıkarılabilecek lokasyon bulunmuyor.");
        }

        Location removedLocation = findLocationInList(oldOrderedLocations, removeLocationId);

        if (removedLocation == null) {
            throw new IllegalArgumentException("Seçilen lokasyon mevcut rotada bulunamadı.");
        }

        if (oldOrderedLocations.get(0).getId() == removeLocationId) {
            throw new IllegalArgumentException("Başlangıç lokasyonu What-If ile çıkarılamaz.");
        }

        undoManager.push(currentPlan);

        Graph simulationGraph = originalGraph.copyGraph();
        simulationGraph.removeLocation(removeLocationId);

        List<Location> newOrderedLocations = new ArrayList<>();

        for (Location location : oldOrderedLocations) {
            if (location.getId() != removeLocationId) {
                Location copiedLocation = simulationGraph.getLocation(location.getId());

                if (copiedLocation != null) {
                    newOrderedLocations.add(copiedLocation);
                }
            }
        }

        TripPlan newPlan = buildPlanFromOrderedLocations(
                newOrderedLocations,
                preference,
                simulationGraph
        );

        return new WhatIfComparison(
                currentPlan,
                newPlan,
                "Lokasyon Çıkarma",
                removedLocation.getName(),
                dailyMinutes
        );
    }

    public WhatIfComparison addLocation(TripPlan currentPlan,
                                        Location newLocation,
                                        RoutePreference preference) {

        if (currentPlan == null) {
            throw new IllegalArgumentException("Mevcut rota boş olamaz.");
        }

        if (newLocation == null) {
            throw new IllegalArgumentException("Eklenecek lokasyon boş olamaz.");
        }

        List<Location> oldOrderedLocations = currentPlan.getOrderedLocations();

        if (oldOrderedLocations == null || oldOrderedLocations.isEmpty()) {
            throw new IllegalArgumentException("Mevcut rota boş olduğu için lokasyon eklenemez.");
        }

        if (containsLocation(oldOrderedLocations, newLocation.getId())) {
            throw new IllegalArgumentException("Bu lokasyon zaten mevcut rotada bulunuyor.");
        }

        undoManager.push(currentPlan);

        int bestIndex = findBestInsertionIndex(
                oldOrderedLocations,
                newLocation,
                preference,
                originalGraph
        );

        List<Location> newOrderedLocations = new ArrayList<>(oldOrderedLocations);
        newOrderedLocations.add(bestIndex, newLocation);

        TripPlan newPlan = buildPlanFromOrderedLocations(
                newOrderedLocations,
                preference,
                originalGraph
        );

        return new WhatIfComparison(
                currentPlan,
                newPlan,
                "Lokasyon Ekleme",
                newLocation.getName(),
                dailyMinutes
        );
    }

    /*
      Insertion Heuristic:
      Yeni lokasyon, mevcut rotadaki her iki lokasyon arasına denenir.
      Maliyet artışı en düşük olan pozisyon seçilir.
     */
    private int findBestInsertionIndex(List<Location> currentRoute,
                                       Location newLocation,
                                       RoutePreference preference,
                                       Graph searchGraph) {

        if (currentRoute.size() == 1) {
            return 1;
        }

        double bestCostIncrease = Double.MAX_VALUE;
        int bestIndex = currentRoute.size();

        for (int i = 0; i < currentRoute.size() - 1; i++) {
            Location from = currentRoute.get(i);
            Location to = currentRoute.get(i + 1);

            double costIncrease = routePlanner.insertionCost(
                    from.getId(),
                    newLocation.getId(),
                    to.getId(),
                    preference,
                    searchGraph
            );

            if (costIncrease < bestCostIncrease) {
                bestCostIncrease = costIncrease;
                bestIndex = i + 1;
            }
        }

        /*
          Yeni lokasyonun rotanın sonuna eklenmesi ihtimali de kontrol edilir.
         */
        Location lastLocation = currentRoute.get(currentRoute.size() - 1);

        double appendCost = routePlanner.dijkstraCost(
                lastLocation.getId(),
                newLocation.getId(),
                preference,
                searchGraph
        );

        if (appendCost < bestCostIncrease) {
            bestIndex = currentRoute.size();
        }

        if (bestCostIncrease == Double.MAX_VALUE && appendCost == Double.MAX_VALUE) {
            throw new IllegalArgumentException("Eklenecek lokasyona ulaşılabilecek bir rota bulunamadı.");
        }

        return bestIndex;
    }

    /*
      Bu metot RoutePlanner'ın Dijkstra metodunu kullanarak
      verilen sıralı lokasyon listesinden TripPlan oluşturur.

      Böylece What-If lokasyon ekleme işleminde bulunan sıra korunur.
     */
    private TripPlan buildPlanFromOrderedLocations(List<Location> orderedLocations,
                                                   RoutePreference preference,
                                                   Graph searchGraph) {

        if (orderedLocations == null || orderedLocations.isEmpty()) {
            throw new IllegalArgumentException("Sıralı lokasyon listesi boş olamaz.");
        }

        TripPlan plan = new TripPlan(preference);

        for (Location location : orderedLocations) {
            plan.addLocation(location);
        }

        for (int i = 0; i < orderedLocations.size() - 1; i++) {
            Location from = orderedLocations.get(i);
            Location to = orderedLocations.get(i + 1);

            List<RouteEdge> edgePath = routePlanner.dijkstra(
                    from.getId(),
                    to.getId(),
                    preference,
                    searchGraph
            );

            if (edgePath.isEmpty()) {
                throw new IllegalArgumentException(
                        from.getName() + " ile " + to.getName() + " arasında rota bulunamadı."
                );
            }

            for (RouteEdge edge : edgePath) {
                Location edgeFrom = searchGraph.getLocation(edge.getFromId());
                Location edgeTo = searchGraph.getLocation(edge.getToId());

                if (edgeFrom != null && edgeTo != null) {
                    RouteStep step = new RouteStep(edgeFrom, edgeTo, edge);
                    plan.addStep(step);
                }
            }
        }

        plan.calculateDifficulty();
        plan.calculateEstimatedDays(dailyMinutes);

        return plan;
    }

    public TripPlan undo() {
        return undoManager.undo();
    }

    public boolean canUndo() {
        return undoManager.canUndo();
    }

    private boolean containsLocation(List<Location> locations, int locationId) {
        return findLocationInList(locations, locationId) != null;
    }

    private Location findLocationInList(List<Location> locations, int locationId) {
        for (Location location : locations) {
            if (location.getId() == locationId) {
                return location;
            }
        }

        return null;
    }
}