package com.vicious.experiencedworlds.common.data;

import com.vicious.viciouscore.aunotamation.isyncablecompoundholder.annotation.Obscured;
import com.vicious.viciouscore.common.data.structures.SyncableCompound;
import com.vicious.viciouscore.common.data.structures.SyncablePrimitive;

public class EWWorldData extends SyncableCompound {
    @Obscured
    public SyncablePrimitive<Double> multiplier = new SyncablePrimitive<>("multiplier",0.0);
    @Obscured
    public SyncablePrimitive<Double> startingSize = new SyncablePrimitive<>("starting",0.0);

    public EWWorldData(String key) {
        super(key);
    }

}