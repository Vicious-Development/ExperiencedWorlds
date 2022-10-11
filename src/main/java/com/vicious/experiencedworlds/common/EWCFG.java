package com.vicious.experiencedworlds.common;

import com.vicious.viciouscore.common.util.file.ViciousDirectories;
import com.vicious.viciouslib.configuration.ConfigurationValue;
import com.vicious.viciouslib.configuration.JSONConfig;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

public class EWCFG extends JSONConfig {
    private static EWCFG instance;

    public static EWCFG getInstance() {
        if(instance == null) instance = new EWCFG();
        return instance;
    }

    public EWCFG() {
        super(((Supplier<Path>) () -> {
            if(ViciousDirectories.configDirectory == null) ViciousDirectories.initializeConfigDependents();
            return Paths.get(ViciousDirectories.viciousConfigDirectory.toAbsolutePath() + "/experiencedworlds.json");
        }).get());
    }
    public ConfigurationValue<Double> advancementMultiplierBase = add(new ConfigurationValue<>("AdvancementMultiplierBase", ()->1.01,this).description("The multiplier base for advancements"));
    public ConfigurationValue<Boolean> multipliersExponentialGain = add(new ConfigurationValue<>("MultiplierExponentialGainOn", ()->true,this).description("If true the multiplier gained per achievement will be equal to advancementMultiplierBase^numAdvancements, if false: advancementMultiplierBase*numAdvancements"));
    public ConfigurationValue<Double> sizeGained = add(new ConfigurationValue<>("SizeGained", ()->1.0,this).description("The amount the world grows each time a stat point reaches the required amount."));
    //public ConfigurationValue<Integer> statRequirementBase = add(new ConfigurationValue<>("StatRequirementBase", ()->10,this).description("This value determines how many stat points are required for a border growth using the formula: log<statBase>(totalStatPoints). With base 10, the border will grow when statpoints reaches 1,10,100, with 2, 1,2,4,8,16"));
    public ConfigurationValue<Integer> startingSize = add(new ConfigurationValue<>("StartingBorderSize",()->1,this).description("The starting world border size."));
    public ConfigurationValue<Long> fairnessCheckMaximumTime = add(new ConfigurationValue<>("MaximumFairnessCheckingTime",()->60L,this).description("The maximum amount of time spent finding a viable spawn position."));
}
