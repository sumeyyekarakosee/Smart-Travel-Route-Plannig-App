package model;

// Rotadaki tek bir kenar geçişini (A -> B) temsil eder.
public class RouteStep {

    private Location from;
    private Location to;
    private double distance;
    private int time;
    private double cost;
    private String transportType;
    private int transferCount;

    public RouteStep(Location from, Location to, RouteEdge edge) {
        this.from = from;
        this.to = to;
        this.distance = edge.getDistance();
        this.time = edge.getTime();
        this.cost = edge.getCost();
        this.transportType = edge.getTransportType();
        this.transferCount = edge.getTransferCount();
    }

    public Location getFrom()        { return from; }
    public Location getTo()          { return to; }
    public double getDistance()      { return distance; }
    public int getTime()             { return time; }
    public double getCost()          { return cost; }
    public String getTransportType() { return transportType; }
    public int getTransferCount()    { return transferCount; }

    @Override
    public String toString() {
        return String.format("%s -> %s | %s | %.1f km | %d dk | %.0f TL | aktarma: %d",
                from.getName(), to.getName(), transportType, distance, time, cost, transferCount);
    }
}
