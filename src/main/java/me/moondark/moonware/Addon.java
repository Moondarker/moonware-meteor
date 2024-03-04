package me.moondark.moonware;

import me.moondark.moonware.hud.MountInfo;
import me.moondark.moonware.modules.*;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;

import org.slf4j.Logger;

public class Addon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY_LITEMATICA = new Category("Litematica", Items.SCAFFOLDING.getDefaultStack());
    public static final HudGroup HUD_GROUP = new HudGroup("MoonWare");

    private boolean isLitematicaPresent() {
        try {
            Class.forName("fi.dy.masa.litematica.Litematica");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor Addon: MoonWare");

        // Modules
        Modules.get().add(new AutoLog());
        Modules.get().add(new EntitySpeed());
        Modules.get().add(new Rotation());
        Modules.get().add(new MapDumper());
        Modules.get().add(new SignLogger());
        if (Modules.getCategoryByHash(CATEGORY_LITEMATICA.hashCode()) != null) {
            Modules.get().add(new SchematicSafeguard());
        }

        // HUD
        Hud.get().register(MountInfo.INFO);
    }

    @Override
    public void onRegisterCategories() {
        if (isLitematicaPresent()) {
            Modules.registerCategory(CATEGORY_LITEMATICA);
        } else {
            LOG.info("[MoonWare] Litematica appears to be missing. Skipping related category.");
        }
    }

    @Override
    public String getPackage() {
        return "me.moondark.moonware";
    }
}
