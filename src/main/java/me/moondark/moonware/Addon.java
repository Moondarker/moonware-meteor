package me.moondark.moonware;

import me.moondark.moonware.hud.HudExample;
import me.moondark.moonware.modules.EntitySpeed;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class Addon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor Addon: MoonWare");

        // Modules
        Modules.get().add(new EntitySpeed());

        // HUD
        Hud.get().register(HudExample.INFO);
    }

    @Override
    public String getPackage() {
        return "me.moondark.moonware";
    }
}
