package net.dawson.adorablehamsterpets.item.custom;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

/**
 * Custom item class for Hamster Armor.
 * Unlike player armor, this armor completely negates valid incoming damage
 * at the cost of its own durability, similar to Wolf Armor.
 */
public class HamsterArmorItem extends Item {

    private final HamsterArmorMaterial material;
    private final Identifier entityTexture;

    public HamsterArmorItem(HamsterArmorMaterial material, Settings settings) {
        super(settings.maxDamage(material.getDurability()));
        this.material = material;

        // --- Fix Texture Path Generation ---
        // Maps material names to the specific file structure.
        // ACORN -> "acorn_armor_base.png"
        // IRON  -> "acorn_armor_iron.png"
        String fileName;
        if (material == HamsterArmorMaterial.ACORN) {
            fileName = "acorn_armor_base";
        } else {
            fileName = "acorn_armor_" + material.getName();
        }

        this.entityTexture = Identifier.of(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/armor/" + fileName + ".png");
    }

    public HamsterArmorMaterial getMaterial() {
        return material;
    }

    public Identifier getEntityTexture() {
        return entityTexture;
    }

    /**
     * Defines the tiers of hamster armor, their durability, and internal names.
     */
    public enum HamsterArmorMaterial {
        ACORN("acorn", 120),          // Standard durability
        IRON("iron", 350),            // High durability
        GOLD("gold", 100),            // Low durability (but offers Speed)
        DIAMOND("diamond", 900),      // Very High durability
        NETHERITE("netherite", 1800); // Extreme durability (offers Knockback Resistance)

        private final String name;
        private final int durability;

        HamsterArmorMaterial(String name, int durability) {
            this.name = name;
            this.durability = durability;
        }

        public String getName() {
            return name;
        }

        public int getDurability() {
            return durability;
        }
    }
}