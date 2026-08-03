package com.jomlom.recipebookaccess.api;

import java.util.List;
import net.minecraft.world.Container;

public interface RecipeBookInventoryProvider {

    List<Container> getInventoriesForAutofill();

    default boolean persistentInventory() {
        return false;
    }

}