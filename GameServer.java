

import java.io.*;
import java.net.*;
import java.util.*;

// Servidor simple que empareja a dos jugadores por batalla
public class GameServer {
    // Guardamos las sesiones activas (pares de handlers)
    private static final List<ClientHandler> waiting = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        // Puerto donde escucha el servidor
        int port = 5000;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Servidor iniciado en puerto " + port);

        while (true) {
            // Espera conexiones entrantes
            Socket clientSocket = serverSocket.accept();
            System.out.println("Nuevo cliente conectado: " + clientSocket.getRemoteSocketAddress());

            // Crea un handler para gestionar ese cliente en un hilo separado
            ClientHandler handler = new ClientHandler(clientSocket);
            handler.start(); // start() porque ClientHandler extiende Thread

            // Guardamos en la lista de espera para emparejar
            synchronized (waiting) {
                waiting.add(handler);
                if (waiting.size() >= 2) {
                    // Emparejar los dos primeros
                    ClientHandler a = waiting.remove(0);
                    ClientHandler b = waiting.remove(0);
                    a.setOpponent(b);
                    b.setOpponent(a);
                    // Notificar a ambos que empiezan la batalla
                    a.sendMessage("MATCH_START");
                    b.sendMessage("MATCH_START");
                }
            }
        }
    }
}
