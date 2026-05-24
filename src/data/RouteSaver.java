package data;

import model.DayPlan;
import model.RouteStep;
import model.TripPlan;
import model.WhatIfComparison;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/*
  RouteSaver sınıfı, oluşturulan gezi rotalarını TXT dosyasına kaydeder.

  Kayıt dosyası:
  resources/saved_routes.txt

  Bu sınıf:
  - Ana rota bilgisini
  - Toplam mesafe, süre, maliyet, aktarma sayısını
  - Günlük planı
  - What-If karşılaştırma sonucunu

  dosyaya yazabilir.
 */
public class RouteSaver {

    private String filePath;

    public RouteSaver(String filePath) {
        this.filePath = filePath;
    }

    public void saveTripPlan(String routeName,
                             TripPlan tripPlan,
                             List<DayPlan> dayPlans) throws IOException {

        if (tripPlan == null) {
            throw new IllegalArgumentException("Kaydedilecek rota boş olamaz.");
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write("========================================");
            writer.newLine();

            writer.write("Rota Adı: " + safeText(routeName));
            writer.newLine();

            writer.write("Kayıt Tarihi: " + getCurrentDateTime());
            writer.newLine();

            writer.write("----------------------------------------");
            writer.newLine();

            writer.write("Rota Özeti:");
            writer.newLine();
            writer.write(tripPlan.getRouteSummary());
            writer.newLine();

            writer.write("----------------------------------------");
            writer.newLine();

            writer.write("Rota Tercihi: " + tripPlan.getPreference().getDisplayName());
            writer.newLine();

            writer.write("Toplam Mesafe: " + String.format("%.1f", tripPlan.getTotalDistance()) + " km");
            writer.newLine();

            writer.write("Toplam Süre: " + tripPlan.getTotalTime() + " dk");
            writer.newLine();

            writer.write("Toplam Maliyet: " + String.format("%.0f", tripPlan.getTotalCost()) + " TL");
            writer.newLine();

            writer.write("Toplam Aktarma: " + tripPlan.getTotalTransfers());
            writer.newLine();

            writer.write("Zorluk Seviyesi: " + safeText(tripPlan.getDifficultyLevel()));
            writer.newLine();

            writer.write("Tahmini Gün: " + tripPlan.getEstimatedDays());
            writer.newLine();

            writer.write("Kullanılan Ulaşımlar: " + tripPlan.getUsedTransports());
            writer.newLine();

            writer.write("----------------------------------------");
            writer.newLine();

            writer.write("Ulaşım Adımları:");
            writer.newLine();

            List<RouteStep> steps = tripPlan.getSteps();

            if (steps == null || steps.isEmpty()) {
                writer.write("Ulaşım adımı bulunamadı.");
                writer.newLine();
            } else {
                for (int i = 0; i < steps.size(); i++) {
                    writer.write((i + 1) + ". " + steps.get(i).toString());
                    writer.newLine();
                }
            }

            writer.write("----------------------------------------");
            writer.newLine();

            writer.write("Günlük Plan:");
            writer.newLine();

            if (dayPlans == null || dayPlans.isEmpty()) {
                writer.write("Günlük plan oluşturulmadı.");
                writer.newLine();
            } else {
                for (DayPlan dayPlan : dayPlans) {
                    writer.write(dayPlan.toString());
                    writer.newLine();
                    writer.newLine();
                }
            }

            writer.write("========================================");
            writer.newLine();
            writer.newLine();
        }
    }

    public void saveWhatIfComparison(String title,
                                     WhatIfComparison comparison) throws IOException {

        if (comparison == null) {
            throw new IllegalArgumentException("Kaydedilecek What-If karşılaştırması boş olamaz.");
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write("========================================");
            writer.newLine();

            writer.write("What-If Kaydı: " + safeText(title));
            writer.newLine();

            writer.write("Kayıt Tarihi: " + getCurrentDateTime());
            writer.newLine();

            writer.write("----------------------------------------");
            writer.newLine();

            writer.write(comparison.getComparisonText());

            writer.write("========================================");
            writer.newLine();
            writer.newLine();
        }
    }

    private String getCurrentDateTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        return LocalDateTime.now().format(formatter);
    }

    private String safeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "-";
        }

        return text;
    }
}