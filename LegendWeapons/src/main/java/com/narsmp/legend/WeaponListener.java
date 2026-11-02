package com.narsmp.legend;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class WeaponListener implements Listener {

    private final LegendWeapons plugin;
    private final Map<String, Long> cooldowns = new HashMap<>();

    public WeaponListener(LegendWeapons plugin) {
        this.plugin = plugin;
    }

    // ---------- util ----------

    private boolean onCooldown(UUID id, String key, long ms) {
        String k = id + ":" + key;
        long now = System.currentTimeMillis();
        Long until = cooldowns.get(k);
        if (until != null && until > now) return true;
        cooldowns.put(k, now + ms);
        return false;
    }

    private Particle.DustOptions dust(Weapon w, float size) {
        return new Particle.DustOptions(w.dustColor(), size);
    }

    private void ring(Location center, Particle particle, Object data, double radius, int points) {
        World world = center.getWorld();
        if (world == null) return;
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            Location l = center.clone().add(Math.cos(a) * radius, 0.2, Math.sin(a) * radius);
            world.spawnParticle(particle, l, 1, 0, 0, 0, 0, data);
        }
    }

    private void heal(Player p, double amount) {
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        double cap = max == null ? 20.0 : max.getValue();
        p.setHealth(Math.min(cap, p.getHealth() + amount));
    }

    // ---------- slash effect (klik kiri) ----------

    @EventHandler
    public void onLeftClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.LEFT_CLICK_AIR && e.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (e.getHand() != EquipmentSlot.HAND) return;

        Player p = e.getPlayer();
        Weapon w = Weapon.fromItem(plugin, p.getInventory().getItemInMainHand());
        if (w == null) return;
        if (onCooldown(p.getUniqueId(), "slash", 350)) return;

        slash(p, w);
    }

    private final Map<UUID, Boolean> flip = new HashMap<>();

    private void slash(Player p, Weapon w) {
        // tiap tebasan bergantian miring kiri / kanan
        boolean left = flip.merge(p.getUniqueId(), true, (old, nv) -> !old);
        double tilt = Math.toRadians(left ? 35 : -35);

        Location eye = p.getEyeLocation();
        Vector f = eye.getDirection().normalize();
        Vector r0 = f.clone().crossProduct(new Vector(0, 1, 0));
        Vector r = r0.lengthSquared() < 1e-4 ? new Vector(1, 0, 0) : r0.normalize();
        Vector u = r.clone().crossProduct(f).normalize();
        Vector axis = r.clone().multiply(Math.cos(tilt)).add(u.clone().multiply(Math.sin(tilt)));
        Location center = eye.clone().subtract(f.clone().multiply(1.2));

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.6f);

        // gambar busur tebasan bertahap selama 5 tick
        for (int i = 0; i < 5; i++) {
            final int seg = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                World world = center.getWorld();
                if (world == null) return;
                for (int s = 0; s < 6; s++) {
                    double th = Math.toRadians(-75 + 30.0 * seg + 5.0 * s);
                    Vector dir = f.clone().multiply(Math.cos(th)).add(axis.clone().multiply(Math.sin(th)));
                    for (double rad : new double[]{3.0, 3.25, 3.5}) {
                        Location l = center.clone().add(dir.clone().multiply(rad));
                        world.spawnParticle(Particle.DUST_COLOR_TRANSITION, l, 1, 0, 0, 0, 0,
                                new Particle.DustTransition(w.dustColor(), org.bukkit.Color.WHITE, 1.4f));
                    }
                    if (s % 2 == 0) accent(world, center.clone().add(dir.clone().multiply(3.25)), w);
                }
            }, i);
        }
    }

    private void accent(World world, Location l, Weapon w) {
        switch (w) {
            case LIGHTNING_SWORD -> world.spawnParticle(Particle.ELECTRIC_SPARK, l, 2, 0.1, 0.1, 0.1, 0.05);
            case WARDEN_AXE -> world.spawnParticle(Particle.SOUL_FIRE_FLAME, l, 1, 0.05, 0.05, 0.05, 0.01);
            case DRACULA_BLADE -> world.spawnParticle(Particle.CRIMSON_SPORE, l, 3, 0.15, 0.15, 0.15, 0);
            case SPARTA_BLADE -> world.spawnParticle(Particle.FLAME, l, 1, 0.05, 0.05, 0.05, 0.01);
        }
    }

    // ---------- efek saat memukul ----------

    private double cfg(Weapon w, String key, double def) {
        return plugin.getConfig().getDouble(w.id() + "." + key, def);
    }

    private long sec(Weapon w, String key, double def) {
        return (long) (cfg(w, key, def) * 1000);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        Weapon w = Weapon.fromItem(plugin, p.getInventory().getItemInMainHand());
        if (w == null) return;

        Location tl = target.getLocation().add(0, 1, 0);
        World world = target.getWorld();

        switch (w) {
            case LIGHTNING_SWORD -> {
                e.setDamage(e.getDamage() + cfg(w, "hit-bonus", 1.5));
                if (!onCooldown(p.getUniqueId(), "hit_lightning", sec(w, "proc-cooldown", 3))) {
                    world.strikeLightningEffect(target.getLocation());
                    e.setDamage(e.getDamage() + cfg(w, "proc-damage", 2.0));
                    world.spawnParticle(Particle.ELECTRIC_SPARK, tl, 40, 0.5, 0.8, 0.5, 0.2);
                }
            }
            case WARDEN_AXE -> {
                e.setDamage(e.getDamage() + cfg(w, "hit-bonus", 2.0));
                if (!onCooldown(p.getUniqueId(), "hit_warden", sec(w, "proc-cooldown", 4))) {
                    world.spawnParticle(Particle.SONIC_BOOM, tl, 1);
                    world.playSound(tl, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.2f);
                    Vector push = target.getLocation().toVector().subtract(p.getLocation().toVector());
                    push.setY(0);
                    if (push.lengthSquared() > 0) {
                        push.normalize().multiply(0.9).setY(0.3);
                        target.setVelocity(push);
                    }
                }
            }
            case DRACULA_BLADE -> {
                e.setDamage(e.getDamage() + cfg(w, "hit-bonus", 1.0));
                heal(p, cfg(w, "hit-heal", 1.0));
                world.spawnParticle(Particle.DUST, tl, 25, 0.4, 0.6, 0.4, 0, dust(w, 1.6f));
            }
            case SPARTA_BLADE -> {
                e.setDamage(e.getDamage() + cfg(w, "hit-bonus", 1.5));
                if (!onCooldown(p.getUniqueId(), "hit_sparta", sec(w, "proc-cooldown", 4))) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 0));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 0));
                    world.spawnParticle(Particle.ENCHANTED_HIT, tl, 30, 0.5, 0.7, 0.5, 0.3);
                    world.playSound(tl, Sound.ITEM_SHIELD_BLOCK, 1f, 0.9f);
                }
            }
        }
    }

    // ---------- kill skill ----------

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        Player p = e.getEntity().getKiller();
        if (p == null) return;

        Weapon w = Weapon.fromItem(plugin, p.getInventory().getItemInMainHand());
        if (w == null) return;

        Location loc = e.getEntity().getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        switch (w) {
            case LIGHTNING_SWORD -> {
                world.strikeLightningEffect(loc);
                ring(loc, Particle.ELECTRIC_SPARK, null, 2.5, 40);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,
                        (int) (cfg(w, "kill-speed-seconds", 3) * 20), 0));
            }
            case WARDEN_AXE -> {
                world.spawnParticle(Particle.SONIC_BOOM, loc.clone().add(0, 1, 0), 1);
                world.playSound(loc, Sound.ENTITY_WARDEN_ROAR, 0.6f, 1f);
                ring(loc, Particle.SCULK_SOUL, null, 3.5, 50);
                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,
                        (int) (cfg(w, "kill-absorb-seconds", 5) * 20), 0));
            }
            case DRACULA_BLADE -> {
                heal(p, cfg(w, "kill-heal", 3.0));
                ring(loc, Particle.DUST, dust(w, 1.8f), 2.0, 40);
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0));
                world.playSound(loc, Sound.ENTITY_BAT_TAKEOFF, 1f, 0.6f);
            }
            case SPARTA_BLADE -> {
                ring(loc, Particle.CRIT, null, 2.5, 40);
                int ticks = (int) (cfg(w, "kill-buff-seconds", 5) * 20);
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, ticks, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, ticks, 0));
                world.playSound(loc, Sound.EVENT_RAID_HORN, 0.7f, 1.4f);
            }
        }
    }

    // ---------- skill aktif (klik kanan) ----------

    private long cooldownLeft(UUID id, String key, long ms) {
        String k = id + ":" + key;
        long now = System.currentTimeMillis();
        Long until = cooldowns.get(k);
        if (until != null && until > now) return until - now;
        cooldowns.put(k, now + ms);
        return 0;
    }

    private Vector flat(Player p) {
        double yaw = Math.toRadians(p.getLocation().getYaw());
        return new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
    }

    private boolean valid(Entity en, Player p) {
        return en instanceof LivingEntity && !en.equals(p) && !(en instanceof ArmorStand);
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getHand() != EquipmentSlot.HAND) return;

        Player p = e.getPlayer();
        Weapon w = Weapon.fromItem(plugin, p.getInventory().getItemInMainHand());
        if (w == null) return;
        e.setCancelled(true);

        LivingEntity target = null;
        if (w != Weapon.SPARTA_BLADE) {
            target = lookTarget(p, w == Weapon.WARDEN_AXE ? 16 : 12);
            if (target == null) {
                p.sendActionBar(Component.text("Arahkan ke musuh dulu", NamedTextColor.GRAY));
                return;
            }
        }

        long cd = switch (w) {
            case LIGHTNING_SWORD -> sec(w, "skill-cooldown", 15);
            case WARDEN_AXE -> sec(w, "skill-cooldown", 18);
            case DRACULA_BLADE -> sec(w, "skill-cooldown", 20);
            case SPARTA_BLADE -> sec(w, "skill-cooldown", 15);
        };
        long left = cooldownLeft(p.getUniqueId(), "skill_" + w.id(), cd);
        if (left > 0) {
            p.sendActionBar(Component.text("Skill cooldown " + (left / 1000 + 1) + " detik", NamedTextColor.RED));
            return;
        }

        switch (w) {
            case LIGHTNING_SWORD -> skillThunder(p, w, target);
            case WARDEN_AXE -> skillSonic(p, w, target);
            case DRACULA_BLADE -> skillBlood(p, w, target);
            case SPARTA_BLADE -> skillCharge(p, w);
        }
    }

    // cari 1 musuh yang sedang dilihat pemain
    private LivingEntity lookTarget(Player p, double range) {
        Location eye = p.getEyeLocation();
        RayTraceResult r = p.getWorld().rayTraceEntities(eye, eye.getDirection(), range, 0.6,
                en -> valid(en, p));
        if (r == null || !(r.getHitEntity() instanceof LivingEntity le)) return null;
        return le;
    }

    // Lightning Sword: petir menyambar 1 target
    private void skillThunder(Player p, Weapon w, LivingEntity target) {
        p.sendActionBar(Component.text("Thunder Strike!", NamedTextColor.YELLOW));
        World world = p.getWorld();
        world.strikeLightningEffect(target.getLocation());
        target.damage(cfg(w, "skill-damage", 5.0), p);
        world.spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0, 1, 0), 50, 0.5, 0.8, 0.5, 0.25);
    }

    // Warden Axe: sinar sonic boom ke 1 target
    private void skillSonic(Player p, Weapon w, LivingEntity target) {
        p.sendActionBar(Component.text("Sonic Boom!", NamedTextColor.AQUA));
        World world = p.getWorld();
        Location eye = p.getEyeLocation();
        Location end = target.getLocation().add(0, target.getHeight() / 2, 0);
        Vector dir = end.toVector().subtract(eye.toVector());
        double dist = dir.length();
        dir.normalize();
        world.playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 1f);
        for (int i = 2; i < dist; i++) {
            Location l = eye.clone().add(dir.clone().multiply(i));
            if (i % 3 == 0) world.spawnParticle(Particle.SONIC_BOOM, l, 1);
            world.spawnParticle(Particle.SCULK_SOUL, l, 2, 0.1, 0.1, 0.1, 0.01);
        }
        target.damage(cfg(w, "skill-damage", 6.0), p);
        target.setVelocity(dir.clone().multiply(1.1).setY(0.3));
    }

    // Dracula Fang Scythe: sedot darah 1 target
    private void skillBlood(Player p, Weapon w, LivingEntity target) {
        p.sendActionBar(Component.text("Blood Drain!", NamedTextColor.DARK_RED));
        World world = p.getWorld();
        target.damage(cfg(w, "skill-damage", 4.0), p);
        heal(p, cfg(w, "skill-heal", 3.0));
        world.spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 40, 0.4, 0.7, 0.4, 0, dust(w, 1.7f));
        world.playSound(p.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1f, 0.5f);
    }

    // Sparta Blade: dash maju, hantam 1 musuh terdekat saat mendarat
    private void skillCharge(Player p, Weapon w) {
        p.sendActionBar(Component.text("This Is Sparta!", NamedTextColor.GOLD));
        p.setVelocity(flat(p).multiply(1.8).setY(0.25));
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 1));
        p.getWorld().playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 0.8f, 1.3f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            Location l = p.getLocation();
            World world = p.getWorld();
            LivingEntity nearest = null;
            double best = Double.MAX_VALUE;
            for (Entity en : world.getNearbyEntities(l, 3, 2, 3)) {
                if (!valid(en, p)) continue;
                double dsq = en.getLocation().distanceSquared(l);
                if (dsq < best) { best = dsq; nearest = (LivingEntity) en; }
            }
            if (nearest != null) {
                nearest.damage(cfg(w, "skill-damage", 4.0), p);
                Vector push = nearest.getLocation().toVector().subtract(l.toVector());
                push.setY(0);
                if (push.lengthSquared() > 0) nearest.setVelocity(push.normalize().multiply(1.0).setY(0.35));
                world.spawnParticle(Particle.CRIT, nearest.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.3);
            }
            world.playSound(l, Sound.ITEM_SHIELD_BLOCK, 1f, 0.7f);
        }, 10L);
    }
}
