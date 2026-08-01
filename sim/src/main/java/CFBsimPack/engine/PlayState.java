package CFBsimPack.engine;

/**
 * Cloneable provisional game state for transactional play commit.
 */
public final class PlayState {
    public int down;
    public int yardsNeed;
    public int yardLine;
    public int gameTime;
    public boolean possessionHome;
    public int homeScore;
    public int awayScore;
    public GamePhase phase;

    public static PlayState from(GameState g) {
        PlayState s = new PlayState();
        s.down = g.down;
        s.yardsNeed = g.yardsNeed;
        s.yardLine = g.yardLine;
        s.gameTime = g.gameTime;
        s.possessionHome = g.possessionHome;
        s.homeScore = g.homeScore;
        s.awayScore = g.awayScore;
        s.phase = g.phase;
        return s;
    }

    public PlayState copy() {
        PlayState s = new PlayState();
        s.down = down;
        s.yardsNeed = yardsNeed;
        s.yardLine = yardLine;
        s.gameTime = gameTime;
        s.possessionHome = possessionHome;
        s.homeScore = homeScore;
        s.awayScore = awayScore;
        s.phase = phase;
        return s;
    }
}
