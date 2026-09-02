package net.dawson.adorablehamsterpets.item.custom;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * Custom item class for Hamster Armor.
 * Unlike player armor, this armor completely negates valid incoming damage
 * at the cost of its own durability, similar to Wolf Armor.
 */
public class HamsterArmorItem extends Item {

    private final HamsterArmorMaterial material;
    private final Identifier entityTexture;

    public HamsterArmorItem(HamsterArmorMaterial material, Properties settings) {
        super(settings.durability(material.getDurability()));
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

        this.entityTexture = Identifier.fromNamespaceAndPath(AdorableHamsterPets.MOD_ID, "textures/entity/hamster/armor/" + fileName + ".png");
    }

    public HamsterArmorMaterial getMaterial() {
        return material;
    }

    public Identifier getEntityTexture() {
        return entityTexture;
    }


    /**
     * Defines the tiers of hamster armor, their internal names, durability, and enchantability.
     */
    public enum HamsterArmorMaterial {
        ACORN("acorn", 120, 15),          // Standard durability, Leather-like enchantability
        IRON("iron", 350, 9),             // High durability, Iron-like enchantability
        GOLD("gold", 100, 25),            // Low durability, Gold-like enchantability
        DIAMOND("diamond", 900, 10),      // Very High durability, Diamond-like enchantability
        NETHERITE("netherite", 1800, 15); // Extreme durability, Netherite-like enchantability

        private final String name;
        private final int durability;
        private final int enchantability;

        HamsterArmorMaterial(String name, int durability, int enchantability) {
            this.name = name;
            this.durability = durability;
            this.enchantability = enchantability;
        }

        public String getName() {
            return name;
        }

        public int getDurability() {
            return durability;
        }

        public int getEnchantability() {
            return enchantability;
        }
    }
}