/*
  Location sınıfı, şehirdeki gezi noktalarını temsil eden model sınıfıdır.

  Her Location nesnesi:
  - Benzersiz bir ID
  - Lokasyon adı
  - Kategori bilgisi
  - Ziyaret süresi
  - Giriş ücreti
  - Popülerlik puanı
  - Harita koordinatları

   gibi bilgileri içerir.

  Bu sınıf graph yapısındaki node (düğüm) verisini temsil eder.
 */

package model;

public class Location {

    private int id;
    private String name;
    private String category;
    private int visitTime;
    private double entryFee;
    private double rating;

    private double x;
    private double y;

    public Location(int id,
                    String name,
                    String category,
                    int visitTime,
                    double entryFee,
                    double rating,
                    double x,
                    double y) {

        this.id = id;
        this.name = name;
        this.category = category;
        this.visitTime = visitTime;
        this.entryFee = entryFee;
        this.rating = rating;
        this.x = x;
        this.y = y;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getVisitTime() {
        return visitTime;
    }

    public void setVisitTime(int visitTime) {
        this.visitTime = visitTime;
    }

    public double getEntryFee() {
        return entryFee;
    }

    public void setEntryFee(double entryFee) {
        this.entryFee = entryFee;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "Location{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", visitTime=" + visitTime +
                ", entryFee=" + entryFee +
                ", rating=" + rating +
                ", x=" + x +
                ", y=" + y +
                '}';
    }
}
