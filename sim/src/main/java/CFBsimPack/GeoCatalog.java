package CFBsimPack;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Offline hometown / school geography from build-time JSON assets.
 */
public final class GeoCatalog {
    public static final class Place {
        public final String geoidfq;
        public final String name;
        public final String state;
        public final double lat;
        public final double lon;
        public final int pop;

        Place(String geoidfq, String name, String state, double lat, double lon, int pop) {
            this.geoidfq = geoidfq;
            this.name = name;
            this.state = state;
            this.lat = lat;
            this.lon = lon;
            this.pop = pop;
        }

        public String display() {
            return name + ", " + state;
        }
    }

    public static final class SchoolGeo {
        public final String abbr;
        public final String name;
        public final double lat;
        public final double lon;
        public final String city;
        public final String state;
        public final String venue;

        SchoolGeo(String abbr, String name, double lat, double lon, String city, String state, String venue) {
            this.abbr = abbr;
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            this.city = city != null ? city : "";
            this.state = state != null ? state : "";
            this.venue = venue != null ? venue : "";
        }
    }

    private static GeoCatalog INSTANCE;

    private final Map<String, Place> placesByGeoid = new HashMap<>();
    private final List<Place> places = new ArrayList<>();
    private final double[] cumWeights;
    private final Map<String, SchoolGeo> schoolsByAbbr = new HashMap<>();

    private static final Pattern OBJECT = Pattern.compile("\\{([^{}]*)\\}");
    private static final Pattern STR = Pattern.compile("\"(\\w+)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern NUM = Pattern.compile("\"(\\w+)\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");

    private GeoCatalog() {
        loadPlaces();
        loadSchools();
        cumWeights = buildWeights();
    }

    public static synchronized GeoCatalog get() {
        if (INSTANCE == null) {
            INSTANCE = new GeoCatalog();
        }
        return INSTANCE;
    }

    /** Test hook. */
    static synchronized void resetForTests() {
        INSTANCE = null;
    }

    public Place place(String geoidfq) {
        if (geoidfq == null) return null;
        return placesByGeoid.get(geoidfq);
    }

    public SchoolGeo school(String abbr) {
        if (abbr == null) return null;
        return schoolsByAbbr.get(abbr);
    }

    public Place sampleHometown(Random rng) {
        Random r = rng != null ? rng : new Random();
        if (places.isEmpty()) return null;
        double roll = r.nextDouble() * cumWeights[cumWeights.length - 1];
        int lo = 0;
        int hi = cumWeights.length - 1;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (cumWeights[mid] < roll) lo = mid + 1;
            else hi = mid;
        }
        return places.get(lo);
    }

    public static double haversineMiles(double lat1, double lon1, double lat2, double lon2) {
        double r = 3958.8;
        double p = Math.PI / 180.0;
        double a = Math.sin((lat2 - lat1) * p / 2.0) * Math.sin((lat2 - lat1) * p / 2.0)
                + Math.cos(lat1 * p) * Math.cos(lat2 * p)
                * Math.sin((lon2 - lon1) * p / 2.0) * Math.sin((lon2 - lon1) * p / 2.0);
        return 2.0 * r * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    public double miles(Player player, Team team) {
        if (player == null || team == null) return 9999;
        Place home = place(player.homeGeoid);
        if (home == null) return 9999;
        double tLat = team.latitude;
        double tLon = team.longitude;
        if (Double.isNaN(tLat) || Double.isNaN(tLon)) {
            SchoolGeo sg = school(team.abbr);
            if (sg == null) return 9999;
            tLat = sg.lat;
            tLon = sg.lon;
        }
        return haversineMiles(home.lat, home.lon, tLat, tLon);
    }

    public static double distanceMultiplier(double miles) {
        if (miles <= 50) return 0.62;
        if (miles <= 150) return 0.80;
        if (miles <= 400) return 0.95;
        if (miles <= 800) return 1.15;
        return 1.30;
    }

    public void applyHometown(Player p, Random rng) {
        if (p == null) return;
        Place place = sampleHometown(rng);
        if (place == null) return;
        p.homeGeoid = place.geoidfq;
        p.homeCity = place.name;
        p.homeState = place.state;
    }

    public void applySchoolCoords(Team team) {
        if (team == null || team.abbr == null) return;
        SchoolGeo sg = school(team.abbr);
        if (sg == null) return;
        team.latitude = sg.lat;
        team.longitude = sg.lon;
        team.venueCity = sg.city;
        team.venueState = sg.state;
    }

    private void loadPlaces() {
        String raw = readResource("places_10k.json");
        if (raw == null) return;
        Matcher m = OBJECT.matcher(raw);
        while (m.find()) {
            String body = m.group(1);
            if (!body.contains("\"geoidfq\"")) continue;
            Map<String, String> strs = stringFields(body);
            Map<String, Double> nums = numFields(body);
            if (!strs.containsKey("geoidfq") || !nums.containsKey("lat")) continue;
            Place p = new Place(
                    strs.get("geoidfq"),
                    strs.getOrDefault("name", ""),
                    strs.getOrDefault("state", ""),
                    nums.get("lat"),
                    nums.getOrDefault("lon", 0.0),
                    nums.getOrDefault("pop", 0.0).intValue());
            places.add(p);
            placesByGeoid.put(p.geoidfq, p);
        }
    }

    private void loadSchools() {
        String raw = readResource("schools_geo.json");
        if (raw == null) return;
        Matcher m = OBJECT.matcher(raw);
        while (m.find()) {
            String body = m.group(1);
            if (!body.contains("\"abbr\"")) continue;
            Map<String, String> strs = stringFields(body);
            Map<String, Double> nums = numFields(body);
            if (!strs.containsKey("abbr") || !nums.containsKey("lat")) continue;
            SchoolGeo s = new SchoolGeo(
                    strs.get("abbr"),
                    strs.getOrDefault("name", ""),
                    nums.get("lat"),
                    nums.getOrDefault("lon", 0.0),
                    strs.getOrDefault("city", ""),
                    strs.getOrDefault("state", ""),
                    strs.getOrDefault("venue", ""));
            schoolsByAbbr.put(s.abbr, s);
        }
    }

    private static Map<String, String> stringFields(String body) {
        Map<String, String> out = new HashMap<>();
        Matcher m = STR.matcher(body);
        while (m.find()) {
            out.put(m.group(1), m.group(2).replace("\\\"", "\""));
        }
        return out;
    }

    private static Map<String, Double> numFields(String body) {
        Map<String, Double> out = new HashMap<>();
        Matcher m = NUM.matcher(body);
        while (m.find()) {
            out.put(m.group(1), Double.parseDouble(m.group(2)));
        }
        return out;
    }

    private double[] buildWeights() {
        double[] cum = new double[places.size()];
        double sum = 0;
        for (int i = 0; i < places.size(); i++) {
            sum += Math.pow(Math.max(1, places.get(i).pop), 0.8);
            cum[i] = sum;
        }
        if (cum.length == 0) return new double[]{1};
        return cum;
    }

    private static String readResource(String name) {
        InputStream in = GeoCatalog.class.getClassLoader().getResourceAsStream(name);
        if (in == null) return null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
