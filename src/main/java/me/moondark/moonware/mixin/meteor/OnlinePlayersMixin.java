package me.moondark.moonware.mixin.meteor;

import me.moondark.moonware.Addon;
import meteordevelopment.meteorclient.utils.network.OnlinePlayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OnlinePlayers.class, remap = false)
public class OnlinePlayersMixin {
    @Inject(at = @At("HEAD"), method = "update()V", cancellable = true)
    public static void update(CallbackInfo info) {
        Addon.LOG.info("[MoonWare] Suppressed Meteor /ping API request routine");
        info.cancel();
    }

    @Inject(at = @At("HEAD"), method = "leave()V", cancellable = true)
    public static void leave(CallbackInfo info) {
        Addon.LOG.info("[MoonWare] Suppressed Meteor /leave API request routine");
        info.cancel();
    }
}
