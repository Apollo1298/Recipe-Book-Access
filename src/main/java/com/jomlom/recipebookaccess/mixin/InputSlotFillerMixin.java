package com.jomlom.recipebookaccess.mixin;

import com.jomlom.recipebookaccess.api.RecipeBookInventoryProvider;
import com.jomlom.recipebookaccess.util.RecipeBookAccessUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;

@Mixin(ServerPlaceRecipe.class)
public abstract class InputSlotFillerMixin {

    @Final @Shadow private ServerPlaceRecipe.CraftingMenuAccess<?> menu;
    @Final @Shadow private List<Slot> slotsToClear;
    @Final @Shadow private Inventory inventory;

    @Redirect(
            method = "placeRecipe(Lnet/minecraft/recipebook/ServerPlaceRecipe$CraftingMenuAccess;IILjava/util/List;Ljava/util/List;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/crafting/RecipeHolder;ZZ)Lnet/minecraft/world/inventory/RecipeBookMenu$PostPlaceAction;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Inventory;fillStackedContents(Lnet/minecraft/world/entity/player/StackedItemContents;)V"
            )
    )
    private static void redirectInventoryPopulate(
            Inventory inventory,
            StackedItemContents recipeFinder,
            ServerPlaceRecipe.CraftingMenuAccess<?> handler,
            int width, int height,
            List<Slot> inputSlots, List<Slot> slotsToReturn,
            Inventory inv,
            RecipeHolder<? extends Recipe<? extends RecipeInput>> recipe,
            boolean craftAll, boolean creative
    )    {
        AbstractContainerMenu screenHandler = RecipeBookAccessUtils.getOuterScreenHandler(handler);
        if (screenHandler instanceof RecipeBookInventoryProvider customPop) {
            RecipeBookAccessUtils.populateCustomRecipeFinder(recipeFinder, customPop);
        } else {
            inventory.fillStackedContents(recipeFinder);
        }
    }

    @Inject(
            method = "moveItemToGrid",
            at = @At("HEAD"), cancellable = true
    )
    private void onFillInputSlot(
            Slot slot,
            Holder<Item> item,
            int count,
            CallbackInfoReturnable<Integer> cir
    ) {
        AbstractContainerMenu screenHandler = RecipeBookAccessUtils.getOuterScreenHandler(menu);
        if (screenHandler instanceof RecipeBookInventoryProvider customPop) {
            int customResult = RecipeBookAccessUtils.customFillInputSlot(slot, item, count, customPop);
            slot.setChanged();
            cir.setReturnValue(customResult);
        }
    }

    @Inject(
            method = "clearGrid",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onReturnInputs(CallbackInfo ci) {
        AbstractContainerMenu screenHandler = RecipeBookAccessUtils.getOuterScreenHandler(menu);
        if (screenHandler instanceof RecipeBookInventoryProvider) {
            for (Slot slot : slotsToClear) {
                ItemStack stack = slot.getItem().copy();
                boolean returned = RecipeBookAccessUtils.tryReturnItemToOrigin(slot, stack);
                if (!returned) {
                    inventory.placeItemBackInInventory(stack, false);
                }
                slot.set(stack);
            }
            menu.clearCraftingContent();
            ci.cancel();
        }
    }
}
