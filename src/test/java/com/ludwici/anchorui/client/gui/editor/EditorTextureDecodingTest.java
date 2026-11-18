package com.ludwici.anchorui.client.gui.editor;

import com.mojang.blaze3d.platform.NativeImage;
import com.ludwici.anchorui.client.gui.TextureImageFormats;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EditorTextureDecodingTest {
    private static final String PNG_FIXTURE = "iVBORw0KGgoAAAANSUhEUgAAAAIAAAADCAIAAAA2iEnWAAAAFElEQVR4nGM8IRLFwMDAxMDAgKAAF8QBPIkOmgkAAAAASUVORK5CYII=";

    @Test
    public void pngIsStillHandledByTheNativeMinecraftLoader() throws IOException {
        assertTrue(TextureImageFormats.isSupported(net.minecraft.resources.ResourceLocation.parse("demo:ui/skin.png")));
        assertTrue(!TextureImageFormats.needsConversion(net.minecraft.resources.ResourceLocation.parse("demo:ui/skin.png")));
        try (NativeImage image = NativeImage.read(new ByteArrayInputStream(Base64.getDecoder().decode(PNG_FIXTURE)))) {
            assertEquals(2, image.getWidth());
            assertEquals(3, image.getHeight());
        }
    }

    @Test
    public void jpegConvertsToAnOpaqueNativeImageWithCorrectColorChannels() throws IOException {
        BufferedImage source = new BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 2; x++) {
                source.setRGB(x, y, 0xFFCC3344);
            }
        }
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(source, "jpeg", encoded));
        try (NativeImage image = TextureImageFormats.decodeJpeg(new ByteArrayInputStream(encoded.toByteArray()))) {
            assertEquals(2, image.getWidth());
            assertEquals(3, image.getHeight());
            int color = image.getPixelRGBA(0, 0);
            assertEquals(0xFF, (color >>> 24) & 0xFF);
            assertTrue(Math.abs((color & 0xFF) - 0xCC) < 10);
            assertTrue(Math.abs(((color >>> 8) & 0xFF) - 0x33) < 10);
            assertTrue(Math.abs(((color >>> 16) & 0xFF) - 0x44) < 10);
        }
    }

    @Test
    public void rejectsNonJpegImageData() {
        assertThrows(IOException.class, () -> TextureImageFormats.decodeJpeg(new ByteArrayInputStream(Base64.getDecoder().decode(PNG_FIXTURE))));
        assertThrows(IOException.class, () -> TextureImageFormats.decodeJpeg(new ByteArrayInputStream(new byte[] {1, 2, 3, 4})));
        assertThrows(IOException.class, () -> TextureImageFormats.decodeJpeg(new ByteArrayInputStream(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0})));
    }
}
