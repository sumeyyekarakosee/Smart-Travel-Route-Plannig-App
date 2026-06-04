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
    private int visitTime;
    private double entryFee;
    private double rating;

    private double latitude;
    private double longitude;

    private double x;
    private double y;

    public Location(int id,
                    String name,
                    int visitTime,
                    double entryFee,
                    double rating,
                    double x,
                    double y) {

        this(id, name, visitTime, entryFee, rating,
                Double.NaN, Double.NaN,
                x, y);
    }

    public Location(int id,
                    String name,
                    int visitTime,
                    double entryFee,
                    double rating,
                    double latitude,
                    double longitude,
                    double x,
                    double y) {

        this.id = id;
        this.name = name;
        this.visitTime = visitTime;
        this.entryFee = entryFee;
        this.rating = rating;
        this.latitude = latitude;
        this.longitude = longitude;
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

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
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

    public boolean hasGeographicCoordinates() {
        return !Double.isNaN(latitude)
                && !Double.isNaN(longitude)
                && Math.abs(latitude) > 0.000001
                && Math.abs(longitude) > 0.000001;
    }

    @Override
    public String toString() {
        return name;
    }
}
