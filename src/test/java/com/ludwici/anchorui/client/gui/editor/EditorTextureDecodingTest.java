package com.ludwici.anchorui.client.gui.editor;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EditorTextureDecodingTest {
    @Test
    public void nativeImageDecodesSupportedImageExamplesForTheTexturePicker() throws IOException {
        Map<String, String> images = Map.of("PNG", "iVBORw0KGgoAAAANSUhEUgAAAAIAAAADCAIAAAA2iEnWAAAAFElEQVR4nGM8IRLFwMDAxMDAgKAAF8QBPIkOmgkAAAAASUVORK5CYII=", "JPEG", "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAADAAIDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwDkaKKK0P0s/9k=", "BMP", "Qk1OAAAAAAAAADYAAAAoAAAAAgAAAAMAAAABABgAAAAAABgAAADEDgAAxA4AAAAAAAAAAAAAWhTIWhTIAABaFMhaFMgAAFoUyFoUyAAA", "TGA", "AAACAAAAAAAAAAAAAgADABgAWhTIWhTIWhTIWhTIWhTIWhTIAAAAAAAAAABUUlVFVklTSU9OLVhGSUxFLgA=", "GIF", "R0lGODdhAgADAIEAAMgUWgAAAAAAAAAAACwAAAAAAgADAAAIBgABCBwYEAA7", "PNM", "UDYKMiAzCjI1NQrIFFrIFFrIFFrIFFrIFFrIFFo=");
        for (var item : images.entrySet()) {
            byte[] data = Base64.getDecoder().decode(item.getValue());
            try {
                EditorTextureCatalog.ImageInfo dimensions = EditorTextureCatalog.decode(new ByteArrayInputStream(data));
                assertEquals(new EditorTextureCatalog.ImageInfo(2, 3), dimensions, item.getKey());
            } catch (IOException exception) {
                throw new IOException("Unsupported decoder fixture: " + item.getKey(), exception);
            }
        }
    }

    @Test
    public void malformedImageDataIsRejectedByTheNativeImageDecoder() {
        assertThrows(IOException.class, () -> EditorTextureCatalog.decode(new ByteArrayInputStream(new byte[] {1, 2, 3, 4})));
    }
}
