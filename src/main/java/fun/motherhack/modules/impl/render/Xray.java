package fun.motherhack.modules.impl.render;

import fun.motherhack.modules.api.Category;
import fun.motherhack.modules.api.Module;
import fun.motherhack.modules.settings.impl.BooleanSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.resource.language.I18n;

import java.util.HashSet;
import java.util.Set;

public class Xray extends Module {

    public BooleanSetting diamondOre = new BooleanSetting(I18n.translate("settings.xray.diamondore"), true);
    public BooleanSetting coalOre = new BooleanSetting(I18n.translate("settings.xray.coalore"), false);
    public BooleanSetting goldOre = new BooleanSetting(I18n.translate("settings.xray.goldore"), true);
    public BooleanSetting ironOre = new BooleanSetting(I18n.translate("settings.xray.ironore"), true);
    public BooleanSetting lapisOre = new BooleanSetting(I18n.translate("settings.xray.lapisore"), true);
    public BooleanSetting redstoneOre = new BooleanSetting(I18n.translate("settings.xray.redstoneore"), false);
    public BooleanSetting netheriteOre = new BooleanSetting(I18n.translate("settings.xray.netheriteore"), true);
    public BooleanSetting obsidian = new BooleanSetting(I18n.translate("settings.xray.obsidian"), true);
    public BooleanSetting water = new BooleanSetting(I18n.translate("settings.xray.water"), false);
    public BooleanSetting lava = new BooleanSetting(I18n.translate("settings.xray.lava"), true);
    public BooleanSetting chests = new BooleanSetting(I18n.translate("settings.xray.chests"), true);

    public Xray() {
        super("Xray", Category.Render);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.worldRenderer != null) {
            mc.worldRenderer.reload();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.worldRenderer != null) {
            mc.worldRenderer.reload();
        }
    }

    public Set<Block> getVisibleBlocks() {
        Set<Block> blocks = new HashSet<>();
        
        if (diamondOre.getValue()) {
            blocks.add(Blocks.DIAMOND_ORE);
            blocks.add(Blocks.DEEPSLATE_DIAMOND_ORE);
        }
        if (coalOre.getValue()) {
            blocks.add(Blocks.COAL_ORE);
            blocks.add(Blocks.DEEPSLATE_COAL_ORE);
        }
        if (goldOre.getValue()) {
            blocks.add(Blocks.GOLD_ORE);
            blocks.add(Blocks.DEEPSLATE_GOLD_ORE);
            blocks.add(Blocks.NETHER_GOLD_ORE);
        }
        if (ironOre.getValue()) {
            blocks.add(Blocks.IRON_ORE);
            blocks.add(Blocks.DEEPSLATE_IRON_ORE);
        }
        if (lapisOre.getValue()) {
            blocks.add(Blocks.LAPIS_ORE);
            blocks.add(Blocks.DEEPSLATE_LAPIS_ORE);
        }
        if (redstoneOre.getValue()) {
            blocks.add(Blocks.REDSTONE_ORE);
            blocks.add(Blocks.DEEPSLATE_REDSTONE_ORE);
        }
        if (netheriteOre.getValue()) {
            blocks.add(Blocks.ANCIENT_DEBRIS);
        }
        if (obsidian.getValue()) {
            blocks.add(Blocks.OBSIDIAN);
            blocks.add(Blocks.CRYING_OBSIDIAN);
        }
        if (water.getValue()) {
            blocks.add(Blocks.WATER);
        }
        if (lava.getValue()) {
            blocks.add(Blocks.LAVA);
        }
        if (chests.getValue()) {
            blocks.add(Blocks.CHEST);
            blocks.add(Blocks.TRAPPED_CHEST);
            blocks.add(Blocks.ENDER_CHEST);
            blocks.add(Blocks.BARREL);
        }
        
        return blocks;
    }

    public boolean isBlockVisible(Block block) {
        return getVisibleBlocks().contains(block);
    }
}
