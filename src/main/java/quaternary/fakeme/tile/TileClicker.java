package quaternary.fakeme.tile;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
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
import java.util.*;
import java.util.function.Function;

import static net.minecraft.block.Block.NULL_AABB;

public class TileClicker extends TileEntity {
	@CapabilityInject(IItemHandler.class)
	public static final Capability<IItemHandler> ITEM_HANDLER_CAP = null;
	
	private static final GameProfile FAKE_UUID = new GameProfile(UUID.fromString("cc8092e8-7d7c-49ac-aeb2-e0d2e906e045"), "[FakeMe Fake Player]");
	private static WeakReference<FakePlayer> fakePlayer = new WeakReference<>(null);
	
	private long lastClickTick = 0;
	private boolean leftClick;
	
	//Used to easily dump the player's inventory into this tile, while not letting hoppers etc do the same
	private boolean allowCheatyInsertion = false;
	private final ItemStackHandler handler = new ItemStackHandler(9) {
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
	
	private void performFakePlayerUpkeep(FakePlayer fake) {
		EnumFacing facing = getFacing();
		
		fake.isDead = false;
		fake.setHealth(20f);
		fake.capabilities.disableDamage = true;
		fake.capabilities.isCreativeMode = false;
		fake.clearActivePotions();
		fake.setScore(1337); //:eyes:
		fake.setPositionAndRotation(pos.getX() + .5, pos.getY(), pos.getZ() + .5, facing.getHorizontalAngle(), 0);
		
		//TODO use Access Transformer and lol hardcoding mcp names
		int ticks = ReflectionHelper.getPrivateValue(EntityLivingBase.class, fake, "ticksSinceLastSwing");
		ticks += (world.getTotalWorldTime() - lastClickTick);
		lastClickTick = world.getTotalWorldTime();
		ReflectionHelper.setPrivateValue(EntityLivingBase.class, fake, ticks, "ticksSinceLastSwing");
	}
	
	private boolean performAction(WorldServer worldServer, BlockPos pos, EnumFacing facing, FakePlayer fake, Function<BlockPos, Boolean> blockAction, Function<Entity, Boolean> entityAction) {
		double reachDistance = 5d;
		IAttributeInstance reachAttribute = fake.getAttributeMap().getAttributeInstance(EntityPlayer.REACH_DISTANCE);
		if(reachAttribute != null) {
			reachDistance = reachAttribute.getAttributeValue();
		}
		
		for(int distance = 1; distance <= reachDistance; distance++) {
			BlockPos offsetPos = pos.offset(facing, distance);
			if(!worldServer.isBlockLoaded(offsetPos)) return false;
			
			IBlockState clickedState = worldServer.getBlockState(offsetPos);
			Material mat = clickedState.getMaterial();
			
			//Try to click the block in front
			if(mat == Material.FIRE || (mat != Material.AIR && clickedState.getBoundingBox(worldServer, offsetPos) != NULL_AABB)) {
				//Found a block that's clickable so time to click it.
				if(blockAction.apply(offsetPos)) return true;
			}
			
			List<Entity> nearbyEntities = worldServer.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(offsetPos), e -> {
				//Fun fact, if you send a "click this" packet to one of these entities in real gameplay
				//You get kicked!
				//Sounds like it could cause troubles so let's skip these
				if(e instanceof EntityItem) return false;
				if(e instanceof EntityXPOrb) return false;
				if(e instanceof EntityArrow) return false;
				if(e.width == 0 || e.height == 0) return false; //can't click that anyways u dip
				return true;
			});
			
			//Try to click the entity in front
			if(!nearbyEntities.isEmpty()) {
				Entity ent = nearbyEntities.get(worldServer.rand.nextInt(nearbyEntities.size()));
				
				if(entityAction.apply(ent)) return true;
			}
		}
		
		return false;
	}
	
	public void doClick(boolean shouldSneak) {
		if(!(getWorld() instanceof WorldServer)) return;
		WorldServer worldServer = (WorldServer) world;
		
		FakePlayer fake = getFakePlayer();
		if(fake == null) return; //make intellij shut up, lol
		
		EnumFacing clickerFacing = getFacing();
		performFakePlayerUpkeep(fake);
		
		if(shouldSneak) fake.setSneaking(true);
		
		//Move the itemstack from the item handler into the fake player's inventory
		fake.inventory.clear();
		fake.inventory.currentItem = 0;
		fake.inventory.setInventorySlotContents(0, handler.extractItem(0, Integer.MAX_VALUE, false));
		
		ItemStack heldStack = fake.getHeldItem(EnumHand.MAIN_HAND);
		fake.getAttributeMap().applyAttributeModifiers(heldStack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND));
		
		fake.resetActiveHand();
		fake.setActiveHand(EnumHand.MAIN_HAND);
		
		//Attempt to use the item
		
		try {			
			if(leftClick) {
				final Function<BlockPos, Boolean> blockAction = clickedPos -> {
					worldServer.getBlockState(clickedPos).getBlock().onBlockClicked(worldServer, clickedPos, fake);
					worldServer.extinguishFire(fake, clickedPos.down(), EnumFacing.UP); //weird
					return true;
				};
				
				final Function<Entity, Boolean> entityAction = ent -> {
					fake.attackTargetEntityWithCurrentItem(ent);
					return true;
				};
				
				performAction(worldServer, pos, clickerFacing, fake, blockAction, entityAction);
			} else {
				final Function<BlockPos, Boolean> blockAction = clickedPos -> {
					IBlockState clickedState = worldServer.getBlockState(clickedPos);
					boolean didSomething = false;
					if(!shouldSneak) {
						didSomething |= clickedState.getBlock().onBlockActivated(worldServer, clickedPos, clickedState, fake, EnumHand.MAIN_HAND, clickerFacing.getOpposite(), .5f, .5f, .5f);
					}
					
					didSomething |= heldStack.getItem().onItemUse(fake, worldServer, clickedPos, EnumHand.MAIN_HAND, clickerFacing.getOpposite(), .5f, .5f, .5f) == EnumActionResult.SUCCESS;
					
					return didSomething;
				};
				
				final Function<Entity, Boolean> entityAction = ent -> {
					boolean didSomething = false;
					if(ent instanceof EntityLivingBase) {
						EntityLivingBase elb = (EntityLivingBase) ent; 
						didSomething |= heldStack.getItem().itemInteractionForEntity(heldStack, fake, elb, EnumHand.MAIN_HAND);
						didSomething |= elb.processInitialInteract(fake, EnumHand.MAIN_HAND);
					}
					
					didSomething |= heldStack.getItem().onItemRightClick(worldServer, fake, EnumHand.MAIN_HAND).getType() == EnumActionResult.SUCCESS;
					
					return didSomething;
				};
				
				boolean didSomething = performAction(worldServer, pos, clickerFacing, fake, blockAction, entityAction);
				if(!didSomething) {
					//fallback to this method
					heldStack.getItem().onItemRightClick(worldServer, fake, EnumHand.MAIN_HAND);
				}
			}
		} catch(Exception e) {
			FakeMe.LOGGER.error("There was a problem " + (leftClick ? "left" : "right") + "-clicking", e);
		}
		
		//Dump the fake player's inventory into the tile again.
		allowCheatyInsertion = true;
		List<ItemStack> leftoverStacks = new ArrayList<>();
		for(int i = 0; i < fake.inventory.getSizeInventory(); i++) {
			ItemStack leftover = ItemHandlerHelper.insertItem(handler, fake.inventory.getStackInSlot(i).copy(), false);
			if(!leftover.isEmpty()) leftoverStacks.add(leftover);
		}
		allowCheatyInsertion = false;
		
		//If there's any leftover default to tossing them on the ground
		for(ItemStack leftover : leftoverStacks) {
			float x = pos.getX() + .5f + (clickerFacing.getFrontOffsetX() / 2f);
			float y = pos.getX() + .5f + (clickerFacing.getFrontOffsetY() / 2f);
			float z = pos.getX() + .5f + (clickerFacing.getFrontOffsetZ() / 2f);
			
			EntityItem ent = new EntityItem(world, x, y, z, leftover);
			ent.motionX = 0; ent.motionY = 0; ent.motionZ = 0;
			worldServer.spawnEntity(ent);
		}
		
		//Clean up
		fake.setSneaking(false);
		fake.getAttributeMap().removeAttributeModifiers(heldStack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND));
		fake.inventory.clear();
	}
	
	private EnumFacing getFacing() {
		return world.getBlockState(pos).getValue(BlockClicker.FACING);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		leftClick = nbt.getBoolean("LeftClick");
		handler.deserializeNBT(nbt.getCompoundTag("Inventory"));
		lastClickTick = nbt.getLong("LastClickTime");
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setBoolean("LeftClick", leftClick);
		nbt.setTag("Inventory", handler.serializeNBT());
		nbt.setLong("LastClickTime", lastClickTick);
		return super.writeToNBT(nbt);
	}
	
	// ಠ_ಠ
	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
		return oldState.getBlock() != newState.getBlock();
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
}
