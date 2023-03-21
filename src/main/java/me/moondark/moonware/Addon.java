package me.moondark.moonware;

import me.moondark.moonware.hud.MountInfo;
import me.moondark.moonware.modules.EntitySpeed;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class Addon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final HudGroup HUD_GROUP = new HudGroup("MoonWare");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor Addon: MoonWare");

        // Modules
        Modules.get().add(new EntitySpeed());

        // HUD
        Hud.get().register(MountInfo.INFO);
    }

    @Override
    public String getPackage() {
        return "me.moondark.moonware";
    }
}
