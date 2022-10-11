package com.vicious.experiencedworlds.common;

import com.vicious.experiencedworlds.common.data.IExperiencedWorlds;
import com.vicious.experiencedworlds.common.data.SyncableWorldBorder;
import com.vicious.serverstatistics.ServerStatistics;
import com.vicious.serverstatistics.common.event.AdvancedFirstTimeEvent;
import com.vicious.serverstatistics.common.event.StatChangedEvent;
import com.vicious.viciouscore.common.data.implementations.attachable.SyncableGlobalData;
import com.vicious.viciouscore.common.util.server.ServerHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Set;

public class EWEventHandler {
    private static final Set<StatType<?>> validStats = Set.of(Stats.BLOCK_MINED,Stats.ENTITY_KILLED);
    @SubscribeEvent
    public static void onStatChanged(StatChangedEvent sce){
        Stat<?> stat = sce.getStat();
        StatType<?> type = stat.getType();
        if(validStats.contains(type)){
            StatsCounter counter = ServerStatistics.getData().counter.getValue();
            int current = counter.getValue(stat);
            if (current > 0) {
                int borderChange = (int)Math.log10(current + sce.getChange()) - (int)Math.log10(current);
                if (borderChange != 0) {
                    increaseBorder(borderChange, sce);
                }
            } else {
                increaseBorder(Math.max(1, (int) Math.log10(current + sce.getChange())), sce);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event){
        SyncableGlobalData.getInstance().executeAs(IExperiencedWorlds.class, (ew) -> {
            for (ServerLevel l : event.getServer().getAllLevels()) {
                l.getWorldBorder().setSize(ew.getExperiencedWorlds().getTransformedBorderSize());
            }
        });
    }

    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer sp){
            SyncableGlobalData.getInstance().executeAs(IExperiencedWorlds.class,(ew)->{
                if(ew.getExperiencedWorlds().fairnesslevel.getValue() > 1){
                    Minecraft.getInstance().execute(()-> EWChatMessage.from(ChatFormatting.RED,ChatFormatting.BOLD,"<1experiencedworlds.unfairworld>",EWCFG.getInstance().fairnessCheckMaximumTime.value()).send(sp));
                }
            });
        }
    }


    @SubscribeEvent
    public static void increaseMultiplier(AdvancedFirstTimeEvent afte){
        SyncableGlobalData.getInstance().executeAs(IExperiencedWorlds.class,(VCGD)-> {
            SyncableWorldBorder swb = VCGD.getExperiencedWorlds();
            double a2 = Math.round(swb.getCurrentMultiplierGain()*100.0)/100.0;
            EWChatMessage.from("<3experiencedworlds.advancementattained>",afte.getPlayer().getDisplayName(),a2,Math.round(swb.getSizeMultiplier()*100.0)/100.0).send(ServerHelper.getPlayers());
            growBorder(swb);
        });
    }

    private static void increaseBorder(int amount, StatChangedEvent sce){
        SyncableGlobalData.getInstance().executeAs(IExperiencedWorlds.class,(VCGD)->{
            SyncableWorldBorder swb = VCGD.getExperiencedWorlds();
            swb.expand(amount);
            double a2 = Math.round(amount*swb.getSizeMultiplier()*EWCFG.getInstance().sizeGained.value()*100.0)/100.0;
            int current = ServerStatistics.getData().counter.getValue().getValue(sce.getStat());
            if(a2 != 1) {
                EWChatMessage.from("<3experiencedworlds.grewborderplural>", sce.getPlayer().getDisplayName(), current+1,a2).send(ServerHelper.getPlayers());
            }
            else{
                EWChatMessage.from("<2experiencedworlds.grewborder>", sce.getPlayer().getDisplayName(), current+1).send(ServerHelper.getPlayers());
            }
            growBorder(swb);
        });
    }

    private static void growBorder(SyncableWorldBorder swb){
        double newSize = swb.getTransformedBorderSize();
        for (ServerLevel level : ServerHelper.server.getAllLevels()) {
            WorldBorder border = level.getWorldBorder();
            double size = border.getSize();
            border.lerpSizeBetween(size, newSize, (long)Math.ceil(newSize-size) * 1000L + border.getLerpRemainingTime());
        }
    }

    @SubscribeEvent
    public static void onWorldInit(LevelEvent.CreateSpawnPosition event){
        SyncableGlobalData.getInstance().executeAs(IExperiencedWorlds.class,(ew)->{
            SyncableWorldBorder swb = ew.getExperiencedWorlds();
            if(swb.fairnesslevel.getValue() == -1) {
                Minecraft.getInstance().execute(() -> {
                    if (event.getLevel() instanceof ServerLevel sl) {
                        if (ServerHelper.server.overworld().equals(sl)) {
                            WorldBorder border = sl.getWorldBorder();
                            BlockPos fairCenter = FairnessFixer.scanDown(0, 0, sl, (bs) -> bs.getMaterial().isSolid());
                            try {
                                fairCenter = FairnessFixer.getFairPos((int) border.getCenterX(), (int) border.getCenterZ(), sl);
                                border.setCenter(fairCenter.getX(), fairCenter.getZ());
                                swb.fairnesslevel.setValue(1);
                            } catch (FairnessFixer.UnfairnessException e) {
                                swb.fairnesslevel.setValue(0);
                            }
                            for (ServerPlayer player : ServerHelper.server.getPlayerList().getPlayers()) {
                                if(swb.fairnesslevel.getValue() == 0){
                                    EWChatMessage.from(ChatFormatting.RED,ChatFormatting.BOLD,"<1experiencedworlds.unfairworld>", EWCFG.getInstance().fairnessCheckMaximumTime.value()).send(player);
                                }
                                else{
                                    EWChatMessage.from(ChatFormatting.GREEN,ChatFormatting.BOLD,"<experiencedworlds.fairworld>").send(player);
                                }
                                player.teleportTo(sl, fairCenter.getX(), fairCenter.getY() + 1, fairCenter.getZ(), 0, 0);
                            }
                        }
                    }
                });
            }
        });
    }
}