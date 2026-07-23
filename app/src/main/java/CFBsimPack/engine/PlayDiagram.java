package CFBsimPack.engine;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Relative 0–1 field diagram geometry for Canvas rendering.
 * Offense faces +X (left = LOS backfield, right = downfield).
 */
public final class PlayDiagram {
    public final float snapX;
    public final float snapY;
    public final List<Route> routes;
    public final List<Zone> zones;

    public PlayDiagram(float snapX, float snapY, List<Route> routes, List<Zone> zones) {
        this.snapX = snapX;
        this.snapY = snapY;
        this.routes = routes != null ? Collections.unmodifiableList(routes) : Collections.emptyList();
        this.zones = zones != null ? Collections.unmodifiableList(zones) : Collections.emptyList();
    }

    public static PlayDiagram offense(Route... routes) {
        return new PlayDiagram(0.28f, 0.50f, Arrays.asList(routes), Collections.emptyList());
    }

    public static PlayDiagram defense(Zone... zones) {
        return new PlayDiagram(0.35f, 0.50f, Collections.emptyList(), Arrays.asList(zones));
    }

    public static final class Route {
        public final String label;
        /** 0 = primary (orange), 1 = secondary (green), 2 = blocker (gray). */
        public final int style;
        public final float[] xs;
        public final float[] ys;

        public Route(String label, int style, float[] xs, float[] ys) {
            this.label = label != null ? label : "";
            this.style = style;
            this.xs = xs != null ? xs : new float[0];
            this.ys = ys != null ? ys : new float[0];
        }

        public static Route of(String label, int style, float... xyPairs) {
            int n = xyPairs.length / 2;
            float[] xs = new float[n];
            float[] ys = new float[n];
            for (int i = 0; i < n; i++) {
                xs[i] = xyPairs[i * 2];
                ys[i] = xyPairs[i * 2 + 1];
            }
            return new Route(label, style, xs, ys);
        }
    }

    public static final class Zone {
        public final float cx;
        public final float cy;
        public final float radius;
        /** 0 = deep blue, 1 = curl yellow, 2 = flat green. */
        public final int style;

        public Zone(float cx, float cy, float radius, int style) {
            this.cx = cx;
            this.cy = cy;
            this.radius = radius;
            this.style = style;
        }
    }
}
