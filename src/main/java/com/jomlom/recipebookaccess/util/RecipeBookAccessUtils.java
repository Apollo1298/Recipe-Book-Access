package com.jomlom.recipebookaccess.util;

import com.jomlom.recipebookaccess.api.RecipeBookInventoryProvider;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class RecipeBookAccessUtils {

    private static final Map<Slot, Container> originMap = new HashMap<>();

    /**
     * Player inventories expose armor/offhand beyond the main 36 slots.
     * Crafting autofill should only use the main inventory, matching vanilla.
     */
    public static int getUsableSlotCount(Container inv) {
        if (inv instanceof Inventory) {
            return Inventory.INVENTORY_SIZE;
        }
        return inv.getContainerSize();
    }

    public static void populateCustomRecipeFinder(StackedItemContents recipeFinder, RecipeBookInventoryProvider customPopulator) {
        for (Container inventory : customPopulator.getInventoriesForAutofill()) {
            int size = getUsableSlotCount(inventory);
            for (int i = 0; i < size; i++) {
                recipeFinder.accountStack(inventory.getItem(i));
            }
        }
    }

    public static void populateCustomRecipeFinder(StackedItemContents recipeFinder, List<ItemStack> items) {
        for (ItemStack itemStack : items) {
            recipeFinder.accountStack(itemStack);
        }
    }

    public static int customFillInputSlot(Slot slot, Holder<Item> item, int count, RecipeBookInventoryProvider customPop) {
        ItemStack slotStack = slot.getItem();

        for (Container inv : customPop.getInventoriesForAutofill()) {
            int matchingIndex = getMatchingSlotForInventory(inv, item, slotStack);
            if (matchingIndex != -1) {
                originMap.put(slot, inv);

                ItemStack invStack = inv.getItem(matchingIndex);
                ItemStack removedStack;
                if (count < invStack.getCount()) {
                    removedStack = inv.removeItem(matchingIndex, count);
                } else {
                    removedStack = inv.removeItemNoUpdate(matchingIndex);
                }

                int removedCount = removedStack.getCount();
                if (slotStack.isEmpty()) {
                    slot.set(removedStack);
                } else {
                    slotStack.grow(removedCount);
                }
                return count - removedCount;
            }
        }
        return -1;
    }

    private static int getMatchingSlotForInventory(Container inv, Holder<Item> item, ItemStack stack) {
        int size = getUsableSlotCount(inv);
        for (int i = 0; i < size; ++i) {
            ItemStack currentStack = inv.getItem(i);
            if (!currentStack.isEmpty()
                    && currentStack.is(item)
                    && usableWhenFillingSlot(stack)
                    && (stack.isEmpty() || ItemStack.isSameItemSameComponents(stack, currentStack))) {
                return i;
            }
        }
        return -1;
    }

    private static boolean usableWhenFillingSlot(ItemStack stack) {
        return !stack.isDamaged() && !stack.isEnchanted() && !stack.has(DataComponents.CUSTOM_NAME);
    }

    public static AbstractContainerMenu getOuterScreenHandler(ServerPlaceRecipe.CraftingMenuAccess<?> handler) {
        Class<?> clazz = handler.getClass();
        for (Field f : clazz.getDeclaredFields()) {
            f.setAccessible(true);
            try {
                Object value = f.get(handler);
                if (value instanceof AbstractContainerMenu screenHandler) {
                    return screenHandler;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static boolean tryReturnItemToOrigin(Slot slot, ItemStack stack) {
        Container originInventory = originMap.get(slot);
        if (originInventory != null) {
            boolean inserted = insertStackIntoInventory(originInventory, stack);
            originMap.remove(slot);
            return inserted;
        }
        return false;
    }

    private static boolean insertStackIntoInventory(Container inv, ItemStack stack) {
        if (inv instanceof Inventory playerInventory) {
            playerInventory.placeItemBackInInventory(stack, false);
            return stack.isEmpty();
        }

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack invStack = inv.getItem(i);
            if (!invStack.isEmpty() && ItemStack.isSameItemSameComponents(invStack, stack)) {
                int maxStackSize = Math.min(invStack.getMaxStackSize(), stack.getMaxStackSize());
                int availableSpace = maxStackSize - invStack.getCount();
                if (availableSpace > 0) {
                    int toTransfer = Math.min(availableSpace, stack.getCount());
                    invStack.grow(toTransfer);
                    stack.shrink(toTransfer);
                    if (stack.isEmpty()) {
                        return true;
                    }
                }
            }
        }
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack invStack = inv.getItem(i);
            if (invStack.isEmpty() && inv.canPlaceItem(i, stack)) {
                inv.setItem(i, stack.copy());
                stack.setCount(0);
                return true;
            }
        }
        return stack.isEmpty();
    }
}
