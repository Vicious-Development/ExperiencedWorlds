package com.vicious.experiencedworlds.common;

import com.vicious.experiencedworlds.ExperiencedWorlds;
import com.vicious.experiencedworlds.common.data.EWWorldData;
import com.vicious.experiencedworlds.common.data.IExperiencedWorlds;
import com.vicious.experiencedworlds.common.data.IWorldSpecificEWDat;
import com.vicious.experiencedworlds.common.data.SyncableWorldBorder;
import com.vicious.serverstatistics.ServerStatistics;
import com.vicious.serverstatistics.common.event.AdvancedFirstTimeEvent;
import com.vicious.serverstatistics.common.event.StatChangedEvent;
import com.vicious.viciouscore.common.capability.VCCapabilities;
import com.vicious.viciouscore.common.data.implementations.attachable.SyncableGlobalData;
import com.vicious.viciouscore.common.util.FuckLazyOptionals;
import com.vicious.viciouscore.common.util.server.ServerHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
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
        for (ServerLevel l : event.getServer().getAllLevels()) {
            if(FuckLazyOptionals.getOrNull(l.getCapability(VCCapabilities.LEVELDATA)) instanceof IWorldSpecificEWDat ew) {
                EWWorldData dat = ew.getExperiencedWorlds();
                double newSize = ExperiencedWorlds.getBorder().getTransformedBorderSize() * Math.max(1, dat.multiplier.getValue()) + dat.startingSize.getValue();
                l.getWorldBorder().setSize(newSize);
            }
        }
    }

    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer sp){
            if(ExperiencedWorlds.getBorder().fairnesslevel.getValue() > 1) {
                if (sp.getServer() != null) {
                    sp.getServer().execute(() -> EWChatMessage.from(ChatFormatting.RED, ChatFormatting.BOLD, "<1experiencedworlds.unfairworld>", EWCFG.getInstance().fairnessCheckMaximumTime.value()).send(sp));
                }
            }
            else if(ExperiencedWorlds.getBorder().fairnesslevel.getValue() == -1){
                EWChatMessage.from(ChatFormatting.GREEN, "<experiencedworlds.searchingforsafety>").send(sp);
                sp.setGameMode(GameType.ADVENTURE);
            }
        }
    }


    @SubscribeEvent
    public static void increaseMultiplier(AdvancedFirstTimeEvent afte){
        SyncableWorldBorder swb = ExperiencedWorlds.getBorder();
        double a2 = Math.round(swb.getCurrentMultiplierGain()*100.0)/100.0;
        EWChatMessage.from("<3experiencedworlds.advancementattained>",afte.getPlayer().getDisplayName(),a2,Math.round(swb.getSizeMultiplier()*100.0)/100.0).send(ServerHelper.getPlayers());
        growBorder(swb);
    }

    private static void increaseBorder(int amount, StatChangedEvent sce){
        SyncableWorldBorder swb = ExperiencedWorlds.getBorder();
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
    }

    public static void growBorder(){
        SyncableGlobalData.getInstance().executeAs(IExperiencedWorlds.class,(c)->{
            growBorder(c.getExperiencedWorlds());
        });
    }
    private static void growBorder(SyncableWorldBorder swb){
        for (ServerLevel level : ServerHelper.server.getAllLevels()) {
            if(FuckLazyOptionals.getOrNull(level.getCapability(VCCapabilities.LEVELDATA)) instanceof IWorldSpecificEWDat ew) {
                EWWorldData dat = ew.getExperiencedWorlds();
                double newSize = swb.getTransformedBorderSize()*Math.max(1,dat.multiplier.getValue())+dat.startingSize.getValue();
                WorldBorder border = level.getWorldBorder();
                double size = border.getSize();
                if(size <= newSize) {
                    border.lerpSizeBetween(size, newSize, (long) Math.ceil(Math.abs(newSize - size)) * 1000L + border.getLerpRemainingTime());
                }
                else{
                    border.lerpSizeBetween(newSize, size, (long) Math.ceil(Math.abs(newSize - size)) * 1000L + border.getLerpRemainingTime());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onWorldInit(LevelEvent.CreateSpawnPosition event){
        SyncableWorldBorder swb = ExperiencedWorlds.getBorder();
        if(event.getLevel() instanceof ServerLevel sl){
            if(swb.fairnesslevel.getValue() == -1) {
                pauseWorld(sl);
                ServerExecutor.execute(() -> {
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
                            if(player.gameMode.isSurvival()){
                                player.setGameMode(GameType.SURVIVAL);
                            }
                            if (swb.fairnesslevel.getValue() == 0) {
                                EWChatMessage.from(ChatFormatting.RED, ChatFormatting.BOLD, "<1experiencedworlds.unfairworld>", EWCFG.getInstance().fairnessCheckMaximumTime.value()).send(player);
                            } else {
                                EWChatMessage.from(ChatFormatting.GREEN, ChatFormatting.BOLD, "<experiencedworlds.fairworld>").send(player);
                            }
                            player.teleportTo(sl, fairCenter.getX(), fairCenter.getY() + 1, fairCenter.getZ(), 0, 0);
                        }
                        pauseWorld(sl);
                    }
                });
            }
        }
    }

    private static Difficulty difficulty;
    private static void pauseWorld(ServerLevel sl){
        if(difficulty == null){
            difficulty = sl.getDifficulty();
        }
        else{
            sl.getServer().setDifficulty(difficulty,true);
        }
    }
}