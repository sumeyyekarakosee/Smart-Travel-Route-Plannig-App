package model;

import util.RoutePreference;
import java.util.ArrayList;
import java.util.List;

// Hesaplanmış bir gezi planının tüm bilgilerini tutar.
public class TripPlan {

    private List<Location>  orderedLocations;
    private List<RouteStep> steps;

    private double totalDistance;
    private int    totalTime;
    private double totalCost;
    private int    totalTransfers;

    private RoutePreference preference;
    private String          difficultyLevel;
    private int             estimatedDays;
    private List<String>    usedTransports;

    public TripPlan(RoutePreference preference) {
        this.preference = preference;
        this.orderedLocations = new ArrayList<>();
        this.steps = new ArrayList<>();
        this.usedTransports = new ArrayList<>();
    }

    // Adım ekler ve toplamları günceller.
    public void addStep(RouteStep step) {
        steps.add(step);
        totalDistance  += step.getDistance();
        totalTime      += step.getTime();
        totalCost      += step.getCost();
        totalTransfers += step.getTransferCount();
        if (!usedTransports.contains(step.getTransportType()))
            usedTransports.add(step.getTransportType());
    }

    // Konum ekler; ziyaret süresi ve giriş ücreti toplama dahil edilir.
    public void addLocation(Location location) {
        orderedLocations.add(location);
        totalTime += location.getVisitTime();
        totalCost += location.getEntryFee();
    }

    // Toplam süreye göre Kolay / Orta / Zor belirler.
    public void calculateDifficulty() {
        if (totalTime < 240)       difficultyLevel = "Kolay";
        else if (totalTime <= 420) difficultyLevel = "Orta";
        else                       difficultyLevel = "Zor";
    }

    // Kaç günde tamamlanacağını hesaplar.
    public void calculateEstimatedDays(int dailyMinutes) {
        if (dailyMinutes <= 0) { estimatedDays = 1; return; }
        estimatedDays = (int) Math.ceil((double) totalTime / dailyMinutes);
    }

    public List<Location>  getOrderedLocations() { return orderedLocations; }
    public List<RouteStep> getSteps()            { return steps; }
    public double getTotalDistance()             { return totalDistance; }
    public int    getTotalTime()                 { return totalTime; }
    public double getTotalCost()                 { return totalCost; }
    public int    getTotalTransfers()            { return totalTransfers; }
    public RoutePreference getPreference()       { return preference; }
    public String getDifficultyLevel()           { return difficultyLevel; }
    public int    getEstimatedDays()             { return estimatedDays; }
    public List<String> getUsedTransports()      { return usedTransports; }

    public double getTransportCost() {
        return steps.stream().mapToDouble(RouteStep::getCost).sum();
    }

    public int getTransportTime() {
        return steps.stream().mapToInt(RouteStep::getTime).sum();
    }

    public String getRouteSummary() {
        if (orderedLocations.isEmpty()) return "Boş Rota";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < orderedLocations.size(); i++) {
            sb.append(orderedLocations.get(i).getName());
            if (i < orderedLocations.size() - 1) sb.append(" -> ");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | %.1f km | %d dk | %.0f TL | %d aktarma | %s | ~%d gün",
                preference.getDisplayName(), getRouteSummary(),
                totalDistance, totalTime, totalCost, totalTransfers,
                difficultyLevel != null ? difficultyLevel : "?", estimatedDays);
    }
}
