package com.narsmp.legend;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class LegendWeapons extends JavaPlugin {

    private NamespacedKey weaponKey;
    private int tick = 0;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.weaponKey = new NamespacedKey(this, "weapon");
        getServer().getPluginManager().registerEvents(new WeaponListener(this), this);

        PluginCommand cmd = getCommand("legend");
        if (cmd != null) {
            LegendCommand handler = new LegendCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        // aura yang berputar di badan pemain saat memegang senjata legend
        Bukkit.getScheduler().runTaskTimer(this, this::aura, 20L, 2L);

        getLogger().info("LegendWeapons aktif. Senjata: " + Weapon.values().length);
    }

    public NamespacedKey weaponKey() {
        return weaponKey;
    }

    private void aura() {
        if (!getConfig().getBoolean("aura", true)) return;
        tick++;
        for (Player p : Bukkit.getOnlinePlayers()) {
            Weapon w = Weapon.fromItem(this, p.getInventory().getItemInMainHand());
            if (w == null) continue;

            World world = p.getWorld();
            Location base = p.getLocation();
            for (int k = 0; k < 3; k++) {
                double a = tick * 0.4 + k * (2 * Math.PI / 3);
                double h = 0.1 + ((tick * 0.04 + k * 0.33) % 1.0) * 1.4;
                Location l = base.clone().add(Math.cos(a) * 0.85, h, Math.sin(a) * 0.85);
                switch (w) {
                    case LIGHTNING_SWORD -> world.spawnParticle(Particle.ELECTRIC_SPARK, l, 1, 0, 0, 0, 0);
                    case WARDEN_AXE -> world.spawnParticle(Particle.SOUL_FIRE_FLAME, l, 1, 0, 0, 0, 0);
                    case DRACULA_BLADE -> world.spawnParticle(Particle.DUST, l, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(k == 0 ? Color.fromRGB(20, 0, 5) : Color.fromRGB(170, 10, 35), 1.3f));
                    case SPARTA_BLADE -> world.spawnParticle(Particle.FLAME, l, 1, 0, 0, 0, 0);
                }
            }
        }
    }
}
