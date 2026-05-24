package algorithm;

import model.DayPlan;
import model.Location;
import model.RouteStep;
import model.TripPlan;

import java.util.ArrayList;
import java.util.List;

/*
  TripScheduler sınıfı, oluşturulan TripPlan nesnesini günlük plana böler.

  Örneğin toplam rota 650 dakika sürüyorsa ve kullanıcı günlük 360 dakika
  gezmek istiyorsa sistem bunu yaklaşık 2 günlük plana ayırır.

  Ayrıca rota zorluk seviyesini ve tahmini gün sayısını hesaplar.
 */
public class TripScheduler {

    public List<DayPlan> createDayPlans(TripPlan tripPlan, int dailyLimitMinutes) {
        if (tripPlan == null) {
            throw new IllegalArgumentException("TripPlan boş olamaz.");
        }

        if (dailyLimitMinutes <= 0) {
            throw new IllegalArgumentException("Günlük süre limiti 0'dan büyük olmalıdır.");
        }

        List<Location> locations = tripPlan.getOrderedLocations();
        List<RouteStep> steps = tripPlan.getSteps();

        List<DayPlan> dayPlans = new ArrayList<>();

        if (locations == null || locations.isEmpty()) {
            return dayPlans;
        }

        int dayNumber = 1;
        DayPlan currentDay = new DayPlan(dayNumber);

        Location firstLocation = locations.get(0);

        /*
          İlk lokasyon plana eklenir.
          Mevcut TripPlan yapısında başlangıç lokasyonunun ziyaret süresi de
          toplam süreye dahil edildiği için burada da includeVisitInfo true kullanıldı.
         */
        currentDay.addLocation(firstLocation, true);

        for (RouteStep step : steps) {
            Location nextLocation = step.getTo();

            int extraTime = step.getTime() + nextLocation.getVisitTime();

            boolean dayLimitWillBeExceeded =
                    currentDay.getTotalTime() + extraTime > dailyLimitMinutes;

            boolean currentDayHasEnoughContent =
                    currentDay.getLocations().size() > 1;

            if (dayLimitWillBeExceeded && currentDayHasEnoughContent) {
                dayPlans.add(currentDay);

                dayNumber++;
                currentDay = new DayPlan(dayNumber);

                /*
                  Yeni gün, bir önceki adımın başlangıç noktasından başlatılır.
                  Ancak bu lokasyon önceki günde zaten ziyaret edildiği için
                  ziyaret süresi ve giriş ücreti tekrar eklenmez.
                 */
                currentDay.addLocation(step.getFrom(), false);
            }

            currentDay.addStep(step);
            currentDay.addLocation(nextLocation, true);
        }

        dayPlans.add(currentDay);

        return dayPlans;
    }

    public int calculateEstimatedDays(TripPlan tripPlan, int dailyLimitMinutes) {
        if (tripPlan == null) {
            throw new IllegalArgumentException("TripPlan boş olamaz.");
        }

        if (dailyLimitMinutes <= 0) {
            return 1;
        }

        return (int) Math.ceil((double) tripPlan.getTotalTime() / dailyLimitMinutes);
    }

    public String calculateDifficultyLevel(TripPlan tripPlan) {
        if (tripPlan == null) {
            throw new IllegalArgumentException("TripPlan boş olamaz.");
        }

        int totalTime = tripPlan.getTotalTime();
        double totalDistance = tripPlan.getTotalDistance();
        int totalTransfers = tripPlan.getTotalTransfers();

        if (totalTime <= 240 && totalDistance <= 8 && totalTransfers <= 1) {
            return "Kolay";
        } else if (totalTime <= 420 && totalDistance <= 18 && totalTransfers <= 3) {
            return "Orta";
        } else {
            return "Zor";
        }
    }

    public boolean isBudgetExceeded(TripPlan tripPlan, double budget) {
        if (tripPlan == null) {
            throw new IllegalArgumentException("TripPlan boş olamaz.");
        }

        return tripPlan.getTotalCost() > budget;
    }

    public double calculateBudgetDifference(TripPlan tripPlan, double budget) {
        if (tripPlan == null) {
            throw new IllegalArgumentException("TripPlan boş olamaz.");
        }

        return tripPlan.getTotalCost() - budget;
    }

    public String createDayPlanText(List<DayPlan> dayPlans) {
        if (dayPlans == null || dayPlans.isEmpty()) {
            return "Günlük plan oluşturulamadı.";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("=== GÜNLÜK GEZİ PLANI ===\n\n");

        for (DayPlan dayPlan : dayPlans) {
            sb.append(dayPlan.toString()).append("\n\n");
        }

        return sb.toString();
    }

    public String createBudgetText(TripPlan tripPlan, double budget) {
        if (tripPlan == null) {
            throw new IllegalArgumentException("TripPlan boş olamaz.");
        }

        StringBuilder sb = new StringBuilder();

        sb.append("Bütçe: ").append(String.format("%.0f", budget)).append(" TL\n");
        sb.append("Toplam Maliyet: ").append(String.format("%.0f", tripPlan.getTotalCost())).append(" TL\n");

        double difference = calculateBudgetDifference(tripPlan, budget);

        if (difference > 0) {
            sb.append("Uyarı: Bütçeniz ")
                    .append(String.format("%.0f", difference))
                    .append(" TL aşıldı.");
        } else {
            sb.append("Rota bütçenize uygundur. Kalan bütçe: ")
                    .append(String.format("%.0f", Math.abs(difference)))
                    .append(" TL");
        }

        return sb.toString();
    }
}