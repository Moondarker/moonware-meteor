/*
 * By Moon_dark (Moondarker) 29.03.23, gotta love the ADHD
 * https://wiki.vg/NBT#Specification
 */

package me.moondark.moonware.utils;

import net.minecraft.nbt.NbtCompound;

import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;

public class NbtHandling {
    public enum Type {
        TAG_End,
        TAG_Byte,
        TAG_Short,
        TAG_Int,
        TAG_Long,
        TAG_Float,
        TAG_Double,
        TAG_Byte_Array,
        TAG_String,
        TAG_List,
        TAG_Compound,
        TAG_Int_Array,
        TAG_Long_Array
    }

    public static String nbtToJson(NbtCompound nbtData) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        return gson.toJson(nbtToJsonObject(nbtData));
    }

    public static JsonObject nbtToJsonObject(NbtCompound nbtData) {
        JsonObject result = new JsonObject();

        for (String key : nbtData.getKeys()) {
            switch (Type.values()[(int) nbtData.getType(key)]) {
                case TAG_Byte:
                    result.addProperty(key, (int) nbtData.getByte(key));
                    break;

                case TAG_Short:
                    result.addProperty(key, nbtData.getShort(key));
                    break;

                case TAG_Int:
                    result.addProperty(key, nbtData.getInt(key));
                    break;

                case TAG_Long:
                    result.addProperty(key, nbtData.getLong(key));
                    break;

                case TAG_Float:
                    result.addProperty(key, nbtData.getFloat(key));
                    break;

                case TAG_Double:
                    result.addProperty(key, nbtData.getDouble(key));
                    break;

                case TAG_Byte_Array:
                    byte[] byteArray = nbtData.getByteArray(key);
                    JsonArray byteArrayJson = new JsonArray();
                    for (byte singleByte : byteArray) {
                        byteArrayJson.add((int) singleByte);
                    }
                    result.add(key, byteArrayJson);
                    break;

                case TAG_String:
                    result.addProperty(key, nbtData.getString(key));
                    break;

                case TAG_List:
                    // NOT IMPLEMENTED
                    break;

                case TAG_Compound:
                    JsonObject innerData = nbtToJsonObject(nbtData.getCompound(key));
                    result.add(key, innerData);
                    break;

                case TAG_Int_Array:
                    int[] intArray = nbtData.getIntArray(key);
                    JsonArray intArrayJson = new JsonArray();
                    for (int singleInt : intArray) {
                        intArrayJson.add(singleInt);
                    }
                    result.add(key, intArrayJson);
                    break;

                case TAG_Long_Array:
                    long[] longArray = nbtData.getLongArray(key);
                    JsonArray longArrayJson = new JsonArray();
                    for (long singleLong : longArray) {
                        longArrayJson.add(singleLong);
                    }
                    result.add(key, longArrayJson);
                    break;
            
                default:
                    break;
            }
        }

        return result;
    }
}
