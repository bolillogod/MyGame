public class Player {
    private String name;
    private int hp;
    private Weapons weapon;
    private final int MAX_HP = 100;

    public Player(String name, int hp, Weapons weapon) {
        this.name = name;
        this.hp = hp;
        this.weapon = weapon;
    }

    public synchronized void takeDamage(int amount, String attacker) {
        System.out.println(attacker + " ataca a " + name + " y hace " + amount + " de daño.");
        hp -= amount;
        if (hp < 0) hp = 0;
        System.out.println("-> Vida de " + name + ": " + hp);
    }

    public void attackEnemy(Enemy enemy) {
        int damage = weapon.getDamage();
        System.out.println(name + " ataca con " + weapon.getName() + " y causa "
                           + damage + " de daño a " + enemy.getEnemyName());
        enemy.takeDamage(damage);
    }

    public void heal(int amount) {
        hp += amount;
        if (hp > MAX_HP) hp = MAX_HP;
        System.out.println(name + " se curó +" + amount + " HP. Vida actual: " + hp);
    }

    public synchronized boolean isAlive() {
        return hp > 0;
    }

    public synchronized int getHp() {
        return hp;
    }

    public String getName() {
        return name;
    }

    public Weapons getWeapon() {
        return weapon;
    }
}