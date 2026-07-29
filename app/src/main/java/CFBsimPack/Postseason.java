package CFBsimPack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 12-team CFP selection/bracket and conference-tied bowl matching.
 */
public final class Postseason {

    public static final int CFP_FIELD_SIZE = 12;
    public static final int AUTO_BIDS = 5;
    public static final int MIN_BOWL_WINS = 6;

    /** NY6 hosts used as CFP quarter display names (not separate non-CFP bowls). */
    public static final String[] CFP_QUARTER_HOSTS = {
            "Rose Bowl", "Sugar Bowl", "Orange Bowl", "Cotton Bowl"
    };

    /** NY6 hosts used as CFP semi display names. */
    public static final String[] CFP_SEMI_HOSTS = {
            "Fiesta Bowl", "Peach Bowl"
    };

    private Postseason() {
    }

    /** Bowl contract with preferred conference sides. */
    public static final class BowlContract {
        public final String name;
        public final String[] homePrefs;
        public final String[] awayPrefs;

        public BowlContract(String name, String[] homePrefs, String[] awayPrefs) {
            this.name = name;
            this.homePrefs = homePrefs;
            this.awayPrefs = awayPrefs;
        }
    }

    private static final String[] SEC = {"SEC"};
    private static final String[] BIG_TEN = {"Big Ten"};
    private static final String[] ACC = {"ACC"};
    private static final String[] BIG_12 = {"Big 12"};
    private static final String[] PAC_12 = {"Pac-12"};
    private static final String[] AMERICAN = {"American"};
    private static final String[] MWC = {"Mountain West"};
    private static final String[] SUN_BELT = {"Sun Belt"};
    private static final String[] MAC = {"MAC"};
    private static final String[] CUSA = {"Conference USA"};
    private static final String[] B1G_OR_PAC = {"Big Ten", "Pac-12"};
    private static final String[] G5_EAST = {"American", "MAC", "Conference USA"};
    private static final String[] G5_SOUTH = {"Sun Belt", "American", "Conference USA"};
    private static final String[] G5_WEST = {"Mountain West", "Pac-12", "American"};

    /**
     * Prestige-ordered non-CFP bowls (NY6 CFP hosts are reserved for playoff rounds).
     */
    public static final BowlContract[] BOWL_CONTRACTS = {
            new BowlContract("Alamo Bowl", BIG_12, B1G_OR_PAC),
            new BowlContract("Citrus Bowl", SEC, BIG_TEN),
            new BowlContract("ReliaQuest Bowl", BIG_TEN, SEC),
            new BowlContract("Gator Bowl", SEC, new String[]{"Big Ten", "ACC"}),
            new BowlContract("Music City Bowl", SEC, BIG_TEN),
            new BowlContract("Texas Bowl", BIG_12, SEC),
            new BowlContract("Holiday Bowl", PAC_12, BIG_TEN),
            new BowlContract("Liberty Bowl", BIG_12, SEC),
            new BowlContract("Duke's Mayo Bowl", ACC, SEC),
            new BowlContract("LA Bowl", PAC_12, MWC),
            new BowlContract("Military Bowl", ACC, AMERICAN),
            new BowlContract("Armed Forces Bowl", BIG_12, AMERICAN),
            new BowlContract("Fenway Bowl", ACC, AMERICAN),
            new BowlContract("Birmingham Bowl", AMERICAN, SEC),
            new BowlContract("Gasparilla Bowl", AMERICAN, ACC),
            new BowlContract("Pinstripe Bowl", ACC, BIG_TEN),
            new BowlContract("Pop-Tarts Bowl", ACC, BIG_12),
            new BowlContract("Sun Bowl", ACC, PAC_12),
            new BowlContract("Independence Bowl", AMERICAN, CUSA),
            new BowlContract("New Mexico Bowl", MWC, CUSA),
            new BowlContract("Famous Idaho Potato Bowl", MWC, MAC),
            new BowlContract("Arizona Bowl", MWC, MAC),
            new BowlContract("Bahamas Bowl", CUSA, MAC),
            new BowlContract("Boca Raton Bowl", AMERICAN, MAC),
            new BowlContract("Cure Bowl", SUN_BELT, AMERICAN),
            new BowlContract("New Orleans Bowl", SUN_BELT, CUSA),
            new BowlContract("Frisco Bowl", AMERICAN, CUSA),
            new BowlContract("Myrtle Beach Bowl", SUN_BELT, G5_EAST),
            new BowlContract("68 Ventures Bowl", SUN_BELT, G5_SOUTH),
            new BowlContract("Hawaii Bowl", MWC, G5_WEST),
    };

    /** Result of CFP selection: poll-seeded field plus which teams were automatic bids. */
    public static final class CfpSelection {
        public final List<Team> field;
        public final Set<Team> autoBids;

        public CfpSelection(List<Team> field, Set<Team> autoBids) {
            this.field = field;
            this.autoBids = autoBids;
        }
    }

    /**
     * Selects 5 auto-bid conference champions + 7 at-large, seeded 1–12 by poll.
     */
    public static CfpSelection selectCfpField(List<Team> teamList) {
        List<Team> champs = new ArrayList<>();
        for (Team t : teamList) {
            if ("CC".equals(t.confChampion)) {
                champs.add(t);
            }
        }
        sortByPoll(champs);
        List<Team> auto = new ArrayList<>();
        for (int i = 0; i < champs.size() && auto.size() < AUTO_BIDS; i++) {
            auto.add(champs.get(i));
        }

        Set<Team> selected = new HashSet<>(auto);
        List<Team> byPoll = new ArrayList<>(teamList);
        sortByPoll(byPoll);
        List<Team> atLarge = new ArrayList<>();
        for (Team t : byPoll) {
            if (selected.contains(t)) {
                continue;
            }
            atLarge.add(t);
            selected.add(t);
            if (auto.size() + atLarge.size() >= CFP_FIELD_SIZE) {
                break;
            }
        }

        List<Team> field = new ArrayList<>(auto.size() + atLarge.size());
        field.addAll(auto);
        field.addAll(atLarge);
        sortByPoll(field);
        if (field.size() > CFP_FIELD_SIZE) {
            field = new ArrayList<>(field.subList(0, CFP_FIELD_SIZE));
        }
        return new CfpSelection(field, new HashSet<>(auto));
    }

    /** First-round pairings: 5v12, 6v11, 7v10, 8v9 (higher seed home). */
    public static Game[] scheduleFirstRound(List<Team> seeds) {
        if (seeds.size() < CFP_FIELD_SIZE) {
            return new Game[0];
        }
        int[][] pairs = {{4, 11}, {5, 10}, {6, 9}, {7, 8}};
        Game[] games = new Game[pairs.length];
        for (int i = 0; i < pairs.length; i++) {
            Team home = seeds.get(pairs[i][0]);
            Team away = seeds.get(pairs[i][1]);
            int homeSeed = pairs[i][0] + 1;
            int awaySeed = pairs[i][1] + 1;
            String name = "CFP First Round, " + homeSeed + "v" + awaySeed;
            games[i] = new Game(home, away, name);
            home.gameSchedule.add(games[i]);
            away.gameSchedule.add(games[i]);
        }
        return games;
    }

    /**
     * Reseed remaining teams by original CFP seed order and pair highest vs lowest.
     *
     * @param remaining teams still alive (byes + winners)
     * @param seedOrder original 1–12 field for seed lookup
     * @param hostNames bowl host labels for each game
     * @param roundSuffix e.g. "CFP Quarter" or "CFP Semi"
     */
    public static Game[] scheduleReseededRound(
            List<Team> remaining,
            List<Team> seedOrder,
            String[] hostNames,
            String roundSuffix) {
        List<Team> ordered = new ArrayList<>(remaining);
        ordered.sort(Comparator.comparingInt(t -> seedIndex(seedOrder, t)));
        int gamesCount = ordered.size() / 2;
        Game[] games = new Game[gamesCount];
        for (int i = 0; i < gamesCount; i++) {
            Team home = ordered.get(i);
            Team away = ordered.get(ordered.size() - 1 - i);
            String host = (hostNames != null && i < hostNames.length)
                    ? hostNames[i]
                    : "CFP";
            String name = host + " (" + roundSuffix + ")";
            games[i] = new Game(home, away, name);
            home.gameSchedule.add(games[i]);
            away.gameSchedule.add(games[i]);
        }
        return games;
    }

    public static Game scheduleNcg(Team a, Team b, List<Team> seedOrder) {
        Team home;
        Team away;
        if (seedIndex(seedOrder, a) <= seedIndex(seedOrder, b)) {
            home = a;
            away = b;
        } else {
            home = b;
            away = a;
        }
        Game ncg = new Game(home, away, "NCG");
        home.gameSchedule.add(ncg);
        away.gameSchedule.add(ncg);
        return ncg;
    }

    /**
     * Greedy conference-tied bowl matching for 6-win teams outside the CFP field.
     */
    public static Game[] matchBowls(List<Team> teamList, Set<Team> cfpField) {
        List<Team> pool = new ArrayList<>();
        for (Team t : teamList) {
            if (cfpField.contains(t)) {
                continue;
            }
            if (t.wins >= MIN_BOWL_WINS) {
                pool.add(t);
            }
        }
        sortByPoll(pool);

        List<Game> games = new ArrayList<>();
        Set<Team> used = new HashSet<>();
        for (BowlContract contract : BOWL_CONTRACTS) {
            if (pool.size() - used.size() < 2) {
                break;
            }
            Team home = pickPreferred(pool, used, contract.homePrefs, null);
            if (home == null) {
                continue;
            }
            used.add(home);
            Team away = pickPreferred(pool, used, contract.awayPrefs, home.conference);
            if (away == null) {
                used.remove(home);
                continue;
            }
            used.add(away);
            Game g = new Game(home, away, contract.name);
            home.gameSchedule.add(g);
            away.gameSchedule.add(g);
            games.add(g);
        }
        return games.toArray(new Game[0]);
    }

    /** Plays a CFP game, applies tags/counters, returns winner. */
    public static Team playCfpGame(Game g, String winTag, String loseTag) {
        g.playGame();
        if (!g.hasPlayed || !g.isDecided()) {
            throw new IllegalStateException("CFP game did not produce a winner");
        }
        Team winner = g.winningTeam();
        Team loser = g.losingTeam();
        winner.semiFinalWL = winTag;
        loser.semiFinalWL = loseTag;
        winner.totalBowls++;
        loser.totalBowlLosses++;
        return winner;
    }

    /** Plays a non-CFP bowl and tags BW/BL. */
    public static void playBowl(Game g) {
        g.playGame();
        if (!g.hasPlayed || !g.isDecided()) {
            return;
        }
        if (g.homeWon()) {
            g.homeTeam.semiFinalWL = "BW";
            g.awayTeam.semiFinalWL = "BL";
            g.homeTeam.totalBowls++;
            g.awayTeam.totalBowlLosses++;
        } else {
            g.homeTeam.semiFinalWL = "BL";
            g.awayTeam.semiFinalWL = "BW";
            g.homeTeam.totalBowlLosses++;
            g.awayTeam.totalBowls++;
        }
    }

    public static List<Team> winnersOf(Game[] games) {
        List<Team> winners = new ArrayList<>();
        if (games == null) {
            return winners;
        }
        for (Game g : games) {
            if (g == null || !g.hasPlayed || !g.isDecided()) {
                continue;
            }
            winners.add(g.winningTeam());
        }
        return winners;
    }

    public static List<Team> byesPlusWinners(List<Team> seeds, Game[] firstRound) {
        List<Team> alive = new ArrayList<>();
        for (int i = 0; i < Math.min(4, seeds.size()); i++) {
            alive.add(seeds.get(i));
        }
        alive.addAll(winnersOf(firstRound));
        return alive;
    }

    static int seedIndex(List<Team> seedOrder, Team t) {
        int idx = seedOrder.indexOf(t);
        return idx < 0 ? Integer.MAX_VALUE : idx;
    }

    static void sortByPoll(List<Team> teams) {
        Collections.sort(teams, new TeamCompPoll());
    }

    private static Team pickPreferred(
            List<Team> pool,
            Set<Team> used,
            String[] prefs,
            String avoidConference) {
        if (prefs != null) {
            for (String conf : prefs) {
                for (Team t : pool) {
                    if (used.contains(t)) {
                        continue;
                    }
                    if (conf.equals(t.conference)) {
                        if (avoidConference != null && avoidConference.equals(t.conference)) {
                            continue;
                        }
                        return t;
                    }
                }
            }
        }
        for (Team t : pool) {
            if (used.contains(t)) {
                continue;
            }
            if (avoidConference != null && avoidConference.equals(t.conference)) {
                continue;
            }
            return t;
        }
        for (Team t : pool) {
            if (!used.contains(t)) {
                return t;
            }
        }
        return null;
    }
}
