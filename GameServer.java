import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

// Servidor simple que empareja a dos jugadores por batalla
public class GameServer {
    // Guardamos las sesiones activas (pares de handlers)
    private static final List<ClientHandler> waiting = new ArrayList<>();
    private static final List<ClientHandler> players = new ArrayList<>();
    private static final List<Enemy> enemies = new ArrayList<>();
    
    // NUEVO: Lista para almacenar resultados de partidas
    private static final List<MatchResult> matchResults = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        // Inicializar enemigos
        enemies.add(new Enemy(null, "e1", 10, 5));
        enemies.add(new Enemy(null, "e2", 10, 7));
        enemies.add(new Enemy(null, "e3", 10, 6));
        enemies.add(new Enemy(null, "e4", 10, 4));
        
        // Puerto donde escucha el servidor
        int port = 5000;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Servidor iniciado en puerto " + port);

        while (true) {
            // Espera conexiones entrantes
            Socket clientSocket = serverSocket.accept();
            System.out.println("Nuevo cliente conectado: " + clientSocket.getRemoteSocketAddress());

            // Crea un handler para gestionar ese cliente en un hilo separado
            ClientHandler handler = new ClientHandler(clientSocket, players, enemies, matchResults);
            players.add(handler);
            handler.start();

            // Guardamos en la lista de espera para emparejar (PvP)
            synchronized (waiting) {
                waiting.add(handler);
                if (waiting.size() >= 2) {
                    // Emparejar los dos primeros
                    ClientHandler a = waiting.remove(0);
                    ClientHandler b = waiting.remove(0);
                    a.setOpponent(b);
                    b.setOpponent(a);
                    a.setInBattle(true);
                    b.setInBattle(true);
                    // Notificar a ambos que empiezan la batalla
                    a.sendMessage("MATCH_START");
                    b.sendMessage("MATCH_START");
                }
            }
        }
    }
    
    // NUEVO: Método para agregar resultados de partidas
    public static void addMatchResult(MatchResult result) {
        synchronized (matchResults) {
            matchResults.add(result);
            System.out.println("Resultado registrado: " + result);
        }
    }
    
    // NUEVO: Método para obtener estadísticas usando Streams
    public static String getStatistics() {
        synchronized (matchResults) {
            if (matchResults.isEmpty()) {
                return "No hay estadísticas disponibles aún.";
            }
            
            StringBuilder stats = new StringBuilder();
            stats.append("=== ESTADÍSTICAS DEL SERVIDOR ===\n");
            
            // 1) Top 3 jugadores por victorias
            Map<String, Long> winsByPlayer = matchResults.stream()
                .collect(Collectors.groupingBy(MatchResult::getWinner, Collectors.counting()));
            
            stats.append("\n--- TOP 3 POR VICTORIAS ---\n");
            winsByPlayer.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .forEach(e -> stats.append(e.getKey()).append(": ").append(e.getValue()).append(" victorias\n"));
            
            // 2) Daño promedio por partida
            double avgDamage = matchResults.stream()
                .collect(Collectors.averagingInt(MatchResult::getDamageDealt));
            stats.append("\nDaño promedio por partida: ").append(String.format("%.2f", avgDamage)).append("\n");
            
            // 3) Duración promedio de partidas
            double avgDuration = matchResults.stream()
                .collect(Collectors.averagingLong(MatchResult::getDurationMs));
            stats.append("Duración promedio: ").append(String.format("%.2f", avgDuration / 1000)).append(" segundos\n");
            
            // 4) Jugadores con daño medio > 50 (filtro con Streams)
            Map<String, Double> avgDamageByPlayer = matchResults.stream()
                .collect(Collectors.groupingBy(MatchResult::getWinner, 
                         Collectors.averagingInt(MatchResult::getDamageDealt)));
            
            stats.append("\n--- JUGADORES CON DAÑO PROMEDIO > 50 ---\n");
            avgDamageByPlayer.entrySet().stream()
                .filter(e -> e.getValue() > 50)
                .forEach(e -> stats.append(e.getKey()).append(": ").append(String.format("%.2f", e.getValue())).append(" de daño promedio\n"));
            
            // 5) Total de partidas jugadas
            stats.append("\nTotal de partidas jugadas: ").append(matchResults.size()).append("\n");
            
            return stats.toString();
        }
    }
}