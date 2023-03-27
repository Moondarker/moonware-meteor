package me.moondark.moonware.modules;

import me.moondark.moonware.Addon;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;

public class MapartShulkerLogger extends Module {
    private static final ItemStack[] ITEMS = new ItemStack[27];

    public MapartShulkerLogger() {
        super(Categories.Misc, "mapart-shulker-logger", "Logs mapart contents of whatever you have in your hand");
    }
    
    @Override
    public void onActivate() {
        ItemStack itemStackHand = getShulker();

        if (itemStackHand != null) {
            Utils.getItemsInContainerItem(itemStackHand, ITEMS);

            Addon.LOG.info("You might need to open the shulker and look at each map in the hand for maps to cache on client side before using this module");
            Addon.LOG.info("ShulkerName;MapId;MapName");
            
            for (ItemStack mapStack : ITEMS) {
                if (mapStack == null) continue;

                if (mapStack.getItem() == Items.FILLED_MAP) {
                    Integer mapId = FilledMapItem.getMapId(mapStack);
                    MapState map = FilledMapItem.getMapState(mapId, mc.world);
                    if (map == null) continue;

                    Addon.LOG.info("\"" + itemStackHand.getName().getString().replaceAll("\"", "\"\"") + "\";" + mapId.toString() + ";\"" + mapStack.getName().getString().replaceAll("\"", "\"\"") + "\"");
                }
            }
        }

        toggle();
    }

    private ItemStack getShulker() {
        ItemStack itemStack = mc.player.getMainHandStack();
        if (Utils.hasItems(itemStack)) return itemStack;

        itemStack = mc.player.getOffHandStack();
        if (Utils.hasItems(itemStack)) return itemStack;

        return null;
    }
}
