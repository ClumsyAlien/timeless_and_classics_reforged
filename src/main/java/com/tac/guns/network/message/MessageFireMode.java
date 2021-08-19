package com.tac.guns.network.message;


import com.tac.guns.common.network.ServerPlayHandler;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;



/**
 * Author: MrCrayfish
 */


public class MessageFireMode implements IMessage
{
    @Override
    public void encode(PacketBuffer buffer) {}

    @Override
    public void decode(PacketBuffer buffer) {}

    @Override
    public void handle(Supplier<NetworkEvent.Context> supplier)
    {
        supplier.get().enqueueWork(() ->
        {
            ServerPlayerEntity player = supplier.get().getSender();
            if(player != null && !player.isSpectator())
            {
                ServerPlayHandler.handleFireMode(player);
            }
        });
        supplier.get().setPacketHandled(true);
    }
}


