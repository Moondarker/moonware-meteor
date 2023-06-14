/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package me.moondark.moonware.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.InventoryTweaks;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.moondark.moonware.modules.MapDumper;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen implements ScreenHandlerProvider<T> {
    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;  
    @Shadow @Final protected T handler;

    public HandledScreenMixin(Text title) {
        super(title);
    }

    @Inject(at = @At("RETURN"), method = "init()V") // Not the best way, but Meteor overrides it in child handler classes ;(
    protected void init(CallbackInfo info) {
        if (handler instanceof ShulkerBoxScreenHandler || handler instanceof GenericContainerScreenHandler) {
            InventoryTweaks invTweaks = Modules.get().get(InventoryTweaks.class);
            MapDumper mapDumper = Modules.get().get(MapDumper.class);
    
            if (mapDumper.isActive()) {
                boolean offsetButtons = invTweaks.isActive() && invTweaks.showButtons();

                addDrawableChild(
                    new ButtonWidget.Builder(Text.literal("Maps ↓"), button -> mapDumper.dump(handler))
                        .position(x + backgroundWidth - (offsetButtons ? 130 : 46), y + 3)
                        .size(40, 12)
                        .build()
                );
            }
        }
    }
}
