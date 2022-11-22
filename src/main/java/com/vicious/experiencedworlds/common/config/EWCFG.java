package com.vicious.experiencedworlds.common.config;

import com.vicious.experiencedworlds.common.EWEventHandler;
import com.vicious.viciouscore.common.util.file.ViciousDirectories;
import com.vicious.viciouscore.common.util.server.ServerHelper;
import com.vicious.viciouslib.aunotamation.Aunotamation;
import com.vicious.viciouslib.persistence.json.JSONFile;
import com.vicious.viciouslib.persistence.json.JSONMap;
import com.vicious.viciouslib.persistence.storage.PersistentAttribute;
import com.vicious.viciouslib.persistence.storage.aunotamations.Save;

import java.util.function.Supplier;

public class EWCFG extends JSONFile {
    private static final EWCFG instance = new EWCFG();
    public static EWCFG getInstance() {
        return instance;
    }

    public EWCFG() {
        super(((Supplier<String>) () -> {
            if(ViciousDirectories.configDirectory == null) ViciousDirectories.initializeConfigDependents();
            return ViciousDirectories.viciousConfigDirectory.toAbsolutePath() + "/experiencedworlds.cfg";
        }).get());
        activeStats.get().put("minecraft:mined",true);
        activeStats.get().put("minecraft:killed",true);
        Aunotamation.processObject(this);
        advancementMultiplierBase.listen(this::reloadBorder);
        multipliersExponentialGain.listen(this::reloadBorder);
        sizeGained.listen(this::reloadBorder);
        startingSize.listen(this::reloadBorder);
    }
    public void reloadBorder(SpyableAttribute<?> attri){
        if(ServerHelper.server != null) {
            EWEventHandler.growBorder();
        }
    }

    @Save(description = "Enables Chat Announcements")
    public PersistentAttribute<Boolean> chatAnnouncements = new PersistentAttribute<>("ChatAnnouncements",Boolean.class,true);

    @Save(description = "Enables 'player has advanced, increasing...' chat announcements", parent = "ChatAnnouncements")
    public PersistentAttribute<Boolean> advancementAnnouncements = new PersistentAttribute<>("Advancements",Boolean.class,true);

    @Save(description = "Enables 'player has made state reach...' chat announcements", parent = "ChatAnnouncements")
    public PersistentAttribute<Boolean> borderGrowthAnnouncements = new PersistentAttribute<>("BorderGrowth",Boolean.class,true);

    @Save(description = "This config is balanced for vanilla minecraft. Modifying one value may put everything out of balance. (This setting doesn't actually do anything, its just a parent)")
    public PersistentAttribute<String> a = new PersistentAttribute<>("GameplayBalanced",String.class,"I acknowledge that by changing these fields... Wait is this an EULA? What's this doing here!");

    @Save(description = "The multiplier base for advancements.", parent = "GameplayBalanced")
    public SpyableAttribute<Double> advancementMultiplierBase = new SpyableAttribute<>("AdvancementMultiplierBase",Double.class,1.01D);

    @Save(description = "If true the multiplier gained per achievement will be equal to advancementMultiplierBase^numAdvancements, if false: advancementMultiplierBase*numAdvancements", parent = "GameplayBalanced")
    public SpyableAttribute<Boolean> multipliersExponentialGain = new SpyableAttribute<>("MultiplierExponentialGainOn",Boolean.class,true);

    @Save(description = "The amount the world grows each time a stat point reaches the required amount.", parent = "GameplayBalanced")
    public SpyableAttribute<Double> sizeGained = new SpyableAttribute<>("SizeGained",Double.class,1.0);

    //This is disabled for the time being because whatever java's Math.logBase is doing, its not doing it right that's for sure.
    //public ConfigurationValue<Integer> statRequirementBase = add(new ConfigurationValue<>("StatRequirementBase", ()->10,this).description("This value determines how many stat points are required for a border growth using the formula: log<statBase>(totalStatPoints). With base 10, the border will grow when statpoints reaches 1,10,100, with 2, 1,2,4,8,16"));
    @Save(description = "The starting world border size.", parent = "GameplayBalanced")
    public SpyableAttribute<Double> startingSize = new SpyableAttribute<>("StartingBorderSize",Double.class,1.0);

    @Save(description = "The maximum advancement multiplier that can be reached.", parent = "GameplayBalanced")
    public SpyableAttribute<Double> advancementMultiplierMax = new SpyableAttribute<>("AdvancementMultiplierMax",Double.class,50.0);

    @Save(description = "The amount of time in milliseconds the border takes to grow 0.5 blocks on every edge (aka increase width by 1 block)", parent = "GameplayBalanced")
    public PersistentAttribute<Long> borderGrowthSpeed = new PersistentAttribute<>("BorderGrowthSpeed",Long.class,1000L);

    @Save(description = "When one of these stats reaches a power of ten (including 1) the border will grow. These are the only supported options: [minecraft:{mined, crafted, used, broken, picked_up, dropped, killed, killed_by}]. There might be other stats that work but I wouldn't recommend trying them as it is untested.", parent = "GameplayBalanced")
    public SpyableAttribute<JSONMap> activeStats = new SpyableAttribute<>("ActiveStatistics",JSONMap.class,new JSONMap());

    @Save(description = "When enabled, the border will grow when a stat reaches a power of 10, when disabled the border will grow everytime a stat grows by 1. If you change this setting, its effects will not be reversed on worlds where you play with it.", parent = "GameplayBalanced")
    public PersistentAttribute<Boolean> logarithmicStatRequirement = new PersistentAttribute<>("LogarithmicStatRequirement",Boolean.class,true);

    @Save(description = "The maximum amount of time spent finding a viable spawn position. By default the vanilla world border is set to 0,0. Very often this can be in the middle of the ocean, the mod will try to find a valid spawn as close to 0,0 as it can.")
    public PersistentAttribute<Long> fairnessCheckMaximumTime = new PersistentAttribute<>("MaximumFairnessCheckingTime",Long.class,60L);


    public boolean sendAdvancementAnnouncements(){
        return chatAnnouncements.get() && advancementAnnouncements.get();
    }

    public boolean sendBorderGrowthAnnouncements(){
        return chatAnnouncements.get() && borderGrowthAnnouncements.get();
    }
}
