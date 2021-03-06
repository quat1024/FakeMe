package quaternary.fakeme.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import quaternary.fakeme.FakeMe;
import quaternary.fakeme.tile.TileClicker;
import quaternary.fakeme.ui.GuiHandler;

import javax.annotation.Nullable;

public class BlockClicker extends Block {
	public BlockClicker() {
		super(Material.ROCK, MapColor.GRAY);
		
		setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.UP).withProperty(POWERED, false));
	}
	
	public static final PropertyEnum<EnumFacing> FACING = BlockDirectional.FACING;
	public static final PropertyBool POWERED = PropertyBool.create("powered");
	
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if(!world.isRemote && !player.isSneaking()) {
			player.openGui(FakeMe.INSTANCE, GuiHandler.CLICKER_UI, world, pos.getX(), pos.getY(), pos.getZ());
		}
		
		return true;
	}
	
	@Override
	public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
		boolean isPowered = state.getValue(POWERED);
		boolean shouldPower = world.isBlockPowered(pos);
		int strength = world.isBlockIndirectlyGettingPowered(pos);
		
		if(shouldPower && !isPowered) {
			world.setBlockState(pos, state.withProperty(POWERED, true));
			TileEntity tile = world.getTileEntity(pos);
			if(tile instanceof TileClicker) {
				((TileClicker)tile).doClick(strength <= 5);
			}
		} else if(isPowered && !shouldPower) {
			world.setBlockState(pos, state.withProperty(POWERED, false));
		}
	}
	
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, FACING, POWERED);
	}
	
	@Override
	public IBlockState getStateFromMeta(int meta) {
		EnumFacing facing = EnumFacing.getFront(meta % 6);
		boolean powered = meta > 6;
		return getDefaultState().withProperty(FACING, facing).withProperty(POWERED, powered);
	}
	
	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(FACING).ordinal() + (state.getValue(POWERED) ? 6 : 0);
	}
	
	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
		return getDefaultState().withProperty(FACING, EnumFacing.getDirectionFromEntityLiving(pos, placer));
	}
	
	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}
	
	@Nullable
	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new TileClicker();
	}
}
