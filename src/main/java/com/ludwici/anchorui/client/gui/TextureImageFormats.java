package com.ludwici.anchorui.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Decode additional image assets into real Minecraft textures without changing resource-pack files. */
public final class TextureImageFormats {
    private static final int MAX_COMPRESSED_BYTES = 64 * 1024 * 1024;
    private static final int MAX_DIMENSION = 16384;
    private static final long MAX_PIXELS = 32L * 1024 * 1024;
    private static final Set<ResourceLocation> REGISTERED = ConcurrentHashMap.newKeySet();

    private TextureImageFormats() {
    }

    public static void clear() {
        REGISTERED.clear();
    }

    public static boolean needsConversion(ResourceLocation id) {
        return id != null && !id.getPath().endsWith(".png");
    }

    public static NativeImage decode(InputStream input) throws IOException {
        byte[] bytes = input.readNBytes(MAX_COMPRESSED_BYTES + 1);
        if (bytes.length == 0 || bytes.length > MAX_COMPRESSED_BYTES) {
            throw new IOException("Image data is empty or exceeds 64 MiB");
        }
        ByteBuffer encoded = MemoryUtil.memAlloc(bytes.length);
        try {
            encoded.put(bytes).flip();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);
                ByteBuffer pixels = STBImage.stbi_load_from_memory(encoded, width, height, channels, 4);
                if (pixels == null) {
                    throw new IOException("Unsupported or corrupt image: " + STBImage.stbi_failure_reason());
                }
                try {
                    int w = width.get(0);
                    int h = height.get(0);
                    if (w <= 0 || h <= 0 || w > MAX_DIMENSION || h > MAX_DIMENSION || (long) w * h > MAX_PIXELS) {
                        throw new IOException("Unsupported image dimensions: " + w + " x " + h);
                    }
                    NativeImage image = new NativeImage(NativeImage.Format.RGBA, w, h, false);
                    try {
                        for (int y = 0; y < h; y++) {
                            for (int x = 0; x < w; x++) {
                                int pixel = (y * w + x) * 4;
                                int r = pixels.get(pixel) & 0xFF;
                                int g = pixels.get(pixel + 1) & 0xFF;
                                int b = pixels.get(pixel + 2) & 0xFF;
                                int a = pixels.get(pixel + 3) & 0xFF;
                                image.setPixelRGBA(x, y, a << 24 | b << 16 | g << 8 | r);
                            }
                        }
                        return image;
                    } catch (RuntimeException exception) {
                        image.close();
                        throw exception;
                    }
                } finally {
                    STBImage.stbi_image_free(pixels);
                }
            }
        } finally {
            MemoryUtil.memFree(encoded);
        }
    }

    public static NativeImage open(ResourceManager resources, ResourceLocation id) throws IOException {
        var found = resources.getResource(id);
        if (found.isEmpty()) {
            throw new IOException("Missing image resource " + id);
        }
        try (InputStream stream = found.get().open()) {
            if (!needsConversion(id)) {
                return NativeImage.read(stream);
            }
            return decode(stream);
        }
    }

    public static void ensureRegistered(Minecraft minecraft, ResourceLocation id) throws IOException {
        if (!needsConversion(id) || REGISTERED.contains(id)) {
            return;
        }
        NativeImage image = open(minecraft.getResourceManager(), id);
        DynamicTexture texture;
        try {
            texture = new DynamicTexture(image);
        } catch (RuntimeException exception) {
            image.close();
            throw exception;
        }
        try {
            minecraft.getTextureManager().register(id, texture);
            REGISTERED.add(id);
        } catch (RuntimeException exception) {
            texture.close();
            throw exception;
        }
    }
}
