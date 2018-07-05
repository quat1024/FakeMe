package quaternary.fakeme.block;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import quaternary.fakeme.FakeMe;

public class ModBlocks {
	public static final class Names {
		public static final String LEFT_CLICKER = "left_clicker";
		public static final String RIGHT_CLICKER = "right_clicker";
	}
	
	@GameRegistry.ObjectHolder(FakeMe.MODID + ":" + Names.LEFT_CLICKER)
	public static final Block LEFT_CLICKER = Blocks.AIR;
	
	@GameRegistry.ObjectHolder(FakeMe.MODID + ":" + Names.RIGHT_CLICKER)
	public static final Block RIGHT_CLICKER = Blocks.AIR;
	
	public static void initBlocks(IForgeRegistry<Block> reg) {
		reg.register(createBlock(new BlockClicker(true), Names.LEFT_CLICKER));
		reg.register(createBlock(new BlockClicker(false), Names.RIGHT_CLICKER));
	}
	
	private static <B extends Block> B createBlock(B b, String name) {
		b.setRegistryName(new ResourceLocation(FakeMe.MODID, name));
		b.setUnlocalizedName(FakeMe.MODID + "." + name);
		
		return b;
	}
}
