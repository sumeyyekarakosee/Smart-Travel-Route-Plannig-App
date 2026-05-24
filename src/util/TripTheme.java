package util;

// Kullanıcının seçtiği gezi temasını belirler.
public enum TripTheme {

    HISTORICAL("Tarihi Gezi"),
    FOOD("Yemek Odaklı Gezi"),
    NATURE("Doğa Rotası"),
    MUSEUM("Müze Rotası"),
    COASTAL("Sahil Rotası"),
    MIXED("Karışık Rota");

    private final String displayName;

    TripTheme(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    // Konum kategorisinin bu temaya uyup uymadığını kontrol eder.
    public boolean matches(String locationCategory) {
        if (this == MIXED) return true;
        return switch (this) {
            case HISTORICAL -> locationCategory.equalsIgnoreCase("Historic");
            case MUSEUM     -> locationCategory.equalsIgnoreCase("Museum");
            case NATURE     -> locationCategory.equalsIgnoreCase("Park");
            case COASTAL    -> locationCategory.equalsIgnoreCase("District");
            case FOOD       -> locationCategory.equalsIgnoreCase("Shopping");
            default         -> false;
        };
    }

    @Override
    public String toString() { return displayName; }
}
