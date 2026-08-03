package com.jomlom.recipebookaccess.network;

import com.jomlom.recipebookaccess.api.RecipeBookInventoryProvider;
import com.jomlom.recipebookaccess.util.RecipeBookAccessUtils;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class ServerRequestReciever {

    public static void handleRequest(ServerPlayer player) {
        AbstractContainerMenu handler = player.containerMenu;
        if (handler instanceof RecipeBookInventoryProvider customPop){
            List<Container> inventories = customPop.getInventoriesForAutofill();
            List<ItemStack> items = new ArrayList<>();
            for (Container inventory : inventories){
                int size = RecipeBookAccessUtils.getUsableSlotCount(inventory);
                for (int i = 0; i < size; i++) {
                    ItemStack stack = inventory.getItem(i);
                    if (!stack.isEmpty()) {
                        items.add(stack.copy());
                    }
                }
            }
            ServerPlayNetworking.send(player, new CustomItemsPayload(items));
        }
    }

}
