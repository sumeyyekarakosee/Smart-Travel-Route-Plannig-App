/*
  RouteEdge sınıfı, iki lokasyon arasındaki bağlantıyı temsil eden model sınıfıdır.

  Her RouteEdge nesnesi:
  - Başlangıç lokasyonu
  - Hedef lokasyon
  - Mesafe bilgisi
  - Ulaşım süresi
  - Ulaşım maliyeti
  - Ulaşım türü
  - Aktarma sayısı

   gibi bağlantı verilerini içerir.

  Bu sınıf graph yapısındaki edge (kenar/bağlantı) verisini temsil eder.
 */

package model;

public class RouteEdge {

    private int fromId;
    private int toId;

    private double distance;
    private int time;
    private double cost;
    private String transportType;
    private int transferCount;

    public RouteEdge(int fromId,
                     int toId,
                     double distance,
                     int time,
                     double cost,
                     String transportType,
                     int transferCount) {

        this.fromId = fromId;
        this.toId = toId;
        this.distance = distance;
        this.time = time;
        this.cost = cost;
        this.transportType = transportType;
        this.transferCount = transferCount;
    }

    public int getFromId() {
        return fromId;
    }

    public void setFromId(int fromId) {
        this.fromId = fromId;
    }

    public int getToId() {
        return toId;
    }

    public void setToId(int toId) {
        this.toId = toId;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public String getTransportType() {
        return transportType;
    }

    public void setTransportType(String transportType) {
        this.transportType = transportType;
    }

    public int getTransferCount() {
        return transferCount;
    }

    public void setTransferCount(int transferCount) {
        this.transferCount = transferCount;
    }

    @Override
    public String toString() {
        return "RouteEdge{" +
                "fromId=" + fromId +
                ", toId=" + toId +
                ", distance=" + distance +
                ", time=" + time +
                ", cost=" + cost +
                ", transportType='" + transportType + '\'' +
                ", transferCount=" + transferCount +
                '}';
    }
}
