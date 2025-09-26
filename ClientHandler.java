import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

public class ClientHandler extends Thread {
    private static final List<Weapons> availableWeapons = Arrays.asList(
        new Weapons("una yuca", 20),
        new Weapons("espada de icopor", 15),
        new Weapons("machete oxidado", 25),
        new Weapons("$800 cebollin", 18)
    );

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ClientHandler opponent;
    private String playerName;
    private int hp = 100;
    private Weapons weapon;
    private boolean inBattle = false;
    private List<Enemy> enemies;
    private List<ClientHandler> players;
    private List<MatchResult> matchResults;
    private Random rand = new Random();
    private long battleStartTime;
    private int totalDamageDealt;

    public ClientHandler(Socket socket, List<ClientHandler> players, List<Enemy> enemies, List<MatchResult> matchResults) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.players = players;
        this.enemies = enemies;
        this.matchResults = matchResults;
    }

    public void setOpponent(ClientHandler opp) {
        this.opponent = opp;
    }

    public void setInBattle(boolean inBattle) {
        this.inBattle = inBattle;
    }

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

    private void equipWeapon(String weaponName) {
        for (Weapons w : availableWeapons) {
            if (w.getName().equalsIgnoreCase(weaponName)) {
                this.weapon = w;
                sendMessage("WEAPON_EQUIPPED:" + weapon.getName() + " (Daño: " + weapon.getDamage() + ")");
                System.out.println(playerName + " equipó: " + weapon.getName());
                return;
            }
        }
        sendMessage("ERROR: Arma no encontrada. Usa 'WEAPONS' para ver las disponibles.");
    }

    private void startBattleTracking() {
        this.battleStartTime = System.currentTimeMillis();
        this.totalDamageDealt = 0;
    }

    private void endBattleTracking(boolean isWinner) {
        if (battleStartTime > 0) {
            long duration = System.currentTimeMillis() - battleStartTime;
            if (isWinner && opponent != null) {
                MatchResult result = new MatchResult(playerName, opponent.getPlayerName(), totalDamageDealt, duration);
                GameServer.addMatchResult(result);
            }
            battleStartTime = 0;
            totalDamageDealt = 0;
        }
    }

    private void resetEnemies() {
        if (enemies == null) return;
        
        for (Enemy enemy : enemies) {
            enemy.reset();
        }
        sendMessage("ENEMIES_RESET");
        
        for (ClientHandler player : players) {
            player.sendMessage("ENEMIES_HAVE_BEEN_RESET");
        }
    }

    private void attackEnemy() {
        if (enemies == null || enemies.isEmpty()) {
            sendMessage("NO_ENEMIES");
            return;
        }

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

        Enemy target = aliveEnemies.get(rand.nextInt(aliveEnemies.size()));
        int damage = weapon != null ? weapon.getDamage() : 10;
        
        target.takeDamage(damage);
        sendMessage("YOU_ATTACKED:" + target.getEnemyName() + ":" + damage);

        if (!target.estaVivo()) {
            sendMessage("ENEMY_DEFEATED:" + target.getEnemyName());
            
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

    private void attackPlayer() {
        if (opponent == null) {
            sendMessage("NO_OPPONENT");
            return;
        }

        int damage = weapon != null ? weapon.getDamage() : 10;
        opponent.takeDamage(damage);
        totalDamageDealt += damage;
        sendMessage("YOU_ATTACKED_OPPONENT:" + damage);

        if (opponent.getHp() <= 0) {
            sendMessage("YOU_WIN");
            opponent.sendMessage("YOU_LOSE");
            endBattleTracking(true);
            opponent.endBattleTracking(false);
        }
    }

    private String getPersonalRanking() {
        synchronized (matchResults) {
            if (matchResults.isEmpty()) {
                return "No hay suficientes datos para ranking.";
            }
            
            StringBuilder ranking = new StringBuilder();
            ranking.append("=== RANKING PERSONAL ===\n");
            
            List<MatchResult> playerMatches = matchResults.stream()
                .filter(m -> m.getWinner().equals(playerName) || (m.getLoser() != null && m.getLoser().equals(playerName)))
                .collect(Collectors.toList());
            
            if (playerMatches.isEmpty()) {
                return "No has participado en partidas aún.";
            }
            
            long wins = playerMatches.stream()
                .filter(m -> m.getWinner().equals(playerName))
                .count();
            
            ranking.append("Tus victorias: ").append(wins).append("/").append(playerMatches.size()).append("\n");
            
            List<MatchResult> wonMatches = playerMatches.stream()
                .filter(m -> m.getWinner().equals(playerName))
                .collect(Collectors.toList());
            
            if (!wonMatches.isEmpty()) {
                double avgDamageInWins = wonMatches.stream()
                    .collect(Collectors.averagingInt(MatchResult::getDamageDealt));
                ranking.append("Daño promedio en victorias: ").append(String.format("%.2f", avgDamageInWins)).append("\n");
                
                Optional<MatchResult> bestMatch = wonMatches.stream()
                    .max(Comparator.comparingInt(MatchResult::getDamageDealt));
                
                if (bestMatch.isPresent()) {
                    ranking.append("Mejor partida: ").append(bestMatch.get().getDamageDealt()).append(" de daño\n");
                }
            }
            
            return ranking.toString();
        }
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Recibido de " + (playerName != null ? playerName : "cliente") + ": " + line);

                if (line.startsWith("NAME:")) {
                    playerName = line.substring(5);
                    sendMessage("WELCOME " + playerName);
                    sendMessage("INFO: Usa 'WEAPONS' para ver armas y 'EQUIP:nombre_arma' para equipar");
                } 
                else if (line.startsWith("WEAPON:")) {
                    String weaponName = line.substring(7);
                    equipWeapon(weaponName);
                } 
                else if (line.startsWith("EQUIP:")) {
                    String weaponName = line.substring(6);
                    equipWeapon(weaponName);
                }
                else if (line.equals("ATTACK")) {
                    if (inBattle && opponent != null) {
                        attackPlayer();
                    } else {
                        attackEnemy();
                    }
                } 
                else if (line.equals("HEAL")) {
                    heal(15);
                } 
                else if (line.equals("help") || line.equals("HELP")) {
                    sendMessage("=== COMANDOS DISPONIBLES ===");
                    sendMessage("ATTACK - Atacar (enemigo u oponente)");
                    sendMessage("HEAL - Curarse 15 puntos de vida");
                    sendMessage("STATUS - Ver tu estado y enemigos");
                    sendMessage("PLAYERS - Listar jugadores conectados");
                    sendMessage("WEAPONS - Ver armas disponibles");
                    sendMessage("EQUIP:nombre - Equipar un arma (ej: EQUIP:una yuca)");
                    sendMessage("CHALLENGE:nombre - Desafiar a jugador");
                    sendMessage("ACCEPT_CHALLENGE:nombre - Aceptar desafío");
                    sendMessage("STATS - Ver estadísticas del servidor");
                    sendMessage("RANKING - Ver tu ranking personal");
                    sendMessage("RESET_ENEMIES - Reiniciar enemigos derrotados");
                    sendMessage("HELP - Mostrar esta ayuda");
                    sendMessage("EXIT - Salir del juego");
                }
                else if (line.equals("STATUS")) {
                    sendMessage("=== ESTADO DE " + (playerName != null ? playerName : "Jugador") + " ===");
                    sendMessage("HP: " + hp + "/100");
                    if (weapon != null) {
                        sendMessage("ARMA: " + weapon.getName() + " (Daño: " + weapon.getDamage() + ")");
                    } else {
                        sendMessage("ARMA: Ninguna equipada (Daño base: 10)");
                    }
                    
                    if (!inBattle) {
                        sendMessage("=== ENEMIGOS ===");
                        boolean aliveEnemies = false;
                        for (Enemy enemy : enemies) {
                            if (enemy.estaVivo()) {
                                aliveEnemies = true;
                                sendMessage(enemy.getEnemyName() + " - HP: " + enemy.getHp() + " - VIVO");
                            }
                        }
                        if (!aliveEnemies) {
                            sendMessage("Todos los enemigos han sido derrotados!");
                        }
                    } else if (opponent != null) {
                        sendMessage("MODO: Batalla PvP contra " + opponent.getPlayerName());
                        sendMessage("HP del oponente: " + opponent.getHp());
                    }
                } 
                else if (line.equals("WEAPONS")) {
                    sendMessage("=== ARMAS DISPONIBLES ===");
                    for (int i = 0; i < availableWeapons.size(); i++) {
                        Weapons w = availableWeapons.get(i);
                        sendMessage((i + 1) + ". " + w.getName() + " (Daño: " + w.getDamage() + ")");
                    }
                    sendMessage("Usa: EQUIP:nombre_arma (ej: EQUIP:una yuca)");
                }
                else if (line.equals("PLAYERS")) {
                    StringBuilder playersList = new StringBuilder();
                    playersList.append("=== JUGADORES CONECTADOS ===\n");
                    boolean otherPlayers = false;
                    for (ClientHandler player : players) {
                        if (player != this) {
                            otherPlayers = true;
                            String name = player.getPlayerName() != null ? player.getPlayerName() : "Unknown";
                            int hpValue = player.getHp();
                            String status = player.inBattle ? "EN BATALLA" : "DISPONIBLE";
                            playersList.append("- ").append(name).append(" | HP: ").append(hpValue)
                                      .append(" | ").append(status).append("\n");
                        }
                    }
                    if (!otherPlayers) {
                        playersList.append("No hay otros jugadores conectados.\n");
                    }
                    sendMessage(playersList.toString());
                }
                else if (line.startsWith("CHALLENGE:")) {
                    String targetName = line.substring(10);
                    boolean found = false;
                    for (ClientHandler player : players) {
                        if (player.getPlayerName() != null && player.getPlayerName().equals(targetName) && player != this) {
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
                else if (line.startsWith("ACCEPT_CHALLENGE:")) {
                    String challengerName = line.substring(17);
                    boolean found = false;
                    for (ClientHandler player : players) {
                        if (player.getPlayerName() != null && player.getPlayerName().equals(challengerName)) {
                            this.setOpponent(player);
                            player.setOpponent(this);
                            this.setInBattle(true);
                            player.setInBattle(true);
                            this.startBattleTracking();
                            player.startBattleTracking();
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
                else if (line.equals("STATS")) {
                    String statistics = GameServer.getStatistics();
                    sendMessage("SERVER_STATISTICS");
                    String[] lines = statistics.split("\n");
                    for (String statLine : lines) {
                        sendMessage("STAT:" + statLine);
                    }
                    sendMessage("END_STATS");
                }
                else if (line.equals("RANKING")) {
                    String ranking = getPersonalRanking();
                    sendMessage("PERSONAL_RANKING");
                    String[] lines = ranking.split("\n");
                    for (String rankLine : lines) {
                        sendMessage("RANK:" + rankLine);
                    }
                    sendMessage("END_RANKING");
                }
                else if (line.equals("RESET_ENEMIES")) {
                    resetEnemies();
                }
                else {
                    sendMessage("UNKNOWN_COMMAND - Usa 'HELP' para ver comandos disponibles");
                }
            }
        } catch (IOException e) {
            System.out.println("Error en handler: " + e.getMessage());
        } finally {
            try { 
                socket.close(); 
            } catch (IOException ignored) {}
            
            if (players != null) {
                players.remove(this);
            }
            
            System.out.println("Jugador " + playerName + " desconectado.");
        }
    }
}