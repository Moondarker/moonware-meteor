package me.moondark.moonware.hud;

import me.moondark.moonware.Addon;
import meteordevelopment.meteorclient.mixininterface.IHorseBaseEntity;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemSteerable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Saddleable;
import net.minecraft.entity.passive.AbstractHorseEntity;

public class MountInfo extends HudElement {
    public static final HudElementInfo<MountInfo> INFO = new HudElementInfo<>(Addon.HUD_GROUP, "mount-info", "Shows mount-related info: speed and jump height.", MountInfo::new);
    public static final float SPEED_CONVERT_FACTOR = 43.17f;
    public static final float JUMP_CONVERT_FACTOR = 5.25f;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMechanics = settings.createGroup("Mechanics");
    private final SettingGroup sgScale = settings.createGroup("Scale");
    private final SettingGroup sgBackground = settings.createGroup("Background");

    protected final MinecraftClient mc;
    private boolean saddleResetNeeded = false;

    // General
    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Text shadow.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
        .name("text-color")
        .description("A.")
        .defaultValue(new SettingColor())
        .build()
    );

    private final Setting<SettingColor> speedColor = sgGeneral.add(new ColorSetting.Builder()
        .name("speed-color")
        .description("First color.")
        .defaultValue(new SettingColor(173, 173, 173))
        .build()
    );

    private final Setting<SettingColor> jumpColor = sgGeneral.add(new ColorSetting.Builder()
        .name("jump-color")
        .description("Second color.")
        .defaultValue(new SettingColor(173, 173, 173))
        .build()
    );

    private final Setting<Integer> border = sgGeneral.add(new IntSetting.Builder()
        .name("border")
        .description("How much space to add around the element.")
        .defaultValue(0)
        .build()
    );

    // Mechanics
    private final Setting<Boolean> overrideSaddle = sgMechanics.add(new BoolSetting.Builder()
        .name("override-saddle")
        .description("If entity is not saddled, put a fake one momentarily to get speed values.")
        .defaultValue(true)
        .build()
    );

    // Scale
    private final Setting<Boolean> customScale = sgScale.add(new BoolSetting.Builder()
        .name("custom-scale")
        .description("Applies custom text scale rather than the global one.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> scale = sgScale.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Custom scale.")
        .visible(customScale::get)
        .defaultValue(1)
        .min(0.5)
        .sliderRange(0.5, 3)
        .build()
    );

    // Background
    private final Setting<Boolean> background = sgBackground.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays background.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgBackground.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color used for the background.")
        .visible(background::get)
        .defaultValue(new SettingColor(25, 25, 25, 50))
        .build()
    );

    public MountInfo() {
        super(INFO);
        this.mc = MinecraftClient.getInstance();
    }

    @Override
    public void render(HudRenderer renderer) {
        Entity mount = mc.player.getVehicle();

        if (background.get()) {
            renderer.quad(this.x, this.y, getWidth(), getHeight(), backgroundColor.get());
        }

        if (isInEditor()) {
            render(renderer, "14.57", "5.25");
            return;
        }

        if (mount == null) return;

        if (mount instanceof AbstractHorseEntity) {
            AbstractHorseEntity horseMount = (AbstractHorseEntity) mount;
            float speed = horseMount.getMovementSpeed();

            if (overrideSaddle.get() && speed == 0 && !horseMount.isSaddled()) {
                ((IHorseBaseEntity) horseMount).setSaddled(true);
                saddleResetNeeded = true;
            }

            if (overrideSaddle.get() && saddleResetNeeded && speed != 0 && horseMount.isSaddled()) {
                ((IHorseBaseEntity) horseMount).setSaddled(false);
                saddleResetNeeded = false;
            }

            render(renderer, String.format("%.2f", horseMount.getMovementSpeed() * SPEED_CONVERT_FACTOR), String.format("%.2f", horseMount.getJumpStrength() * JUMP_CONVERT_FACTOR));
            return;
        }

        if (mount instanceof Saddleable && mount instanceof LivingEntity) {
            LivingEntity steerableMount = (LivingEntity) mount;
            render(renderer, String.format("%.2f", steerableMount.getSaddledSpeed((LivingEntity) mc.player) * SPEED_CONVERT_FACTOR), "0");
            return;
        }
    }

    private void render(HudRenderer renderer, String speed, String jump) {
        double x = this.x + border.get();
        double y = this.y + border.get();

        double x2 = renderer.text("Speed: ", x, y, textColor.get(), shadow.get(), getScale());
        x2 = renderer.text(speed, x2, y, speedColor.get(), shadow.get(), getScale());
        x2 = renderer.text(" Jump: ", x2, y, textColor.get(), shadow.get(), getScale());
        x2 = renderer.text(jump, x2, y, jumpColor.get(), shadow.get(), getScale());

        setSize(x2 - x, renderer.textHeight(shadow.get(), getScale()));
    }

    private double getScale() {
        return customScale.get() ? scale.get() : -1;
    }
}
