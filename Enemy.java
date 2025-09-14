import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Enemy extends Thread {
    private List<Player> jugadores;
    private String enemyName;
    private int attackPower;
    private int attacks;
    private int hp = 50;
    private boolean estaVivo = true; // Cambié el nombre

    public Enemy(List<Player> jugadores, String enemyName, int attackPower, int attacks) {
        this.jugadores = jugadores;
        this.enemyName = enemyName;
        this.attackPower = attackPower;
        this.attacks = attacks;
    }

    @Override
    public void run() {
        Random rand = new Random();
        for (int i = 0; i < attacks && estaVivo; i++) { // Usar estaVivo
            boolean alguienVivo = jugadores.stream().anyMatch(Player::isAlive);
            if (!alguienVivo) {
                System.out.println(enemyName + " no tiene a quién atacar y deja de atacar.");
                break;
            }

            if (!estaVivo) break;

            List<Player> vivos = new ArrayList<>();
            for (Player p : jugadores) {
                if (p.isAlive()) vivos.add(p);
            }

            Player objetivo = vivos.get(rand.nextInt(vivos.size()));
            objetivo.takeDamage(attackPower, enemyName);

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println(enemyName + " terminó sus ataques.");
    }

    public synchronized void takeDamage(int amount) {
        hp -= amount;
        System.out.println(enemyName + " recibe " + amount + " de daño. HP: " + hp);
        if (hp <= 0) {
            hp = 0;
            estaVivo = false;
            System.out.println("¡" + enemyName + " ha sido derrotado!");
        }
    }

    public String getEnemyName() {
        return enemyName;
    }

    // CAMBIÉ EL NOMBRE DEL MÉTODO
    public boolean estaVivo() {
        return estaVivo;
    }

    public int getHp() {
        return hp;
    }
}