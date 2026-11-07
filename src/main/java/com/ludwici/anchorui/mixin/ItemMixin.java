package com.ludwici.anchorui.mixin;

import com.ludwici.anchorui.client.KeyRegistry;
import com.ludwici.anchorui.client.LoreLoader;
import com.ludwici.anchorui.client.gui.ItemLoreScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Item.class)
public class ItemMixin {

    @Inject(method = "appendHoverText", at = @At("HEAD"))
    public void appendHoveredText(ItemStack pStack, Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced, CallbackInfo ci) {
        showTip(pStack, pTooltipComponents);
    }

    @Unique
    @OnlyIn(Dist.CLIENT)
    private void showTip(ItemStack stack, List<Component> components) {
        var data = LoreLoader.INSTANCE.getData();
        var r = ForgeRegistries.ITEMS.getKey(stack.getItem());

        if (r == null) {
            return;
        }

        var loreComponent = data.values().stream().filter(c -> {
                var r1 = ResourceLocation.tryParse(c.item);
                if (r1 == null) {
                    return false;
                }
                return r.compareTo(r1) == 0;
            }
        ).findFirst().orElse(null);

        if (loreComponent == null) {
            return;
        }

        components.add(Component.literal(" "));
        components.add(Component.literal("Нажми X, чтобы узнать больше").withStyle(ChatFormatting.GRAY));

        var minecraft = Minecraft.getInstance();
        if (GLFW.glfwGetKey(minecraft.getWindow().getWindow(), KeyRegistry.SHOW_LORE.getKey().getValue()) == 1) {
            if (minecraft.screen instanceof ItemLoreScreen) {
                return;
            }
            ItemLoreScreen itemLoreScreen = new ItemLoreScreen(loreComponent);
            minecraft.setScreen(itemLoreScreen);
        }
    }

}
