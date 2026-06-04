package util;

/**
 * UI ve test tarafında lokasyonları gruplamak için kullanılır.
 */
public enum TripTheme {

    ALL("Tümü"),
    DISTRICTS("İlçeler / Semtler"),
    HISTORICAL("Tarihi / Turistik");

    private final String displayName;

    TripTheme(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
