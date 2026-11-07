package com.ludwici.anchorui.mixin;

import com.ludwici.anchorui.client.LoreLoader;
import com.ludwici.anchorui.client.gui.ItemLoreScreen;
import com.ludwici.anchorui.KeyRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(Item.class)
public class ItemMixin {

    @Inject(method = "appendHoverText", at = @At("HEAD"))
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag, CallbackInfo ci) {
        showTip(stack, context, tooltipComponents);
    }

    @Unique
    @OnlyIn(Dist.CLIENT)
    private void showTip(ItemStack stack, Item.TooltipContext context, List<Component> components) {
        var data = LoreLoader.INSTANCE.getData();
        var r = BuiltInRegistries.ITEM.getKey(stack.getItem());

        var loreEntity = data.values().stream().filter(e -> {
            var r1 = ResourceLocation.tryParse(e.item);
            return r.equals(r1);
        }).findFirst().orElse(null);

        if (loreEntity == null) {
            return;
        }

        components.add(Component.literal(" "));
        components.add(Component.literal("Нажми X, чтобы узнать больше").withStyle(ChatFormatting.GRAY));

        var minecraft = Minecraft.getInstance();

        if (GLFW.glfwGetKey(minecraft.getWindow().getWindow(), KeyRegistry.SHOW_LORE.getKey().getValue()) == 1) {
            if (minecraft.screen instanceof ItemLoreScreen screen) {
                return;
            }
            Screen itemLoreScreen = new ItemLoreScreen(minecraft.screen, stack, loreEntity);
            minecraft.setScreen(itemLoreScreen);
        }
    }
}
