package com.vicious.experiencedworlds.common;

import com.vicious.experiencedworlds.ExperiencedWorlds;
import com.vicious.experiencedworlds.common.config.EWCFG;
import com.vicious.experiencedworlds.common.data.EWWorldData;
import com.vicious.experiencedworlds.common.data.IExperiencedWorlds;
import com.vicious.experiencedworlds.common.data.IWorldSpecificEWDat;
import com.vicious.experiencedworlds.common.data.SyncableWorldBorder;
import com.vicious.serverstatistics.ServerStatistics;
import com.vicious.serverstatistics.common.event.StatChangedEvent;
import com.vicious.viciouscore.common.capability.VCCapabilities;
import com.vicious.viciouscore.common.data.GlobalData;
import com.vicious.viciouscore.common.util.FuckLazyOptionals;
import com.vicious.viciouscore.common.util.server.ServerHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.border.WorldBorder;

public class BorderManager {
    private static long lastExpand = System.currentTimeMillis();
    static Difficulty difficulty;
    static boolean checking = false;

    static void growBorder(SyncableWorldBorder swb){
        boolean doFastExpand = lastExpand+50 > System.currentTimeMillis();
        for (ServerLevel level : ServerHelper.server.getAllLevels()) {
            if(FuckLazyOptionals.getOrNull(level.getCapability(VCCapabilities.LEVELDATA)) instanceof IWorldSpecificEWDat ew) {
                EWWorldData dat = ew.getExperiencedWorlds();
                double newSize = swb.getTransformedBorderSize()*Math.max(1,dat.multiplier.getValue())+dat.startingSize.getValue();
                WorldBorder border = level.getWorldBorder();
                double size = border.getSize();
                long change = (long) Math.ceil(Math.abs(newSize - size));
                if(size <= newSize) {
                    border.lerpSizeBetween(size, newSize,  change * (!doFastExpand ? EWCFG.getInstance().borderGrowthSpeed.get() : 1L) + border.getLerpRemainingTime());
                }
                else{
                    border.lerpSizeBetween(size, newSize, change * (!doFastExpand ? EWCFG.getInstance().borderGrowthSpeed.get() : 1L) + border.getLerpRemainingTime());
                }
            }
        }
        lastExpand = System.currentTimeMillis();
    }

    public static void growBorder(){
        GlobalData.getGlobalData().executeAs(IExperiencedWorlds.class,(c)-> growBorder(c.getExperiencedWorlds()));
    }

    static void pauseWorld(ServerLevel sl){
        if(difficulty == null){
            difficulty = sl.getDifficulty();
        }
        else{
            sl.getServer().setDifficulty(difficulty,true);
            difficulty = null;
        }
    }

    static void relocateToSafeLocation(ServerPlayer player){
        player.getServer().execute(() -> {
            ServerLevel sl = player.getLevel();
            WorldBorder border = sl.getWorldBorder();
            BlockPos fairCenter = FairnessFixer.scanDown((int) border.getCenterX(), (int) border.getCenterZ(), sl, (bs) -> bs.getMaterial().isSolid());
            player.teleportTo(sl, fairCenter.getX(), fairCenter.getY() + 1, fairCenter.getZ(), 0, 0);
        });
    }

    static void increaseBorder(int amount, StatChangedEvent sce){
        SyncableWorldBorder swb = ExperiencedWorlds.getBorder();
        swb.expand(amount);
        double a2 = Math.round(amount*swb.getSizeMultiplier()*EWCFG.getInstance().sizeGained.value()*100.0)/100.0;
        int current = ServerStatistics.getData().counter.getValue().getValue(sce.getStat());
        if(a2 != 1) {
            if(EWCFG.getInstance().sendBorderGrowthAnnouncements() && !swb.maximumBorderSize()){
                EWChatMessage.from("<3experiencedworlds.grewborderplural>", sce.getPlayer().getDisplayName(), current+1,a2).send(ServerHelper.getPlayers());
            }
        }
        else{
            if(EWCFG.getInstance().sendBorderGrowthAnnouncements() && !swb.maximumBorderSize()){
                EWChatMessage.from("<2experiencedworlds.grewborder>", sce.getPlayer().getDisplayName(), current+1).send(ServerHelper.getPlayers());
            }
        }
        growBorder(swb);
    }

    public static void handleStatisticsReset(ServerPlayer sp) {
        ExperiencedWorlds.getBorder().fairnesslevel.setValue(1);
        BorderManager.relocateToSafeLocation(sp);
    }

    public static void fixBorder(ServerLevel sl, SyncableWorldBorder swb){
        checking = true;
        pauseWorld(sl);
        ServerExecutor.execute(() -> {
            WorldBorder border = sl.getWorldBorder();
            BlockPos fairCenter = FairnessFixer.scanDown(0, 0, sl, (bs) -> bs.getMaterial().isSolid());
            try {
                fairCenter = FairnessFixer.getFairPos((int) border.getCenterX(), (int) border.getCenterZ(), sl);
                border.setCenter(fairCenter.getX(), fairCenter.getZ());
                swb.fairnesslevel.setValue(1);
            } catch (FairnessFixer.UnfairnessException e) {
                swb.fairnesslevel.setValue(0);
            }
            checking = false;
            for (ServerPlayer player : ServerHelper.server.getPlayerList().getPlayers()) {
                if (player.gameMode.isSurvival()) {
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
        });
    }

    public static void checkNeedsFixing(ServerLevel sl, SyncableWorldBorder swb){
        if (swb.fairnesslevel.getValue() == -1) {
            if (!checking) {
                if (sl.getServer().overworld() == sl) {
                    fixBorder(sl,swb);
                }
            }
        }
    }
}
