package quaternary.fakeme.ui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;
import quaternary.fakeme.FakeMe;
import quaternary.fakeme.tile.TileClicker;

public class GuiClicker extends GuiContainer {
	public GuiClicker(Container inventorySlotsIn, EntityPlayer player, TileClicker clicker) {
		super(inventorySlotsIn);
		
		this.player = player;
		this.clicker = clicker;
	}
	
	private static final ResourceLocation BG_TEX = new ResourceLocation(FakeMe.MODID, "textures/ui/clicker.png");
	private static final ResourceLocation SELECTED_TEX = new ResourceLocation(FakeMe.MODID, "textures/ui/selected.png");
	
	private final EntityPlayer player;
	private final TileClicker clicker;
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		GlStateManager.color(1, 1, 1, 1);
		mc.getTextureManager().bindTexture(BG_TEX);
		drawTexturedModalRect((width - xSize) / 2, (height - ySize) / 2, 0, 0, xSize, ySize);
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		for(int i = 0; i < inventorySlots.inventorySlots.size() - (9 * 4); i++) {
			int slotX = (i / 3) * 18 + 8;
			int slotY = (i % 3) * 18 + 10;
			
			if(i == 0) {
				GlStateManager.color(1, 1, 1, 1);
				mc.getTextureManager().bindTexture(SELECTED_TEX);
				drawModalRectWithCustomSizedTexture(slotX - 4, slotY - 4, 0, 0, 32, 32, 32, 32);
			} else {
				drawRect(slotX, slotY, slotX + 16, slotY + 16, 0x55000000);
			}
		}
	}
}
