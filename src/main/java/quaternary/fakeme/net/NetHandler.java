package quaternary.fakeme.net;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import quaternary.fakeme.FakeMe;

public class NetHandler {
	
	private static SimpleNetworkWrapper NET;
	
	public static void init() {
		NET = new SimpleNetworkWrapper(FakeMe.MODID);
		
		int id = 0;
		
		NET.registerMessage(ServerboundUpdateClickerMouseButton.Handler.class, ServerboundUpdateClickerMouseButton.class, id++, Side.SERVER);
	}
	
	public static void sendToServer(IMessage message) {
		NET.sendToServer(message);
	}
}
