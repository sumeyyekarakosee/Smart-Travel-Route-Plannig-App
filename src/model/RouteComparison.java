package model;

import util.RoutePreference;
import java.util.List;

// Farklı rota seçeneklerini karşılaştırmak için kullanılır.
public class RouteComparison {

    private final List<TripPlan> plans;

    public RouteComparison(List<TripPlan> plans) {
        this.plans = plans;
    }

    public List<TripPlan> getPlans() { return plans; }

    public TripPlan getPlanByPreference(RoutePreference preference) {
        return plans.stream().filter(p -> p.getPreference() == preference)
                .findFirst().orElse(null);
    }

    public TripPlan getShortestDistancePlan() {
        return plans.stream().min((a, b) -> Double.compare(a.getTotalDistance(), b.getTotalDistance())).orElse(null);
    }

    public TripPlan getCheapestPlan() {
        return plans.stream().min((a, b) -> Double.compare(a.getTotalCost(), b.getTotalCost())).orElse(null);
    }

    public TripPlan getFastestPlan() {
        return plans.stream().min((a, b) -> Integer.compare(a.getTotalTime(), b.getTotalTime())).orElse(null);
    }

    public TripPlan getLeastTransferPlan() {
        return plans.stream().min((a, b) -> Integer.compare(a.getTotalTransfers(), b.getTotalTransfers())).orElse(null);
    }

    // Karşılaştırma tablosunu konsola yazdırır.
    public void printComparisonTable() {
        System.out.printf("%-22s | %8s | %6s | %8s | %8s%n", "Rota Türü", "Mesafe", "Süre", "Maliyet", "Aktarma");
        System.out.println("-".repeat(70));
        for (TripPlan plan : plans) {
            System.out.printf("%-22s | %7.1f km | %5d dk | %7.0f TL | %7d%n",
                    plan.getPreference().getDisplayName(),
                    plan.getTotalDistance(), plan.getTotalTime(),
                    plan.getTotalCost(), plan.getTotalTransfers());
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("=== Rota Karşılaştırması ===\n");
        for (TripPlan plan : plans) sb.append(plan).append("\n");
        return sb.toString();
    }
}
