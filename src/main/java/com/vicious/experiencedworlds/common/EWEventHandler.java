package com.vicious.experiencedworlds.common;

import com.vicious.experiencedworlds.ExperiencedWorlds;
import com.vicious.experiencedworlds.common.config.EWCFG;
import com.vicious.experiencedworlds.common.config.SpyableAttribute;
import com.vicious.experiencedworlds.common.data.EWWorldData;
import com.vicious.experiencedworlds.common.data.IWorldSpecificEWDat;
import com.vicious.experiencedworlds.common.data.SyncableWorldBorder;
import com.vicious.experiencedworlds.common.math.EWMath;
import com.vicious.serverstatistics.ServerStatistics;
import com.vicious.serverstatistics.common.event.AdvancedFirstTimeEvent;
import com.vicious.serverstatistics.common.event.ServerStatsResetEvent;
import com.vicious.serverstatistics.common.event.StatChangedEvent;
import com.vicious.viciouscore.common.capability.VCCapabilities;
import com.vicious.viciouscore.common.util.FuckLazyOptionals;
import com.vicious.viciouscore.common.util.server.ServerHelper;
import com.vicious.viciouslib.persistence.json.JSONMap;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.HashSet;
import java.util.Set;

public class EWEventHandler {
    private static final Set<StatType<?>> validStats = new HashSet<>();
    @SubscribeEvent
    public static void onStatChanged(StatChangedEvent sce){
        Stat<?> stat = sce.getStat();
        StatType<?> type = stat.getType();
        if(validStats.contains(type)){
            StatsCounter counter = ServerStatistics.getData().counter.getValue();
            int current = counter.getValue(stat);
            if (current > 0) {
                int borderChange = EWCFG.getInstance().logarithmicStatRequirement.get() ? (int) EWMath.logConfigBase(current + sce.getChange()) - (int)EWMath.logConfigBase(current) : current+sce.getChange() - current;
                if (borderChange != 0) {
                    BorderManager.increaseBorder(borderChange, sce);
                }
            } else if(EWCFG.getInstance().awardOne.get()) {
                BorderManager.increaseBorder(1, sce);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event){
        for (ServerLevel l : event.getServer().getAllLevels()) {
            if(FuckLazyOptionals.getOrNull(l.getCapability(VCCapabilities.LEVELDATA)) instanceof IWorldSpecificEWDat ew) {
                EWWorldData dat = ew.getExperiencedWorlds();
                double newSize = ExperiencedWorlds.getBorder().getTransformedBorderSize() * Math.max(1, dat.multiplier.getValue()) + dat.startingSize.getValue();
                l.getWorldBorder().setSize(newSize);
            }
        }
        updateValidStatTypes(EWCFG.getInstance().activeStats);
        EWCFG.getInstance().activeStats.listen(EWEventHandler::updateValidStatTypes);
    }

    private static void updateValidStatTypes(SpyableAttribute<JSONMap> map){
        validStats.clear();
        IForgeRegistry<StatType<?>> statsReg = ForgeRegistries.STAT_TYPES;
        map.get().forEach((k,v)->{
            if(v.softAs(Boolean.class)) {
                validStats.add(statsReg.getValue(ResourceLocation.tryParse(k)));
            }
        });
    }

    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer sp) {
            if (hasReset) {
                BorderManager.handleStatisticsReset(sp);
            }
            SyncableWorldBorder swb = ExperiencedWorlds.getBorder();
            ServerLevel sl = sp.getLevel();
            BorderManager.checkNeedsFixing(sl,swb);
            if (BorderManager.checking) {
                EWChatMessage.from(ChatFormatting.GREEN, ChatFormatting.BOLD, "<experiencedworlds.searchingforsafety>").send(sp);
                sp.setGameMode(GameType.ADVENTURE);
            }
        }
    }

    public static boolean hasReset = false;

    @SubscribeEvent
    public static void serverStatsReset(ServerStatsResetEvent reset){
        ExperiencedWorlds.getBorder().reset();
        hasReset = true;
    }


    @SubscribeEvent
    public static void increaseMultiplier(AdvancedFirstTimeEvent afte){
        SyncableWorldBorder swb = ExperiencedWorlds.getBorder();
        boolean announce = EWCFG.getInstance().sendAdvancementAnnouncements() && !swb.maximumMultiplier();
        double a2 = Math.round(swb.getCurrentMultiplierGain()*100.0)/100.0;
        if(announce){
            EWChatMessage.from("<3experiencedworlds.advancementattained>",afte.getPlayer().getDisplayName(),a2,Math.round(swb.getSizeMultiplier()*100.0)/100.0).send(ServerHelper.getPlayers());
        }
        BorderManager.growBorder(swb);
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event){
        if(BorderManager.difficulty != null){
            event.setCanceled(true);
        }
    }

}