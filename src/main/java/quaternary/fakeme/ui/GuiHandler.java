package quaternary.fakeme.ui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import quaternary.fakeme.tile.TileClicker;

import javax.annotation.Nullable;

public class GuiHandler implements IGuiHandler {
	public static final int CLICKER_UI = 0;
	
	@Nullable
	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		switch(ID) {
			case CLICKER_UI: {
				TileClicker clicker = (TileClicker) world.getTileEntity(new BlockPos(x, y, z));
				return new ContainerClicker(player, clicker);
			}
		}
		
		return null;
	}
	
	@Nullable
	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		switch(ID) {
			case CLICKER_UI: {
				TileClicker clicker = (TileClicker) world.getTileEntity(new BlockPos(x, y, z));
				return new GuiClicker((Container) getServerGuiElement(ID, player, world, x, y, z), player, clicker);
			}
		}
		return null;
	}
}
