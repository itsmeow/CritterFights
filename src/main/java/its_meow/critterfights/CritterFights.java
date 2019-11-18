package its_meow.critterfights;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod.EventBusSubscriber(modid = CritterFights.MODID)
@Mod(modid = CritterFights.MODID, name = CritterFights.NAME, version = CritterFights.VERSION)
public class CritterFights {
    
    public static final String MODID = "critterfights";
    public static final String NAME = "Critter Fights";
    public static final String VERSION = "@VERSION@";
    
    @EventHandler
    public void serverInit(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandCFight());
        event.registerServerCommand(new CommandCAggro());
    }

}