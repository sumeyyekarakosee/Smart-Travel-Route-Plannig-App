package model;

import java.util.ArrayList;
import java.util.List;

/*
  DayPlan sınıfı, oluşturulan gezi rotasının günlük plana bölünmüş
  halini temsil eder.

  Örneğin rota 2 gün sürecekse:
  - 1. gün hangi lokasyonlara gidilecek?
  - 2. gün hangi lokasyonlara gidilecek?
  - Günlük toplam süre, maliyet, mesafe ve aktarma sayısı nedir?

  Bu bilgiler bu sınıfta tutulur.
 */
public class DayPlan {

    private int dayNumber;
    private List<Location> locations;
    private List<RouteStep> steps;

    private int totalTime;
    private double totalCost;
    private double totalDistance;
    private int totalTransfers;

    public DayPlan(int dayNumber) {
        this.dayNumber = dayNumber;
        this.locations = new ArrayList<>();
        this.steps = new ArrayList<>();
    }

    /*
      Günlük plana lokasyon ekler.

      includeVisitInfo true ise:
      - Lokasyonun ziyaret süresi toplam süreye eklenir.
      - Lokasyonun giriş ücreti toplam maliyete eklenir.

      includeVisitInfo false ise:
      - Lokasyon sadece gün başlangıcını göstermek için eklenir.
      - Süre ve maliyet tekrar eklenmez.
     */
    public void addLocation(Location location, boolean includeVisitInfo) {
        if (location == null) {
            return;
        }

        locations.add(location);

        if (includeVisitInfo) {
            totalTime += location.getVisitTime();
            totalCost += location.getEntryFee();
        }
    }

    /*
      Günlük plana ulaşım adımı ekler.
      RouteStep içindeki ulaşım süresi, mesafe, maliyet ve aktarma bilgileri
      günlük toplam değerlere dahil edilir.
     */
    public void addStep(RouteStep step) {
        if (step == null) {
            return;
        }

        steps.add(step);

        totalDistance += step.getDistance();
        totalTime += step.getTime();
        totalCost += step.getCost();
        totalTransfers += step.getTransferCount();
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public List<Location> getLocations() {
        return locations;
    }

    public List<RouteStep> getSteps() {
        return steps;
    }

    public int getTotalTime() {
        return totalTime;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public int getTotalTransfers() {
        return totalTransfers;
    }

    public String getRouteSummary() {
        if (locations.isEmpty()) {
            return "Boş Gün Planı";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < locations.size(); i++) {
            sb.append(locations.get(i).getName());

            if (i < locations.size() - 1) {
                sb.append(" -> ");
            }
        }

        return sb.toString();
    }

    public String getStepSummary() {
        if (steps.isEmpty()) {
            return "Ulaşım adımı bulunmuyor.";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < steps.size(); i++) {
            sb.append(i + 1)
                    .append(". ")
                    .append(steps.get(i).toString())
                    .append("\n");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return dayNumber + ". Gün\n"
                + "Rota: " + getRouteSummary() + "\n"
                + "Toplam Mesafe: " + String.format("%.1f", totalDistance) + " km\n"
                + "Toplam Süre: " + totalTime + " dk\n"
                + "Toplam Maliyet: " + String.format("%.0f", totalCost) + " TL\n"
                + "Aktarma Sayısı: " + totalTransfers;
    }
}