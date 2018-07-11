package quaternary.fakeme.tile;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.*;
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
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.items.*;
import quaternary.fakeme.FakeMe;
import quaternary.fakeme.block.BlockClicker;
import quaternary.fakeme.util.CustomFakePlayer;
import quaternary.fakeme.util.MiscUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

import static net.minecraft.block.Block.NULL_AABB;

public class TileClicker extends TileEntity {
	@CapabilityInject(IItemHandler.class)
	public static final Capability<IItemHandler> ITEM_HANDLER_CAP = null;
	
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
	
	@Nullable
	private Entity getEntityAtBlock(WorldServer worldServer, BlockPos pos) {
		List<Entity> nearbyEntities = worldServer.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(pos), e -> {
			//Fun fact, if you send a "click this" packet to one of these entities in real gameplay
			//You get kicked!
			//Sounds like it could cause troubles so let's skip these
			if(e instanceof EntityItem) return false;
			if(e instanceof EntityXPOrb) return false;
			if(e instanceof EntityArrow) return false;
			if(e.width == 0 || e.height == 0) return false; //can't click that anyways u dip
			return true;
		});
		
		if(nearbyEntities.isEmpty()) return null;
		return nearbyEntities.get(worldServer.rand.nextInt(nearbyEntities.size()));
	}
	
	public void doClick(boolean shouldSneak) {
		if(!(getWorld() instanceof WorldServer)) return;
		WorldServer worldServer = (WorldServer) world;
		
		FakePlayer fake = CustomFakePlayer.get(worldServer);
		if(fake == null) return; //make intellij shut up, lol
		
		EnumFacing facing = getFacing();
		
		performFakePlayerUpkeep(fake);
		if(shouldSneak) fake.setSneaking(true);
		
		//Dump the item handler into the fake player's inventory
		fake.inventory.clear();
		for(int i = 0; i < 9; i++) {
			fake.inventory.setInventorySlotContents(i, handler.extractItem(i, Integer.MAX_VALUE, false));
		}
		//Select the first item
		fake.inventory.currentItem = 0;
		
		//Apply attribute modifiers, mainly so attack damage can be a thing
		ItemStack heldStack = fake.getHeldItem(EnumHand.MAIN_HAND);
		fake.getAttributeMap().applyAttributeModifiers(heldStack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND));
		
		//Set the "active hand" whatever this is, checked in attackTargetEntityWithCurrentItem maybe other things
		fake.resetActiveHand();
		fake.setActiveHand(EnumHand.MAIN_HAND);
		
		double reachDistance = MiscUtil.getReachDistance(fake);
		
		//Use the item
		try {			
			if(leftClick) {
				
				for(int distance = 1; distance <= reachDistance; distance++) {
					BlockPos clickedPos = pos.offset(facing, distance);
					System.out.println("LEFT CLICKIN checking pos " + clickedPos);
					IBlockState clickedState = worldServer.getBlockState(clickedPos);
					clickedState = clickedState.getActualState(worldServer, clickedPos);
					Block clickedBlock = clickedState.getBlock();
					Material clickedMaterial = clickedState.getMaterial();
					
					//Try to left click a block
					if(clickedState.getBoundingBox(worldServer, clickedPos) != NULL_AABB) {
						clickedBlock.onBlockClicked(worldServer, clickedPos, fake);
						System.out.println("onBlockClicked");
						break;
					}
					
					//Try to put out fire
					if(clickedMaterial == Material.FIRE) {
						//lol offset it in 2 opposite directions because mojang
						worldServer.extinguishFire(fake, clickedPos.down(), EnumFacing.UP);
						System.out.println("fire");
						break;
					}
					
					//Don't continue scanning forwards if this block is too solid
					if(!MiscUtil.canBeClickedThrough(worldServer, clickedPos, clickedState)) {
						System.out.println("quitting because solid block");
						break;
					}
					
					//Try to attack an entity
					Entity entityHere = getEntityAtBlock(worldServer, clickedPos);
					if(entityHere != null) {
						fake.attackTargetEntityWithCurrentItem(entityHere);
						System.out.println("Attacking");
						break;
					}
				}
				
			} else {
				//Thanks shadows
				Vec3d start = new Vec3d(fake.posX, fake.posY + fake.getEyeHeight(), fake.posZ).add(fake.getLookVec().scale(.6));
				Vec3d end = start.add(fake.getLookVec().scale(MiscUtil.getReachDistance(fake)));
				RayTraceResult trace = worldServer.rayTraceBlocks(start, end, false, false, true);
				
				AxisAlignedBB aabb = new AxisAlignedBB(pos, pos.offset(facing, (int) MiscUtil.getReachDistance(fake)));
				List<Entity> nearbyEntities = worldServer.getEntitiesWithinAABB(Entity.class, aabb, e -> {
					if(e instanceof EntityItem) return false;
					if(e instanceof EntityXPOrb) return false;
					if(e instanceof EntityArrow) return false;
					if(e.width == 0 || e.height == 0) return false; //can't click that anyways u dip
					return true;
				});
				
				nearbyEntities.sort(Comparator.comparingDouble(ent -> MiscUtil.entityDistanceSq(fake, ent)));
				
				boolean clickedBlock = false;
				if(trace != null && trace.typeOfHit == RayTraceResult.Type.BLOCK) {
					BlockPos hitPos = trace.getBlockPos();
					if(worldServer.getBlockState(hitPos) != Material.AIR) {
						float hitX = (float) trace.hitVec.x - pos.getX();
						float hitY = (float) trace.hitVec.y - pos.getY();
						float hitZ = (float) trace.hitVec.z - pos.getZ();
						
						clickedBlock = fake.interactionManager.processRightClickBlock(fake, worldServer, heldStack, EnumHand.MAIN_HAND, hitPos, facing.getOpposite(), hitX, hitY, hitZ) == EnumActionResult.SUCCESS;
					}
				}
				
				if(!clickedBlock) {
					if(heldStack.isEmpty()) {
						if(trace == null || trace.typeOfHit == RayTraceResult.Type.MISS) {
							ForgeHooks.onEmptyClick(fake, EnumHand.MAIN_HAND);
						}
					} else {
						fake.interactionManager.processRightClick(fake, world, heldStack, EnumHand.MAIN_HAND);
					}
				}
				
				/*
				for(int distance = 1; distance <= reachDistance; distance++) {
					BlockPos clickedPos = pos.offset(facing, distance);
					System.out.println("let's get RIGHT into the CLICK " + clickedPos);
					IBlockState clickedState = worldServer.getBlockState(clickedPos);
					clickedState = clickedState.getActualState(worldServer, clickedPos);
					Block clickedBlock = clickedState.getBlock();
					
					//Try to right click a block
					
					
					if(!shouldSneak) {
						if(clickedBlock.onBlockActivated(worldServer, clickedPos, clickedState, fake, EnumHand.MAIN_HAND, facing.getOpposite(), .5f, .5f, .5f)) {
							System.out.println("onBlockActivated");
							break;
						}
					}
					
					//Don't continue scanning forwards if this block is too solid
					if(!MiscUtil.canBeClickedThrough(worldServer, clickedPos, clickedState)) {
						System.out.println("quitting because solid block");
						break;
					}
					
					//Try to right click on an entity using the held item
					Entity entityHere = getEntityAtBlock(worldServer, clickedPos);
					if(entityHere instanceof EntityLivingBase) {
						EntityLivingBase elb = (EntityLivingBase) entityHere;
						if(heldStack.getItem().itemInteractionForEntity(heldStack, fake, elb, EnumHand.MAIN_HAND)) {
							System.out.println("itemInteractionForEntity");
							break;
						}
						
						if(elb.processInitialInteract(fake, EnumHand.MAIN_HAND)) {
							System.out.println("processInitialInteract");
							break;
						}
					}
					
					//Try to right click against a block
					BlockPos nextClickedPos = pos.offset(facing, distance + 1);
					IBlockState nextClickedState = worldServer.getBlockState(nextClickedPos);
					nextClickedState = nextClickedState.getActualState(worldServer, nextClickedPos);
					
					if(!MiscUtil.canBeClickedThrough(worldServer, nextClickedPos, nextClickedState)) {
						if(heldStack.getItem().onItemUse(fake, worldServer, nextClickedPos, EnumHand.MAIN_HAND, facing.getOpposite(), .5f, .5f, .5f) == EnumActionResult.SUCCESS) {
							System.out.println("onItemUse succ");
							break;
						}
					}
					
					//"general" right click spaghetti sauce mojang methods
					ActionResult<ItemStack> meme = heldStack.getItem().onItemRightClick(worldServer, fake, EnumHand.MAIN_HAND);
					if(meme.getType() == EnumActionResult.SUCCESS) {
						System.out.println("onItemRightClick succ");
						resultStack = meme.getResult();
						break;
					}
				}*/
			}
		} catch(Exception oof) {
			FakeMe.LOGGER.error("There was a problem " + (leftClick ? "left" : "right") + "-clicking", oof);
		}
		
		//Dump the fake player's inventory back into the tile again
		allowCheatyInsertion = true;
		List<ItemStack> leftoverStacks = new ArrayList<>();
		for(int i = 0; i < fake.inventory.getSizeInventory(); i++) {
			ItemStack leftover = ItemHandlerHelper.insertItem(handler, fake.inventory.getStackInSlot(i).copy(), false);
			if(!leftover.isEmpty()) leftoverStacks.add(leftover);
		}
		allowCheatyInsertion = false;
		
		//If there's any leftover default to tossing them on the ground
		for(ItemStack leftover : leftoverStacks) {
			float x = pos.getX() + .5f + (facing.getFrontOffsetX() / 2f);
			float y = pos.getX() + .5f + (facing.getFrontOffsetY() / 2f);
			float z = pos.getX() + .5f + (facing.getFrontOffsetZ() / 2f);
			
			EntityItem ent = new EntityItem(world, x, y, z, leftover);
			ent.motionX = 0; ent.motionY = 0; ent.motionZ = 0;
			worldServer.spawnEntity(ent);
		}
		
		//Clean up
		fake.setSneaking(false);
		fake.getAttributeMap().removeAttributeModifiers(heldStack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND));
		fake.inventory.clear();
	}
	
	public boolean isLeftClick() {
		return leftClick;
	}
	
	public void setLeftClick(boolean leftClick) {
		this.leftClick = leftClick;
		markDirty();
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
