package quaternary.fakeme.tile;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.items.*;
import quaternary.fakeme.FakeMe;
import quaternary.fakeme.block.BlockClicker;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.UUID;

public class TileClicker extends TileEntity implements ITickable {
	public TileClicker() {
		leftClick = false;
	}
	
	public TileClicker(boolean leftClick) {
		this.leftClick = leftClick;
	}
	
	@CapabilityInject(IItemHandler.class)
	public static final Capability<IItemHandler> ITEM_HANDLER_CAP = null;
	
	private static final GameProfile FAKE_UUID = new GameProfile(UUID.fromString("cc8092e8-7d7c-49ac-aeb2-e0d2e906e045"), "[FakeMe Fake Player]");
	private static WeakReference<FakePlayer> fakePlayer = new WeakReference<>(null);
	
	private boolean leftClick;
	
	//Used to easily dump the player's inventory into this tile, while not letting hoppers etc do the same
	private boolean allowCheatyInsertion = false;
	private final ItemStackHandler handler = new ItemStackHandler(36){
		@Nonnull
		@Override
		public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
			//Only allow inserting into the first slot
			if(allowCheatyInsertion || slot == 0) return super.insertItem(slot, stack, simulate);
			else return stack;
		}
	};
	
	private FakePlayer getFakePlayer() {
		if(!(world instanceof WorldServer)) return null;
		
		if(fakePlayer.get() == null) {
			fakePlayer = new WeakReference<>(FakePlayerFactory.get((WorldServer) world, FAKE_UUID));
		}
		
		return fakePlayer.get();
	}
	
	@Override
	public void update() {
		//Perform some upkeep on the fake player
		FakePlayer fake = getFakePlayer();
		if(fake == null) return; //make intellij shut up, lol
		
		fake.isDead = false;
		fake.setHealth(20f);
		fake.capabilities.disableDamage = true;
		fake.clearActivePotions();
		fake.setScore(1337);
		fake.setPositionAndRotation(pos.getX() + .5, pos.getY(), pos.getZ() + .5, getFacing().getHorizontalAngle(), 0);
		
		//Basically FakePlayer overrides onUpdate, but there's some things I want to update, such as cooldown time.
		
		//TODO AT this instead of reflecting im just lazy rn
		int ticks = ReflectionHelper.getPrivateValue(EntityLivingBase.class, fake, "ticksSinceLastSwing");
		ticks++;
		ReflectionHelper.setPrivateValue(EntityLivingBase.class, fake, ticks, "ticksSinceLastSwing");
	}
	
	public void doClick() {
		if(!(getWorld() instanceof WorldServer)) return;
		WorldServer worldServer = (WorldServer) world;
		
		FakePlayer fake = getFakePlayer();
		if(fake == null) return; //make intellij shut up, lol
		
		//Move the itemstack from the item handler into the fake player's inventory
		fake.inventory.clear();
		fake.inventory.currentItem = 0;
		fake.inventory.setInventorySlotContents(0, handler.extractItem(0, Integer.MAX_VALUE, false));
		fake.setActiveHand(EnumHand.MAIN_HAND);
		
		System.out.println(fake.getHeldItem(EnumHand.MAIN_HAND));
		
		//Attempt to use the item
		try {
			if(leftClick) {
				List<Entity> nearbyEntites = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(pos.offset(getFacing())));
				if(nearbyEntites.isEmpty()) {
					IBlockState stateInFront = world.getBlockState(pos.offset(getFacing()));
					stateInFront.getBlock().onBlockClicked(worldServer, pos.offset(getFacing()), fake);
				} else {
					Entity nearby = nearbyEntites.get(worldServer.rand.nextInt(nearbyEntites.size()));
					fake.attackTargetEntityWithCurrentItem(nearby);
				}
			} else {
				ItemStack stack = fake.getHeldItem(EnumHand.MAIN_HAND);
				stack.getItem().onItemRightClick(worldServer, fake, EnumHand.MAIN_HAND);
			}
		} catch (Exception e) {
			FakeMe.LOGGER.error("There was a problem " + (leftClick ? "left" : "right") + "-clicking", e);
		}
		
		//Dump the fake player's inventory into the tile again.
		allowCheatyInsertion = true;
		for(int i = 0; i < fake.inventory.getSizeInventory(); i++) {
			ItemHandlerHelper.insertItem(handler, fake.inventory.getStackInSlot(i), false);
		}
		allowCheatyInsertion = false;
		
		//Be extra triple double sure inventory doesn't leak to other tiles using the fake player
		fake.inventory.clear();
	}
	
	private EnumFacing getFacing() {
		return world.getBlockState(pos).getValue(BlockClicker.FACING);
	}
	
	@Override
	@SuppressWarnings("ConstantConditions")
	public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
		if(capability == ITEM_HANDLER_CAP) return true;
		else return super.hasCapability(capability, facing);
	}
	
	@Nullable
	@Override
	@SuppressWarnings({"ConstantConditions", "unchecked"})
	public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
		if(capability == ITEM_HANDLER_CAP) return (T) handler;
		else return super.getCapability(capability, facing);
	}
	
	// ಠ_ಠ
	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
		return oldState.getBlock() != newState.getBlock();
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setBoolean("LeftClick", leftClick);
		nbt.setTag("Inventory", handler.serializeNBT());
		return super.writeToNBT(nbt);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		leftClick = nbt.getBoolean("LeftClick");
		handler.deserializeNBT(nbt.getCompoundTag("Inventory"));
	}
}
