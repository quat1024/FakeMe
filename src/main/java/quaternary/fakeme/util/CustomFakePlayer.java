package quaternary.fakeme.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IMerchant;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.CommandBlockBaseLogic;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.WeakHashMap;

public class CustomFakePlayer extends FakePlayer {
	public CustomFakePlayer(WorldServer world, GameProfile name) {
		super(world, name);
		
		//Cube boi
		setSize(1f, 1f);
	}
	
	@Override
	public float getEyeHeight() {
		return 0.5f;
	}
	
	//Some methods forge forgot apparently
	//Some of these mayb don't actually crash
	@Override
	public void displayGUIChest(IInventory chestInventory) {
		return;
	}
	
	@Override
	public void displayVillagerTradeGui(IMerchant villager) {
		return;
	}
	
	@Override
	public void displayGuiCommandBlock(TileEntityCommandBlock commandBlock) {
		return;
	}
	
	@Override
	public void displayGuiEditCommandCart(CommandBlockBaseLogic commandBlock) {
		return;
	}
	
	@Override
	public void sendContainerToPlayer(Container containerIn) {
		return;
	}
	
	private static WeakHashMap<WorldServer, WeakReference<CustomFakePlayer>> instances = new WeakHashMap<>();
	private static final GameProfile FAKE_PROFILE = new GameProfile(UUID.fromString("cc8092e8-7d7c-49ac-aeb2-e0d2e906e045"), "[FakeMe Fake Player]");
	
	public static CustomFakePlayer get(WorldServer world) {
		WeakReference<CustomFakePlayer> playerRef = instances.computeIfAbsent(world, CustomFakePlayer::createFakePlayerRef);
		
		if(playerRef.get() == null) {
			playerRef = createFakePlayerRef(world);
			instances.put(world, playerRef);
		}
		
		return playerRef.get();
	}
	
	private static WeakReference<CustomFakePlayer> createFakePlayerRef(WorldServer ws) {
		return new WeakReference<>(new CustomFakePlayer(ws, FAKE_PROFILE));
	}
}
