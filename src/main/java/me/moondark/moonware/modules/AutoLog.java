/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Modified-By: Moondarker (https://github.com/Moondarker/moonware-meteor).
 * Copyright (c) Meteor Development.
 */

package me.moondark.moonware.modules;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.ServerConnectBeginEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class AutoLog extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDestination = settings.createGroup("DestinationLog");

    private final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder()
        .name("health")
        .description("Automatically disconnects when health is lower or equal to this value.")
        .defaultValue(6)
        .range(0, 19)
        .sliderMax(19)
        .build()
    );

    private final Setting<Boolean> smart = sgGeneral.add(new BoolSetting.Builder()
        .name("smart")
        .description("Disconnects when you're about to take enough damage to kill you.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyTrusted = sgGeneral.add(new BoolSetting.Builder()
        .name("only-trusted")
        .description("Disconnects when a player not on your friends list appears in render distance.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> instantDeath = sgGeneral.add(new BoolSetting.Builder()
        .name("32K")
        .description("Disconnects when a player near you can instantly kill you.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> crystalLog = sgGeneral.add(new BoolSetting.Builder()
        .name("crystal-nearby")
        .description("Disconnects when a crystal appears near you.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("How close a crystal has to be to you before you disconnect.")
        .defaultValue(4)
        .range(1, 10)
        .sliderMax(5)
        .visible(crystalLog::get)
        .build()
    );

    private final Setting<Boolean> smartToggle = sgGeneral.add(new BoolSetting.Builder()
        .name("smart-toggle")
        .description("Disables Auto Log after a low-health logout. WILL re-enable once you heal.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> toggleOff = sgGeneral.add(new BoolSetting.Builder()
        .name("toggle-off")
        .description("Disables Auto Log after usage.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> disableAutoReconnect = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-auto-reconnect")
        .description("Disables Auto Reconnect on AutoLog and enables after AutoLog is re-enabled.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> destinationLog = sgDestination.add(new BoolSetting.Builder()
        .name("destination-log")
        .description("Disconnects you when you're withing a specified range to your target destination.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> destinationIgnoreY = sgDestination.add(new BoolSetting.Builder()
        .name("destination-ignore-y")
        .description("Ignore Y (altitude) coordinate.")
        .defaultValue(true)
        .visible(destinationLog::get)
        .build()
    );

    private final Setting<Integer> destinationRadius = sgDestination.add(new IntSetting.Builder()
        .name("destination-radius")
        .description("Distance to target to trigger disconnect at.")
        .defaultValue(250)
        .range(1, 60000000)
        .sliderMax(10000)
        .visible(destinationLog::get)
        .build()
    );   

    private final Setting<Integer> destinationX = sgDestination.add(new IntSetting.Builder()
        .name("destination-x")
        .description("Destination X coordinate.")
        .defaultValue(0)
        .range(-30000000, 30000000)
        .noSlider()
        .visible(destinationLog::get)
        .build()
    );

    private final Setting<Integer> destinationY = sgDestination.add(new IntSetting.Builder()
        .name("destination-y")
        .description("Destination Y coordinate.")
        .defaultValue(0)
        .range(-30000000, 30000000)
        .noSlider()
        .visible(() -> { return destinationLog.get() && !destinationIgnoreY.get(); })
        .build()
    );
    
    private final Setting<Integer> destinationZ = sgDestination.add(new IntSetting.Builder()
        .name("destination-z")
        .description("Destination Z coordinate.")
        .defaultValue(0)
        .range(-30000000, 30000000)
        .noSlider()
        .visible(destinationLog::get)
        .build()
    );

    public boolean enableAutoReconnect = false;

    public AutoLog() {
        super(Categories.Combat, "auto-log", "Automatically disconnects you when certain requirements are met.");

        MeteorClient.EVENT_BUS.subscribe(joinListener);
    }

    @Override
    public String getInfoString() {
        return String.valueOf((destinationLog.get() ? "DL or " : "") + health.get() + " HP");
    }

    private void handleAutoReconnect() {
        Module autoReconnect = Modules.get().get(AutoReconnect.class);
        if (disableAutoReconnect.get() && autoReconnect != null && autoReconnect.isActive()) {
            enableAutoReconnect = true;
            autoReconnect.toggle();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        float playerHealth = mc.player.getHealth();
        if (playerHealth <= 0) {
            this.toggle();
            return;
        }

        if (playerHealth <= health.get()) {
            disconnect("Health was lower than " + health.get() + ".");
            if (smartToggle.get()) {
                this.toggle();
                enableHealthListener();
            }
            return;
        }

        if (smart.get() && playerHealth + mc.player.getAbsorptionAmount()
                - PlayerUtils.possibleHealthReductions() < health.get()) {
            disconnect("Health was going to be lower than " + health.get() + ".");
            if (toggleOff.get()) this.toggle();
            return;
        }

        if (destinationLog.get()) {
            BlockPos playerPos = mc.player.getBlockPos();
            BlockPos targetPos = new BlockPos(destinationX.get(), destinationIgnoreY.get() ? playerPos.getY() : destinationY.get(), destinationZ.get());

            if (playerPos.isWithinDistance(targetPos, destinationRadius.get())) {
                disconnect("You have arrived at your destination!");
                destinationRadius.set(1);
                destinationX.set(0);
                destinationY.set(0);
                destinationZ.set(0);
                destinationLog.set(false);
            }
        }

        if (!onlyTrusted.get() && !instantDeath.get() && !crystalLog.get()) return; // only check all entities if needed

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity && entity.getUuid() != mc.player.getUuid()) {
                if (onlyTrusted.get() && entity != mc.player && !Friends.get().isFriend((PlayerEntity) entity)) {
                    disconnect(" A non-trusted player appeared in your render distance.");
                    if (toggleOff.get()) this.toggle();
                    return;
                }

                if (PlayerUtils.isWithin(entity, 8) && instantDeath.get() && DamageUtils
                        .getSwordDamage((PlayerEntity) entity, true) > playerHealth + mc.player.getAbsorptionAmount()) {
                    disconnect("Anti-32k measures.");
                    if (toggleOff.get()) this.toggle();
                    return;
                }
            }

            if (crystalLog.get() && entity instanceof EndCrystalEntity && PlayerUtils.isWithin(entity, range.get())) {
                disconnect("End Crystal appeared within specified range.");
                if (toggleOff.get()) this.toggle();
            }
        }
    }

    private void disconnect(String reason) {
        handleAutoReconnect();
        mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("[AutoLog] " + reason)));
    }

    private class StaticHealthListener {
        @EventHandler
        private void eventHandler(TickEvent.Post event) {
            if (isActive())
                disableHealthListener();

            else if (Utils.canUpdate()
                    && mc.player != null
                    && !mc.player.isDead()
                    && mc.player.canTakeDamage()
                    && mc.player.getHealth() > health.get()
                    && !mc.player.getInventory().isEmpty()) {
                toggle();
                disableHealthListener();
            }
        }
    }

    private final StaticHealthListener healthListener = new StaticHealthListener();

    private void enableHealthListener() {
        MeteorClient.EVENT_BUS.subscribe(healthListener);
    }

    private void disableHealthListener() {
        MeteorClient.EVENT_BUS.unsubscribe(healthListener);
    }

    private class StaticJoinListener {
        @EventHandler
        private void onGameJoined(ServerConnectBeginEvent event) {
            AutoLog autoLog = Modules.get().get(AutoLog.class);
            AutoReconnect autoReconnect = Modules.get().get(AutoReconnect.class);
            if (autoLog.enableAutoReconnect) {
                autoLog.enableAutoReconnect = false;
                if (!autoReconnect.isActive())
                    autoReconnect.toggle();
            }
        }
    }

    private final StaticJoinListener joinListener = new StaticJoinListener();
}