package quaternary.fakeme.block;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import quaternary.fakeme.FakeMe;

public class ModBlocks {
	public static final class Names {
		public static final String CLICKER = "clicker";
	}
	
	@GameRegistry.ObjectHolder(FakeMe.MODID + ":" + Names.CLICKER)
	public static final Block CLICKER = Blocks.AIR;
	
	public static void initBlocks(IForgeRegistry<Block> reg) {
		reg.register(createBlock(new BlockClicker(), Names.CLICKER));
	}
	
	private static <B extends Block> B createBlock(B b, String name) {
		b.setRegistryName(new ResourceLocation(FakeMe.MODID, name));
		b.setUnlocalizedName(FakeMe.MODID + "." + name);
		
		return b;
	}
}
