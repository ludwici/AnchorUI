package com.ludwici.anchorui.client.gui.editor;

import com.mojang.blaze3d.platform.NativeImage;
import com.ludwici.anchorui.client.gui.TextureImageFormats;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EditorTextureDecodingTest {
    private static final Map<String, String> IMAGES = Map.of("PNG", "iVBORw0KGgoAAAANSUhEUgAAAAIAAAADCAIAAAA2iEnWAAAAFElEQVR4nGM8IRLFwMDAxMDAgKAAF8QBPIkOmgkAAAAASUVORK5CYII=", "BMP", "Qk1OAAAAAAAAADYAAAAoAAAAAgAAAAMAAAABABgAAAAAABgAAADEDgAAxA4AAAAAAAAAAAAAWhTIWhTIAABaFMhaFMgAAFoUyFoUyAAA", "TGA", "AAACAAAAAAAAAAAAAgADABgAWhTIWhTIWhTIWhTIWhTIWhTIAAAAAAAAAABUUlVFVklTSU9OLVhGSUxFLgA=", "GIF", "R0lGODdhAgADAIEAAMgUWgAAAAAAAAAAACwAAAAAAgADAAAIBgABCBwYEAA7", "PNM", "UDYKMiAzCjI1NQrIFFrIFFrIFFrIFFrIFFrIFFo=");

    @Test
    public void decodesPNG() throws IOException {
        assertDecodes("PNG");
    }

    @Test
    public void decodesJPEG() throws IOException {
        assertDecodes("JPEG");
    }

    @Test
    public void decodesBMP() throws IOException {
        assertDecodes("BMP");
    }

    @Test
    public void decodesTGA() throws IOException {
        assertDecodes("TGA");
    }

    @Test
    public void decodesGIF() throws IOException {
        assertDecodes("GIF");
    }

    @Test
    public void decodesPNM() throws IOException {
        assertDecodes("PNM");
    }

    private static void assertDecodes(String format) throws IOException {
        byte[] data;
        if ("JPEG".equals(format)) {
            BufferedImage image = new BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(image, "jpeg", bytes);
            data = bytes.toByteArray();
        } else {
            data = Base64.getDecoder().decode(IMAGES.get(format));
        }
        EditorTextureCatalog.ImageInfo dimensions = EditorTextureCatalog.decode(new ByteArrayInputStream(data));
        assertEquals(new EditorTextureCatalog.ImageInfo(2, 3), dimensions, format);
    }

    @Test
    public void convertedImagePixelsKeepCorrectColorAndAlphaChannelOrder() throws IOException {
        byte[] data = Base64.getDecoder().decode(IMAGES.get("PNG"));
        try (NativeImage image = TextureImageFormats.decode(new ByteArrayInputStream(data))) {
            assertEquals(2, image.getWidth());
            assertEquals(3, image.getHeight());
            assertEquals(0xFF5A14C8, image.getPixelRGBA(0, 0));
        }
    }

    @Test
    public void malformedImageDataIsRejectedByTheNativeImageDecoder() {
        assertThrows(IOException.class, () -> EditorTextureCatalog.decode(new ByteArrayInputStream(new byte[] {1, 2, 3, 4})));
    }
}
