package quaternary.fakeme.tile;

import net.minecraftforge.fml.common.registry.GameRegistry;
import quaternary.fakeme.FakeMe;

public class ModTiles {
	public static void initTiles() {
		GameRegistry.registerTileEntity(TileClicker.class, FakeMe.MODID + ":clicker");
	}
}
