package quaternary.fakeme.util;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

public class MiscUtil {
	private MiscUtil() {}
	
	private static final Vec3d VEC1 = new Vec3d(.5, .49, .5);
	private static final Vec3d VEC2 = new Vec3d(.5, .51, .5);
	
	public static boolean canBeClickedThrough(World world, BlockPos pos, IBlockState state) {
		if(state.getMaterial().isLiquid() || state.getMaterial() == Material.AIR) return true;
		if(state.getCollisionBoundingBox(world, pos) == Block.NULL_AABB) return true;
		AxisAlignedBB selectionBox = state.getBoundingBox(world, pos);
		return !(selectionBox.contains(VEC1) && selectionBox.contains(VEC2));
	}
	
	public static double getReachDistance(EntityPlayer player) {
		double reachDistance = 5d;
		IAttributeInstance reachAttribute = player.getAttributeMap().getAttributeInstance(EntityPlayer.REACH_DISTANCE);
		if(reachAttribute != null) {
			reachDistance = reachAttribute.getAttributeValue();
		}
		
		return reachDistance;
	}
	
	public static double entityDistanceSq(Entity a, Entity b) {
		return (a.posX - b.posX) * (a.posX - b.posX) + (a.posY - b.posY) * (a.posY - b.posY) + (a.posZ - b.posZ) * (a.posZ - b.posZ);
	}
}
