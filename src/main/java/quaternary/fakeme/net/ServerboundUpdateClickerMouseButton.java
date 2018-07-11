package quaternary.fakeme.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import quaternary.fakeme.tile.TileClicker;

public class ServerboundUpdateClickerMouseButton implements IMessage {
	public ServerboundUpdateClickerMouseButton(){}
	
	public ServerboundUpdateClickerMouseButton(boolean leftClick, BlockPos pos) {
		this.leftClick = leftClick;
		this.pos = pos;
	}
	
	boolean leftClick;
	BlockPos pos;
	
	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeBoolean(leftClick);
		buf.writeInt(pos.getX());
		buf.writeInt(pos.getY());
		buf.writeInt(pos.getZ());
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		leftClick = buf.readBoolean();
		pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
	}
	
	public static class Handler implements IMessageHandler<ServerboundUpdateClickerMouseButton, IMessage> {
		@Override
		public IMessage onMessage(ServerboundUpdateClickerMouseButton message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
				EntityPlayerMP player = ctx.getServerHandler().player;
				World world = player.getEntityWorld();
				
				if(!world.isBlockLoaded(message.pos)) return; //Naughty
				
				TileEntity te = world.getTileEntity(message.pos);
				if(te instanceof TileClicker) {
					((TileClicker)te).setLeftClick(message.leftClick);
				}
			});
			
			return null;
		}
	}
}
