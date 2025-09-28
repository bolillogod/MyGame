import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class GameMain {
    public static void main(String[] args) {
        List<Weapons> armas = new ArrayList<>();
        armas.add(new Weapons("una yuca", 20));
        armas.add(new Weapons("espada de icopor", 15));
        armas.add(new Weapons("machete oxidado", 25));
        armas.add(new Weapons("cebollin (lanzable)", 18));

        Scanner sc = new Scanner(System.in);
        Random rand = new Random();

        List<Player> jugadores = new ArrayList<>();
        System.out.print("¿Cuántos se van a dar trompa? ");
        int numJugadores = sc.nextInt();
        sc.nextLine();

        for (int j = 0; j < numJugadores; j++) {
            System.out.print("\nNombre del jugador " + (j + 1) + ": ");
            String nombre = sc.nextLine();

            Weapons chosen = null;
            while (chosen == null) {
                System.out.println(nombre + ", elige tu arma:");
                for (int i = 0; i < armas.size(); i++) {
                    Weapons w = armas.get(i);
                    System.out.println((i + 1) + ". " + w.getName() + " (daño: " + w.getDamage() + ")");
                }
                System.out.print("Opción: ");
                int choice = sc.nextInt();
                sc.nextLine();

                if (choice >= 1 && choice <= armas.size()) {
                    chosen = armas.get(choice - 1);
                } else {
                    System.out.println("Opción inválida. Intenta de nuevo.\n");
                }
            }

            jugadores.add(new Player(nombre, 100, chosen));
            System.out.println(nombre + " equipado con " + chosen.getName());
        }

        List<Enemy> enemigos = new ArrayList<>();
        enemigos.add(new Enemy(jugadores, "e1", 15, 5));
        enemigos.add(new Enemy(jugadores, "e2", 10, 7));
        enemigos.add(new Enemy(jugadores, "e3", 12, 6));
        enemigos.add(new Enemy(jugadores, "e4", 18, 4));

        for (Enemy e : enemigos) {
            e.start();
        }

        while (jugadores.stream().anyMatch(Player::isAlive)) {
            for (Player p : jugadores) {
                if (p.isAlive()) {
                    System.out.println("\nTurno de " + p.getName() + " (vida: " + p.getHp() + ")");
                    System.out.println("Presiona ENTER para continuar...");
                    sc.nextLine();

                    System.out.print("¿Deseas atacar este turno? (y/n): ");
                    String decision = sc.nextLine();

                    if (decision.equalsIgnoreCase("y")) {
                        int dado = rand.nextInt(100);
                        if (dado < 50) {
                            for (Enemy e : enemigos) {
                                if (e.estaVivo()) {
                                    p.attackEnemy(e);
                                }
                            }
                        } else {
                            System.out.println(p.getName() + " falló su ataque.");
                        }
                    } else {
                        p.heal(15);
                    }
                }
            }
        }

        for (Enemy e : enemigos) {
            try {
                e.join();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        if (jugadores.stream().anyMatch(Player::isAlive)) {
            System.out.println("\n¡El equipo de jugadores sobrevivió!");
            for (Player p : jugadores) {
                System.out.println(p.getName() + " vida: " + p.getHp());
            }
        } else {
            System.out.println("Todos los jugadores fueron derrotados.");
        }

        System.out.println("Fin de la simulación.");
    }
}