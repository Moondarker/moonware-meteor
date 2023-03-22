package me.moondark.moonware.mixin.meteor;

import me.moondark.moonware.Addon;
import meteordevelopment.meteorclient.utils.network.OnlinePlayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OnlinePlayers.class, remap = false)
public class OnlinePlayersMixin {
    @Redirect(at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/utils/network/MeteorExecutor;execute(Ljava/lang/Runnable;)V"), method = "update()V")
    private static void update(Runnable task) {
        Addon.LOG.info("[MoonWare] Suppressed Meteor /ping API request");
    }

    @Inject(at = @At("HEAD"), method = "leave()V", cancellable = true)
    private static void leave(CallbackInfo info) {
        Addon.LOG.info("[MoonWare] Suppressed Meteor /leave API request");
        info.cancel();
    }
}
