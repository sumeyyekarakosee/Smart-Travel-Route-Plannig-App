package ui.view;

import model.Location;

/**
 * Provides approximate but geographically sensible Istanbul coordinates for
 * the demo dataset.
 */
public final class IstanbulGeoProjection {

    private IstanbulGeoProjection() {
    }

    public static void project(Location location) {
        if (location == null) {
            return;
        }

        double[] coords = coordinatesFor(location.getId());
        location.setLatitude(coords[0]);
        location.setLongitude(coords[1]);
    }

    public static String categoryFor(Location location) {
        if (location == null) {
            return "Bilinmiyor";
        }
        return location.getId() <= 20 ? "İlçe / Semt" : "Tarihi / Turistik";
    }

    private static double[] coordinatesFor(int id) {
        return switch (id) {
            case 1 -> new double[]{40.9909, 29.0287};
            case 2 -> new double[]{41.0236, 29.0224};
            case 3 -> new double[]{41.0160, 28.9719};
            case 4 -> new double[]{41.0055, 28.9768};
            case 5 -> new double[]{41.0220, 28.9744};
            case 6 -> new double[]{41.0369, 28.9850};
            case 7 -> new double[]{41.0430, 29.0094};
            case 8 -> new double[]{41.0470, 29.0270};
            case 9 -> new double[]{41.0303, 28.9488};
            case 10 -> new double[]{41.0302, 29.0421};
            case 11 -> new double[]{40.9820, 29.0212};
            case 12 -> new double[]{40.9686, 29.0985};
            case 13 -> new double[]{40.9780, 28.8727};
            case 14 -> new double[]{41.0185, 28.9498};
            case 15 -> new double[]{41.0480, 28.9349};
            case 16 -> new double[]{41.1664, 29.0564};
            case 17 -> new double[]{41.0789, 29.0467};
            case 18 -> new double[]{41.0636, 29.0476};
            case 19 -> new double[]{41.1235, 29.0878};
            case 20 -> new double[]{40.9920, 29.1248};
            case 21 -> new double[]{41.0086, 28.9802};
            case 22 -> new double[]{41.0115, 28.9833};
            case 23 -> new double[]{41.0054, 28.9768};
            case 24 -> new double[]{41.0084, 28.9779};
            case 25 -> new double[]{41.0140, 28.9816};
            case 26 -> new double[]{41.0111, 28.9814};
            case 27 -> new double[]{41.0107, 28.9681};
            case 28 -> new double[]{41.0165, 28.9701};
            case 29 -> new double[]{41.0160, 28.9639};
            case 30 -> new double[]{41.0256, 28.9744};
            case 31 -> new double[]{41.0369, 28.9844};
            case 32 -> new double[]{41.0392, 29.0003};
            case 33 -> new double[]{41.0210, 29.0048};
            case 34 -> new double[]{41.0525, 28.9355};
            case 35 -> new double[]{41.0523, 28.9487};
            case 36 -> new double[]{41.0240, 29.0689};
            case 37 -> new double[]{41.0265, 29.0696};
            case 38 -> new double[]{41.0427, 29.0448};
            case 39 -> new double[]{41.0861, 29.0504};
            case 40 -> new double[]{41.0848, 29.0650};
            case 41 -> new double[]{41.0487, 28.9365};
            case 42 -> new double[]{41.0328, 28.9496};
            case 43 -> new double[]{41.0313, 28.9389};
            case 44 -> new double[]{41.0404, 28.9461};
            case 45 -> new double[]{40.9926, 28.9219};
            case 46 -> new double[]{41.1088, 29.0467};
            case 47 -> new double[]{40.9657, 29.0364};
            case 48 -> new double[]{41.0010, 29.0173};
            case 49 -> new double[]{41.0411, 29.0205};
            case 50 -> new double[]{41.0834, 29.0502};
            default -> new double[]{41.0082, 28.9784};
        };
    }
}
