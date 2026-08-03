package com.jomlom.recipebookaccess.network;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record  CustomItemsPayload(List<ItemStack> itemStacks) implements CustomPacketPayload {

    public CustomItemsPayload {
        itemStacks = itemStacks.stream()
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .collect(Collectors.toList());
    }
    
    public static final CustomPacketPayload.Type<CustomItemsPayload> ID = new CustomPacketPayload.Type<>(NetworkConstants.ITEMS_PACKET_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, CustomItemsPayload> CODEC =
            StreamCodec.composite(
                ByteBufCodecs.collection(ArrayList::new, ItemStack.STREAM_CODEC),
                CustomItemsPayload::itemStacks,
                CustomItemsPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
