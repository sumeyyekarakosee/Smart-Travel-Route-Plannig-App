package algorithm;

import graph.Graph;
import model.Location;
import model.RouteEdge;
import model.RouteStep;
import model.TripPlan;
import util.RoutePreference;
import util.TripTheme;

import java.util.*;

/*
  Bu sınıf tüm rota hesaplamalarını yönetir:
   - Dijkstra algoritması (mesafe / maliyet / süre / aktarma / dengeli)
   - En Yakın Komşu yaklaşımı ile akıllı sıralama
   - Tema bazlı konum filtreleme
   - Tüm alternatifleri kapsayan rota seti üretimi
   - What-If için yeniden hesaplama ve insertion cost desteği
 */
public class RoutePlanner {

    private final Graph graph;

    public RoutePlanner(Graph graph) {
        this.graph = graph;
    }
    //DİJKSTRA ALGORİTMASI
    /**
     *
     * @param sourceId    Başlangıç konumu ID'si
     * @param targetId    Hedef konum ID'si
     * @param preference  Hangi ağırlık kullanılacak (mesafe/maliyet/süre vb.)
     * @param searchGraph Hangi graf üzerinde arama yapılacak
     *                    (orijinal ya da What-If kopyası)
     * @return Başlangıçtan hedefe giden RouteEdge listesi
     */
    public List<RouteEdge> dijkstra(int sourceId, int targetId,
                                    RoutePreference preference,
                                    Graph searchGraph) {

        Map<Integer, Double> dist = new HashMap<>();

        Map<Integer, RouteEdge> prev = new HashMap<>();

        Set<Integer> visited = new HashSet<>();

        PriorityQueue<double[]> pq =
                new PriorityQueue<>(Comparator.comparingDouble(a -> a[0]));

        for (Location loc : searchGraph.getAllLocations()) {
            dist.put(loc.getId(), Double.MAX_VALUE);
        }
        dist.put(sourceId, 0.0);
        pq.offer(new double[]{0.0, sourceId});

        while (!pq.isEmpty()) {
            double[] current     = pq.poll();
            double   currentCost = current[0];
            int      currentId   = (int) current[1];

            if (visited.contains(currentId)) continue;
            visited.add(currentId);

            if (currentId == targetId) break;

            for (RouteEdge edge : searchGraph.getNeighbors(currentId)) {
                int    neighborId = edge.getToId();
                if (visited.contains(neighborId)) continue;

                double weight  = getEdgeWeight(edge, preference);
                double newCost = currentCost + weight;

                if (newCost < dist.getOrDefault(neighborId, Double.MAX_VALUE)) {
                    dist.put(neighborId, newCost);
                    prev.put(neighborId, edge);
                    pq.offer(new double[]{newCost, neighborId});
                }
            }
        }

        if (sourceId != targetId && !prev.containsKey(targetId)) {
            return Collections.emptyList();
        }

        return reconstructPath(sourceId, targetId, prev);
    }

    public List<RouteEdge> dijkstra(int sourceId, int targetId,
                                    RoutePreference preference) {
        return dijkstra(sourceId, targetId, preference, graph);
    }

    private double getEdgeWeight(RouteEdge edge, RoutePreference preference) {
        return switch (preference) {
            case SHORTEST_DISTANCE -> edge.getDistance();
            case LOWEST_COST       -> edge.getCost();
            case SHORTEST_TIME     -> edge.getTime();
            case LEAST_TRANSFER    -> edge.getTransferCount();
            case BALANCED          -> computeBalancedWeight(edge);
        };
    }

    private double computeBalancedWeight(RouteEdge edge) {
        double normDist     = edge.getDistance()      / 15.0;
        double normTime     = edge.getTime()          / 60.0;
        double normCost     = edge.getCost()          / 100.0;
        double normTransfer = edge.getTransferCount() / 3.0;
        return (normDist + normTime + normCost + normTransfer) / 4.0;
    }

    private List<RouteEdge> reconstructPath(int sourceId, int targetId,
                                            Map<Integer, RouteEdge> prev) {
        LinkedList<RouteEdge> path = new LinkedList<>();
        int current = targetId;
        while (current != sourceId) {
            RouteEdge edge = prev.get(current);
            if (edge == null) return Collections.emptyList();
            path.addFirst(edge);
            current = edge.getFromId();
        }
        return path;
    }

    // İKİ NOKTA ARASI MALİYET
    public double dijkstraCost(int sourceId, int targetId,
                               RoutePreference preference,
                               Graph searchGraph) {

        Map<Integer, Double> dist    = new HashMap<>();
        Set<Integer>         visited = new HashSet<>();
        PriorityQueue<double[]> pq   =
                new PriorityQueue<>(Comparator.comparingDouble(a -> a[0]));

        for (Location loc : searchGraph.getAllLocations()) {
            dist.put(loc.getId(), Double.MAX_VALUE);
        }
        dist.put(sourceId, 0.0);
        pq.offer(new double[]{0.0, sourceId});

        while (!pq.isEmpty()) {
            double[] cur  = pq.poll();
            int      curId = (int) cur[1];
            if (visited.contains(curId)) continue;
            visited.add(curId);
            if (curId == targetId) break;

            for (RouteEdge edge : searchGraph.getNeighbors(curId)) {
                int    nId     = edge.getToId();
                if (visited.contains(nId)) continue;
                double newCost = cur[0] + getEdgeWeight(edge, preference);
                if (newCost < dist.getOrDefault(nId, Double.MAX_VALUE)) {
                    dist.put(nId, newCost);
                    pq.offer(new double[]{newCost, nId});
                }
            }
        }
        return dist.getOrDefault(targetId, Double.MAX_VALUE);
    }

    public double dijkstraCost(int sourceId, int targetId,
                               RoutePreference preference) {
        return dijkstraCost(sourceId, targetId, preference, graph);
    }

    //  3. EN YAKIN KOMŞU (Nearest Neighbor Heuristic)

    /**
     * @param startId     Başlangıç konumu ID'si
     * @param selectedIds Kullanıcının gezmek istediği yer ID'leri
     * @param preference  Hangi ağırlık kriterine göre sıralansın
     * @return Ziyaret sırasına göre Location listesi (başlangıç dahil)
     */
    public List<Location> orderLocationsByNearestNeighbor(int startId,
                                                          List<Integer> selectedIds,
                                                          RoutePreference preference) {
        Set<Integer> remaining = new LinkedHashSet<>(selectedIds);
        remaining.remove(startId);

        List<Location> ordered = new ArrayList<>();
        ordered.add(graph.getLocation(startId));

        int currentId = startId;

        while (!remaining.isEmpty()) {
            int    bestId   = -1;
            double bestCost = Double.MAX_VALUE;

            for (int candidateId : remaining) {
                double cost = dijkstraCost(currentId, candidateId, preference);
                if (cost < bestCost) {
                    bestCost = cost;
                    bestId   = candidateId;
                }
            }

            if (bestId == -1) break; // ulaşılamayan düğüm

            ordered.add(graph.getLocation(bestId));
            remaining.remove(bestId);
            currentId = bestId;
        }

        return ordered;
    }
    // TAM GEZİ PLANI OLUŞTURMA

    /**
     * @param startId      Başlangıç konumu ID'si
     * @param selectedIds  Gezilecek yer ID'leri
     * @param preference   Rota kriteri
     * @param searchGraph  Graf (orijinal veya What-If simülasyon kopyası)
     * @return Eksiksiz TripPlan
     */
    public TripPlan buildTripPlan(int startId,
                                  List<Integer> selectedIds,
                                  RoutePreference preference,
                                  Graph searchGraph) {

        TripPlan plan = new TripPlan(preference);

        List<Location> ordered =
                orderLocationsByNearestNeighbor(startId, selectedIds, preference);

        for (Location loc : ordered) {
            plan.addLocation(loc);
        }

        for (int i = 0; i < ordered.size() - 1; i++) {
            Location from = ordered.get(i);
            Location to   = ordered.get(i + 1);

            List<RouteEdge> edgePath =
                    dijkstra(from.getId(), to.getId(), preference, searchGraph);

            for (RouteEdge edge : edgePath) {
                Location edgeFrom = searchGraph.getLocation(edge.getFromId());
                Location edgeTo   = searchGraph.getLocation(edge.getToId());
                if (edgeFrom != null && edgeTo != null) {
                    plan.addStep(new RouteStep(edgeFrom, edgeTo, edge));
                }
            }
        }

        return plan;
    }

    public TripPlan buildTripPlan(int startId,
                                  List<Integer> selectedIds,
                                  RoutePreference preference) {
        return buildTripPlan(startId, selectedIds, preference, graph);
    }

    // ALTERNATİF ROTA SETİ
    /**
     * Tüm rota kriterlerine göre 5 farklı plan üretir.
     *
     * @param startId      Başlangıç konumu ID'si
     * @param selectedIds  Gezilecek yer ID'leri
     * @param dailyMinutes Günlük gezi süresi (gün hesabı için)
     * @return Her RoutePreference için bir TripPlan içeren liste
     */
    public List<TripPlan> buildAllAlternatives(int startId,
                                               List<Integer> selectedIds,
                                               int dailyMinutes) {
        List<TripPlan> alternatives = new ArrayList<>();

        for (RoutePreference pref : RoutePreference.values()) {
            TripPlan plan = buildTripPlan(startId, selectedIds, pref);
            plan.calculateDifficulty();
            plan.calculateEstimatedDays(dailyMinutes);
            alternatives.add(plan);
        }

        return alternatives;
    }
    // TEMA BAZLI FİLTRELEME
    /**
     * Verilen ID listesinden temaya uymayan konumları filtreler.
     *
     * @param locationIds Filtre uygulanacak konum ID listesi
     * @param theme       Seçilen gezi teması
     * @return Temaya uyan konum ID'leri
     */
    public List<Integer> filterByTheme(List<Integer> locationIds, TripTheme theme) {
        if (theme == TripTheme.MIXED) return new ArrayList<>(locationIds);

        List<Integer> filtered = new ArrayList<>();
        for (int id : locationIds) {
            Location loc = graph.getLocation(id);
            if (loc != null && theme.matches(loc.getCategory())) {
                filtered.add(id);
            }
        }
        return filtered;
    }

    //  WHAT-IF DESTEĞİ
    /**
     * What-If için verilen simülasyon grafı üzerinde yeniden rota hesaplar.
     *
     * @param startId         Başlangıç konumu
     * @param remainingIds    Çıkarma/ekleme sonrası güncel ID listesi
     * @param preference      Rota kriteri
     * @param simulationGraph 1. kişinin graph.copyGraph() ile ürettiği kopya
     * @param dailyMinutes    Günlük gezi süresi
     * @return Güncel TripPlan
     */
    public TripPlan recalculateForWhatIf(int startId,
                                         List<Integer> remainingIds,
                                         RoutePreference preference,
                                         Graph simulationGraph,
                                         int dailyMinutes) {
        TripPlan plan = buildTripPlan(startId, remainingIds, preference, simulationGraph);
        plan.calculateDifficulty();
        plan.calculateEstimatedDays(dailyMinutes);
        return plan;
    }

    /**
     * Insertion Heuristic için ekleme maliyet artışını hesaplar.
     * Formül: cost(A → new) + cost(new → B) − cost(A → B)
     *
     * @param fromId       Aralığın sol tarafı
     * @param newId        Eklenmek istenen konum
     * @param toId         Aralığın sağ tarafı
     * @param preference   Ağırlık kriteri
     * @param searchGraph  Graf
     * @return Maliyet artışı (küçük = daha iyi pozisyon)
     */
    public double insertionCost(int fromId, int newId, int toId,
                                RoutePreference preference,
                                Graph searchGraph) {
        double costAN = dijkstraCost(fromId, newId, preference, searchGraph);
        double costNB = dijkstraCost(newId,  toId,  preference, searchGraph);
        double costAB = dijkstraCost(fromId, toId,  preference, searchGraph);
        return costAN + costNB - costAB;
    }

    public double insertionCost(int fromId, int newId, int toId,
                                RoutePreference preference) {
        return insertionCost(fromId, newId, toId, preference, graph);
    }

    // SIRALAMA YARDIMCILARI

    public List<Location> getLocationsSortedByRating() {
        List<Location> list = new ArrayList<>(graph.getAllLocations());
        list.sort(Comparator.comparingDouble(Location::getRating).reversed());
        return list;
    }

    public List<Location> getLocationsSortedByEntryFee() {
        List<Location> list = new ArrayList<>(graph.getAllLocations());
        list.sort(Comparator.comparingDouble(Location::getEntryFee));
        return list;
    }
}
