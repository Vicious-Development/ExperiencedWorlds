package com.vicious.experiencedworlds.common;

import com.vicious.experiencedworlds.common.data.IExperiencedWorlds;
import com.vicious.experiencedworlds.common.data.SyncableWorldBorder;
import com.vicious.serverstatistics.ServerStatistics;
import com.vicious.serverstatistics.common.event.AdvancedFirstTimeEvent;
import com.vicious.serverstatistics.common.event.StatChangedEvent;
import com.vicious.viciouscore.common.data.implementations.attachable.SyncableGlobalData;
import com.vicious.viciouscore.common.util.server.ServerHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.event.entity.player.PlayerEvent;
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
            //int base = EWCFG.getInstance().statRequirementBase.value();
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
    public static void onAdvancementEarned(AdvancedFirstTimeEvent afte){
        if (EWCFG.getInstance().multipliersExponentialGain.getBoolean()) {
            increaseMultiplier(Math.pow(1.0+EWCFG.getInstance().advancementMultiplierBase.value(), ServerStatistics.getData().advancers.size())-1.0, afte);
        } else {
            increaseMultiplier(EWCFG.getInstance().advancementMultiplierBase.value() * ServerStatistics.getData().advancers.size(), afte);
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
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if(event.getEntity() instanceof ServerPlayer sp){
            relocate(sp);
        }
    }

    private static void relocate(ServerPlayer plr){
        ServerLevel level = plr.getLevel();
        WorldBorder border = level.getWorldBorder();
        if(!border.isWithinBounds(plr.position().x,plr.position().y,plr.position().z)){
            int airCount = 0;
            BlockPos start = new BlockPos(border.getCenterX(),255,border.getCenterZ());
            BlockPos pos = new BlockPos(start);
            while(pos.getY() > 63){
                BlockState state = level.getBlockState(pos);
                Material material = state.getMaterial();
                if(!material.blocksMotion()){
                    airCount++;
                }
                else if(airCount > 1){
                    break;
                }
                else{
                    airCount = 0;
                }
            }
            plr.setPos(pos.getX(),pos.getY()+1,pos.getZ());
        }
    }


    private static void increaseMultiplier(double amount, AdvancedFirstTimeEvent afte){
        SyncableGlobalData.getInstance().executeAs(IExperiencedWorlds.class,(VCGD)-> {
            SyncableWorldBorder swb = VCGD.getExperiencedWorlds();
            swb.setSizeMultiplier(swb.getSizeMultiplier()+amount);
            double a2 = Math.round(amount*100.0)/100.0;
            EWChatMessage.from("<3experiencedworlds.advancementattained>",afte.getPlayer().getDisplayName(),a2,Math.round(swb.getSizeMultiplier()*100.0)/100.0).send(ServerHelper.getPlayers());
            growBorder(swb);
        });
    }

    private static void increaseBorder(int amount, StatChangedEvent sce){
        SyncableGlobalData.getInstance().executeAs(IExperiencedWorlds.class,(VCGD)->{
            SyncableWorldBorder swb = VCGD.getExperiencedWorlds();
            swb.expand(amount);
            double a2 = Math.round(amount*(1+swb.getSizeMultiplier())*EWCFG.getInstance().sizeGained.value()*100.0)/100.0;
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
}