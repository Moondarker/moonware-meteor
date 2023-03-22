package me.moondark.moonware.mixin.meteor;

import meteordevelopment.meteorclient.utils.Utils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Utils.class, remap = false)
public class UtilsMixin {
    @Shadow
    public static boolean firstTimeTitleScreen = false; // Skips initial Meteor version check
}
