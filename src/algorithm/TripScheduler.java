package algorithm;

import model.DayPlan;
import model.Location;
import model.RouteStep;
import model.TripPlan;

import java.util.ArrayList;
import java.util.List;

/*
  TripScheduler sinifi, olusturulan TripPlan nesnesini gunluk plana boler.

  Gunluk limit saat bazinda alinir. Bu sinif, gun planlarini
  tahmini gun sayisiyla ayni sayida olacak sekilde dagitir.
 */
public class TripScheduler {

    public List<DayPlan> createDayPlans(TripPlan tripPlan, int dailyLimitHours) {
        if (tripPlan == null) {
            throw new IllegalArgumentException("TripPlan bos olamaz.");
        }

        if (dailyLimitHours <= 0) {
            throw new IllegalArgumentException("Gunluk sure limiti 0'dan buyuk olmalidir.");
        }

        List<Location> locations = tripPlan.getOrderedLocations();
        List<RouteStep> steps = tripPlan.getSteps();
        List<DayPlan> dayPlans = new ArrayList<>();

        if (locations == null || locations.isEmpty()) {
            return dayPlans;
        }

        int estimatedDays = tripPlan.getEstimatedDays();
        if (estimatedDays <= 0) {
            estimatedDays = calculateEstimatedDays(tripPlan, dailyLimitHours);
        }

        int dayCount = Math.max(1, Math.min(estimatedDays, locations.size()));
        if (dayCount == 1) {
            dayPlans.add(buildSingleDayPlan(1, locations, steps));
            return dayPlans;
        }

        List<Integer> cutPoints = calculateCutPoints(tripPlan, dayCount);

        int locationStartIndex = 0;

        for (int dayNumber = 1; dayNumber <= dayCount; dayNumber++) {
            int locationEndIndex = dayNumber == dayCount
                    ? locations.size() - 1
                    : cutPoints.get(dayNumber - 1);

            DayPlan dayPlan = new DayPlan(dayNumber);
            Location firstLocation = locations.get(locationStartIndex);
            dayPlan.addLocation(firstLocation, dayNumber == 1);

            for (int locIndex = locationStartIndex; locIndex < locationEndIndex; locIndex++) {
                if (locIndex < steps.size()) {
                    RouteStep step = steps.get(locIndex);
                    dayPlan.addStep(step);
                }

                Location nextLocation = locations.get(locIndex + 1);
                dayPlan.addLocation(nextLocation, true);
            }

            dayPlans.add(dayPlan);
            locationStartIndex = locationEndIndex;
        }

        return dayPlans;
    }

    public int calculateEstimatedDays(TripPlan tripPlan, int dailyLimitHours) {
        if (tripPlan == null) {
            throw new IllegalArgumentException("TripPlan bos olamaz.");
        }

        if (dailyLimitHours <= 0) {
            return 1;
        }

        int dailyLimitMinutes = dailyLimitHours * 60;
        return (int) Math.ceil((double) tripPlan.getTotalTime() / dailyLimitMinutes);
    }

    public String calculateDifficultyLevel(TripPlan tripPlan) {
        if (tripPlan == null) {
            throw new IllegalArgumentException("TripPlan bos olamaz.");
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
            throw new IllegalArgumentException("TripPlan bos olamaz.");
        }

        return tripPlan.getTotalCost() > budget;
    }

    public double calculateBudgetDifference(TripPlan tripPlan, double budget) {
        if (tripPlan == null) {
            throw new IllegalArgumentException("TripPlan bos olamaz.");
        }

        return tripPlan.getTotalCost() - budget;
    }

    public String createDayPlanText(List<DayPlan> dayPlans) {
        if (dayPlans == null || dayPlans.isEmpty()) {
            return "Gunluk plan olusturulamadi.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== GUNLUK GEZI PLANI ===\n\n");

        for (DayPlan dayPlan : dayPlans) {
            sb.append(dayPlan.toString()).append("\n\n");
        }

        return sb.toString();
    }

    public String createBudgetText(TripPlan tripPlan, double budget) {
        if (tripPlan == null) {
            throw new IllegalArgumentException("TripPlan bos olamaz.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Butce: ").append(String.format("%.0f", budget)).append(" TL\n");
        sb.append("Toplam Maliyet: ").append(String.format("%.0f", tripPlan.getTotalCost())).append(" TL\n");

        double difference = calculateBudgetDifference(tripPlan, budget);

        if (difference > 0) {
            sb.append("Uyari: Butceniz ")
                    .append(String.format("%.0f", difference))
                    .append(" TL asildi.");
        } else {
            sb.append("Rota butcenize uygundur. Kalan butce: ")
                    .append(String.format("%.0f", Math.abs(difference)))
                    .append(" TL");
        }

        return sb.toString();
    }

    private DayPlan buildSingleDayPlan(int dayNumber, List<Location> locations, List<RouteStep> steps) {
        DayPlan dayPlan = new DayPlan(dayNumber);
        if (locations.isEmpty()) {
            return dayPlan;
        }

        dayPlan.addLocation(locations.get(0), true);

        for (int i = 0; i < steps.size(); i++) {
            RouteStep step = steps.get(i);
            dayPlan.addStep(step);
            if (i + 1 < locations.size()) {
                dayPlan.addLocation(locations.get(i + 1), true);
            }
        }

        return dayPlan;
    }

    private List<Integer> calculateCutPoints(TripPlan tripPlan, int dayCount) {
        List<Location> locations = tripPlan.getOrderedLocations();
        List<RouteStep> steps = tripPlan.getSteps();
        List<Integer> cutPoints = new ArrayList<>();

        int totalTime = Math.max(1, tripPlan.getTotalTime());
        double targetSlice = (double) totalTime / dayCount;

        int lastCut = -1;

        for (int dayIndex = 1; dayIndex < dayCount; dayIndex++) {
            int desiredTime = (int) Math.ceil(targetSlice * dayIndex);
            int maxCut = locations.size() - (dayCount - dayIndex) - 1;
            int chosenCut = maxCut;

            int runningTime = 0;
            for (int i = 0; i < locations.size() - 1; i++) {
                if (i == 0) {
                    runningTime += locations.get(0).getVisitTime();
                }

                if (i < steps.size()) {
                    runningTime += steps.get(i).getTime();
                }
                runningTime += locations.get(i + 1).getVisitTime();

                if (i > lastCut && runningTime >= desiredTime) {
                    chosenCut = Math.min(i, maxCut);
                    break;
                }
            }

            if (chosenCut <= lastCut) {
                chosenCut = Math.min(lastCut + 1, maxCut);
            }

            if (chosenCut < 0) {
                chosenCut = 0;
            }

            cutPoints.add(chosenCut);
            lastCut = chosenCut;
        }

        return cutPoints;
    }
}
