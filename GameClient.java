import java.io.*;
import java.net.*;
import java.util.Scanner;

public class GameClient {
    private static boolean showMenu = true;

    public static void main(String[] args) {
        String host = "localhost"; // Cambia por la IP del servidor si es necesario
        int port = 5000;

        try {
            System.out.println("Conectando al servidor " + host + ":" + port + "...");
            Socket socket = new Socket(host, port);
            System.out.println("¡Conectado exitosamente!");

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // Hilo para leer mensajes del servidor
            Thread reader = new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        // Procesar mensajes especiales del servidor
                        if (message.startsWith("HP:")) {
                            System.out.println("💚 Vida actual: " + message.substring(3));
                        } else if (message.startsWith("HEALED:")) {
                            System.out.println("✨ Te curaste " + message.substring(7) + " puntos de vida");
                        } else if (message.equals("YOU_DIED")) {
                            System.out.println("💀 ¡HAS MUERTO! Usa HEAL para recuperarte.");
                        } else if (message.startsWith("WELCOME")) {
                            System.out.println("🎮 " + message);
                        } else if (message.startsWith("BATTLE_START:")) {
                            System.out.println("⚔️  ¡BATALLA INICIADA! " + message.substring(13));
                            showMenu = false;
                        } else if (message.equals("YOU_WIN")) {
                            System.out.println("🏆 ¡GANASTE LA BATALLA!");
                        } else if (message.equals("YOU_LOSE")) {
                            System.out.println("😞 Perdiste la batalla. ¡Mejor suerte la próxima vez!");
                        } else if (message.startsWith("CHALLENGE_REQUEST:")) {
                            String challenger = message.substring(18);
                            System.out.println("⚔️  " + challenger + " te ha desafiado a una batalla!");
                            System.out.println("   Escribe 'ACCEPT:" + challenger + "' para aceptar");
                        } else if (message.startsWith("CHALLENGE_SENT:")) {
                            System.out.println("📤 " + message.substring(15));
                        } else if (message.startsWith("YOU_ATTACKED:")) {
                            String[] parts = message.substring(13).split(":");
                            if (parts.length >= 2) {
                                System.out.println("⚔️  Atacaste a " + parts[0] + " causando " + parts[1] + " de daño");
                            }
                        } else if (message.startsWith("YOU_ATTACKED_OPPONENT:")) {
                            System.out.println("⚔️  Atacaste a tu oponente causando " + message.substring(22) + " de daño");
                        } else if (message.startsWith("ENEMY_DEFEATED:")) {
                            System.out.println("🎯 ¡Derrotaste a " + message.substring(15) + "!");
                        } else if (message.equals("VICTORY")) {
                            System.out.println("🎊 ¡VICTORIA! ¡Todos los enemigos han sido derrotados!");
                        } else if (message.equals("ALL_ENEMIES_DEFEATED")) {
                            System.out.println("🎯 Todos los enemigos ya están derrotados. Usa RESET_ENEMIES para reiniciar.");
                        } else if (message.startsWith("ARMA EQUIPADA:")) {
                            System.out.println("🗡️  " + message);
                        } else if (message.equals("ENEMIES_RESET")) {
                            System.out.println("🔄 Has reiniciado todos los enemigos");
                        } else if (message.equals("ENEMIES_HAVE_BEEN_RESET")) {
                            System.out.println("🔄 Otro jugador ha reiniciado los enemigos");
                        } else if (message.equals("CONNECTED_TO_SERVER")) {
                            System.out.println("🌟 ¡Bienvenido al juego!");
                        } else if (message.equals("TU_OPONENTE_SE_DESCONECTO")) {
                            System.out.println("⚠️  Tu oponente se desconectó. Volviendo al modo normal...");
                            showMenu = true;
                        } else if (message.startsWith("ERROR:")) {
                            System.out.println("❌ " + message);
                        } else if (message.equals("BATTLE_END:VICTORY")) {
                            System.out.println("🏆 ¡BATALLA TERMINADA! Eres el vencedor.");
                            showMenu = true;
                        } else if (message.equals("BATTLE_END:DEFEAT")) {
                            System.out.println("😞 La batalla ha terminado. ¡Mejor suerte la próxima vez!");
                            showMenu = true;
                        } else if (message.startsWith("HP_OPPONENT:")) {
                            System.out.println("💔 HP del oponente: " + message.substring(12));
                        } else if (message.equals("NO_OPPONENT")) {
                            System.out.println("❌ No tienes oponente. Usa CHALLENGE para desafiar a alguien.");
                        } else {
                            // Mensaje normal del servidor
                            System.out.println(message);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("❌ Conexión cerrada por el servidor.");
                }
            });
            reader.start();

            // Hilo principal para entrada del usuario
            Scanner sc = new Scanner(System.in);
            
            // Solicitar nombre del jugador
            System.out.print("🎮 Ingresa tu nombre de jugador: ");
            String name = sc.nextLine().trim();
            while (name.isEmpty()) {
                System.out.print("❌ El nombre no puede estar vacío. Ingresa tu nombre: ");
                name = sc.nextLine().trim();
            }
            out.println("NAME:" + name);

            System.out.println("\n⏳ Esperando confirmación del servidor...");
            Thread.sleep(1000); // Dar tiempo para recibir el mensaje de bienvenida

            // Bucle principal de comandos
            while (true) {
                if (showMenu) {
                    System.out.println("\n" + "=".repeat(50));
                    System.out.println("🎮 ¿Qué quieres hacer? (escribe el número o comando completo)");
                    System.out.println("=".repeat(50));
                }

                System.out.print(">>> ");
                String cmd = sc.nextLine().trim();
                
                if (cmd.equalsIgnoreCase("EXIT") || cmd.equals("0")) {
                    System.out.println("👋 ¡Hasta luego!");
                    break;
                }
                
                if (cmd.equalsIgnoreCase("CLEAR") || cmd.equalsIgnoreCase("CLS")) {
                    // Limpiar pantalla (funciona en la mayoría de terminales)
                    System.out.print("\033[2J\033[1;1H");
                    continue;
                }

                if (!cmd.isEmpty()) {
                    out.println(cmd);
                    
                    // Controlar cuándo mostrar el menú
                    if (cmd.equals("9") || cmd.equalsIgnoreCase("HELP") || 
                        cmd.equals("5") || cmd.equalsIgnoreCase("WEAPONS")) {
                        showMenu = false;
                    } else if (cmd.equals("3") || cmd.equalsIgnoreCase("STATUS") ||
                               cmd.equals("4") || cmd.equalsIgnoreCase("PLAYERS")) {
                        showMenu = false;
                    } else {
                        showMenu = true;
                    }
                } else {
                    System.out.println("❌ Comando vacío. Escribe '9' o 'HELP' para ver comandos disponibles.");
                }
            }

            socket.close();
            sc.close();
            System.exit(0);

        } catch (IOException e) {
            System.err.println("❌ Error de conexión: " + e.getMessage());
            System.err.println("🔧 Verifica que el servidor esté ejecutándose y la dirección IP sea correcta.");
        } catch (InterruptedException e) {
            System.err.println("❌ Proceso interrumpido.");
        }
    }
}