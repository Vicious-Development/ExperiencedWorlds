package com.vicious.experiencedworlds;

import com.mojang.logging.LogUtils;
import com.vicious.experiencedworlds.common.EWCFG;
import com.vicious.experiencedworlds.common.EWCommands;
import com.vicious.experiencedworlds.common.EWEventHandler;
import com.vicious.experiencedworlds.common.data.IExperiencedWorlds;
import com.vicious.experiencedworlds.common.data.SyncableWorldBorder;
import com.vicious.viciouscore.common.data.GlobalData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ExperiencedWorlds.MODID)
public class ExperiencedWorlds {

    public static final String MODID = "experiencedworlds";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ExperiencedWorlds() {
        EWCFG.getInstance();
        MinecraftForge.EVENT_BUS.register(EWEventHandler.class);
        MinecraftForge.EVENT_BUS.register(EWCommands.class);
    }
    public static SyncableWorldBorder getBorder(){
        IExperiencedWorlds iew = (IExperiencedWorlds) GlobalData.getGlobalData();
        return iew.getExperiencedWorlds();
    }
}
