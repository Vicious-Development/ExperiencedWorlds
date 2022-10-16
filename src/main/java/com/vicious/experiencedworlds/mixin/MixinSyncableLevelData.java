package com.vicious.experiencedworlds.mixin;

import com.vicious.experiencedworlds.common.data.EWWorldData;
import com.vicious.experiencedworlds.common.data.IWorldSpecificEWDat;
import com.vicious.viciouscore.aunotamation.isyncablecompoundholder.annotation.Obscured;
import com.vicious.viciouscore.common.data.implementations.attachable.SyncableLevelData;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SyncableLevelData.class)
public class MixinSyncableLevelData implements IWorldSpecificEWDat {
    @Obscured
    public EWWorldData experiencedWorldData = new EWWorldData("ewworlddata");

    @Override
    public EWWorldData getExperiencedWorlds() {
        return experiencedWorldData;
    }
}
