
    import java.io.*;
import java.net.*;

// Maneja la comunicación con un cliente (una conexión)
public class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ClientHandler opponent; // referencia al oponente cuando esté emparejado
    private String playerName;
    private int hp = 100; // estado simple del jugador en el servidor

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    public void setOpponent(ClientHandler opp) {
        this.opponent = opp;
    }

    // Enviar mensaje al cliente
    public void sendMessage(String msg) {
        out.println(msg);
    }

    @Override
    public void run() {
        try {
            // Leemos el nombre del jugador (protocol: NAME:<nombre>)
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Recibido: " + line);

                if (line.startsWith("NAME:")) {
                    playerName = line.substring(5);
                    sendMessage("WELCOME " + playerName);
                } else if (line.equals("ATTACK") && opponent != null) {
                    // Atacar al oponente: sincronizamos para evitar condiciones de carrera
                    synchronized (opponent) {
                        opponent.hp -= 20; // daño fijo
                        opponent.sendMessage("DAMAGE:20");
                        // Si el oponente muere, notificar a ambos
                        if (opponent.hp <= 0) {
                            sendMessage("YOU_WIN");
                            opponent.sendMessage("YOU_LOSE");
                        }
                    }
                } else if (line.equals("STATUS")) {
                    sendMessage("HP:" + hp);
                } else {
                    sendMessage("UNKNOWN_CMD");
                }
            }
        } catch (IOException e) {
            System.out.println("Error en handler: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}

