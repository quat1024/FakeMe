package quaternary.fakeme.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.registries.IForgeRegistry;
import quaternary.fakeme.FakeMe;
import quaternary.fakeme.block.ModBlocks;

public class ModItems {
	public static void initItems(IForgeRegistry<Item> reg) {
		reg.register(createItem(new ItemBlock(ModBlocks.LEFT_CLICKER)));
		reg.register(createItem(new ItemBlock(ModBlocks.RIGHT_CLICKER)));
	}
	
	private static <IB extends ItemBlock> IB createItem(IB itemBlock) {
		itemBlock.setRegistryName(itemBlock.getBlock().getRegistryName());
		itemBlock.setCreativeTab(FakeMe.TAB);
		return itemBlock;
	}
}
