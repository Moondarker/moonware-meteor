/*
 * By Moon_dark (Moondarker) 29.03.23, gotta love the ADHD
 */

package me.moondark.moonware.modules;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import me.moondark.moonware.Addon;
import me.moondark.moonware.utils.NbtHandling;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.MapRendererAccessor;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;

// TODO: REWRITE THIS - figure out a way to avoid polling, prepare a mixin event for whenever textures are ready for use (this might be useful for dumping in-world entity maps too).
public class MapDumper extends Module {
    private final Integer MAGIC_NO_TEXTURE_SIZE = 703; // Idk why, and this is massive crutch, but length of unloaded maps in bytes is 703
    private static final File FOLDER = new File(MeteorClient.FOLDER, "maps");

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> mapMoveDelay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The minimum delay between moving the next map stack in milliseconds.")
        .defaultValue(20)
        .sliderMax(1000)
        .build()
    );

    private final Setting<Integer> mapAnalyzeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("analyze-delay")
        .description("The minimum delay before trying to check if the map has been cached from the remote server in milliseconds.")
        .defaultValue(20)
        .range(10, 1000)
        .sliderMax(1000)
        .build()
    );

    private final Setting<Integer> mapReturnDelay = sgGeneral.add(new IntSetting.Builder()
        .name("return-delay")
        .description("The minimum delay before trying to return the cached map stack to original container in milliseconds.")
        .defaultValue(20)
        .range(10, 1000)
        .sliderMax(1000)
        .build()
    );

    private final Setting<Integer> mapRandomDelay = sgGeneral.add(new IntSetting.Builder()
        .name("random")
        .description("Randomly adds a delay of up to the specified time in milliseconds.")
        .min(0)
        .sliderMax(1000)
        .defaultValue(50)
        .build()
    );

    private final Setting<Integer> mapCacheRetryCount = sgGeneral.add(new IntSetting.Builder()
        .name("map-cache-retry-count")
        .description("The maximum amount of times a cache miss might occur for the map before skipping it.")
        .defaultValue(20)
        .range(5, 100)
        .sliderMax(100)
        .build()
    );

    private enum SleepType {
        Move,
        Analyze,
        Return
    }

    public MapDumper() {
        super(Categories.Misc, "map-dumper", "Logs map contents of currently open containers");
    }

    public void dump(ScreenHandler handler) {
        MeteorExecutor.execute(() -> {
            if (!this.isActive()) return;

            int playerInvOffset = getRows(handler) * 9;
            int playerHotbarOffset = playerInvOffset + 3 * 9;

            FindItemResult emptySlot = InvUtils.findInHotbar(ItemStack::isEmpty);

            if (!emptySlot.found()) {
                info("No empty hotbar slots found");
                return;
            }

            int targetHotbarSlot = emptySlot.slot();
            int originalHotbarSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = targetHotbarSlot;

            int targetSlot = targetHotbarSlot + playerHotbarOffset;

            for (int i = 0; i < playerInvOffset; i++) {
                ItemStack currentStack = handler.getSlot(i).getStack();
                if (currentStack == null || currentStack.getItem() != Items.FILLED_MAP) continue;

                MapIdComponent mapId = currentStack.get(DataComponentTypes.MAP_ID);
                MapState mapState = FilledMapItem.getMapState(currentStack, mc.world);
                String mapName = currentStack.getName().getString();

                if (mapState == null) {
                    moveSleep(SleepType.Move);
                    if (mc.currentScreen == null) return;

                    InvUtils.click().slotId(i);
                    InvUtils.click().slotId(targetSlot);

                    moveSleep(SleepType.Analyze);
                    if (mc.currentScreen == null) return;

                    MapPollingResult mapData = awaitMapState(targetSlot, handler);
                    mapId = mapData.getMapId();
                    mapState = mapData.getMapState();

                    awaitTextureState(mapId, mapState, targetHotbarSlot);

                    InvUtils.click().slotId(targetSlot);
                    InvUtils.click().slotId(i);

                    currentStack = handler.getSlot(i).getStack();
                }

                if (mapState != null) {
                    exportMap(mapId, mapState, currentStack.getName().getString(), getOutputPath());
                } else {
                    Addon.LOG.info("[MoonWare] [MapDumper] " + mapName + " (" + String.valueOf(mapId) + ") - failed");
                }
            }

            mc.player.getInventory().selectedSlot = originalHotbarSlot;
        });
    }

    private MapPollingResult awaitMapState(int slotId, ScreenHandler handler) {
        int retryCounter = 0;
        MapState mapState = null;
        ItemStack currentStack = handler.getSlot(slotId).getStack();
        MapIdComponent mapId = currentStack.get(DataComponentTypes.MAP_ID);

        while (mapState == null && retryCounter < mapCacheRetryCount.get()) {
            moveSleep(SleepType.Analyze);
            if (mc.currentScreen == null) return null;

            currentStack = handler.getSlot(slotId).getStack();
            mapId = currentStack.get(DataComponentTypes.MAP_ID);
            mapState = FilledMapItem.getMapState(mapId, mc.world);
            retryCounter++;
        }

        return new MapPollingResult(mapId, mapState);
    }

    private void awaitTextureState(MapIdComponent mapId, MapState mapState, int targetHotbarSlot) {
        int retryCounter = 0;
        int textureSize = 0;

        if (mapState == null) return;

        while (textureSize <= MAGIC_NO_TEXTURE_SIZE && retryCounter < mapCacheRetryCount.get()) {
            moveSleep(SleepType.Return);
            if (mc.currentScreen == null) return;

            if (mc.player.getInventory().selectedSlot != targetHotbarSlot)
                mc.player.getInventory().selectedSlot = targetHotbarSlot;

            textureSize = getMapTextureSize(mapId, mapState);
            retryCounter++;
        }
    }

    private int getMapTextureSize(MapIdComponent mapId, MapState mapState) {
        MapRenderer mapRenderer = mc.gameRenderer.getMapRenderer();
        MapRenderer.MapTexture texture = ((MapRendererAccessor) mapRenderer).invokeGetMapTexture(mapId, mapState);
        NativeImage mapImage = texture.texture.getImage();
        
        try {
            return mapImage.getBytes().length;
        } catch (IOException e) {
            Addon.LOG.error("[MoonWare] [MapDumper] While calculating map size for " + mapId + ": " + e.getMessage());
        }

        return -1;
    }

    private void exportMap(MapIdComponent mapId, MapState mapState, String mapName, File path) {
        MapRenderer mapRenderer = mc.gameRenderer.getMapRenderer();
        MapRenderer.MapTexture texture = ((MapRendererAccessor) mapRenderer).invokeGetMapTexture(mapId, mapState);

        try {
            String timestamp = ZonedDateTime
                .now(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd.MM.uuuu HH.mm.ss"));
            String filename = mapId.id() + " at " + timestamp;

            texture.texture.getImage().writeTo(new File(path, filename + ".png"));

            NbtCompound mapData = new NbtCompound();
            mapData.putInt("id", mapId.id());
            mapData.putString("name", mapName);
            mapState.writeNbt(mapData, mc.world.getRegistryManager()); // FixMe: This is unlikely to work, lol
            byte[] colors = mapData.getByteArray("colors");
            mapData.remove("colors");
            FileOutputStream colorsFile = new FileOutputStream(new File(path, filename + ".bin"));
            colorsFile.write(colors);
            colorsFile.close();

            FileOutputStream dataFile = new FileOutputStream(new File(path, filename + ".json"));
            dataFile.write(NbtHandling.nbtToJson(mapData).getBytes(StandardCharsets.UTF_8));
            dataFile.close();
        } catch (IOException e) {
            Addon.LOG.error("[MoonWare] [MapDumper] While saving map data for " + mapId.id() + ": " + e.getMessage());
        }
    }

    private int getSleepTime(SleepType sleepType) {
        int baseDelay = 0;

        switch (sleepType) {
            case Move:
                baseDelay = mapMoveDelay.get();
                break;
            case Analyze:
                baseDelay = mapAnalyzeDelay.get();
                break;
            case Return:
                baseDelay = mapReturnDelay.get();
                break;
            default:
                break;
        }

        return baseDelay + (mapRandomDelay.get() > 0 ? ThreadLocalRandom.current().nextInt(0, mapRandomDelay.get()) : 0);
    }

    private int getRows(ScreenHandler handler) {
        return (handler instanceof GenericContainerScreenHandler ? ((GenericContainerScreenHandler) handler).getRows() : 3);
    }

    private void moveSleep(SleepType sleepType) {
        int sleep = getSleepTime(sleepType);
        if (sleep > 0) {
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private String getServerAddress() {
        ServerInfo serverData = mc.getCurrentServerEntry();

        if (serverData == null) return "unknown";

        return serverData.address;
    }

    private File getOutputPath() {
        File result = new File(FOLDER, getServerAddress());
        result.mkdirs();

        return result;
    }

    private class MapPollingResult {
        private MapIdComponent mapId;
        private MapState mapState;

        public MapPollingResult(MapIdComponent mapId, MapState mapState) {
            this.mapId = mapId;
            this.mapState = mapState;
        }

        public MapIdComponent getMapId() {
            return mapId;
        }

        public MapState getMapState() {
            return mapState;
        }
    }
}
