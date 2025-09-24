
import java.io.*;
import java.net.*;
import java.util.*;

// Maneja la comunicación con un cliente (una conexión)
public class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ClientHandler opponent; // referencia al oponente cuando esté emparejado
    private String playerName;
    private int hp = 100; // estado simple del jugador en el servidor
    private Weapons weapon;
    private boolean inBattle = false;
    private List<Enemy> enemies;
    private List<ClientHandler> players;
    private Random rand = new Random();

    public ClientHandler(Socket socket, List<ClientHandler> players, List<Enemy> enemies) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.players = players;
        this.enemies = enemies;
    }

    public void setOpponent(ClientHandler opp) {
        this.opponent = opp;
    }

    public void setInBattle(boolean inBattle) {
        this.inBattle = inBattle;
    }

    // Enviar mensaje al cliente
    public void sendMessage(String msg) {
        out.println(msg);
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getHp() {
        return hp;
    }

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
        if (hp > 100) hp = 100;
        sendMessage("HEALED:" + amount);
        sendMessage("HP:" + hp);
    }

    public void setWeapon(Weapons weapon) {
        this.weapon = weapon;
    }

    public Weapons getWeapon() {
        return weapon;
    }

    // Método para atacar a un enemigo
    private void attackEnemy() {
        if (enemies == null || enemies.isEmpty()) {
            sendMessage("NO_ENEMIES");
            return;
        }

        // Buscar enemigos vivos
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

        // Atacar a un enemigo aleatorio
        Enemy target = aliveEnemies.get(rand.nextInt(aliveEnemies.size()));
        int damage = weapon != null ? weapon.getDamage() : 10; // Daño por defecto si no tiene arma
        
        target.takeDamage(damage);
        sendMessage("YOU_ATTACKED:" + target.getEnemyName() + ":" + damage);

        // Verificar si el enemigo fue derrotado
        if (!target.estaVivo()) {
            sendMessage("ENEMY_DEFEATED:" + target.getEnemyName());
            
            // Verificar si todos los enemigos fueron derrotados
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
                }
            }
        }
    }

    // Método para atacar a otro jugador (PvP)
    private void attackPlayer() {
        if (opponent == null) {
            sendMessage("NO_OPPONENT");
            return;
        }

        int damage = weapon != null ? weapon.getDamage() : 10;
        opponent.takeDamage(damage);
        sendMessage("YOU_ATTACKED_OPPONENT:" + damage);

        // Si el oponente muere, notificar a ambos
        if (opponent.getHp() <= 0) {
            sendMessage("YOU_WIN");
            opponent.sendMessage("YOU_LOSE");
        }
    }

    @Override
    public void run() {
        try {
            // Leemos el nombre del jugador (protocol: NAME:<nombre>)
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Recibido de " + (playerName != null ? playerName : "cliente") + ": " + line);

                if (line.startsWith("NAME:")) {
                    playerName = line.substring(5);
                    sendMessage("WELCOME " + playerName);
                } 
                else if (line.startsWith("WEAPON:")) {
                    // Selección de arma
                    String weaponName = line.substring(7);
                    // Aquí deberías tener una lista de armas disponibles
                    // Por simplicidad, asumimos que el arma ya está creada
                    sendMessage("WEAPON_EQUIPPED:" + weaponName);
                } 
                else if (line.equals("ATTACK")) {
                    if (inBattle && opponent != null) {
                        // Modo PvP: atacar al oponente
                        attackPlayer();
                    } else {
                        // Modo PvE: atacar enemigos
                        attackEnemy();
                    }
                } 
                else if (line.equals("HEAL")) {
                    heal(15);
                } 
                else if (line.equals("STATUS")) {
                    sendMessage("HP:" + hp);
                    if (weapon != null) {
                        sendMessage("WEAPON:" + weapon.getName() + ":" + weapon.getDamage());
                    }
                    
                    // Información de enemigos si está en modo PvE
                    if (!inBattle) {
                        for (Enemy enemy : enemies) {
                            sendMessage("ENEMY:" + enemy.getEnemyName() + ":" + enemy.getHp() + ":" + enemy.estaVivo());
                        }
                    }
                } 
                else if (line.equals("PLAYERS")) {
                    // Listar jugadores conectados
                    StringBuilder playersList = new StringBuilder();
                    for (ClientHandler player : players) {
                        if (player != this) {
                            playersList.append(player.getPlayerName()).append(":").append(player.getHp()).append(";");
                        }
                    }
                    sendMessage("PLAYERS_LIST:" + playersList.toString());
                }
                else if (line.startsWith("CHALLENGE:")) {
                    // Desafiar a otro jugador
                    String targetName = line.substring(10);
                    for (ClientHandler player : players) {
                        if (player.getPlayerName().equals(targetName)) {
                            player.sendMessage("CHALLENGE_REQUEST:" + playerName);
                            sendMessage("CHALLENGE_SENT:" + targetName);
                            break;
                        }
                    }
                }
                else if (line.startsWith("ACCEPT_CHALLENGE:")) {
                    // Aceptar desafío
                    String challengerName = line.substring(17);
                    for (ClientHandler player : players) {
                        if (player.getPlayerName().equals(challengerName)) {
                            this.setOpponent(player);
                            player.setOpponent(this);
                            this.setInBattle(true);
                            player.setInBattle(true);
                            sendMessage("BATTLE_START:" + challengerName);
                            player.sendMessage("BATTLE_START:" + playerName);
                            break;
                        }
                    }
                }
                else {
                    sendMessage("UNKNOWN_CMD");
                }
            }
        } catch (IOException e) {
            System.out.println("Error en handler: " + e.getMessage());
        } finally {
            try { 
                socket.close(); 
            } catch (IOException ignored) {}
            
            // Remover de la lista de jugadores
            if (players != null) {
                players.remove(this);
            }
            
            System.out.println("Jugador " + playerName + " desconectado.");
        }
    }
}