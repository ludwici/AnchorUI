package com.ludwici.anchorui.client;

import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;

import java.io.File;

import static com.ludwici.anchorui.AnchorUI.MODID;

public class ResLoader {
    public static File dir;

    public static void load() {
        if (dir == null) {
            dir = new File("lore");
            if (!dir.exists()) {
                dir.mkdir();
            }
        }

        File[] subdirs = dir.listFiles(File::isDirectory);
        if (subdirs == null) {
            return;
        }

        for (File d : subdirs) {
            ResourceLocation res;
            try {
                res = ResourceLocation.fromNamespaceAndPath(MODID, d.getName());
            } catch (ResourceLocationException ex) {
                System.out.println(ex.getMessage());
                continue;
            }
        }
    }
}
