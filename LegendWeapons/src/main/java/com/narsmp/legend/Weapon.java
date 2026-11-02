package com.narsmp.legend;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Bahan dasar sengaja cangkul (hoe), BUKAN pedang, supaya efek tebasan
 * bawaan Minecraft (sweep) tidak muncul. Damage dan kecepatan serang diatur
 * lewat atribut. Tampilan asli diganti resource pack (item_model).
 */
public enum Weapon {

    LIGHTNING_SWORD("lightning_sword", Material.GOLDEN_HOE, "Lightning Sword",
            NamedTextColor.YELLOW, Color.YELLOW, 6.0, -2.4,
            List.of("Sambaran petir saat menyerang.", "Kill skill: Thunder Rush (speed + ledakan petir)",
                    "Klik kanan: Thunder Strike (petir ke target)")),

    WARDEN_AXE("warden_axe", Material.DIAMOND_HOE, "Warden Axe",
            NamedTextColor.DARK_AQUA, Color.TEAL, 8.0, -3.0,
            List.of("Sonic boom menghempaskan musuh.", "Kill skill: Deep Dark Roar (absorption)",
                    "Klik kanan: Sonic Boom (sinar ke target)")),

    DRACULA_BLADE("dracula_blade", Material.IRON_HOE, "Dracula Fang Scythe",
            NamedTextColor.DARK_RED, Color.RED, 5.0, -2.2,
            List.of("Sabit dari taring Dracula, menyerap darah.", "Kill skill: Blood Feast (heal + regen)",
                    "Klik kanan: Blood Drain (sedot darah target)")),

    SPARTA_BLADE("sparta_blade", Material.STONE_HOE, "Sparta Blade",
            NamedTextColor.GOLD, Color.ORANGE, 6.0, -2.6,
            List.of("Tebasan perisai memperlambat musuh.", "Kill skill: This Is Sparta (strength + resistance)",
                    "Klik kanan: Spartan Charge (dash + hantam 1 musuh)"));

    private final String id;
    private final Material material;
    private final String displayName;
    private final NamedTextColor color;
    private final Color dustColor;
    private final double attackDamage;
    private final double attackSpeedMod;
    private final List<String> lore;

    Weapon(String id, Material material, String displayName, NamedTextColor color,
           Color dustColor, double attackDamage, double attackSpeedMod, List<String> lore) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.color = color;
        this.dustColor = dustColor;
        this.attackDamage = attackDamage;
        this.attackSpeedMod = attackSpeedMod;
        this.lore = lore;
    }

    public String id() { return id; }
    public Color dustColor() { return dustColor; }
    public NamedTextColor color() { return color; }

    public ItemStack create(LegendWeapons plugin) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(displayName, color, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lines = new ArrayList<>();
        lines.add(Component.text("LEGENDARY", NamedTextColor.GOLD, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        for (String s : lore) {
            lines.add(Component.text(s, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lines);

        meta.setUnbreakable(true);
        meta.setEnchantmentGlintOverride(true);
        meta.setItemModel(NamespacedKey.fromString("narsmp:" + id));

        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
                new NamespacedKey(plugin, id + "_damage"), attackDamage,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(
                new NamespacedKey(plugin, id + "_speed"), attackSpeedMod,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));

        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        meta.getPersistentDataContainer().set(plugin.weaponKey(), PersistentDataType.STRING, id);

        item.setItemMeta(meta);
        return item;
    }

    public static Weapon fromId(String id) {
        for (Weapon w : values()) {
            if (w.id.equalsIgnoreCase(id)) return w;
        }
        return null;
    }

    public static Weapon fromItem(LegendWeapons plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer()
                .get(plugin.weaponKey(), PersistentDataType.STRING);
        return id == null ? null : fromId(id);
    }
}
