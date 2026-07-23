package CFBsimPack.engine;

import CFBsimPack.Player;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Player-keyed box score accumulators for one game.
 */
public final class PlayerGameStats {
    public final Map<String, Line> byKey = new LinkedHashMap<>();

    public static String key(Player p) {
        if (p == null) return "unknown";
        return p.position + "|" + p.name + "|" + System.identityHashCode(p);
    }

    public Line line(Player p) {
        String k = key(p);
        Line line = byKey.get(k);
        if (line == null) {
            line = new Line(p);
            byKey.put(k, line);
        }
        return line;
    }

    public static final class Line {
        public final Player player;
        public int passAtt;
        public int passComp;
        public int passYards;
        public int passTd;
        public int passInt;
        public int sacks;
        public int rushAtt;
        public int rushYards;
        public int rushTd;
        public int receptions;
        public int recYards;
        public int recTd;
        public int drops;
        public int fumbles;
        public int fgMade;
        public int fgAtt;
        public int xpMade;
        public int xpAtt;

        Line(Player player) {
            this.player = player;
        }
    }
}
