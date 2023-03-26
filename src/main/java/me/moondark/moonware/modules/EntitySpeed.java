/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Modified-By: Moondarker (https://github.com/Moondarker/moonware-meteor).
 * Copyright (c) Meteor Development.
 */

package me.moondark.moonware.modules;

import meteordevelopment.meteorclient.events.entity.LivingEntityMoveEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public class EntitySpeed extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("Horizontal speed in blocks per second.")
            .defaultValue(10)
            .min(0)
            .sliderMax(50)
            .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Use speed only when standing on a block.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> inWater = sgGeneral.add(new BoolSetting.Builder()
            .name("in-water")
            .description("Use speed when in water.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> tpsAdjust = sgGeneral.add(new BoolSetting.Builder()
            .name("tps-adjust")
            .description("Auto-adjust speed based on TPS (useless if server adjusts limits based on TPS on its side).")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> showSpeed = sgGeneral.add(new BoolSetting.Builder()
            .name("show-speed")
            .description("Show speed after module name in module list.")
            .defaultValue(false)
            .build()
    );

    public EntitySpeed() {
        super(Categories.Movement, "entity-speed", "Makes you go faster when riding entities. [MoonWare: added TPS Adjust]");
    }

    private double calcSpeed() {
        float tps = TickRate.INSTANCE.getTickRate();
        if (!tpsAdjust.get() || Float.isNaN(tps)) return speed.get();

        return speed.get() * Math.max(tps / 20, 0.25F);
    }

    @Override
    public String getInfoString() {
        return showSpeed.get() ? String.format("%.2f", speed.get()) + (tpsAdjust.get() ? " (" + String.format("%.2f", calcSpeed()) + " adj)" : "") : null;
    }

    @EventHandler
    private void onLivingEntityMove(LivingEntityMoveEvent event) {
        if (event.entity.getPrimaryPassenger() != mc.player) return;

        // Check for onlyOnGround and inWater
        LivingEntity entity = event.entity;
        if (onlyOnGround.get() && !entity.isOnGround()) return;
        if (!inWater.get() && entity.isTouchingWater()) return;

        // Set horizontal velocity
        Vec3d vel = PlayerUtils.getHorizontalVelocity(calcSpeed());
        ((IVec3d) event.movement).setXZ(vel.x, vel.z);
    }
}
