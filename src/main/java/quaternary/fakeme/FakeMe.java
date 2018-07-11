package quaternary.fakeme;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quaternary.fakeme.block.ModBlocks;
import quaternary.fakeme.item.ModItems;
import quaternary.fakeme.net.NetHandler;
import quaternary.fakeme.tile.ModTiles;
import quaternary.fakeme.ui.GuiHandler;

@Mod(modid = FakeMe.MODID, name = FakeMe.NAME, version = FakeMe.VERSION)
public class FakeMe {
	public static final String MODID = "fakeme";
	public static final String NAME = "Fake Me";
	public static final String VERSION = "1.0";
	
	public static final Logger LOGGER = LogManager.getLogger(NAME);
	
	@GameRegistry.ItemStackHolder(MODID + ":" + "left_clicker")
	public static final ItemStack ICON_STACK = ItemStack.EMPTY;
	
	public static final CreativeTabs TAB = new CreativeTabs(MODID) {
		@SideOnly(Side.CLIENT)
		@Override
		public ItemStack getTabIconItem() {
			return ICON_STACK;
		}
	};
	
	@Mod.Instance(MODID)
	public static FakeMe INSTANCE = null;
	
	@Mod.EventHandler
	public static void init(FMLInitializationEvent e) {
		NetworkRegistry.INSTANCE.registerGuiHandler(INSTANCE, new GuiHandler());
		
		NetHandler.init();
	}
	
	@Mod.EventBusSubscriber
	public static class Events {
		@SubscribeEvent
		public static void blocks(RegistryEvent.Register<Block> e) {
			ModBlocks.initBlocks(e.getRegistry());
			ModTiles.initTiles();
		}
		
		@SubscribeEvent
		public static void items(RegistryEvent.Register<Item> e) {
			ModItems.initItems(e.getRegistry());
		}
	}
}
