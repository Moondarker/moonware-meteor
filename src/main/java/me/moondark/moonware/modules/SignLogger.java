/*
 * By Moon_dark (Moondarker) 04.03.24
 * Based on StashFinder - Copyright (c) Meteor Development.
 */

package me.moondark.moonware.modules;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HangingSignBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.block.WallHangingSignBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SignLogger extends Module {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> sendNotifications = sgGeneral.add(new BoolSetting.Builder()
        .name("notifications")
        .description("Sends Minecraft notifications when new signs are found.")
        .defaultValue(true)
        .build()
    );

    public List<Chunk> chunks = new ArrayList<>();

    public SignLogger() {
        super(Categories.World, "sign-logger", "Searches loaded chunks for signs. Saves to <your minecraft folder>/meteor-client");
    }

    @Override
    public void onActivate() {
        load();
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        Chunk chunk = new Chunk(event.chunk().getPos());

        for (BlockEntity blockEntity : event.chunk().getBlockEntities().values()) {
            if (blockEntity instanceof SignBlockEntity) {
                SignBlockEntity signBlockEntity = (SignBlockEntity) blockEntity;
                BlockState blockState = event.chunk().getBlockState(blockEntity.getPos());
                chunk.signs.add(new Sign(signBlockEntity, blockState));
            }
        }

        if (chunk.getTotal() >= 1) {
            Chunk prevChunk = null;
            int i = chunks.indexOf(chunk);

            if (i < 0) chunks.add(chunk);
            else prevChunk = chunks.set(i, chunk);

            saveJson();
            saveCsv();

            if (sendNotifications.get() && (!chunk.equals(prevChunk) || !chunk.countsEqual(prevChunk))) {
                info("Found signs at (highlight)%s(default), (highlight)%s(default).", chunk.x, chunk.z);
            }
        }
    }

    private void load() {
        boolean loaded = false;

        // Try to load json
        File file = getJsonFile();
        if (file.exists()) {
            try {
                FileReader reader = new FileReader(file);
                chunks = GSON.fromJson(reader, new TypeToken<List<Chunk>>() {}.getType());
                reader.close();

                for (Chunk chunk : chunks) chunk.calculatePos();

                loaded = true;
            } catch (Exception ignored) {
                if (chunks == null) chunks = new ArrayList<>();
            }
        }

        // Try to load csv
        file = getCsvFile();
        if (!loaded && file.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                reader.readLine();

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] values = line.split("(?!\\B\"[^\"]*),(?![^\"]*\"\\B)");
                    ChunkPos chunkPos = new ChunkPos(Integer.parseInt(values[0]) >> 4, Integer.parseInt(values[2]) >> 4);
                    Chunk chunk = chunks.stream().filter(c -> c.chunkPos.equals(chunkPos)).findAny().orElse(null);

                    if (chunk == null) {
                        chunk = new Chunk(chunkPos);
                        chunks.add(chunk);
                    }

                    chunk.signs.add(new Sign(values));
                }

                reader.close();
            } catch (Exception ignored) {
                if (chunks == null) chunks = new ArrayList<>();
            }
        }
    }

    private void saveCsv() {
        try {
            File file = getCsvFile();
            file.getParentFile().mkdirs();
            Writer writer = new FileWriter(file);

            writer.write("X,Y,Z,Type,Facing,FrontText,BackText\n");
            for (Chunk chunk : chunks) chunk.write(writer);

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveJson() {
        try {
            File file = getJsonFile();
            file.getParentFile().mkdirs();
            Writer writer = new FileWriter(file);
            GSON.toJson(chunks, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getJsonFile() {
        return new File(new File(new File(MeteorClient.FOLDER, "signs"), Utils.getFileWorldName()), "signs.json");
    }

    private File getCsvFile() {
        return new File(new File(new File(MeteorClient.FOLDER, "signs"), Utils.getFileWorldName()), "signs.csv");
    }

    @Override
    public String getInfoString() {
        return String.valueOf(chunks.size());
    }

    public enum Mode {
        Chat,
        Toast,
        Both
    }

    public class Sign {
        public transient int x, y, z;
        public String type;
        public String facing;
        public Text[] frontText;
        public Text[] backText;

        public Sign(SignBlockEntity signBlockEntity, BlockState blockState) {
            Block block = blockState.getBlock();
            BlockPos pos = signBlockEntity.getPos();
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();

            this.type = "Unknown";
            this.facing = "unknown";
            if (block instanceof HangingSignBlock) {
                this.type = "HangingSignBlock";
                this.facing = blockState.get(HangingSignBlock.ROTATION).toString();
            } else if (block instanceof SignBlock) {
                this.type = "SignBlock";
                this.facing = blockState.get(SignBlock.ROTATION).toString();
            } else if (block instanceof WallHangingSignBlock) {
                this.type = "WallHangingSignBlock";
                this.facing = blockState.get(WallHangingSignBlock.FACING).asString();
            } else if (block instanceof WallSignBlock) {
                this.type = "WallSignBlock";
                this.facing = blockState.get(WallSignBlock.FACING).asString();
            }

            this.frontText = signBlockEntity.getFrontText().getMessages(false);
            this.backText = signBlockEntity.getBackText().getMessages(false);
        }

        public Sign(String[] values) {
            this.x = Integer.parseInt(values[0]);
            this.y = Integer.parseInt(values[1]);
            this.z = Integer.parseInt(values[2]);

            this.type = values[3];
            this.facing = values[4];

            String[] linesFront = values[5].substring(1, values[4].length() - 1)
                                    .replaceAll("\"\"", "\"")
                                    .split("\\n");

            String[] linesBack = values[6].substring(1, values[4].length() - 1)
                                    .replaceAll("\"\"", "\"")
                                    .split("\\n");
            

            for (int line = 0; line < 4; line++) {
                this.frontText[line] = Text.literal(linesFront[line]);
                this.backText[line] = Text.literal(linesBack[line]);
            }
        }

        public String[] getTextAsStrings(boolean backText) {
            String[] result = new String[]{"", "", "", ""};

            for (int line = 0; line < 4; line++) {
                TextContent content = (backText ? this.backText : this.frontText)[line].getContent();
                if (content instanceof PlainTextContent.Literal) {
                    result[line] = ((PlainTextContent.Literal) content).string();
                } else {
                    result[line] = content.toString();
                }
            }

            return result;
        }
    }

    public static class Chunk {
        private static final StringBuilder sb = new StringBuilder();

        public ChunkPos chunkPos;
        public transient int x, z;
        public ArrayList<Sign> signs;

        public Chunk(ChunkPos chunkPos) {
            this.chunkPos = chunkPos;
            this.signs = new ArrayList<Sign>();

            calculatePos();
        }

        public void calculatePos() {
            x = chunkPos.x * 16 + 8;
            z = chunkPos.z * 16 + 8;
        }

        public int getTotal() {
            return signs.size();
        }

        public void write(Writer writer) throws IOException {
            sb.setLength(0);
            for (Sign sign : signs) {
                sb.append(sign.x).append(',')
                  .append(sign.y).append(',')
                  .append(sign.z).append(',')
                  .append(sign.type).append(',')
                  .append(sign.facing).append(",\"")
                  .append(String.join("\\\\n", sign.getTextAsStrings(false)).replaceAll("\"","\"\"")).append("\",\"")
                  .append(String.join("\\\\n", sign.getTextAsStrings(true)).replaceAll("\"","\"\"")).append("\"\n");
            }
            writer.write(sb.toString());
        }

        public boolean countsEqual(Chunk c) {
            if (c == null) return false;
            return signs.size() != c.signs.size();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Chunk chunk = (Chunk) o;
            return Objects.equals(chunkPos, chunk.chunkPos);
        }

        @Override
        public int hashCode() {
            return Objects.hash(chunkPos);
        }
    }
}
