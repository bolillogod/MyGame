import java.io.*;
import java.net.*;
import java.util.Scanner;

public class GameClient {
    public static void main(String[] args) throws IOException {
        String host = "localhost";
        int port = 5000;

        Socket socket = new Socket(host, port);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        Thread reader = new Thread(() -> {
            try {
                String s;
                while ((s = in.readLine()) != null) {
                    if (s.equals("SERVER_STATISTICS")) {
                        System.out.println("\n=== ESTADÍSTICAS DEL SERVIDOR ===");
                    } else if (s.startsWith("STAT:")) {
                        System.out.println(s.substring(5));
                    } else if (s.equals("END_STATS")) {
                        System.out.println("==================================\n");
                    } else if (s.equals("PERSONAL_RANKING")) {
                        System.out.println("\n=== TU RANKING ===");
                    } else if (s.startsWith("RANK:")) {
                        System.out.println(s.substring(5));
                    } else if (s.equals("END_RANKING")) {
                        System.out.println("==================\n");
                    } else if (s.startsWith("WEAPON_LIST:")) {
                        System.out.println("=== ARMAS DISPONIBLES ===");
                        String[] weapons = s.substring(12).split(";");
                        for (String weapon : weapons) {
                            System.out.println(weapon);
                        }
                    } else if (s.startsWith("ELIGE_WEAPON:")) {
                        System.out.println(s.substring(13));
                    } else {
                        System.out.println("[SERVER] " + s);
                    }
                }
            } catch (IOException e) {
                System.out.println("Conexión cerrada.");
            }
        });
        reader.start();

        Scanner sc = new Scanner(System.in);
        System.out.print("Tu nombre: ");
        String name = sc.nextLine();
        out.println("NAME:" + name);

        System.out.println("Comandos: ATTACK, HEAL, STATUS, PLAYERS, WEAPONS, EQUIP:nombre, CHALLENGE:nombre, STATS, RANKING, RESET_ENEMIES, HELP, EXIT");

        while (true) {
            System.out.print("Comando: ");
            String cmd = sc.nextLine().trim();
            
            if (cmd.equalsIgnoreCase("EXIT")) {
                break;
            } else if (cmd.matches("\\d+")) {
                out.println("CHOOSE_WEAPON:" + cmd);
            } else {
                out.println(cmd.toUpperCase());
            }
        }

        socket.close();
        sc.close();
    }
}