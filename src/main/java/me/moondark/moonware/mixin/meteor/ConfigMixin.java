package me.moondark.moonware.mixin.meteor;

// import meteordevelopment.meteorclient.settings.BoolSetting;
// import meteordevelopment.meteorclient.settings.Setting;
// import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.config.Config;

// import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
// import org.spongepowered.asm.mixin.Shadow;
// import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Config.class, remap = false)
public class ConfigMixin {
    // @Final
    // @Shadow
    // private SettingGroup sgMisc;

    // @Unique
    // public final Setting<Boolean> respectUserPrivacy = sgMisc.add(new BoolSetting.Builder()
    //     .name("respect-user-privacy")
    //     .description("When enabled, MoonWare will try to cut off all Meteor's requests to external servers")
    //     .defaultValue(true)
    //     .build()
    // );
}
