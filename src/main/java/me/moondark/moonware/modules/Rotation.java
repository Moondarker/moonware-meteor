/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Modified-By: Moondarker (https://github.com/Moondarker/moonware-meteor).
 * Copyright (c) Meteor Development.
 */

package me.moondark.moonware.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class Rotation extends Module {
    private final SettingGroup sgYaw = settings.createGroup("Yaw");
    private final SettingGroup sgPitch = settings.createGroup("Pitch");

    // Yaw

    private final Setting<LockMode> yawLockMode = sgYaw.add(new EnumSetting.Builder<LockMode>()
        .name("yaw-lock-mode")
        .description("The way in which your yaw is locked.")
        .defaultValue(LockMode.Simple)
        .build()
    );

    private final Setting<Double> yawAngle = sgYaw.add(new DoubleSetting.Builder()
        .name("yaw-angle")
        .description("Yaw angle in degrees.")
        .defaultValue(0)
        .sliderMax(360)
        .max(360)
        .visible(() -> yawLockMode.get() == LockMode.Simple)
        .build()
    );

    // Pitch

    private final Setting<LockMode> pitchLockMode = sgPitch.add(new EnumSetting.Builder<LockMode>()
        .name("pitch-lock-mode")
        .description("The way in which your pitch is locked.")
        .defaultValue(LockMode.Simple)
        .build()
    );

    private final Setting<Double> pitchAngle = sgPitch.add(new DoubleSetting.Builder()
        .name("pitch-angle")
        .description("Pitch angle in degrees.")
        .defaultValue(0)
        .range(-90, 90)
        .sliderRange(-90, 90)
        .visible(() -> pitchLockMode.get() == LockMode.Simple)
        .build()
    );

    public Rotation() {
        super(Categories.Player, "rotation", "Changes/locks your yaw and pitch.");
    }

    @Override
    public void onActivate() {
        onTick(null);
    }

    @Override
    public void toggle() {
        super.toggle();

        if (this.isActive()) {
            if (yawLockMode.get() == LockMode.SmartLocked) {
                yawAngle.set((double) getSmartYawDirection());
            }
    
            if (pitchLockMode.get() == LockMode.SmartLocked) {
                pitchAngle.set((double) getSmartPitchDirection());
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        switch (yawLockMode.get()) {
            case Simple, SmartLocked -> setYawAngle(yawAngle.get().floatValue());
            case Smart  -> setYawAngle(getSmartYawDirection());
            case None -> {}
        }

        switch (pitchLockMode.get()) {
            case Simple, SmartLocked -> mc.player.setPitch(pitchAngle.get().floatValue());
            case Smart  -> mc.player.setPitch(getSmartPitchDirection());
            case None -> {}
        }
    }

    private float getSmartYawDirection() {
        return Math.round((mc.player.getYaw() + 1f) / 45f) * 45f;
    }

    private float getSmartPitchDirection() {
        return Math.round((mc.player.getPitch() + 1f) / 30f) * 30f;
    }

    private void setYawAngle(float yawAngle) {
        mc.player.setYaw(yawAngle);
        mc.player.headYaw = yawAngle;
        mc.player.bodyYaw = yawAngle;
    }

    public enum LockMode {
        Smart,
        Simple,
        None,
        SmartLocked
    }
}
