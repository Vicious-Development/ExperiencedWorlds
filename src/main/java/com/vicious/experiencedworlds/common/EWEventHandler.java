package com.vicious.experiencedworlds.common;

import com.vicious.experiencedworlds.ExperiencedWorlds;
import com.vicious.experiencedworlds.common.config.EWCFG;
import com.vicious.experiencedworlds.common.config.SpyableAttribute;
import com.vicious.experiencedworlds.common.data.EWWorldData;
import com.vicious.experiencedworlds.common.data.IExperiencedWorlds;
import com.vicious.experiencedworlds.common.data.IWorldSpecificEWDat;
import com.vicious.experiencedworlds.common.data.SyncableWorldBorder;
import com.vicious.serverstatistics.ServerStatistics;
import com.vicious.serverstatistics.common.event.AdvancedFirstTimeEvent;
import com.vicious.serverstatistics.common.event.ServerStatsResetEvent;
import com.vicious.serverstatistics.common.event.StatChangedEvent;
import com.vicious.viciouscore.common.capability.VCCapabilities;
import com.vicious.viciouscore.common.data.GlobalData;
import com.vicious.viciouscore.common.util.FuckLazyOptionals;
import com.vicious.viciouscore.common.util.server.ServerHelper;
import com.vicious.viciouslib.persistence.json.JSONMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.border.WorldBorder;
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
                int borderChange = EWCFG.getInstance().logarithmicStatRequirement.get() ? (int)EWMath.logConfigBase(current + sce.getChange()) - (int)EWMath.logConfigBase(current) : current+sce.getChange() - current;
                if (borderChange != 0) {
                    increaseBorder(borderChange, sce);
                }
            } else if(EWCFG.getInstance().awardOne.get()) {
                increaseBorder(1, sce);
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

    private static boolean checking = false;

    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer sp) {
            if (hasReset) {
                ExperiencedWorlds.getBorder().fairnesslevel.setValue(1);
                sp.getServer().execute(() -> {
                    ServerLevel sl = sp.getLevel();
                    WorldBorder border = sl.getWorldBorder();
                    BlockPos fairCenter = FairnessFixer.scanDown((int) border.getCenterX(), (int) border.getCenterZ(), sl, (bs) -> bs.getMaterial().isSolid());
                    sp.teleportTo(sl, fairCenter.getX(), fairCenter.getY() + 1, fairCenter.getZ(), 0, 0);
                });
            }
            SyncableWorldBorder swb = ExperiencedWorlds.getBorder();
            ServerLevel sl = sp.getLevel();
            if (swb.fairnesslevel.getValue() == -1) {
                if (!checking) {
                    if (sl.getServer().overworld() == sl) {
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
                }
            }
            if (checking) {
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
        growBorder(swb);
    }

    private static void increaseBorder(int amount, StatChangedEvent sce){
        SyncableWorldBorder swb = ExperiencedWorlds.getBorder();
        swb.expand(amount);
        double a2 = Math.round(amount*swb.getSizeMultiplier()*EWCFG.getInstance().sizeGained.value()*100.0)/100.0;
        int current = ServerStatistics.getData().counter.getValue().getValue(sce.getStat());
        if(a2 != 1) {
            if(EWCFG.getInstance().sendBorderGrowthAnnouncements()) EWChatMessage.from("<3experiencedworlds.grewborderplural>", sce.getPlayer().getDisplayName(), current+1,a2).send(ServerHelper.getPlayers());
        }
        else{
            if(EWCFG.getInstance().sendBorderGrowthAnnouncements()) EWChatMessage.from("<2experiencedworlds.grewborder>", sce.getPlayer().getDisplayName(), current+1).send(ServerHelper.getPlayers());
        }
        growBorder(swb);
    }

    public static void growBorder(){
        GlobalData.getGlobalData().executeAs(IExperiencedWorlds.class,(c)-> growBorder(c.getExperiencedWorlds()));
    }
    private static long lastExpand = System.currentTimeMillis();
    private static void growBorder(SyncableWorldBorder swb){
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
                    border.lerpSizeBetween(newSize, size, change * (!doFastExpand ? EWCFG.getInstance().borderGrowthSpeed.get() : 1L) + border.getLerpRemainingTime());
                }
            }
        }
        lastExpand = System.currentTimeMillis();
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event){
        if(difficulty != null){
            event.setCanceled(true);
        }
    }

    private static Difficulty difficulty;
    private static void pauseWorld(ServerLevel sl){
        if(difficulty == null){
            difficulty = sl.getDifficulty();
        }
        else{
            sl.getServer().setDifficulty(difficulty,true);
            difficulty = null;
        }
    }
}