package com.jomlom.recipebookaccess.mixin.client;

import com.jomlom.recipebookaccess.api.RecipeBookInventoryProvider;
import com.jomlom.recipebookaccess.network.ClientItemsReciever;
import com.jomlom.recipebookaccess.network.RequestItemsPayload;
import com.jomlom.recipebookaccess.util.RecipeBookAccessUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookWidgetMixin {

	@Redirect(
			method = "updateStackedContents",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/player/Inventory;fillStackedContents(Lnet/minecraft/world/entity/player/StackedItemContents;)V"
			)
	)
	private void redirectPopulateRecipeFinderRefresh(Inventory inventory, StackedItemContents recipeFinder) {
		redirect(inventory, recipeFinder);
	}

	@Redirect(
			method = "initVisuals",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/player/Inventory;fillStackedContents(Lnet/minecraft/world/entity/player/StackedItemContents;)V"
			)
	)
	private void redirectPopulateRecipeFinderReset(Inventory inventory, StackedItemContents recipeFinder) {
		redirect(inventory, recipeFinder);
	}

	@Unique
	private void redirect(Inventory inventory, StackedItemContents recipeFinder) {
		RecipeBookComponent<?> widget = (RecipeBookComponent<?>)(Object)this;

		RecipeBookMenu handler = ((RecipeBookWidgetAccessor)widget).getMenu();

		if (handler instanceof RecipeBookInventoryProvider) {
			ClientPlayNetworking.send(new RequestItemsPayload(1));
			ClientItemsReciever.setOnUpdate(() -> {
				List<ItemStack> updatedItems = ClientItemsReciever.getItemStacks();
				RecipeBookAccessUtils.populateCustomRecipeFinder(recipeFinder, updatedItems);
				widget.recipesUpdated();
			});
		} else {
			inventory.fillStackedContents(recipeFinder);
		}
	}


}