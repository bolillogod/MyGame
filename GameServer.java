import java.io.*;
import java.net.*;
import java.util.*;

// Servidor simple que empareja a dos jugadores por batalla
public class GameServer {
    // Guardamos las sesiones activas (pares de handlers)
    private static final List<ClientHandler> waiting = new ArrayList<>();
    private static final List<ClientHandler> players = new ArrayList<>();
    private static final List<Enemy> enemies = new ArrayList<>();

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
            ClientHandler handler = new ClientHandler(clientSocket, players, enemies);
            players.add(handler);
            handler.start(); // start() porque ClientHandler extiende Thread

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
}       
    