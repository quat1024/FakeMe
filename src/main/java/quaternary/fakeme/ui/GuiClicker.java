package quaternary.fakeme.ui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiButtonToggle;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import quaternary.fakeme.FakeMe;
import quaternary.fakeme.net.NetHandler;
import quaternary.fakeme.net.ServerboundUpdateClickerMouseButton;
import quaternary.fakeme.tile.TileClicker;

import java.io.IOException;

public class GuiClicker extends GuiContainer {
	public GuiClicker(Container inventorySlotsIn, EntityPlayer player, TileClicker clicker) {
		super(inventorySlotsIn);
		
		this.player = player;
		this.clicker = clicker;
	}
	
	private static final ResourceLocation BG_TEX = new ResourceLocation(FakeMe.MODID, "textures/ui/clicker.png");
	private static final ResourceLocation SELECTED_TEX = new ResourceLocation(FakeMe.MODID, "textures/ui/selected.png");
	
	private static final String LANG_LEFTCLICK = "fakeme.misc.leftClick";
	private static final String LANG_RIGHTCLICK = "fakeme.misc.rightClick";
	
	private final EntityPlayer player;
	private final TileClicker clicker;
	
	private boolean leftClick;
	private GuiButton leftClickButton = null;
	
	@Override
	public void initGui() {
		super.initGui();
		
		leftClick = clicker.isLeftClick();
		leftClickButton = addButton(new GuiButton(0, guiLeft + 64, guiTop + 26, 106, 20, ""));
		setButtonText(leftClickButton, leftClick ? LANG_LEFTCLICK : LANG_RIGHTCLICK);
	}
	
	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if(button == leftClickButton) {
			NetHandler.sendToServer(new ServerboundUpdateClickerMouseButton(!leftClick, clicker.getPos()));
			leftClick ^= true;
			setButtonText(button, leftClick ? LANG_LEFTCLICK : LANG_RIGHTCLICK);
		}
	}
	
	private void setButtonText(GuiButton button, String langKey) {
		button.displayString = I18n.translateToLocal(langKey);
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		drawDefaultBackground();
		
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
