package quaternary.fakeme.ui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.*;
import quaternary.fakeme.tile.TileClicker;

public class ContainerClicker extends Container {
	public ContainerClicker(EntityPlayer player, TileClicker tile) {
		this.player = player;
		this.tile = tile;
		
		IItemHandler clickerInventory = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
		if(clickerInventory == null) throw new NullPointerException("Null clicker inventory?!??!???!"); //TODO
		
		//an inventory for the clicker itself
		int clickerSlotCount = clickerInventory.getSlots();
		for(int i = 0; i < clickerSlotCount; i++) {
			int slotX = (i / 3) * 18 + 8;
			int slotY = (i % 3) * 18 + 10;
			
			addSlotToContainer(new SlotItemHandler(clickerInventory, i, slotX, slotY) {
				@Override
				public void onSlotChanged() {
					tile.markDirty();
				}
			});
		}
		
		//add a player inventory
		for(int row = 0; row < 3; row++) {
			for(int column = 0; column < 9; column++) {
				addSlotToContainer(new Slot(player.inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
			}
		}
		
		//& hotbar
		for(int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
			addSlotToContainer(new Slot(player.inventory, hotbarSlot, 8 + hotbarSlot * 18, 142));
		}
	}
	
	private final EntityPlayer player;
	private final TileClicker tile;
	
	@Override
	public boolean canInteractWith(EntityPlayer playerIn) {
		return true;
	}
	
	@Override
	//Copied wholesale from Shadowfact's tutorial lmao
	//http://shadowfacts.net/tutorials/forge-modding-112/tile-entities-inventory-gui/
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = inventorySlots.get(index);
		
		if (slot != null && slot.getHasStack()) {
			ItemStack itemstack1 = slot.getStack();
			itemstack = itemstack1.copy();
			
			int containerSlots = inventorySlots.size() - player.inventory.mainInventory.size();
			
			if (index < containerSlots) {
				if (!this.mergeItemStack(itemstack1, containerSlots, inventorySlots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.mergeItemStack(itemstack1, 0, containerSlots, false)) {
				return ItemStack.EMPTY;
			}
			
			if (itemstack1.getCount() == 0) {
				slot.putStack(ItemStack.EMPTY);
			} else {
				slot.onSlotChanged();
			}
			
			if (itemstack1.getCount() == itemstack.getCount()) {
				return ItemStack.EMPTY;
			}
			
			slot.onTake(player, itemstack1);
		}
		
		return itemstack;
	}
}
