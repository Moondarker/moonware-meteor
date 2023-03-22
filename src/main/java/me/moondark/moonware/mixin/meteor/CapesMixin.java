package me.moondark.moonware.mixin.meteor;

import me.moondark.moonware.Addon;
import meteordevelopment.meteorclient.utils.network.Capes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Capes.class, remap = false)
public class CapesMixin {
    @Inject(at = @At("HEAD"), method = "init()V", cancellable = true)
    private static void init(CallbackInfo info) {
        Addon.LOG.info("[MoonWare] Suppressed Meteor cape list requests");
        info.cancel();
    }
}
