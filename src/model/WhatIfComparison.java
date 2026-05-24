package model;

/*
  WhatIfComparison sınıfı, What-If simülasyonu sonucunda
  eski rota ile yeni rotayı karşılaştırmak için kullanılır.

  Örneğin:
  - Kullanıcı bir lokasyonu çıkarır.
  - Ya da yeni bir lokasyon ekler.
  - Sistem eski rota ve yeni rota arasındaki farkları hesaplar.

  Bu sınıf özellikle:
  - Mesafe farkı
  - Süre farkı
  - Maliyet farkı
  - Aktarma farkı
  - Gün farkı

  bilgilerini tutar.
 */
public class WhatIfComparison {

    private TripPlan oldPlan;
    private TripPlan newPlan;

    private String actionType;
    private String changedLocationName;

    private double distanceDifference;
    private int timeDifference;
    private double costDifference;
    private int transferDifference;
    private int dayDifference;

    public WhatIfComparison(TripPlan oldPlan,
                            TripPlan newPlan,
                            String actionType,
                            String changedLocationName,
                            int dailyMinutes) {

        this.oldPlan = oldPlan;
        this.newPlan = newPlan;
        this.actionType = actionType;
        this.changedLocationName = changedLocationName;

        calculateDifferences(dailyMinutes);
    }

    private void calculateDifferences(int dailyMinutes) {
        distanceDifference = newPlan.getTotalDistance() - oldPlan.getTotalDistance();
        timeDifference = newPlan.getTotalTime() - oldPlan.getTotalTime();
        costDifference = newPlan.getTotalCost() - oldPlan.getTotalCost();
        transferDifference = newPlan.getTotalTransfers() - oldPlan.getTotalTransfers();

        int oldDays = calculateDays(oldPlan.getTotalTime(), dailyMinutes);
        int newDays = calculateDays(newPlan.getTotalTime(), dailyMinutes);

        dayDifference = newDays - oldDays;
    }

    private int calculateDays(int totalTime, int dailyMinutes) {
        if (dailyMinutes <= 0) {
            return 1;
        }

        return (int) Math.ceil((double) totalTime / dailyMinutes);
    }

    public TripPlan getOldPlan() {
        return oldPlan;
    }

    public TripPlan getNewPlan() {
        return newPlan;
    }

    public String getActionType() {
        return actionType;
    }

    public String getChangedLocationName() {
        return changedLocationName;
    }

    public double getDistanceDifference() {
        return distanceDifference;
    }

    public int getTimeDifference() {
        return timeDifference;
    }

    public double getCostDifference() {
        return costDifference;
    }

    public int getTransferDifference() {
        return transferDifference;
    }

    public int getDayDifference() {
        return dayDifference;
    }

    public String getComparisonText() {
        StringBuilder sb = new StringBuilder();

        sb.append("=== WHAT-IF ROTA KARŞILAŞTIRMASI ===\n\n");

        sb.append("İşlem Türü: ").append(actionType).append("\n");
        sb.append("Değiştirilen Lokasyon: ").append(changedLocationName).append("\n\n");

        sb.append("Eski Rota:\n");
        sb.append(oldPlan.getRouteSummary()).append("\n\n");

        sb.append("Yeni Rota:\n");
        sb.append(newPlan.getRouteSummary()).append("\n\n");

        sb.append("Karşılaştırma:\n");
        sb.append("Mesafe Değişimi: ").append(formatDouble(distanceDifference)).append(" km\n");
        sb.append("Süre Değişimi: ").append(formatInt(timeDifference)).append(" dk\n");
        sb.append("Maliyet Değişimi: ").append(formatDouble(costDifference)).append(" TL\n");
        sb.append("Aktarma Değişimi: ").append(formatInt(transferDifference)).append("\n");
        sb.append("Gün Değişimi: ").append(formatInt(dayDifference)).append(" gün\n");

        return sb.toString();
    }

    private String formatDouble(double value) {
        if (value > 0) {
            return "+" + String.format("%.1f", value);
        }

        return String.format("%.1f", value);
    }

    private String formatInt(int value) {
        if (value > 0) {
            return "+" + value;
        }

        return String.valueOf(value);
    }

    @Override
    public String toString() {
        return getComparisonText();
    }
}