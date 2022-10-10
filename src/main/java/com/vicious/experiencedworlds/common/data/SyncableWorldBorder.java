package com.vicious.experiencedworlds.common.data;

import com.vicious.experiencedworlds.common.EWCFG;
import com.vicious.viciouscore.aunotamation.isyncablecompoundholder.annotation.Obscured;
import com.vicious.viciouscore.common.data.structures.SyncableCompound;
import com.vicious.viciouscore.common.data.structures.SyncablePrimitive;

public class SyncableWorldBorder extends SyncableCompound implements IWorldBorderData {
    @Obscured
    public SyncablePrimitive<Double> multiplier = new SyncablePrimitive<>("multiplier",0.0);
    @Obscured
    public SyncablePrimitive<Integer> expansions = new SyncablePrimitive<>("expansions",0);

    public SyncableWorldBorder(String key) {
        super(key);
    }

    @Override
    public double getExpansions() {
        return expansions.getValue();
    }

    @Override
    public void expand(int expansions) {
        this.expansions.setValue(expansions+this.expansions.getValue());
    }

    @Override
    public double getSizeMultiplier() {
        return multiplier.getValue();
    }

    @Override
    public void setSizeMultiplier(double multiplier) {
        this.multiplier.setValue(multiplier);
    }

    @Override
    public double getTransformedBorderSize() {
        return EWCFG.getInstance().startingSize.value()+expansions.getValue()* EWCFG.getInstance().sizeGained.value()*(1.0+multiplier.getValue());
    }
}
