package com.vicious.experiencedworlds.mixin;

import com.vicious.experiencedworlds.common.data.IExperiencedWorlds;
import com.vicious.experiencedworlds.common.data.SyncableWorldBorder;
import com.vicious.viciouscore.aunotamation.isyncablecompoundholder.annotation.Obscured;
import com.vicious.viciouscore.common.data.implementations.attachable.SyncableGlobalData;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SyncableGlobalData.class)
public class MixinSyncableGlobalData implements IExperiencedWorlds {
    @Obscured
    public SyncableWorldBorder experiencedworlds = new SyncableWorldBorder("experiencedworlds");

    @Override
    public SyncableWorldBorder getExperiencedWorlds() {
        return experiencedworlds;
    }
}
