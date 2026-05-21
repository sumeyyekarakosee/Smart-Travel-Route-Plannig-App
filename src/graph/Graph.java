/*
  Graph sınıfı, şehirdeki lokasyonları ve bu lokasyonlar arasındaki
  bağlantıları graph veri yapısı kullanarak temsil eder.

  Bu yapı:
  - Location nesnelerini node (düğüm)
  - RouteEdge nesnelerini edge (kenar/bağlantı)
  olarak saklar.

  Graph yapısı adjacency list yaklaşımı kullanılarak tasarlanmıştır.
  Bu sayede komşu lokasyonlara hızlı ve bellek açısından verimli erişim sağlanır.

  Bu sınıfın temel sorumlulukları:
  - Lokasyon eklemek
  - Bağlantı eklemek
  - Komşu lokasyonları döndürmek
  - Graph kopyalamak
  - Geçici lokasyon silme işlemleri yapmak
 */



package graph;

import model.Location;
import model.RouteEdge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Graph {

    // Location ID -> Location nesnesi eşleşmesini tutar
    private HashMap<Integer, Location> locations;

    // Her lokasyonun komşu bağlantılarını adjacency list yapısında tutar
    private HashMap<Integer, List<RouteEdge>> adjacencyList;

    // Boş graph veri yapısını başlatır
    public Graph() {

        locations = new HashMap<>();
        adjacencyList = new HashMap<>();
    }


    /*
      Graph yapısına yeni bir lokasyon ekler.
      Her lokasyon için ayrıca boş bir komşu listesi oluşturulur.
     */

    public void addLocation(Location location) {

        // Lokasyon nesnesi ID bilgisi ile birlikte hashmap içine eklenir
        locations.put(location.getId(), location);

        // Yeni lokasyon için boş komşu listesi oluşturulur
        adjacencyList.put(location.getId(), new ArrayList<>());
    }

    /*
      İki lokasyon arasındaki bağlantıyı graph yapısına ekler.

      Projede yollar çift yönlü kabul edildiği için
      ters bağlantı (reverse edge) da otomatik oluşturulur.
     */

    public void addEdge(RouteEdge edge) {

        // Başlangıç ve hedef lokasyon ID bilgileri alınır
        int fromId = edge.getFromId();
        int toId = edge.getToId();

        if (adjacencyList.containsKey(fromId)) {

            // Normal yönlü bağlantı adjacency list yapısına eklenir
            adjacencyList.get(fromId).add(edge);
        }

        // Çift yönlü ulaşım desteği için ters bağlantı oluşturulur
        RouteEdge reverseEdge = new RouteEdge(
                toId,
                fromId,
                edge.getDistance(),
                edge.getTime(),
                edge.getCost(),
                edge.getTransportType(),
                edge.getTransferCount()
        );

        if (adjacencyList.containsKey(toId)) {

            // Ters bağlantı adjacency list yapısına eklenir
            adjacencyList.get(toId).add(reverseEdge);
        }
    }

    /*
      Verilen lokasyonun komşu bağlantılarını döndürür.

      Defensive copy kullanılarak iç veri yapısının
      dışarıdan değiştirilmesi engellenir.
     */

    public List<RouteEdge> getNeighbors(int locationId) {

        // İlgili lokasyonun komşu bağlantıları alınır
        List<RouteEdge> neighbors =
                adjacencyList.get(locationId);

        // Lokasyon bulunamazsa boş liste döndürülür
        if (neighbors == null) {

            return new ArrayList<>();
        }

        // İç veri yapısını korumak için liste kopyası döndürülür
        return new ArrayList<>(neighbors);
    }


    // Verilen ID bilgisine ait Location nesnesini döndürür.
    public Location getLocation(int id) {

        return locations.get(id);
    }

    // Belirtilen ID'ye sahip lokasyonun graph içinde olup olmadığını kontrol eder.
    public boolean containsLocation(int id) {

        return locations.containsKey(id);
    }

    /*
      Mevcut graph yapısının deep copy kopyasını oluşturur.

      Bu yöntem özellikle what-if analizleri sırasında
      ana graph yapısını korumak amacıyla kullanılır.
     */

    public Graph copyGraph() {

        // Bağımsız yeni graph nesnesi oluşturulur
        Graph copy = new Graph();

        // Lokasyon nesneleri bağımsız olacak şekilde deep copy yapılır
        for (Location location : locations.values()) {

            Location copiedLocation = new Location(
                    location.getId(),
                    location.getName(),
                    location.getCategory(),
                    location.getVisitTime(),
                    location.getEntryFee(),
                    location.getRating(),
                    location.getX(),
                    location.getY()
            );

            copy.locations.put(
                    copiedLocation.getId(),
                    copiedLocation
            );

            copy.adjacencyList.put(
                    copiedLocation.getId(),
                    new ArrayList<>()
            );
        }

        // Edge nesneleri bağımsız olacak şekilde deep copy yapılır
        for (Integer locationId : adjacencyList.keySet()) {

            List<RouteEdge> copiedEdges = new ArrayList<>();

            for (RouteEdge edge : adjacencyList.get(locationId)) {

                RouteEdge copiedEdge = new RouteEdge(
                        edge.getFromId(),
                        edge.getToId(),
                        edge.getDistance(),
                        edge.getTime(),
                        edge.getCost(),
                        edge.getTransportType(),
                        edge.getTransferCount()
                );

                copiedEdges.add(copiedEdge);
            }

            copy.adjacencyList.put(locationId, copiedEdges);
        }

        return copy;
    }

    /*
      Belirtilen lokasyonu ve bu lokasyona bağlı tüm bağlantıları graph'tan kaldırır.

      Bu işlem genellikle geçici senaryo analizleri için
      graph kopyası üzerinde kullanılır.
     */

    public void removeLocation(int id) {

        // Lokasyon hashmap yapısından kaldırılır
        locations.remove(id);

        // Lokasyona ait komşu listesi kaldırılır
        adjacencyList.remove(id);

        for (List<RouteEdge> edges : adjacencyList.values()) {

            // Silinen lokasyona bağlı edge'ler temizlenir
            edges.removeIf(edge ->
                    edge.getToId() == id
            );
        }

    }


    //Graph içerisindeki tüm lokasyonları liste halinde döndürür.
    public List<Location> getAllLocations() {

        // İç veri yapısını korumak amacıyla liste kopyası döndürülür
        return new ArrayList<>(locations.values());
    }


    //Graph içerisindeki tüm edge bağlantılarını tek liste halinde döndürür.
    public List<RouteEdge> getAllEdges() {

        // Tüm edge bağlantılarını tutacak birleşik liste
        List<RouteEdge> allEdges = new ArrayList<>();

        for (List<RouteEdge> edges : adjacencyList.values()) {

            // Her adjacency listesindeki edge'ler birleşik listeye eklenir
            allEdges.addAll(edges);
        }

        return allEdges;
    }

    //Graph içerisindeki toplam lokasyon sayısını döndürür.
    public int size() {

        return locations.size();
    }



}
