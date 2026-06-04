import algorithm.RoutePlanner;
import data.FileManager;
import graph.Graph;
import model.*;
import util.RoutePreference;
import util.TripTheme;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RoutePlannerTest {

    public static void main(String[] args) {

        // Veri yükleme
        FileManager fileManager = new FileManager();
        List<Location> locations = fileManager.loadLocations("resources/locations.txt");
        List<RouteEdge> routes   = fileManager.loadRoutes("resources/routes.txt");

        Graph graph = new Graph();
        for (Location loc : locations) graph.addLocation(loc);
        for (RouteEdge route : routes)  graph.addEdge(route);

        System.out.println("Konum: " + locations.size() + " | Kenar: " + routes.size() + "\n");

        RoutePlanner planner = new RoutePlanner(graph);
        List<Integer> selected = Arrays.asList(3, 4, 5, 6); // Ayasofya, Topkapı, Galata, Taksim

        // TEST 1: Dijkstra
        System.out.println("── TEST 1: Dijkstra (Kadıköy → Galata) ──");
        printPath(planner, 1, 5, RoutePreference.SHORTEST_DISTANCE, graph);
        printPath(planner, 1, 5, RoutePreference.LOWEST_COST, graph);
        printPath(planner, 1, 5, RoutePreference.LEAST_TRANSFER, graph);

        // TEST 2: En Yakın Komşu
        System.out.println("\n── TEST 2: En Yakın Komşu Sıralama ──");
        List<Location> ordered = planner.orderLocationsByNearestNeighbor(1, selected, RoutePreference.SHORTEST_DISTANCE);
        ordered.forEach(l -> System.out.print(l.getName() + " -> "));
        System.out.println("son");

        // TEST 3: Tam rota planı
        System.out.println("\n── TEST 3: Tam Rota Planı ──");
        TripPlan plan = planner.buildTripPlan(1, selected, RoutePreference.SHORTEST_DISTANCE);
        plan.calculateDifficulty();
        plan.calculateEstimatedDays(360);
        System.out.println("Rota    : " + plan.getRouteSummary());
        System.out.printf("Mesafe  : %.1f km | Süre: %d dk | Maliyet: %.0f TL | Aktarma: %d%n",
                plan.getTotalDistance(), plan.getTotalTime(), plan.getTotalCost(), plan.getTotalTransfers());
        System.out.println("Zorluk  : " + plan.getDifficultyLevel() + " | Gün: " + plan.getEstimatedDays());
        plan.getSteps().forEach(s -> System.out.println("  " + s));

        // TEST 4: Alternatif karşılaştırma
        System.out.println("\n── TEST 4: Alternatif Karşılaştırma ──");
        new RouteComparison(planner.buildAllAlternatives(1, selected, 360)).printComparisonTable();

        // TEST 5: Tema filtresi
        System.out.println("\n── TEST 5: Tema Filtresi (Historic) ──");
        List<Integer> allIds = locations.stream().map(Location::getId).collect(Collectors.toList());
        planner.filterByTheme(allIds, TripTheme.HISTORICAL)
                .forEach(id -> System.out.print(graph.getLocation(id).getName() + " "));
        System.out.println();

        // TEST 6: Insertion Cost
        System.out.println("\n── TEST 6: Insertion Cost (Dolmabahçe için en iyi pozisyon) ──");
        int[] routeIds = {1, 2, 3, 4};
        int newLoc = 9;
        int bestPos = -1; double bestCost = Double.MAX_VALUE;
        for (int i = 0; i < routeIds.length - 1; i++) {
            double cost = planner.insertionCost(routeIds[i], newLoc, routeIds[i + 1], RoutePreference.SHORTEST_DISTANCE);
            System.out.printf("  %s -> [%s] -> %s  artış: %.2f%n",
                    graph.getLocation(routeIds[i]).getName(),
                    graph.getLocation(newLoc).getName(),
                    graph.getLocation(routeIds[i + 1]).getName(), cost);
            if (cost < bestCost) { bestCost = cost; bestPos = i; }
        }
        System.out.println("En iyi: " + graph.getLocation(routeIds[bestPos]).getName() + " sonrası");

        // TEST 7: What-If konum çıkarma
        System.out.println("\n── TEST 7: What-If — Ayasofya(3) Çıkarma ──");
        Graph simGraph = graph.copyGraph();
        simGraph.removeLocation(3);
        TripPlan newPlan = planner.recalculateForWhatIf(1, Arrays.asList(4, 5, 6), RoutePreference.SHORTEST_DISTANCE, simGraph, 360);
        System.out.println("Eski: " + plan.getRouteSummary());
        System.out.println("Yeni: " + newPlan.getRouteSummary());
        System.out.printf("Fark → Mesafe: %+.1f km | Süre: %+d dk | Maliyet: %+.0f TL%n",
                newPlan.getTotalDistance() - plan.getTotalDistance(),
                newPlan.getTotalTime()     - plan.getTotalTime(),
                newPlan.getTotalCost()     - plan.getTotalCost());

        System.out.println("\n✓ Tüm testler tamamlandı.");
    }

    private static void printPath(RoutePlanner planner, int from, int to,
                                  RoutePreference pref, Graph graph) {
        List<RouteEdge> path = planner.dijkstra(from, to, pref);
        System.out.printf("  [%-20s] ", pref.getDisplayName());
        path.forEach(e -> System.out.print(graph.getLocation(e.getFromId()).getName() + " -> "));
        if (!path.isEmpty()) System.out.print(graph.getLocation(path.get(path.size()-1).getToId()).getName());
        System.out.println();
    }
}
