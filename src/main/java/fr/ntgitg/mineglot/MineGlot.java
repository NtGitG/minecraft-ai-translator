package fr.ntgitg.mineglot;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = MineGlot.MOD_ID, name = MineGlot.MOD_NAME, version = MineGlot.VERSION,
        clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class MineGlot {
    public static final String MOD_ID = "mineglot";
    public static final String MOD_NAME = "MineGlot";
    public static final String VERSION = "1.0.0";

    @Mod.Instance(MOD_ID)
    private static MineGlot instance;

    private final MineGlotLifecycle lifecycle = new MineGlotLifecycle();

    public static MineGlot getInstance() {
        if (instance == null) {
            throw new IllegalStateException("MineGlot instance not initialized yet");
        }
        return instance;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        lifecycle.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        lifecycle.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        lifecycle.postInit(event);
    }
}
