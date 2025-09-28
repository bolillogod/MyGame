
import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler extends Thread {

    // LISTA DE ARMAS DISPONIBLES EN EL JUEGO (estática para que sea compartida por todas las instancias)
    private static final List<Weapons> availableWeapons = Arrays.asList(
            new Weapons("una yuca", 20),
            new Weapons("el poder de la amistad", 15),
            new Weapons("machete oxidado", 25),
            new Weapons("$800 de cebollin", 18),
            new Weapons("hueso de pollo", 22)
    );

    // VARIABLES DE CONEXIÓN Y COMUNICACIÓN
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ClientHandler opponent;
    private String playerName;
    private int hp = 100;
    private int maxHp = 100;
    private Weapons weapon;
    private boolean inBattle = false;
    private List<Enemy> enemies;
    private List<ClientHandler> players;
    private Random rand = new Random();
    private boolean inWeaponMenu = false;

    // CONSTRUCTOR
    public ClientHandler(Socket socket, List<ClientHandler> players, List<Enemy> enemies) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.players = players;
        this.enemies = enemies;
    }

    // MÉTODOS GETTER Y SETTER
    public void setOpponent(ClientHandler opp) {
        this.opponent = opp;
    }

    public void setInBattle(boolean inBattle) {
        this.inBattle = inBattle;
    }

    public void sendMessage(String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getHp() {
        return hp;
    }

    public Weapons getWeapon() {
        return weapon;
    }

    public void setWeapon(Weapons weapon) {
        this.weapon = weapon;
    }

    // MÉTODOS DE COMBATE
    public void takeDamage(int amount) {
        hp -= amount;
        if (hp <= 0) {
            hp = 0;
            sendMessage("YOU_DIED");
        }
        sendMessage("HP:" + hp);
    }

    public void heal(int amount) {
        hp += amount;
        if (hp > maxHp) {
            hp = maxHp;
        }
        sendMessage("HEALED:" + amount);
        sendMessage("HP:" + hp);
    }

    // MÉTODO PARA EQUIPAR ARMAS
    private void equipWeapon(String weaponName) {
        for (Weapons w : availableWeapons) {
            if (w.getName().equalsIgnoreCase(weaponName)) {
                this.weapon = w;
                sendMessage("ARMA EQUIPADA: " + weapon.getName() + " (Daño: " + weapon.getDamage() + ")");
                System.out.println(playerName + " equipó: " + weapon.getName());
                return;
            }
        }
        sendMessage("ERROR: Arma no encontrada. Usa '5' o 'WEAPONS' para ver las disponibles.");
    }

    // MÉTODO PARA REINICIAR ENEMIGOS
    private void resetEnemies() {
        if (enemies == null) {
            return;
        }

        for (Enemy enemy : enemies) {
            enemy.reset();
        }
        sendMessage("ENEMIES_RESET");

        // Notificar a todos los jugadores excepto al actual
        for (ClientHandler player : players) {
            if (player != this) {
                player.sendMessage("ENEMIES_HAVE_BEEN_RESET");
            }
        }
    }

    // MÉTODO PARA ATACAR ENEMIGOS
    private void attackEnemy() {
        if (enemies == null || enemies.isEmpty()) {
            sendMessage("NO_ENEMIES");
            return;
        }

        // Crear lista de enemigos vivos
        List<Enemy> aliveEnemies = new ArrayList<>();
        for (Enemy enemy : enemies) {
            if (enemy.estaVivo()) {
                aliveEnemies.add(enemy);
            }
        }

        if (aliveEnemies.isEmpty()) {
            sendMessage("ALL_ENEMIES_DEFEATED");
            return;
        }

        // Seleccionar enemigo aleatorio
        Enemy target = aliveEnemies.get(rand.nextInt(aliveEnemies.size()));
        int damage = weapon != null ? weapon.getDamage() : 10;

        target.takeDamage(damage);
        sendMessage("YOU_ATTACKED:" + target.getEnemyName() + ":" + damage);

        // Verificar si el enemigo fue derrotado
        if (!target.estaVivo()) {
            sendMessage("ENEMY_DEFEATED:" + target.getEnemyName());

            // Verificar si todos los enemigos están derrotados
            boolean allDefeated = true;
            for (Enemy enemy : enemies) {
                if (enemy.estaVivo()) {
                    allDefeated = false;
                    break;
                }
            }

            if (allDefeated) {
                for (ClientHandler player : players) {
                    player.sendMessage("VICTORY");
                    player.sendMessage("USE_**RESET_ENEMIES**_TO_RESTART");
                }
            }
        }
    }

    // MÉTODO PARA ATACAR JUGADORES
    private void attackPlayer() {
        if (opponent == null) {
            sendMessage("NO_OPPONENT");
            return;
        }

        int damage = weapon != null ? weapon.getDamage() : 10;
        opponent.takeDamage(damage);
        sendMessage("YOU_ATTACKED_OPPONENT:" + damage);

        if (opponent.getHp() <= 0) {
            sendMessage("YOU_WIN");
            opponent.sendMessage("YOU_LOSE");
            // Finalizar batalla
            this.inBattle = false;
            opponent.inBattle = false;
            this.opponent = null;
            opponent.opponent = null;
        }
    }

    // MÉTODO PARA DESAFIAR JUGADORES - CORREGIDO
    private void challengePlayer(String targetName) {
        if (targetName == null || targetName.trim().isEmpty()) {
            sendMessage("ERROR: Debes especificar el nombre del jugador a desafiar");
            sendMessage("Uso: CHALLENGE:nombre_del_jugador");
            return;
        }

        boolean found = false;
        for (ClientHandler player : players) {
            if (player.getPlayerName() != null && 
                player.getPlayerName().equalsIgnoreCase(targetName.trim()) && 
                player != this) {
                if (player.inBattle) {
                    sendMessage("ERROR: " + targetName + " ya está en batalla");
                } else {
                    player.sendMessage("CHALLENGE_REQUEST:" + playerName);
                    sendMessage("CHALLENGE_SENT:" + targetName + " - Esperando respuesta...");
                }
                found = true;
                break;
            }
        }
        if (!found) {
            sendMessage("ERROR: Jugador '" + targetName + "' no encontrado o no disponible");
        }
    }

    // MÉTODO PARA ACEPTAR DESAFÍOS - CORREGIDO
    private void acceptChallenge(String challengerName) {
        if (challengerName == null || challengerName.trim().isEmpty()) {
            sendMessage("ERROR: Debes especificar el nombre del jugador que te desafió");
            sendMessage("Uso: ACCEPT:nombre_del_jugador");
            return;
        }

        boolean found = false;
        for (ClientHandler player : players) {
            if (player.getPlayerName() != null && 
                player.getPlayerName().equalsIgnoreCase(challengerName.trim())) {
                this.setOpponent(player);
                player.setOpponent(this);
                this.setInBattle(true);
                player.setInBattle(true);
                sendMessage("BATTLE_START:" + challengerName + " - ¡Que comience la batalla!");
                player.sendMessage("BATTLE_START:" + playerName + " - ¡Que comience la batalla!");
                found = true;
                break;
            }
        }
        if (!found) {
            sendMessage("ERROR: Jugador '" + challengerName + "' no encontrado");
        }
    }

    // MÉTODO PRINCIPAL DEL HILO - CORREGIDO
    @Override
    public void run() {
        try {
            sendMessage("CONNECTED_TO_SERVER");
            sendMainMenu();

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Recibido de " + (playerName != null ? playerName : "cliente") + ": " + line);

                // Manejar menú de armas
                if (inWeaponMenu) {
                    processWeaponSelection(line);
                    continue;
                }

                // PROCESAMIENTO DE COMANDOS - CORREGIDO
                if (line.startsWith("NAME:")) {
                    playerName = line.substring(5).trim();
                    sendMessage("WELCOME " + playerName);
                    System.out.println("Jugador registrado: " + playerName);
                } 
                else if (line.startsWith("WEAPON:")) {
                    String weaponName = line.substring(7).trim();
                    equipWeapon(weaponName);
                } 
                else if (line.equals("ATTACK") || line.equals("1")) {
                    if (inBattle && opponent != null) {
                        attackPlayer();
                    } else {
                        attackEnemy();
                    }
                } 
                else if (line.equals("HEAL") || line.equals("2")) {
                    heal(15);
                } 
                else if (line.equals("STATUS") || line.equals("3")) {
                    sendStatus();
                } 
                else if (line.equals("PLAYERS") || line.equals("4")) {
                    sendPlayersList();
                } 
                else if (line.equals("WEAPONS") || line.equals("5")) {
                    enterWeaponMenu();
                } 
                // CORREGIDO: Separar lógica de CHALLENGE
                else if (line.startsWith("CHALLENGE:")) {
                    String targetName = line.substring(10).trim();
                    challengePlayer(targetName);
                } 
                else if (line.equals("6")) {
                    sendMessage("ERROR: Uso correcto - CHALLENGE:nombre_del_jugador");
                    sendMessage("Ejemplo: CHALLENGE:Juan");
                } 
                // CORREGIDO: Separar lógica de ACCEPT
                else if (line.startsWith("ACCEPT:")) {
                    String challengerName = line.substring(7).trim();
                    acceptChallenge(challengerName);
                } 
                else if (line.equals("7")) {
                    sendMessage("ERROR: Uso correcto - ACCEPT:nombre_del_jugador");
                    sendMessage("Ejemplo: ACCEPT:Maria");
                } 
                else if (line.equals("RESET_ENEMIES") || line.equals("8")) {
                    resetEnemies();
                } 
                else if (line.equals("HELP") || line.equals("9")) {
                    sendMainMenu();
                } 
                // AGREGADO: Comando EXIT que faltaba
                else if (line.equals("EXIT") || line.equals("0")) {
                    sendMessage("¡Hasta luego!");
                    break;
                } 
                else {
                    sendMessage("UNKNOWN_COMMAND - Usa '9' o 'HELP' para ver comandos disponibles");
                }
            }
        } catch (IOException e) {
            System.out.println("Error en handler para " + playerName + ": " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    // MÉTODO PARA ENVIAR STATUS - MEJORADO
    private void sendStatus() {
        sendMessage("=== TU ESTADO ===");
        sendMessage("HP: " + hp + "/" + maxHp);
        if (weapon != null) {
            sendMessage("ARMA: " + weapon.getName() + " (Daño: " + weapon.getDamage() + ")");
        } else {
            sendMessage("ARMA: Ninguna equipada (Daño base: 10)");
        }
        
        if (inBattle && opponent != null) {
            sendMessage("ESTADO: EN BATALLA contra " + opponent.getPlayerName());
            sendMessage("HP DEL OPONENTE: " + opponent.getHp());
        } else {
            sendMessage("ESTADO: Disponible para batalla");
            sendMessage("=== ENEMIGOS ===");
            for (Enemy enemy : enemies) {
                String status = enemy.estaVivo() ? "VIVO" : "DERROTADO";
                sendMessage("- " + enemy.getEnemyName() + ": " + enemy.getHp() + " HP (" + status + ")");
            }
        }
    }

    // MÉTODO PARA ENVIAR LISTA DE JUGADORES - MEJORADO
    private void sendPlayersList() {
        StringBuilder playersList = new StringBuilder();
        playersList.append("=== JUGADORES CONECTADOS ===\n");
        boolean otherPlayers = false;
        
        for (ClientHandler player : players) {
            if (player != this && player.getPlayerName() != null) {
                otherPlayers = true;
                String name = player.getPlayerName();
                int hpValue = player.getHp();
                String status = player.inBattle ? "EN BATALLA" : "DISPONIBLE";
                String weapon = player.getWeapon() != null ? player.getWeapon().getName() : "Sin arma";
                playersList.append("- ").append(name).append(" | HP: ").append(hpValue)
                          .append(" | ").append(status).append(" | Arma: ").append(weapon).append("\n");
            }
        }
        
        if (!otherPlayers) {
            playersList.append("No hay otros jugadores conectados.\n");
        }
        
        playersList.append("=============================");
        sendMessage(playersList.toString());
    }

    // MÉTODOS DEL MENÚ DE ARMAS
    private void enterWeaponMenu() {
        inWeaponMenu = true;
        sendWeaponsMenu();
    }

    private void processWeaponSelection(String input) {
        if (input.equalsIgnoreCase("BACK") || input.equalsIgnoreCase("B") || input.equals("0")) {
            inWeaponMenu = false;
            sendMessage("Volviendo al menú principal...");
            return;
        }

        try {
            int weaponNumber = Integer.parseInt(input);
            if (weaponNumber >= 1 && weaponNumber <= availableWeapons.size()) {
                Weapons selectedWeapon = availableWeapons.get(weaponNumber - 1);
                this.weapon = selectedWeapon;
                sendMessage("ARMA EQUIPADA: " + selectedWeapon.getName() + " (Daño: " + selectedWeapon.getDamage() + ")");
                System.out.println(playerName + " equipó: " + selectedWeapon.getName());
                
                sendMessage("\n¿Quieres seleccionar otra arma?");
                sendMessage("Escribe otro número (1-" + availableWeapons.size() + ") para cambiar de arma");
                sendMessage("O escribe 'B', 'BACK' o '0' para volver al menú principal");
            } else {
                sendMessage("ERROR: Número de arma inválido. Debe ser entre 1 y " + availableWeapons.size());
            }
        } catch (NumberFormatException e) {
            sendMessage("ERROR: Entrada inválida. Debes escribir un número entre 1 y " + availableWeapons.size());
        }
    }

    private void sendWeaponsMenu() {
        StringBuilder menu = new StringBuilder();
        menu.append("=== ARMAS DISPONIBLES ===\n");

        for (int i = 0; i < availableWeapons.size(); i++) {
            Weapons w = availableWeapons.get(i);
            String equippedIndicator = (weapon != null && weapon.getName().equals(w.getName())) ? " (EQUIPADA)" : "";
            menu.append(i + 1).append(". ").append(w.getName())
                .append(" - Daño: ").append(w.getDamage()).append(equippedIndicator).append("\n");
        }

        menu.append("==========================\n");
        menu.append("Para equipar un arma, escribe su número (1-").append(availableWeapons.size()).append(")\n");
        menu.append("Escribe '0', 'B' o 'BACK' para volver al menú principal");

        sendMessage(menu.toString());
    }

    private void sendMainMenu() {
        StringBuilder menu = new StringBuilder();
        menu.append("=== COMANDOS DISPONIBLES ===\n")
            .append("1  - ATTACK       - Atacar (enemigo u oponente)\n")
            .append("2  - HEAL         - Curarse 15 puntos de vida\n")
            .append("3  - STATUS       - Ver tu estado y enemigos\n")
            .append("4  - PLAYERS      - Listar jugadores conectados\n")
            .append("5  - WEAPONS      - Ver menú de armas disponibles\n")
            .append("6  - CHALLENGE:nombre - Desafiar a jugador\n")
            .append("7  - ACCEPT:nombre    - Aceptar desafío\n")
            .append("8  - RESET_ENEMIES    - Reiniciar enemigos derrotados\n")
            .append("9  - HELP         - Mostrar esta ayuda\n")
            .append("0  - EXIT         - Salir del juego\n")
            .append("=============================\n")
            .append("EJEMPLOS:\n")
            .append("- CHALLENGE:Juan    (desafía a Juan)\n")
            .append("- ACCEPT:Maria      (acepta desafío de Maria)\n")
            .append("- WEAPON:yuca       (equipa 'una yuca')\n");

        sendMessage(menu.toString());
    }

    // LIMPIEZA AL DESCONECTAR
    private void cleanup() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }

        if (players != null) {
            players.remove(this);
        }

        // Si estaba en batalla, liberar al oponente
        if (opponent != null) {
            opponent.opponent = null;
            opponent.inBattle = false;
            opponent.sendMessage("TU_OPONENTE_SE_DESCONECTO");
        }

        System.out.println("Jugador " + (playerName != null ? playerName : "desconocido") + " desconectado.");
    }
}