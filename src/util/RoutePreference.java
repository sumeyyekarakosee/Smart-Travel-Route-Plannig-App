package util;

// Dijkstra'nın hangi ağırlıkla çalışacağını belirler.
public enum RoutePreference {

    SHORTEST_DISTANCE("En Kısa Mesafe"),
    LOWEST_COST("En Düşük Maliyet"),
    SHORTEST_TIME("En Kısa Süre"),
    LEAST_TRANSFER("En Az Aktarma"),
    BALANCED("Dengeli Rota");

    private final String displayName;

    RoutePreference(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    @Override
    public String toString() { return displayName; }
}
