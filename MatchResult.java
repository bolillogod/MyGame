import java.util.Date;

// Modelo simple para almacenar resultados de una partida
public class MatchResult {
    private String winner;
    private String loser;
    private int damageDealt;
    private long durationMs;
    private Date timestamp;

    public MatchResult(String winner, String loser, int damageDealt, long durationMs) {
        this.winner = winner;
        this.loser = loser;
        this.damageDealt = damageDealt;
        this.durationMs = durationMs;
        this.timestamp = new Date();
    }

    public String getWinner() { return winner; }
    public String getLoser() { return loser; }
    public int getDamageDealt() { return damageDealt; }
    public long getDurationMs() { return durationMs; }
    public Date getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return String.format("MatchResult{winner='%s', loser='%s', damage=%d, duration=%dms}", 
                           winner, loser, damageDealt, durationMs);
    }
}