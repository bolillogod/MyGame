import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameServer {
    // Usar listas thread-safe para evitar problemas de concurrencia
    private static final List<ClientHandler> waiting = Collections.synchronizedList(new ArrayList<>());
    private static final List<ClientHandler> players = new CopyOnWriteArrayList<>();
    private static final List<Enemy> enemies = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        try {
            // ================= CREACIÓN DE ENEMIGOS =================
            // CORREGIDO: Crear enemigos con listas vacías (serán populadas después)
            List<Player> emptyPlayerList = new ArrayList<>(); // Lista compatible con Enemy original
            enemies.add(new Enemy(emptyPlayerList, "Orco Salvaje", 12, 5));
            enemies.add(new Enemy(emptyPlayerList, "Esqueleto Guerrero", 10, 7));
            enemies.add(new Enemy(emptyPlayerList, "Gólem de Piedra", 15, 4));
            enemies.add(new Enemy(emptyPlayerList, "Araña Gigante", 8, 8));
            enemies.add(new Enemy(emptyPlayerList, "Dragón Menor", 18, 3));

            // ================= INICIO DEL SERVIDOR =================
            int port = 5000;
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("=== SERVIDOR DE JUEGO INICIADO ===");
            System.out.println("Puerto: " + port);
            System.out.println("Esperando conexiones...");
            System.out.println("===================================");

            // Configurar shutdown hook para cerrar el servidor correctamente
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    System.out.println("\nCerrando servidor...");
                    serverSocket.close();
                } catch (IOException e) {
                    System.err.println("Error al cerrar el servidor: " + e.getMessage());
                }
            }));

            // ================= ACEPTANDO CLIENTES =================
            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Nuevo cliente conectado desde: " + 
                                     clientSocket.getRemoteSocketAddress());

                    // Crear handler para el cliente
                    ClientHandler handler = new ClientHandler(clientSocket, players, enemies);
                    players.add(handler);

                    handler.start();

                    System.out.println("Total de jugadores conectados: " + players.size());

                    // ELIMINADO: Sistema automático de emparejamiento
                    // El emparejamiento ahora se hace manualmente con CHALLENGE/ACCEPT

                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        System.err.println("Error aceptando cliente: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error iniciando el servidor: " + e.getMessage());
        }
    }

    // Método para obtener la lista de jugadores (usado por ClientHandler)
    public static List<ClientHandler> getPlayers() {
        return players;
    }

    // Método para obtener la lista de enemigos (usado por ClientHandler)  
    public static List<Enemy> getEnemies() {
        return enemies;
    }

    // Método para remover un jugador (llamado cuando se desconecta)
    public static void removePlayer(ClientHandler player) {
        players.remove(player);
        System.out.println("Jugador removido. Total conectados: " + players.size());
    }
}
